package com.travelplus.managers;

import com.travelplus.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class LocationManager {

    private final Main plugin;
    private File locationsFile;
    private FileConfiguration locationsConfig;

    public LocationManager(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[DEBUG] Initializing LocationManager..."); // Added
        setupLocationsFile();
        loadLocations();
        plugin.getLogger().info("[DEBUG] LocationManager initialized."); // Added
    }

    private void setupLocationsFile() {
        plugin.getLogger().info("[DEBUG] Setting up locations file..."); // Added
        locationsFile = new File(plugin.getDataFolder(), "coords.yml"); // Changed filename
        plugin.getLogger().info("[DEBUG] Location file path: " + locationsFile.getAbsolutePath()); // Added
        if (!locationsFile.exists()) {
            plugin.getLogger().info("[DEBUG] coords.yml does not exist. Attempting to create..."); // Added
            try {
                // Ensure parent directory exists
                File parentDir = locationsFile.getParentFile();
                if (!parentDir.exists()) {
                     plugin.getLogger().info("[DEBUG] Plugin data folder does not exist. Attempting to create: " + parentDir.getAbsolutePath()); // Added
                     if (parentDir.mkdirs()) {
                         plugin.getLogger().info("[DEBUG] Created plugin data folder."); // Added
                     } else {
                         plugin.getLogger().warning("[DEBUG] Failed to create plugin data folder!"); // Added
                     }
                } else {
                     plugin.getLogger().info("[DEBUG] Plugin data folder already exists."); // Added
                }

                if (locationsFile.createNewFile()) { // Check return value
                     plugin.getLogger().info("[DEBUG] Successfully created coords.yml"); // Changed log level
                } else {
                     plugin.getLogger().warning("[DEBUG] createNewFile() returned false for coords.yml"); // Added
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[DEBUG] IOException during coords.yml creation!", e); // Added DEBUG prefix
            } catch (SecurityException e) {
                 plugin.getLogger().log(Level.SEVERE, "[DEBUG] SecurityException during coords.yml creation!", e); // Added for permissions check
            }
        } else {
            plugin.getLogger().info("[DEBUG] coords.yml already exists."); // Added
        }
    }

    private void loadLocations() {
        plugin.getLogger().info("[DEBUG] Loading locations from coords.yml..."); // Added
        if (locationsFile != null && locationsFile.exists() && locationsFile.canRead()) { // Added checks
            locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
            plugin.getLogger().info("[DEBUG] Locations loaded."); // Added
        } else {
            plugin.getLogger().warning("[DEBUG] Cannot load coords.yml. File exists: " + (locationsFile != null ? locationsFile.exists() : "null file") + ", Can read: " + (locationsFile != null && locationsFile.exists() ? locationsFile.canRead() : "N/A")); // Added
            // Initialize empty config if file cannot be loaded
            locationsConfig = new YamlConfiguration();
            plugin.getLogger().info("[DEBUG] Initialized empty locationsConfig."); // Added
        }
    }

    private void saveLocations() {
        plugin.getLogger().info("[DEBUG] Attempting to save locations to coords.yml..."); // Added
        try {
            if (locationsConfig == null) {
                 plugin.getLogger().severe("[DEBUG] locationsConfig is null! Cannot save."); // Added check
                 return;
            }
            if (locationsFile == null) {
                 plugin.getLogger().severe("[DEBUG] locationsFile is null! Cannot save."); // Added check
                 return;
            }
            // Check if file exists and is writable before saving
            if (locationsFile.exists() && !locationsFile.canWrite()) {
                 plugin.getLogger().severe("[DEBUG] Cannot write to coords.yml! Check permissions.");
                 return;
            }
             if (!locationsFile.exists()) {
                 // Try creating parent dirs again just in case
                 File parentDir = locationsFile.getParentFile();
                 if (!parentDir.exists()) {
                     parentDir.mkdirs();
                 }
                 plugin.getLogger().info("[DEBUG] coords.yml does not exist, attempting save anyway (will create).");
             }

            locationsConfig.save(locationsFile); // Actual file write
            plugin.getLogger().info("[DEBUG] Successfully saved locations to coords.yml. File size: " + locationsFile.length()); // Log size

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[DEBUG] IOException during coords.yml save!", e); // Added DEBUG prefix
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DEBUG] Unexpected exception during coords.yml save!", e); // Added generic catch
        }
    }

    /**
     * Saves the player's current location for the world they are currently in.
     * @param player The player whose location is being saved.
     */
    public void saveLastLocation(Player player) {
        plugin.getLogger().info("[DEBUG] saveLastLocation called for player " + player.getName() + " in world " + player.getWorld().getName()); // Added
        if (locationsConfig == null) {
             plugin.getLogger().severe("[DEBUG] locationsConfig is null in saveLastLocation! Aborting save."); // Added check
             return;
        }
        UUID playerUUID = player.getUniqueId();
        String worldName = player.getWorld().getName();
        Location location = player.getLocation();

        String path = "locations." + playerUUID.toString() + "." + worldName;
        locationsConfig.set(path + ".world", worldName); // Redundant but good for clarity
        locationsConfig.set(path + ".x", location.getX());
        locationsConfig.set(path + ".y", location.getY());
        locationsConfig.set(path + ".z", location.getZ());
        locationsConfig.set(path + ".yaw", location.getYaw());
        locationsConfig.set(path + ".pitch", location.getPitch());
        plugin.getLogger().info("[DEBUG] Updated locationsConfig in memory for " + player.getName()); // Added

        saveLocations(); // Calls the method to write to file
        // Removed fine log as saveLocations now logs success/failure
    }

    /**
     * Retrieves the last known location for a player in a specific world.
     * @param player The player.
     * @param worldName The name of the world to get the location for.
     * @return The Location object, or null if no location is stored or the world is not loaded.
     */
    public Location getLastLocation(Player player, String worldName) {
        plugin.getLogger().fine("[DEBUG] getLastLocation called for player " + player.getName() + " in world " + worldName); // Added debug
        if (locationsConfig == null) { // Added check
            plugin.getLogger().warning("[DEBUG] locationsConfig is null in getLastLocation!");
            return null;
        }
        UUID playerUUID = player.getUniqueId();
        String path = "locations." + playerUUID.toString() + "." + worldName;

        if (!locationsConfig.contains(path)) {
            plugin.getLogger().fine("[DEBUG] No location found in coords.yml for path: " + path); // Added debug
            return null; // No location stored for this player/world combination
        }

        ConfigurationSection section = locationsConfig.getConfigurationSection(path);
        if (section == null) {
             plugin.getLogger().warning("[DEBUG] ConfigurationSection is null for path: " + path); // Added debug
             return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[DEBUG] Attempted to get last location for player " + player.getName() + " in world '" + worldName + "', but world is not loaded."); // Added debug prefix
            return null; // World not loaded
        }

        try {
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw");
            float pitch = (float) section.getDouble("pitch");

            plugin.getLogger().fine("[DEBUG] Retrieved last location for " + player.getName() + " in world " + worldName); // Added debug prefix
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DEBUG] Error reading location data for player " + player.getName() + " in world " + worldName, e); // Added debug prefix
            return null;
        }
    }
}

