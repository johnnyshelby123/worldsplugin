package com.travelplus.commands;

import com.travelplus.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("travellerplus.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        try {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "TravellerPlus configuration reloaded successfully!");
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while reloading the configuration. Check console for details.");
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error reloading configuration triggered by " + sender.getName(), e);
        }

        return true;
    }
}

