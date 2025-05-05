package com.travelplus.listeners;

import com.travelplus.Main;
import com.travelplus.managers.InventoryManager;
import com.travelplus.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap; // Added for death locations map
import java.util.List;
import java.util.Map;
import java.util.UUID; // Added for death locations map

public class PlayerListener implements Listener {

    private final Main plugin;
    private final InventoryManager inventoryManager;
    private final Map<UUID, Location> deathLocations = new HashMap<>(); // Store death locations for respawn logic

    public PlayerListener(Main plugin, InventoryManager inventoryManager) {
        this.plugin = plugin;
        this.inventoryManager = inventoryManager;
    }

    // Handle first join spawn
    @EventHandler(priority = EventPriority.LOWEST) // Run early, before other plugins might interfere
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if it's the player's first time joining EVER
        if (!player.hasPlayedBefore()) {
            String spawnWorldName = plugin.getSpawnWorldName();
            if (spawnWorldName != null && !spawnWorldName.isEmpty()) {
                World targetWorld = Bukkit.getWorld(spawnWorldName);
                if (targetWorld != null) {
                    plugin.getLogger().info("Player " + player.getName() + " is joining for the first time. Attempting to spawn in configured world: " + spawnWorldName);

                    Location targetLocation;
                    Map<String, Location> customSpawns = plugin.getCustomSpawnPoints();

                    if (customSpawns.containsKey(spawnWorldName)) {
                        targetLocation = customSpawns.get(spawnWorldName).clone();
                        targetLocation.setWorld(targetWorld);
                        plugin.getLogger().info("Using custom spawn point for first join in world: " + spawnWorldName);
                    } else {
                        Location defaultSpawn = targetWorld.getSpawnLocation();
                        targetLocation = LocationUtils.findTrulySafeLocation(defaultSpawn, 10);
                        if (targetLocation == null) {
                            plugin.getLogger().warning("Could not find a truly safe location near spawn in configured spawn world \'" + targetWorld.getName() + "\'. Using default spawn directly.");
                            targetLocation = defaultSpawn.clone().add(0.5, 0, 0.5);
                        } else {
                            plugin.getLogger().info("Found safe location near spawn for first join in world: " + spawnWorldName);
                        }
                    }

                    // Ensure world is set
                    if (targetLocation.getWorld() == null) {
                        targetLocation.setWorld(targetWorld);
                    }

                    // Teleport the player slightly delayed to avoid potential login conflicts
                    Location finalTargetLocation = targetLocation;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.teleport(finalTargetLocation);
                        plugin.getLogger().info("Teleported first-time player " + player.getName() + " to spawn in " + spawnWorldName);
                    }, 1L); // 1 tick delay

                } else {
                    plugin.getLogger().warning("Configured spawn-world \'" + spawnWorldName + "\' not found or not loaded. Using default server spawn.");
                }
            } else {
                 plugin.getLogger().fine("No spawn-world configured or player has played before. Using default server spawn.");
            }
        } else {
             plugin.getLogger().finest("Player " + player.getName() + " has played before. Skipping first join spawn logic.");
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        World toWorld = player.getWorld(); // Destination world

        String fromWorldName = fromWorld.getName().toLowerCase();
        String toWorldName = toWorld.getName().toLowerCase();

        boolean fromSeparate = plugin.isSeparateInventoryWorld(fromWorldName);
        boolean toSeparate = plugin.isSeparateInventoryWorld(toWorldName);

        // Handle Inventory Separation
        if (fromSeparate != toSeparate) {
            if (fromSeparate) {
                // Leaving a separate inventory world -> Save world inv, load main inv
                inventoryManager.saveInventory(player, fromWorldName);
                inventoryManager.loadInventory(player, "main");
                player.sendMessage(ChatColor.YELLOW + "Restored main inventory.");
                plugin.getLogger().info("Player " + player.getName() + " left separate world " + fromWorld.getName() + ", restored main inventory.");
            } else {
                // Entering a separate inventory world -> Save main inv, load world inv
                inventoryManager.saveInventory(player, "main");
                inventoryManager.loadInventory(player, toWorldName);
                player.sendMessage(ChatColor.YELLOW + "Loaded inventory for " + toWorld.getName() + ".");
                plugin.getLogger().info("Player " + player.getName() + " entered separate world " + toWorld.getName() + ", loaded world inventory.");
            }
        }

        // Apply World Settings on Entry (Added for step 019)
        Difficulty worldDifficulty = plugin.getDifficultyForWorld(toWorldName);
        if (worldDifficulty != null && toWorld.getDifficulty() != worldDifficulty) {
            toWorld.setDifficulty(worldDifficulty);
            plugin.getLogger().info("Set difficulty for world \'" + toWorldName + "\' to " + worldDifficulty.name() + " (triggered by " + player.getName() + ")");
            // Note: This affects the entire world, not just the player.
        }

        GameMode defaultGamemode = plugin.getDefaultGamemodeForWorld(toWorldName);
        if (defaultGamemode != null && player.getGameMode() != defaultGamemode) {
            player.setGameMode(defaultGamemode);
            player.sendMessage(ChatColor.AQUA + "Set gamemode to " + defaultGamemode.name() + " for world " + toWorld.getName() + ".");
            plugin.getLogger().info("Set gamemode for player " + player.getName() + " to " + defaultGamemode.name() + " upon entering world " + toWorldName);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName().toLowerCase();

        // Remove from death location map on quit
        deathLocations.remove(player.getUniqueId());

        // Use the helper method from Main (Refactored in step 014)
        if (plugin.isSeparateInventoryWorld(worldName)) {
            // Player quitting from a separate inventory world, save its inventory
            inventoryManager.saveInventory(player, worldName);
            plugin.getLogger().info("Player " + player.getName() + " quit from separate world " + worldName + ", saved world inventory.");
        } else {
            // Player quitting from a normal world, save the main inventory
            inventoryManager.saveInventory(player, "main");
            plugin.getLogger().info("Player " + player.getName() + " quit from normal world " + worldName + ", saved main inventory.");
        }
    }

    // Store death location (Added for step 015)
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathLocations.put(player.getUniqueId(), player.getLocation());
        plugin.getLogger().fine("Stored death location for " + player.getName() + " in world " + player.getWorld().getName());
    }

    // Handle respawn location (Added for step 015)
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.remove(player.getUniqueId()); // Get and remove location

        if (deathLocation != null) {
            World deathWorld = deathLocation.getWorld();
            if (deathWorld != null) {
                String deathWorldName = deathWorld.getName().toLowerCase();
                if (plugin.shouldRespawnInWorld(deathWorldName)) {
                    plugin.getLogger().info("Player " + player.getName() + " died in world \'" + deathWorldName + "\' which is configured for local respawn.");

                    Location respawnLocation;
                    Map<String, Location> customSpawns = plugin.getCustomSpawnPoints();

                    // Check if there's a custom spawn point defined for the death world
                    if (customSpawns.containsKey(deathWorldName)) {
                        respawnLocation = customSpawns.get(deathWorldName).clone();
                        respawnLocation.setWorld(deathWorld); // Ensure world is set
                        plugin.getLogger().info("Using custom spawn point for respawn in world: " + deathWorldName);
                    } else {
                        // Otherwise, use the world's default spawn point
                        Location worldSpawn = deathWorld.getSpawnLocation();
                        respawnLocation = LocationUtils.findTrulySafeLocation(worldSpawn, 10);
                        if (respawnLocation == null) {
                            plugin.getLogger().warning("Could not find a truly safe location near spawn in death world \'" + deathWorld.getName() + "\". Using world spawn directly.");
                            respawnLocation = worldSpawn.clone().add(0.5, 0, 0.5);
                        } else {
                             plugin.getLogger().info("Found safe location near spawn for respawn in world: " + deathWorldName);
                        }
                    }

                    // Ensure world is set correctly on the final location
                    if (respawnLocation.getWorld() == null) {
                        respawnLocation.setWorld(deathWorld);
                    }

                    event.setRespawnLocation(respawnLocation);
                    plugin.getLogger().info("Set respawn location for " + player.getName() + " to world: " + deathWorldName);
                    return; // Respawn location handled
                } else {
                    plugin.getLogger().fine("Player " + player.getName() + " died in world \'" + deathWorldName + "\', but it is not configured for local respawn.");
                }
            } else {
                 plugin.getLogger().warning("Could not get death world from stored location for player " + player.getName());
            }
        } else {
             plugin.getLogger().finest("No recent death location found for player " + player.getName() + ". Using default respawn logic.");
        }
        // If not handled above, Bukkit's default respawn logic applies
    }
}

