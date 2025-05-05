package com.travelplus;

import com.travelplus.config.WorldConfig;
import com.travelplus.commands.CreateWorldCommand;
import com.travelplus.commands.GetTravelSignCommand;
import com.travelplus.commands.ReloadCommand;
import com.travelplus.commands.SpawnCommand; // Added for /spawn
import com.travelplus.commands.TravelCommand;
import com.travelplus.listeners.PlayerListener;
import com.travelplus.listeners.SignListener;
import com.travelplus.managers.InventoryManager;
import com.travelplus.managers.LocationManager;
import com.travelplus.managers.TeleportManager;
import com.travelplus.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {

    // --- Configuration Data ---
    private List<String> allowedWorlds;
    private String spawnWorldName;
    private Map<String, WorldConfig> worldSettings;
    private Map<String, Location> customSpawnPoints;

    // --- Managers ---
    private InventoryManager inventoryManager;
    private TeleportManager teleportManager;
    private LocationManager locationManager;

    @Override
    public void onEnable() {
        // Initialize managers
        inventoryManager = new InventoryManager(this);
        teleportManager = new TeleportManager(this);
        locationManager = new LocationManager(this);

        // Setup utilities
        LocationUtils.setupUnsafeMaterials();

        // Load configuration FIRST
        saveDefaultConfig();
        reloadPluginConfig(); // Load config data

        // Auto-load worlds listed in config (NEW)
        loadConfiguredWorlds();

        // Apply settings to already loaded worlds (including newly loaded ones)
        applySettingsToAllLoadedWorlds();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        getLogger().info("TravellerPlus enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TravellerPlus disabled!");
    }

    private void registerCommands() {
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

        ReloadCommand reloadExecutor = new ReloadCommand(this);
        if (this.getCommand("tpreload") != null) {
            this.getCommand("tpreload").setExecutor(reloadExecutor);
        } else {
            getLogger().severe("Command /tpreload is not registered in plugin.yml!");
        }

        GetTravelSignCommand getSignExecutor = new GetTravelSignCommand(this);
        if (this.getCommand("gettravelsign") != null) {
            this.getCommand("gettravelsign").setExecutor(getSignExecutor);
            this.getCommand("gettravelsign").setTabCompleter(getSignExecutor);
        } else {
            getLogger().severe("Command /gettravelsign is not registered in plugin.yml!");
        }

        // Register Spawn command
        SpawnCommand spawnExecutor = new SpawnCommand(this);
        if (this.getCommand("spawn") != null) {
            this.getCommand("spawn").setExecutor(spawnExecutor);
            // No tab completer needed for /spawn
        } else {
            getLogger().severe("Command /spawn is not registered in plugin.yml!");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this, inventoryManager), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        // TeleportManager now registers itself as a listener in its constructor
    }

    // Method to load worlds listed in the config
    private void loadConfiguredWorlds() {
        getLogger().info("Attempting to auto-load worlds listed in TravellerPlus config...");
        Set<String> worldsToLoad = new HashSet<>();

        // Add worlds from allowed-worlds
        if (allowedWorlds != null) {
            worldsToLoad.addAll(allowedWorlds);
        }

        // Add worlds from world-settings keys
        if (worldSettings != null) {
            worldsToLoad.addAll(worldSettings.keySet());
        }

        // Add the global spawn world if defined
        if (spawnWorldName != null && !spawnWorldName.isEmpty()) {
            worldsToLoad.add(spawnWorldName);
        }

        if (worldsToLoad.isEmpty()) {
            getLogger().info("No worlds found in TravellerPlus config to auto-load.");
            return;
        }

        for (String worldName : worldsToLoad) {
            if (Bukkit.getWorld(worldName) == null) {
                getLogger().info("World \'" + worldName + "\' is not loaded. Attempting to load...");
                try {
                    World createdWorld = new WorldCreator(worldName).createWorld();
                    if (createdWorld != null) {
                        getLogger().info("Successfully loaded world: " + createdWorld.getName());
                        // Settings will be applied in applySettingsToAllLoadedWorlds()
                    } else {
                        // This might happen if the world folder doesn't exist or is corrupted
                        getLogger().warning("Failed to load world \'" + worldName + "\'. It might not exist or is configured incorrectly.");
                    }
                } catch (Exception e) {
                    getLogger().severe("An error occurred while trying to load world \'" + worldName + "\': " + e.getMessage());
                    e.printStackTrace(); // Log stack trace for debugging
                }
            } else {
                getLogger().fine("World \'" + worldName + "\' is already loaded.");
            }
        }
        getLogger().info("Finished attempting to auto-load configured worlds.");
    }

    // Method to apply settings to ALL currently loaded worlds based on config
    private void applySettingsToAllLoadedWorlds() {
        getLogger().info("Applying TravellerPlus settings to all loaded worlds...");
        for (World world : Bukkit.getWorlds()) {
            String worldNameLower = world.getName().toLowerCase();
            WorldConfig configToApply = getWorldConfig(worldNameLower); // Gets specific or default config
            applyWorldSettings(worldNameLower, configToApply);
        }
        getLogger().info("Finished applying settings to loaded worlds.");
    }


    public void reloadPluginConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        getLogger().info("[TravellerPlus Diagnostics] Reloading configuration...");

        // Load allowed worlds
        this.allowedWorlds = config.getStringList("allowed-worlds").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        getLogger().info("[TravellerPlus Diagnostics] Loaded allowed worlds (lowercase): " + this.allowedWorlds);

        // Load spawn world
        this.spawnWorldName = config.getString("spawn-world", null);
        if (this.spawnWorldName != null) {
            this.spawnWorldName = this.spawnWorldName.toLowerCase();
            getLogger().info("[TravellerPlus Diagnostics] Loaded spawn world (lowercase): " + this.spawnWorldName);
        } else {
            getLogger().info("[TravellerPlus Diagnostics] No spawn-world defined in config.");
        }

        // Load per-world settings
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
                    boolean respawnInWorld = worldSection.getBoolean("respawn-in-world", WorldConfig.DEFAULTS.respawnInWorld()); // Still load, though respawn logic changed
                    boolean pvp = worldSection.getBoolean("pvp", WorldConfig.DEFAULTS.pvp());
                    boolean spawnMobs = worldSection.getBoolean("spawn-mobs", WorldConfig.DEFAULTS.spawnMobs());

                    Difficulty difficulty = WorldConfig.DEFAULTS.difficulty();
                    String difficultyStr = worldSection.getString("difficulty");
                    if (difficultyStr != null && !difficultyStr.isEmpty()) {
                        try {
                            difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("[TravellerPlus Diagnostics] Invalid difficulty value \'" + difficultyStr + "\' for world \'" + worldNameKey + "\". Using server default.");
                        }
                    }

                    GameMode defaultGamemode = WorldConfig.DEFAULTS.defaultGamemode();
                    String gamemodeStr = worldSection.getString("default-gamemode");
                    if (gamemodeStr != null && !gamemodeStr.isEmpty()) {
                        try {
                            defaultGamemode = GameMode.valueOf(gamemodeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("[TravellerPlus Diagnostics] Invalid default-gamemode value \'" + gamemodeStr + "\' for world \'" + worldNameKey + "\". Using no change.");
                        }
                    }

                    WorldConfig loadedConfig = new WorldConfig(separateInv, opOnly, respawnInWorld, difficulty, defaultGamemode, pvp, spawnMobs);
                    worldSettings.put(worldNameLower, loadedConfig);
                    getLogger().info("[TravellerPlus Diagnostics] Loaded settings for world: " + worldNameLower +
                                       " (SeparateInv: " + separateInv + ", OpOnly: " + opOnly + ", RespawnInWorld: " + respawnInWorld +
                                       ", Difficulty: " + (difficulty != null ? difficulty.name() : "DEFAULT") +
                                       ", Gamemode: " + (defaultGamemode != null ? defaultGamemode.name() : "NONE") +
                                       ", PVP: " + pvp + ", SpawnMobs: " + spawnMobs + ")");

                    // Settings are now applied later in onEnable or after reload command

                } else {
                    getLogger().warning("[TravellerPlus Diagnostics] Invalid section for world \'" + worldNameKey + "\' under world-settings. Skipping.");
                }
            }
        } else {
            getLogger().warning("[TravellerPlus Diagnostics] No \'world-settings\' section found in config. Default settings will apply to all worlds.");
        }

        // Load custom spawn points
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
                        customSpawnPoints.put(worldNameLower, new Location(null, x, y, z, yaw, pitch));
                        getLogger().info("[TravellerPlus Diagnostics] Loaded custom spawn for: " + worldNameLower);
                    } catch (Exception e) {
                        getLogger().warning("[TravellerPlus Diagnostics] Error reading custom spawn coords for world \'" + worldNameKey + "\". Skipping. Error: " + e.getMessage());
                    }
                }
            }
        } else {
            getLogger().info("[TravellerPlus Diagnostics] No \'custom-spawn-points\' section found in config.");
        }
        getLogger().info("[TravellerPlus Diagnostics] Finished loading config. Allowed: " + (allowedWorlds != null ? allowedWorlds.size() : 0) +
                ", WorldSettings defined: " + worldSettings.size() +
                ", CustomSpawns: " + customSpawnPoints.size());

        // After reloading config, re-apply settings to all currently loaded worlds
        applySettingsToAllLoadedWorlds();
    }

    // Method to apply settings to a specific world
    public void applyWorldSettings(String worldNameLower, WorldConfig config) {
        World world = Bukkit.getWorld(worldNameLower);
        if (world != null) {
            getLogger().info("Applying settings to world: " + world.getName());

            // Apply Difficulty
            if (config.difficulty() != null && world.getDifficulty() != config.difficulty()) {
                world.setDifficulty(config.difficulty());
                getLogger().info(" -> Set difficulty to " + config.difficulty().name());
            }

            // Apply PVP
            if (world.getPVP() != config.pvp()) {
                world.setPVP(config.pvp());
                getLogger().info(" -> Set PVP to " + config.pvp());
            }

            // Apply Mob Spawning
            boolean currentMonsterSpawns = world.getAllowMonsters();
            boolean currentAnimalSpawns = world.getAllowAnimals();
            if (currentMonsterSpawns != config.spawnMobs() || currentAnimalSpawns != config.spawnMobs()) {
                 try {
                     world.setSpawnFlags(config.spawnMobs(), config.spawnMobs());
                     getLogger().info(" -> Set monster/animal spawn flags to " + config.spawnMobs());
                 } catch (NoSuchMethodError e) {
                     world.setMonsterSpawnLimit(config.spawnMobs() ? world.getMonsterSpawnLimit() : 0);
                     world.setAnimalSpawnLimit(config.spawnMobs() ? world.getAnimalSpawnLimit() : 0);
                     getLogger().warning(" -> Using fallback method to set mob spawning to " + config.spawnMobs() + " (setSpawnFlags not available)");
                 }
            }

        } else {
            // Don't log warning here, as this method is called for all loaded worlds,
            // including potentially default ones not in our config.
            // getLogger().warning("Tried to apply settings to world \'" + worldNameLower + "\', but it is not loaded.");
        }
    }

    // --- Getters ---

    public List<String> getAllowedWorlds() {
        return allowedWorlds != null ? new ArrayList<>(allowedWorlds) : new ArrayList<>();
    }

    @NotNull
    public WorldConfig getWorldConfig(String worldNameLower) {
        return worldSettings.getOrDefault(worldNameLower.toLowerCase(), WorldConfig.DEFAULTS);
    }

    public boolean isSeparateInventoryWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).separateInventory();
    }

    public boolean isOpOnlyWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).opOnly();
    }

    // This setting is no longer directly used by respawn logic but kept for potential future use
    public boolean shouldRespawnInWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).respawnInWorld();
    }

    @Nullable
    public Difficulty getDifficultyForWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).difficulty();
    }

    @Nullable
    public GameMode getDefaultGamemodeForWorld(String worldNameLower) {
        return getWorldConfig(worldNameLower).defaultGamemode();
    }

    public boolean isPvpEnabled(String worldNameLower) {
        return getWorldConfig(worldNameLower).pvp();
    }

    public boolean areMobsSpawning(String worldNameLower) {
        return getWorldConfig(worldNameLower).spawnMobs();
    }

    public Map<String, Location> getCustomSpawnPoints() {
        return new HashMap<>(customSpawnPoints);
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    @Nullable
    public String getSpawnWorldName() {
        return spawnWorldName;
    }
}

