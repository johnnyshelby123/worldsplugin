package com.travelplus.config;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.jetbrains.annotations.Nullable;

// Record to hold per-world settings
public record WorldConfig(
    boolean separateInventory,
    boolean opOnly,
    boolean respawnInWorld,
    @Nullable Difficulty difficulty,
    @Nullable GameMode defaultGamemode,
    boolean pvp, // Added for PVP setting
    boolean spawnMobs // Added for Mob Spawning setting
) {
    // Default configuration if a world is not explicitly defined in config.yml
    // Defaults to null for difficulty/gamemode (no change/server default)
    // Defaults PVP and Mob Spawning to true (standard Minecraft behavior)
    public static final WorldConfig DEFAULTS = new WorldConfig(false, false, false, null, null, true, true);
}

