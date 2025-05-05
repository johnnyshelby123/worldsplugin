package com.travelplus;

import com.travelplus.config.WorldConfig;
import com.travelplus.commands.CreateWorldCommand;
import com.travelplus.commands.GetTravelSignCommand; // Added for sign command
import com.travelplus.commands.ReloadCommand;
import com.travelplus.commands.TravelCommand;
import com.travelplus.listeners.PlayerListener;
import com.travelplus.listeners.SignListener; // Added for travel signs
import com.travelplus.managers.InventoryManager;
import com.travelplus.managers.LocationManager; // Added for last location memory
import com.travelplus.managers.TeleportManager; // Added for teleport countdown
import com.travelplus.utils.LocationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Location; // Added missing import
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {

    // --- Configuration Data ---
    private List<String> allowedWorlds; // Still used for /travel validation/tab-completion
    private String spawnWorldName; // Global setting
    private Map<String, WorldConfig> worldSettings; // Per-world settings (Refactored in step 014)
    private Map<String, Location> customSpawnPoints; // Per-world custom spawns

    // --- Managers ---
    private InventoryManager inventoryManager;
    private TeleportManager teleportManager; // Added for teleport countdown
    private LocationManager locationManager; // Added for last location memory

    @Override
    public void onEnable() {
        // Initialize managers
        inventoryManager = new InventoryManager(this);
        teleportManager = new TeleportManager(this); // Initialize TeleportManager
        locationManager = new LocationManager(this); // Initialize LocationManager

        // Setup utilities
        LocationUtils.setupUnsafeMaterials();

        // Load configuration
        saveDefaultConfig();
        reloadPluginConfig(); // Use the dedicated method

        // Register commands
        TravelCommand travelExecutor = new TravelCommand(this);
        if (this.getCommand("travel") != null) {
            this.getCommand("travel").setExecutor(travelExecutor);
            this.getCommand("travel").setTabCompleter(travelExecutor);
        } else {
            getLogger().severe("Command /travel is not registered in plugin.yml!");
        }

        CreateWorldCommand createWorldExecutor = new CreateWorldCommand(this);
        if (this.getCommand("createworld") != null) {
            this.getCommand("createworld").setExecutor(createWorldExecutor);
            this.getCommand("createworld").setTabCompleter(createWorldExecutor);
        } else {
            getLogger().severe("Command /createworld is not registered in plugin.yml!");
        }

        // Register reload command
        ReloadCommand reloadExecutor = new ReloadCommand(this);
        if (this.getCommand("tpreload") != null) {
            this.getCommand("tpreload").setExecutor(reloadExecutor);
            // No tab completer needed for reload command
        } else {
            getLogger().severe("Command /tpreload is not registered in plugin.yml!");
        }

        // Register gettravelsign command (New)
        GetTravelSignCommand getSignExecutor = new GetTravelSignCommand(this);
        if (this.getCommand("gettravelsign") != null) {
            this.getCommand("gettravelsign").setExecutor(getSignExecutor);
            this.getCommand("gettravelsign").setTabCompleter(getSignExecutor);
        } else {
            getLogger().severe("Command /gettravelsign is not registered in plugin.yml!");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, inventoryManager), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this); // Added for travel signs

        getLogger().info("TravellerPlus enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup if needed
        getLogger().info("TravellerPlus disabled!");
    }

    // Renamed from reloadConfiguration to avoid conflict and be more descriptive
    public void reloadPluginConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        getLogger().info("[TravellerPlus Diagnostics] Reloading configuration...");

        // Load allowed worlds (Still used for /travel validation/tab-completion)
        this.allowedWorlds = config.getStringList("allowed-worlds").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        getLogger().info("[TravellerPlus Diagnostics] Loaded allowed worlds (lowercase): " + this.allowedWorlds);

        // Load spawn world (Global setting)
        this.spawnWorldName = config.getString("spawn-world", null);
        if (this.spawnWorldName != null) {
            this.spawnWorldName = this.spawnWorldName.toLowerCase();
            getLogger().info("[TravellerPlus Diagnostics] Loaded spawn world (lowercase): " + this.spawnWorldName);
        } else {
            getLogger().info("[TravellerPlus Diagnostics] No spawn-world defined in config.");
        }

        // Load per-world settings (Refactored in step 014)
        this.worldSettings = new HashMap<>();
        ConfigurationSection worldSettingsSection = config.getConfigurationSection("world-settings");
        if (worldSettingsSection != null) {
            getLogger().info("[TravellerPlus Diagnostics] Loading per-world settings...");
            for (String worldNameKey : worldSettingsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldSettingsSection.getConfigurationSection(worldNameKey);
                if (worldSection != null) {
                    String worldNameLower = worldNameKey.toLowerCase();
                    boolean separateInv = worldSection.getBoolean("separate-inventory", WorldConfig.DEFAULTS.separateInventory());
                    boolean opOnly = worldSection.getBoolean("op-only", WorldConfig.DEFAULTS.opOnly());
                    boolean respawnInWorld = worldSection.getBoolean("respawn-in-world", WorldConfig.DEFAULTS.respawnInWorld());

                    // Load Difficulty (Added for step 019)
                    Difficulty difficulty = null;
                    String difficultyStr = worldSection.getString("difficulty");
                    if (difficultyStr != null && !difficultyStr.isEmpty()) {
                        try {
                            difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("[TravellerPlus Diagnostics] Invalid difficulty value \'" + difficultyStr + "\' for world \'" + worldNameKey + "\'. Using server default.");
                        }
                    }

                    // Load Default Gamemode (Added for step 019)
                    GameMode defaultGamemode = null;
                    String gamemodeStr = worldSection.getString("default-gamemode");
                    if (gamemodeStr != null && !gamemodeStr.isEmpty()) {
                        try {
                            defaultGamemode = GameMode.valueOf(gamemodeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("[TravellerPlus Diagnostics] Invalid default-gamemode value \'" + gamemodeStr + "\' for world \'" + worldNameKey + "\'. Using no change.");
                        }
                    }

                    worldSettings.put(worldNameLower, new WorldConfig(separateInv, opOnly, respawnInWorld, difficulty, defaultGamemode));
                    getLogger().info("[TravellerPlus Diagnostics] Loaded settings for world: " + worldNameLower +
                                       " (SeparateInv: " + separateInv + ", OpOnly: " + opOnly + ", RespawnInWorld: " + respawnInWorld +
                                       ", Difficulty: " + (difficulty != null ? difficulty.name() : "DEFAULT") +
                                       ", Gamemode: " + (defaultGamemode != null ? defaultGamemode.name() : "NONE") + ")");
                } else {
                    getLogger().warning("[TravellerPlus Diagnostics] Invalid section for world \'" + worldNameKey + "\' under world-settings. Skipping.");
                }
            }
        } else {
            getLogger().warning("[TravellerPlus Diagnostics] No \'world-settings\' section found in config. Default settings will apply to all worlds.");
        }

        // Load custom spawn points (Remains the same)
        this.customSpawnPoints = new HashMap<>();
        ConfigurationSection customSpawnsSection = config.getConfigurationSection("custom-spawn-points");
        if (customSpawnsSection != null) {
            getLogger().info("[TravellerPlus Diagnostics] Loading custom spawn points...");
            for (String worldNameKey : customSpawnsSection.getKeys(false)) {
                ConfigurationSection worldSection = customSpawnsSection.getConfigurationSection(worldNameKey);
                if (worldSection != null) {
                    String worldNameLower = worldNameKey.toLowerCase();
                    if (!worldSection.contains("x") || !worldSection.contains("y") || !worldSection.contains("z")) {
                        getLogger().warning("[TravellerPlus Diagnostics] Custom spawn for world \'" + worldNameKey + "\' is missing coords. Skipping.");
                        continue;
                    }
                    try {
                        double x = worldSection.getDouble("x");
                        double y = worldSection.getDouble("y");
                        double z = worldSection.getDouble("z");
                        float yaw = (float) worldSection.getDouble("yaw", 0.0);
                        float pitch = (float) worldSection.getDouble("pitch", 0.0);
                        // Location needs a world, but we don't have it loaded yet. Store coords only.
                        // The command executor will resolve the world later.
                        customSpawnPoints.put(worldNameLower, new Location(null, x, y, z, yaw, pitch));
                        getLogger().info("[TravellerPlus Diagnostics] Loaded custom spawn for: " + worldNameLower);
                    } catch (Exception e) {
                        getLogger().warning("[TravellerPlus Diagnostics] Error reading custom spawn coords for world \'" + worldNameKey + "\'. Skipping. Error: " + e.getMessage());
                    }
                }
            }
        } else {
            getLogger().info("[TravellerPlus Diagnostics] No \'custom-spawn-points\' section found in config.");
        }
        getLogger().info("[TravellerPlus Diagnostics] Finished loading config. Allowed: " + allowedWorlds.size() +
                ", WorldSettings defined: " + worldSettings.size() +
                ", CustomSpawns: " + customSpawnPoints.size());
    }

    // --- Getters for config values needed by other classes ---

    public List<String> getAllowedWorlds() {
        // Return a copy to prevent external modification
        return new ArrayList<>(allowedWorlds);
    }

    // Get WorldConfig for a specific world, returning defaults if not defined
    @NotNull
    public WorldConfig getWorldConfig(String worldNameLower) {
        return worldSettings.getOrDefault(worldNameLower.toLowerCase(), WorldConfig.DEFAULTS);
    }

    // Helper method for checking separate inventory status (Refactored in step 014)
    public boolean isSeparateInventoryWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).separateInventory();
    }

    // Helper method for checking OP-only status (Refactored in step 014)
    public boolean isOpOnlyWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).opOnly();
    }

    // Helper method for checking respawn-in-world status (Added for step 015)
    public boolean shouldRespawnInWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).respawnInWorld();
    }

    // Helper method for getting world difficulty (Added for step 019)
    @Nullable
    public Difficulty getDifficultyForWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).difficulty();
    }

    // Helper method for getting world default gamemode (Added for step 019)
    @Nullable
    public GameMode getDefaultGamemodeForWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).defaultGamemode();
    }

    public Map<String, Location> getCustomSpawnPoints() {
        // Return a copy
        return new HashMap<>(customSpawnPoints);
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    // Getter for TeleportManager (Added for countdown)
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    // Getter for LocationManager (Added for last location memory)
    public LocationManager getLocationManager() {
        return locationManager;
    }

    // Getter for spawn world (Added for step 013)   @Nullable
    public String getSpawnWorldName() {
        return spawnWorldName;
    }
}

