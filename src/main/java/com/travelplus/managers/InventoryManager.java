package com.travelplus.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class InventoryManager {

    private final JavaPlugin plugin;
    private final File inventoryDataFolder;

    public InventoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.inventoryDataFolder = new File(plugin.getDataFolder(), "inventories");
        if (!inventoryDataFolder.exists()) {
            if (!inventoryDataFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create inventory data folder: " + inventoryDataFolder.getPath());
            }
        }
    }

    private File getInventoryFile(Player player, String inventoryKey) {
        // inventoryKey can be 'main' or a world name (lowercase)
        return new File(inventoryDataFolder, player.getUniqueId().toString() + "_" + inventoryKey.toLowerCase() + ".yml");
    }

    public void saveInventory(Player player, String inventoryKey) {
        File inventoryFile = getInventoryFile(player, inventoryKey);
        YamlConfiguration inventoryConfig = new YamlConfiguration();

        inventoryConfig.set("inventory.content", player.getInventory().getContents());
        inventoryConfig.set("inventory.armor", player.getInventory().getArmorContents());
        inventoryConfig.set("inventory.extra", player.getInventory().getExtraContents()); // Off-hand etc.
        inventoryConfig.set("enderchest.content", player.getEnderChest().getContents()); // Save Ender Chest
        inventoryConfig.set("stats.level", player.getLevel());
        inventoryConfig.set("stats.exp", player.getExp());
        inventoryConfig.set("stats.health", player.getHealth());
        inventoryConfig.set("stats.food", player.getFoodLevel());
        inventoryConfig.set("stats.saturation", player.getSaturation());
        // Consider saving potion effects, game mode, etc. if needed

        try {
            inventoryConfig.save(inventoryFile);
            plugin.getLogger().fine("Saved inventory [" + inventoryKey + "] for player " + player.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save inventory [" + inventoryKey + "] for player " + player.getName(), e);
        }
    }

    @SuppressWarnings("unchecked") // Suppress warning for List<ItemStack> cast
    public void loadInventory(Player player, String inventoryKey) {
        File inventoryFile = getInventoryFile(player, inventoryKey);
        if (!inventoryFile.exists()) {
            // No specific inventory saved, clear the player's current inventory
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]); // Size based on standard armor slots
            player.getInventory().setExtraContents(new ItemStack[1]); // Size based on standard extra slots (offhand)
            player.getEnderChest().clear(); // Clear Ender Chest too
            player.setLevel(0);
            player.setExp(0f);
            // Reset health/food to defaults
            // Use Attribute API for max health if available and needed
            try {
                 player.setHealth(player.getMaxHealth()); // Use deprecated method for broader compatibility
            } catch (Exception e) {
                 player.setHealth(20.0); // Fallback
            }
            player.setFoodLevel(20);
            player.setSaturation(5.0f); // Default saturation for full food
            // Clear active potion effects
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

            plugin.getLogger().fine("No inventory file found for key [" + inventoryKey + "] for player " + player.getName() + ". Cleared inventory and stats.");
            return;
        }

        YamlConfiguration inventoryConfig = YamlConfiguration.loadConfiguration(inventoryFile);

        try {
            // Load Inventory Contents
            List<?> rawContent = inventoryConfig.getList("inventory.content");
            List<?> rawArmor = inventoryConfig.getList("inventory.armor");
            List<?> rawExtra = inventoryConfig.getList("inventory.extra");
            List<?> rawEnder = inventoryConfig.getList("enderchest.content"); // Load Ender Chest

            ItemStack[] content = (rawContent != null) ? rawContent.toArray(new ItemStack[0]) : new ItemStack[player.getInventory().getSize()];
            ItemStack[] armor = (rawArmor != null) ? rawArmor.toArray(new ItemStack[0]) : new ItemStack[4];
            ItemStack[] extra = (rawExtra != null) ? rawExtra.toArray(new ItemStack[0]) : new ItemStack[1];
            ItemStack[] ender = (rawEnder != null) ? rawEnder.toArray(new ItemStack[0]) : new ItemStack[player.getEnderChest().getSize()]; // Ender Chest contents

            // Ensure arrays are not null and have correct size before setting
            if (content.length != player.getInventory().getSize()) content = Arrays.copyOf(content, player.getInventory().getSize());
            if (armor.length != 4) armor = Arrays.copyOf(armor, 4);
            if (extra.length != 1) extra = Arrays.copyOf(extra, 1);
            if (ender.length != player.getEnderChest().getSize()) ender = Arrays.copyOf(ender, player.getEnderChest().getSize()); // Ensure Ender Chest size

            player.getInventory().setContents(content);
            player.getInventory().setArmorContents(armor);
            player.getInventory().setExtraContents(extra);
            player.getEnderChest().setContents(ender); // Set Ender Chest contents

            // Load Stats
            player.setLevel(inventoryConfig.getInt("stats.level", 0));
            player.setExp((float) inventoryConfig.getDouble("stats.exp", 0.0));
            player.setHealth(inventoryConfig.getDouble("stats.health", player.getMaxHealth())); // Use deprecated method for broader compatibility
            player.setFoodLevel(inventoryConfig.getInt("stats.food", 20));
            player.setSaturation((float) inventoryConfig.getDouble("stats.saturation", 5.0));

            // Clear active potion effects before applying saved ones (if saved)
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            // TODO: Add loading of potion effects if they were saved

            plugin.getLogger().fine("Loaded inventory [" + inventoryKey + "] for player " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading inventory [" + inventoryKey + "] for player " + player.getName() + ". Inventory might be corrupted or empty.", e);
            // Optionally clear inventory on error to prevent item duplication or issues
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setExtraContents(new ItemStack[1]);
            player.setLevel(0);
            player.setExp(0f);
        }
    }
}

