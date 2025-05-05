package com.travelplus.commands;

import com.travelplus.Main;
import com.travelplus.config.WorldConfig;
import com.travelplus.managers.TeleportManager;
import com.travelplus.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SpawnCommand implements CommandExecutor {

    private final Main plugin;
    private final TeleportManager teleportManager;

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("travellerplus.spawn")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Get the configured spawn world name
        String spawnWorldName = plugin.getSpawnWorldName();
        if (spawnWorldName == null || spawnWorldName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "The spawn world is not configured on the server.");
            plugin.getLogger().warning("Player " + player.getName() + " tried to use /spawn, but no spawn-world is configured.");
            return true;
        }

        // Check if the player is already in the spawn world
        if (player.getWorld().getName().equalsIgnoreCase(spawnWorldName)) {
            player.sendMessage(ChatColor.YELLOW + "You are already in the spawn world!");
            // Optional: Still teleport them to the exact spawn point if desired?
            // For now, just inform them.
            return true;
        }

        // Get the target world (verify it exists before calling TeleportManager)
        World targetWorld = Bukkit.getWorld(spawnWorldName);
        if (targetWorld == null) {
            player.sendMessage(ChatColor.RED + "The configured spawn world (\"" + spawnWorldName + "\") is not loaded or does not exist.");
            plugin.getLogger().severe("Player " + player.getName() + " tried to use /spawn, but the configured spawn world (\"" + spawnWorldName + "\") could not be found.");
            return true;
        }

        // Check OP-only restriction for the spawn world
        WorldConfig worldConfig = plugin.getWorldConfig(spawnWorldName); // Use lowercase name internally
        if (worldConfig.opOnly() && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Only OPs are allowed to travel to the spawn world (\"" + spawnWorldName + "\").");
            return true;
        }

        // Initiate teleport via TeleportManager using the world name
        // TeleportManager will handle finding the correct location (custom spawn, default, etc.)
        teleportManager.startTeleport(player, spawnWorldName);

        return true;
    }

    // Removed the redundant getTargetSpawnLocation helper method as TeleportManager handles this
}

