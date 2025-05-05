package com.travelplus.managers;

import com.travelplus.Main;
import com.travelplus.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager implements org.bukkit.event.Listener { // Implement Listener for move event

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeTeleports = new HashMap<>();
    private final Map<UUID, Location> initialLocations = new HashMap<>();
    private static final int COUNTDOWN_SECONDS = 3;
    public static final String BYPASS_PERMISSION = "travellerplus.countdown.bypass";

    public TeleportManager(Main plugin) {
        this.plugin = plugin;
        // Register this class as a listener for PlayerMoveEvent
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startTeleport(Player player, String worldName) {
        UUID playerUUID = player.getUniqueId();

        // Cancel any existing teleport task for this player
        if (activeTeleports.containsKey(playerUUID)) {
            cancelTeleport(player, false); // Don't send cancellation message if starting a new one
        }

        // Check bypass permission (Step 008)
        if (player.hasPermission(BYPASS_PERMISSION)) {
            plugin.getLogger().info("Player " + player.getName() + " has bypass permission, teleporting instantly to " + worldName);
            performTeleport(player, worldName);
            return;
        }

        // --- Start Countdown --- 
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            // Attempt case-sensitive lookup if lowercase failed
            targetWorld = Bukkit.getWorld(worldName); // Use original case provided
             if (targetWorld == null) {
                 player.sendMessage(ChatColor.RED + "World \'" + worldName + "\' not found or not loaded.");
                 return;
             }
             // If found case-sensitively, update worldName to the correct case for consistency
             worldName = targetWorld.getName();
        }
        final String finalWorldName = worldName; // Final variable for runnable
        final World finalTargetWorld = targetWorld; // Final variable for runnable

        player.sendMessage(ChatColor.YELLOW + "Teleporting to " + finalWorldName + " in " + COUNTDOWN_SECONDS + " seconds... Don't move!");
        initialLocations.put(playerUUID, player.getLocation()); // Store initial location

        BukkitTask task = new BukkitRunnable() {
            private int countdown = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!player.isOnline()) { // Player logged off
                    cancel();
                    activeTeleports.remove(playerUUID);
                    initialLocations.remove(playerUUID);
                    return;
                }

                if (countdown > 0) {
                    player.sendMessage(ChatColor.YELLOW + "..." + countdown);
                    countdown--;
                } else {
                    // Countdown finished, perform teleport
                    performTeleport(player, finalWorldName);
                    cancel(); // Stop this runnable
                    activeTeleports.remove(playerUUID);
                    initialLocations.remove(playerUUID);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run task every second (20 ticks)

        activeTeleports.put(playerUUID, task);
    }

    public void cancelTeleport(Player player, boolean showMessage) {
        UUID playerUUID = player.getUniqueId();
        BukkitTask task = activeTeleports.remove(playerUUID);
        initialLocations.remove(playerUUID);
        if (task != null) {
            task.cancel();
            if (showMessage) {
                player.sendMessage(ChatColor.RED + "Teleport cancelled!");
            }
            plugin.getLogger().info("Cancelled teleport for player " + player.getName());
        }
    }

    // Actual teleport logic (extracted from TravelCommand)
    private void performTeleport(Player player, String worldName) {
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            player.sendMessage(ChatColor.RED + "Error: World \'" + worldName + "\' became unavailable.");
            plugin.getLogger().severe("Failed to perform teleport for " + player.getName() + ": World \'" + worldName + "\' is null.");
            return;
        }

        String worldNameLower = worldName.toLowerCase();
        Map<String, Location> customSpawnPoints = plugin.getCustomSpawnPoints();

        Location targetLocation;

        // --- Start: Check for last known location ---
        Location lastLocation = plugin.getLocationManager().getLastLocation(player, worldName);
        if (lastLocation != null) {
            targetLocation = lastLocation;
            plugin.getLogger().info("Using last known location for player " + player.getName() + " in world " + worldName);
        } else {
            // --- Fallback to custom spawn or default spawn ---
            if (customSpawnPoints.containsKey(worldNameLower)) {
                targetLocation = customSpawnPoints.get(worldNameLower).clone();
                targetLocation.setWorld(targetWorld); // Ensure world is set
                plugin.getLogger().info("Using custom spawn point for world: " + worldNameLower + " for player " + player.getName());
            } else {
                Location defaultSpawn = targetWorld.getSpawnLocation();
                targetLocation = LocationUtils.findTrulySafeLocation(defaultSpawn, 10);
                if (targetLocation == null) {
                    plugin.getLogger().warning("Could not find a truly safe location near spawn in \'" + targetWorld.getName() + "\' for player " + player.getName() + ". Using default spawn directly.");
                    targetLocation = defaultSpawn.clone().add(0.5, 0, 0.5); // Center on block
                } else {
                    plugin.getLogger().info("Found safe location near spawn for world: " + worldNameLower + " for player " + player.getName());
                }
            }
        }
        // --- End: Location logic ---

        // Ensure the location's world is set correctly before teleporting
        if (targetLocation.getWorld() == null) {
            targetLocation.setWorld(targetWorld);
        }

         // Save the player's current location BEFORE teleporting
        plugin.getLocationManager().saveLastLocation(player);

        // Perform teleportation (consider using PaperMC's async teleport if available/needed)
        player.teleport(targetLocation);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + targetWorld.getName() + "!");
        plugin.getLogger().info("Successfully teleported player " + player.getName() + " to " + worldName);

        // Note: Inventory handling and world settings application are done by PlayerListener on PlayerChangedWorldEvent
    }

    // --- Event Handler for Movement Cancellation ---
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (activeTeleports.containsKey(playerUUID)) {
            Location initialLoc = initialLocations.get(playerUUID);
            Location currentLoc = event.getTo(); // Use getTo() for future location

            // Check if the player has moved significantly (ignore minor head movements)
            // Compare block coordinates for simplicity
            if (initialLoc != null && currentLoc != null &&
                (initialLoc.getBlockX() != currentLoc.getBlockX() ||
                 initialLoc.getBlockY() != currentLoc.getBlockY() ||
                 initialLoc.getBlockZ() != currentLoc.getBlockZ()))
            {
                cancelTeleport(player, true); // Cancel and show message
            }
        }
    }

     // Also cancel on quit
    @org.bukkit.event.EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer(), false);
    }

    // Optional: Cancel on death?
    @org.bukkit.event.EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
         cancelTeleport(event.getEntity(), false); // Cancel silently on death
    }

}

