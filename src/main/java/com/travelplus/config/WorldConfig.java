package com.travelplus.config;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.jetbrains.annotations.Nullable;

// Record to hold per-world settings
public record WorldConfig(
    boolean separateInventory,
    boolean opOnly,
    boolean respawnInWorld,
    @Nullable Difficulty difficulty, // Added for step 019
    @Nullable GameMode defaultGamemode // Added for step 019
) {
    // Default configuration if a world is not explicitly defined in config.yml
    // Defaults to null for difficulty/gamemode, meaning no change/server default
    public static final WorldConfig DEFAULTS = new WorldConfig(false, false, false, null, null);
}

