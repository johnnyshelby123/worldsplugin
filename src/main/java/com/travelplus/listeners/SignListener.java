package com.travelplus.listeners;

import com.travelplus.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent; // Added for sign protection
import org.bukkit.event.block.BlockExplodeEvent; // Added for explosion protection
import org.bukkit.event.block.BlockPlaceEvent; // Added for sign placement
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack; // Added for sign placement
import org.bukkit.inventory.meta.BlockStateMeta; // Added for sign placement
import org.bukkit.inventory.meta.ItemMeta; // Added for sign placement
import org.bukkit.persistence.PersistentDataContainer; // Added for PDC
import org.bukkit.persistence.PersistentDataType; // Added for PDC
import com.travelplus.commands.GetTravelSignCommand; // Import for PDC key

public class SignListener implements Listener {

    private final Main plugin;
    // Define the new sign format (Bold Red)
    public static final String SIGN_LINE_1 = ""; // Line 1 is empty or can be used for something else if needed
    public static final String SIGN_LINE_2 = ChatColor.RED + "" + ChatColor.BOLD + "RIGHT CLICK TO";
    public static final String SIGN_LINE_3 = ChatColor.RED + "" + ChatColor.BOLD + "TRAVEL TO WORLD";
    // Line 4 will contain the world name, also formatted

    public SignListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // Check if the interaction is a right-click on a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) {
            return;
        }

        // Check if the clicked block is a sign
        if (clickedBlock.getState() instanceof Sign) {
            Sign sign = (Sign) clickedBlock.getState();
            String[] lines = sign.getLines();

            // Check if the sign matches the new TravelPlus format (Lines 2 and 3)
            if (lines.length >= 4 && SIGN_LINE_2.equals(lines[1]) && SIGN_LINE_3.equals(lines[2])) {
                // Prevent sign text editing if it's a valid travel sign
                event.setCancelled(true);

                // --- Get world name from PDC ---
                PersistentDataContainer blockPDC = sign.getPersistentDataContainer();
                String worldName = blockPDC.get(GetTravelSignCommand.WORLD_NAME_KEY, PersistentDataType.STRING);
                // --- End PDC retrieval ---

                // String worldNameLine = lines[3]; // Get world name line (Old method)
                // String worldName = ChatColor.stripColor(worldNameLine); // Strip color/formatting to get the raw name (Old method)

                if (worldName != null && !worldName.isEmpty()) {
                    // Initiate teleport using TeleportManager
                    plugin.getLogger().info("Player " + player.getName() + " clicked travel sign for world: " + worldName);
                    plugin.getTeleportManager().startTeleport(player, worldName);

                } else {
                    plugin.getLogger().warning("Player " + player.getName() + " clicked a TravelPlus sign with an empty world name on line 4.");
                }
            }
        }
    }

    // Handles applying the sign data when a special sign item is placed
    @EventHandler
    public void onSignPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block blockPlaced = event.getBlockPlaced();
        ItemStack itemInHand = event.getItemInHand();

        // Check if the placed block is a sign and the item used has meta
        // Also check the material type to be sure it's a sign material
        if (!(blockPlaced.getState() instanceof Sign) || !itemInHand.hasItemMeta() || !itemInHand.getType().name().endsWith("_SIGN")) {
            return;
        }

        ItemMeta itemMeta = itemInHand.getItemMeta();

        // Check if the item is a special TravelPlus sign (using BlockStateMeta)
        if (itemMeta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
            if (blockStateMeta.hasBlockState() && blockStateMeta.getBlockState() instanceof Sign) {
                Sign itemSignState = (Sign) blockStateMeta.getBlockState();
                String[] itemLines = itemSignState.getLines();

                // Verify if the item's sign data matches our format
                if (itemLines.length >= 4 && SIGN_LINE_2.equals(itemLines[1]) && SIGN_LINE_3.equals(itemLines[2])) {
                    // It's our special sign item, apply its state to the placed block
                    Sign placedSignState = (Sign) blockPlaced.getState();

                    // Copy lines from the item's sign state to the placed sign state
                    for (int i = 0; i < itemLines.length; i++) {
                        placedSignState.setLine(i, itemLines[i]);
                    }

                    // --- Copy PDC data from item to placed block ---
                    PersistentDataContainer itemPDC = blockStateMeta.getPersistentDataContainer();
                    PersistentDataContainer blockPDC = placedSignState.getPersistentDataContainer();
                    String worldName = itemPDC.get(GetTravelSignCommand.WORLD_NAME_KEY, PersistentDataType.STRING);
                    if (worldName != null) {
                        blockPDC.set(GetTravelSignCommand.WORLD_NAME_KEY, PersistentDataType.STRING, worldName);
                        plugin.getLogger().fine("Copied world name PDC ("+worldName+") from item to placed sign at " + blockPlaced.getLocation());
                    } else {
                        plugin.getLogger().warning("Could not find world name PDC on placed sign item at " + blockPlaced.getLocation());
                    }
                    // --- End PDC copy ---

                    // Update the placed sign block
                    placedSignState.update(true); // true forces the update
                    plugin.getLogger().info("Applied TravelPlus sign data for world '" + ChatColor.stripColor(itemLines[3]) + "' placed by " + player.getName());
                }
            }
        }
    }

    // Handles preventing players from breaking travel signs
    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if the broken block is a sign
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();

            // Check if the sign matches the TravelPlus format
            if (lines.length >= 4 && SIGN_LINE_2.equals(lines[1]) && SIGN_LINE_3.equals(lines[2])) {
                // It's a travel sign, unconditionally cancel the break event
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This special travel sign cannot be broken.");
                plugin.getLogger().info("Prevented player " + player.getName() + " from breaking a travel sign at " + block.getLocation());
            }
        }
    }

    // Handles preventing explosions from breaking travel signs
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Iterate through the list of blocks that will be destroyed by the explosion
        // Use an iterator to safely remove elements while iterating
        java.util.Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            // Check if the block is a sign
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String[] lines = sign.getLines();

                // Check if the sign matches the TravelPlus format
                if (lines.length >= 4 && SIGN_LINE_2.equals(lines[1]) && SIGN_LINE_3.equals(lines[2])) {
                    // It's a travel sign, remove it from the list of blocks to be destroyed
                    iterator.remove();
                    plugin.getLogger().fine("Protected a travel sign from explosion at " + block.getLocation());
                }
            }
        }
    }
}

