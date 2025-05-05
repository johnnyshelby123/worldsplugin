package com.travelplus.commands;

import com.travelplus.Main;
import com.travelplus.utils.ArenaGenerator; // Added for step 021
import com.travelplus.utils.VoidGenerator;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CreateWorldCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final List<String> worldTypes = Arrays.asList("NORMAL", "NETHER", "VOID", "ARENA"); // Removed THE_END, Added ARENA

    public CreateWorldCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("travellerplus.createworld")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /createworld <world_name> [NORMAL|NETHER|VOID|ARENA]");
            return false;
        }

        String worldName = args[0];
        // Relaxed regex to allow more characters, but still avoid problematic ones like /
        if (!worldName.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            sender.sendMessage(ChatColor.RED + "Invalid world name. Use only letters, numbers, underscores, hyphens, and periods.");
            return true;
        }

        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage(ChatColor.RED + "A world with this name already exists or is loaded.");
            return true;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            sender.sendMessage(ChatColor.RED + "A world folder with this name already exists. Delete the folder manually first.");
            return true;
        }

        String typeArg = (args.length == 2) ? args[1].toUpperCase() : "NORMAL";
        World.Environment environment;
        ChunkGenerator generator = null;

        switch (typeArg) {
            case "NORMAL":
                environment = World.Environment.NORMAL;
                break;
            case "NETHER":
                environment = World.Environment.NETHER;
                break;
            // THE_END case removed as requested
            case "VOID":
                environment = World.Environment.NORMAL; // Void worlds often use NORMAL environment
                generator = new VoidGenerator(); // Uses the existing void generator
                sender.sendMessage(ChatColor.YELLOW + "Creating a VOID world...");
                break;
            case "ARENA": // Added ARENA case
                environment = World.Environment.NORMAL; // Arena worlds use NORMAL environment
                generator = new ArenaGenerator(); // Use the specific ArenaGenerator (Updated in step 021)
                sender.sendMessage(ChatColor.YELLOW + "Creating an ARENA world...");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid world type. Use NORMAL, NETHER, VOID, or ARENA.");
                return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Creating world \'" + worldName + "\' with type " + typeArg + ". This may take a moment...");

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(environment);
        if (generator != null) {
            creator.generator(generator);
        }

        // World creation should be synchronous
        World newWorld = null;
        try {
            newWorld = creator.createWorld();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating world \'" + worldName + "\'", e);
            sender.sendMessage(ChatColor.RED + "An error occurred during world creation. Check console.");
            return true;
        }

        // Handle result synchronously
        if (newWorld != null) {
            String worldNameLower = worldName.toLowerCase();
            plugin.reloadPluginConfig(); // Reload config to get latest lists before modifying
            FileConfiguration currentConfig = plugin.getConfig();
            List<String> currentAllowedWorlds = currentConfig.getStringList("allowed-worlds");
            // Ensure list exists even if empty in config
            if (currentAllowedWorlds == null) currentAllowedWorlds = new ArrayList<>();
            // Work with a mutable list of lowercase names
            List<String> mutableAllowedWorlds = new ArrayList<>(currentAllowedWorlds.stream().map(String::toLowerCase).collect(Collectors.toList()));

            if (!mutableAllowedWorlds.contains(worldNameLower)) {
                mutableAllowedWorlds.add(worldNameLower);
                // Save the list back to config (using lowercase for consistency)
                currentConfig.set("allowed-worlds", mutableAllowedWorlds);
                plugin.saveConfig();
                plugin.reloadPluginConfig(); // Reload again to update in-memory lists in Main
                sender.sendMessage(ChatColor.GREEN + "World \'" + worldName + "\' created successfully and added to allowed travel list!");
                plugin.getLogger().info("World \'" + worldName + "\' created by " + sender.getName() + " and added to config.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "World \'" + worldName + "\' created successfully! (Already in allowed list)");
                plugin.getLogger().info("World \'" + worldName + "\' created by " + sender.getName() + ".");
            }
            // TODO: Add logic here or in reloadPluginConfig to potentially add to op-only or separate-inv lists if needed?
            // Currently, it only adds to allowed-worlds.
        } else {
            // Error should have been caught above, but handle just in case
            sender.sendMessage(ChatColor.RED + "Failed to create world \'" + worldName + "\'. Check console.");
            plugin.getLogger().severe("Failed to create world \'" + worldName + "\' requested by " + sender.getName() + " (returned null).");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // No suggestions for world name
            return Collections.emptyList();
        } else if (args.length == 2) {
            // Suggest world types
            return StringUtil.copyPartialMatches(args[1], worldTypes, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

