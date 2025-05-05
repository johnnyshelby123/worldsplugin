package com.travelplus.commands;

import com.travelplus.Main;
import com.travelplus.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TravelCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public TravelCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;

        // Get latest config values from the plugin instance
        List<String> allowedWorlds = plugin.getAllowedWorlds();
        Map<String, Location> customSpawnPoints = plugin.getCustomSpawnPoints();
        // TODO: Add OP-only world check here later

        if (allowedWorlds.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Plugin configuration error. No allowed worlds defined.");
            plugin.getLogger().warning("Player " + player.getName() + " tried /travel but allowedWorlds list is empty.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /travel <world_name>");
            return false;
        }

        String requestedWorldName = args[0];
        String worldNameLower = requestedWorldName.toLowerCase();

        // Check if world is allowed
        if (!allowedWorlds.contains(worldNameLower)) {
            player.sendMessage(ChatColor.RED + "World \'" + requestedWorldName + "\' is not allowed.");
            return true;
        }

        // Check OP permission if required
        if (plugin.isOpOnlyWorld(worldNameLower) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "World \'" + requestedWorldName + "\' requires OP permissions to travel to.");
            return true;
        }

        World targetWorld = Bukkit.getWorld(worldNameLower);
        if (targetWorld == null) {
            targetWorld = Bukkit.getWorld(requestedWorldName); // Try case-sensitive
            if (targetWorld == null) {
                player.sendMessage(ChatColor.RED + "World \'" + requestedWorldName + "\' not found or not loaded.");
                return true;
            }
            worldNameLower = targetWorld.getName().toLowerCase(); // Update lower name if found
        }

        // Inventory handling is done by PlayerListener

        // --- Start teleport process using TeleportManager ---
        // This handles countdown, bypass, location saving/loading, and the actual teleport.
        plugin.getTeleportManager().startTeleport(player, targetWorld.getName());
        // --- End teleport process ---

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> availableWorlds = new ArrayList<>(plugin.getAllowedWorlds());
            // Filter out OP-only worlds if sender is not OP
            if (sender instanceof Player && !sender.isOp()) {
                availableWorlds.removeIf(plugin::isOpOnlyWorld);
            }
            return StringUtil.copyPartialMatches(args[0], availableWorlds, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

