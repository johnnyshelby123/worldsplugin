package com.travelplus.listeners;

import com.travelplus.Main;
import com.travelplus.managers.InventoryManager;
import com.travelplus.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final InventoryManager inventoryManager;
    // Removed deathLocations map as it's no longer needed for respawn logic

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
            String spawnWorldName = plugin.getSpawnWorldName(); // Get from reloaded config
            if (spawnWorldName != null && !spawnWorldName.isEmpty()) {
                World targetWorld = Bukkit.getWorld(spawnWorldName);
                if (targetWorld != null) {
                    plugin.getLogger().info("Player " + player.getName() + " is joining for the first time. Attempting to spawn in configured world: " + spawnWorldName);

                    Location targetLocation = getTargetSpawnLocation(spawnWorldName, targetWorld);

                    // Teleport the player slightly delayed to avoid potential login conflicts
                    Location finalTargetLocation = targetLocation;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Re-check if player is still online before teleporting
                        if (player.isOnline()) {
                            player.teleportAsync(finalTargetLocation).thenAccept(success -> {
                                if (success) {
                                    plugin.getLogger().info("Teleported first-time player " + player.getName() + " to spawn in " + spawnWorldName);
                                } else {
                                     plugin.getLogger().warning("Failed to teleport first-time player " + player.getName() + " to spawn in " + spawnWorldName);
                                }
                            });
                        }
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

        // Apply World Settings on Entry
        plugin.applyWorldSettings(toWorldName, plugin.getWorldConfig(toWorldName));

        // Apply Default Gamemode on Entry
        GameMode defaultGamemode = plugin.getDefaultGamemodeForWorld(toWorldName);
        if (defaultGamemode != null && player.getGameMode() != defaultGamemode) {
            // Only change gamemode if they aren't already in spectator (common after death)
            if (player.getGameMode() != GameMode.SPECTATOR) {
                 player.setGameMode(defaultGamemode);
                 player.sendMessage(ChatColor.AQUA + "Set gamemode to " + defaultGamemode.name() + " for world " + toWorld.getName() + ".");
                 plugin.getLogger().info("Set gamemode for player " + player.getName() + " to " + defaultGamemode.name() + " upon entering world " + toWorldName);
            } else {
                 plugin.getLogger().info("Player " + player.getName() + " is in spectator mode. Skipping default gamemode change for world " + toWorldName);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName().toLowerCase();

        // Use the helper method from Main
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

    // Added handler for Keep Inventory on Death
    @EventHandler(priority = EventPriority.HIGH) // Run before potential drops happen but after other plugins might modify
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();
        String worldNameLower = world.getName().toLowerCase();

        // Check if keepInventory is enabled for this world in the config
        if (plugin.isKeepInventoryEnabled(worldNameLower)) {
            // Set Bukkit's keepInventory flag for the event
            event.setKeepInventory(true);
            // Clear the drops list to prevent items from dropping anyway
            event.getDrops().clear();
            // Optional: Set keepLevel to true if desired (usually goes with keepInventory)
            event.setKeepLevel(true);
            // Optional: Clear dropped EXP
            event.setDroppedExp(0);

            plugin.getLogger().info("Keep inventory applied for player " + player.getName() + " dying in world " + world.getName());
        } else {
            plugin.getLogger().fine("Keep inventory is not enabled for world " + world.getName() + ". Default death behavior applies.");
        }
    }

    // Handle respawn location - REVISED TO FULLY OVERRIDE DEFAULTS
    @EventHandler(priority = EventPriority.HIGHEST) // Run late to allow other plugins (like bed respawn) to set it first
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Check if the respawn is explicitly set to a bed or respawn anchor
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
             plugin.getLogger().fine("Player " + player.getName() + " respawning at Bed/Anchor. Skipping TravellerPlus global spawn logic.");
             return; // Let Bukkit/other plugins handle it
        }

        // If NOT a bed/anchor spawn, ALWAYS force respawn to the configured global spawn world
        String globalSpawnWorldName = plugin.getSpawnWorldName();
        if (globalSpawnWorldName != null && !globalSpawnWorldName.isEmpty()) {
            World globalSpawnWorld = Bukkit.getWorld(globalSpawnWorldName);
            if (globalSpawnWorld != null) {
                plugin.getLogger().info("Player " + player.getName() + " respawning without bed/anchor. Forcing respawn to global spawn world: " + globalSpawnWorldName);

                Location respawnLocation = getTargetSpawnLocation(globalSpawnWorldName, globalSpawnWorld);

                event.setRespawnLocation(respawnLocation);
                plugin.getLogger().info("Set respawn location for " + player.getName() + " to global spawn: " + globalSpawnWorldName + " at " + respawnLocation.toString());
                return; // Respawn location handled

            } else {
                 plugin.getLogger().warning("Configured global spawn-world \'" + globalSpawnWorldName + "\' not found or not loaded. Unable to force respawn. Using default server respawn logic.");
            }
        } else {
             plugin.getLogger().fine("No global spawn-world configured and no bed/anchor spawn detected. Using default server respawn logic.");
        }
        // If not handled above, Bukkit's default respawn logic applies
    }

    // Helper method to get the target spawn location (custom or default safe)
    private Location getTargetSpawnLocation(String worldNameLower, World targetWorld) {
        Location targetLocation;
        Map<String, Location> customSpawns = plugin.getCustomSpawnPoints(); // Get fresh config

        if (customSpawns.containsKey(worldNameLower)) {
            targetLocation = customSpawns.get(worldNameLower).clone();
            // Ensure world is set correctly on the custom spawn location object
            if (targetLocation.getWorld() == null || !targetLocation.getWorld().equals(targetWorld)) {
                targetLocation.setWorld(targetWorld);
            }
            plugin.getLogger().info("Using custom spawn point for world: " + worldNameLower);
        } else {
            // Otherwise, use the world's default spawn point
            Location worldSpawn = targetWorld.getSpawnLocation();
            targetLocation = LocationUtils.findTrulySafeLocation(worldSpawn, 10);
            if (targetLocation == null) {
                plugin.getLogger().warning("Could not find a truly safe location near spawn in world \'" + targetWorld.getName() + "\'. Using world spawn directly.");
                targetLocation = worldSpawn.clone().add(0.5, 0, 0.5);
            } else {
                 plugin.getLogger().info("Found safe location near spawn for world: " + worldNameLower);
            }
            // Ensure world is set on the default/safe location
            if (targetLocation.getWorld() == null) {
                 targetLocation.setWorld(targetWorld);
            }
        }

        // Final check: Ensure world is set correctly on the final location
        if (targetLocation.getWorld() == null || !targetLocation.getWorld().equals(targetWorld)) {
            plugin.getLogger().warning("Final target spawn location had incorrect/null world. Forcing target world: " + targetWorld.getName());
            targetLocation.setWorld(targetWorld);
        }
        return targetLocation;
    }

    // Handle PVP based on world config
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true) // Run early to cancel if needed
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();

        // Check if both are players
        if (damager instanceof Player && damaged instanceof Player) {
            Player attacker = (Player) damager;
            Player victim = (Player) damaged;
            World world = attacker.getWorld();
            String worldNameLower = world.getName().toLowerCase();

            // Check if PVP is disabled in this world via config
            if (!plugin.isPvpEnabled(worldNameLower)) {
                event.setCancelled(true); // Cancel the damage event
                attacker.sendMessage(ChatColor.GRAY + "You can't PVP here."); // Send message to attacker
                plugin.getLogger().fine("Cancelled PVP attempt by " + attacker.getName() + " on " + victim.getName() + " in world " + world.getName());
            }
        }
        // TODO: Handle projectiles fired by players if needed
    }
}

