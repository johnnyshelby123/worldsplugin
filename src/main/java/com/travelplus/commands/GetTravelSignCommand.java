package com.travelplus.commands;

import com.travelplus.Main;
import com.travelplus.listeners.SignListener; // Import for sign text constants
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer; // Added for PDC
import org.bukkit.persistence.PersistentDataType; // Added for PDC
import org.bukkit.NamespacedKey; // Added for PDC
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetTravelSignCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    public static final NamespacedKey WORLD_NAME_KEY = new NamespacedKey("travelplus", "world_name"); // PDC Key

    public GetTravelSignCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("travellerplus.gettravelsign")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /gettravelsign <world_name>");
            return false;
        }

        String worldName = args[0];
        String worldNameLower = worldName.toLowerCase();

        // Basic validation: Check if the world is in the allowed list (more robust checks could be added)
        // Alternatively, check Bukkit.getWorld(worldName) != null if it must be loaded
        if (!plugin.getAllowedWorlds().contains(worldNameLower)) {
             // Check case-sensitively as well, just in case config wasn't lowercased somehow
             boolean foundCaseSensitive = false;
             for (String allowed : plugin.getAllowedWorlds()) {
                 if (allowed.equalsIgnoreCase(worldName)) {
                     worldName = allowed; // Use the correctly cased name from config if found
                     foundCaseSensitive = true;
                     break;
                 }
             }
             if (!foundCaseSensitive) {
                 player.sendMessage(ChatColor.RED + "World '" + worldName + "' is not in the allowed travel list in config.yml.");
                 player.sendMessage(ChatColor.RED + "Add it to 'allowed-worlds' and run /tpreload first.");
                 return true;
             }
        }

        // Create the sign item
        ItemStack signItem = new ItemStack(Material.OAK_SIGN); // Or another sign type
        ItemMeta meta = signItem.getItemMeta();

        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            if (blockStateMeta.getBlockState() instanceof Sign) {
                Sign signState = (Sign) blockStateMeta.getBlockState();

                // Set the sign lines according to the new format
                signState.setLine(0, SignListener.SIGN_LINE_1); // Empty
                signState.setLine(1, SignListener.SIGN_LINE_2); // RIGHT CLICK TO
                signState.setLine(2, SignListener.SIGN_LINE_3); // TRAVEL TO WORLD
                // Format the world name line - ensure it fits (max 15 chars per line generally)
                String formattedWorldName = ChatColor.RED + "" + ChatColor.BOLD + worldName;
                if (formattedWorldName.length() > 15) {
                    // Simple truncation if too long
                    formattedWorldName = formattedWorldName.substring(0, 15);
                    // Consider alternative handling like using multiple lines or abbreviation if needed
                }
                signState.setLine(3, formattedWorldName);

                // Update the BlockStateMeta
                blockStateMeta.setBlockState(signState);

                // Set display name and lore for the item itself
                blockStateMeta.setDisplayName(ChatColor.GOLD + "Travel Sign (" + worldName + ")");
                blockStateMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Place this sign to create a travel point."));

                // --- Store full world name in PDC ---
                PersistentDataContainer pdc = blockStateMeta.getPersistentDataContainer();
                pdc.set(WORLD_NAME_KEY, PersistentDataType.STRING, worldName); // Store the original, non-truncated name
                plugin.getLogger().fine("Stored full world name '" + worldName + "' in sign item PDC.");
                // --- End PDC storage ---

                // Apply the meta back to the item stack
                signItem.setItemMeta(blockStateMeta);

                // Give the item to the player
                player.getInventory().addItem(signItem);
                player.sendMessage(ChatColor.GREEN + "You received a Travel Sign for world: " + worldName);
                plugin.getLogger().info("Gave travel sign for world '" + worldName + "' to player " + player.getName());

            } else {
                player.sendMessage(ChatColor.RED + "Error: Could not get sign state from item meta.");
                return true;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Error: Could not get BlockStateMeta from sign item.");
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("travellerplus.gettravelsign")) {
            // Suggest allowed world names
            List<String> allowedWorlds = plugin.getAllowedWorlds(); // Already lowercase
            // We might want to show the original casing if available, but lowercase works for matching
            return StringUtil.copyPartialMatches(args[0], allowedWorlds, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}

