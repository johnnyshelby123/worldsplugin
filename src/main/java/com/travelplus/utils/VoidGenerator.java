package com.travelplus.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Generates empty chunks for a void world.
 * Optionally creates a small spawn platform.
 */
public class VoidGenerator extends ChunkGenerator {

    @Override
    public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
        // Return completely empty chunk data
        return Bukkit.createChunkData(world);
    }

    /**
     * Provides a fixed spawn location, creating a platform if necessary.
     * This ensures players don't immediately fall into the void.
     */
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        // Check if the spawn block exists, if not, create it.
        // This runs synchronously when the world is loaded/created.
        Block block = world.getBlockAt(0, 64, 0);
        if (block.getType() == Material.AIR || block.getType() == Material.VOID_AIR) {
            block.setType(Material.OBSIDIAN); // Or another suitable block like GLASS
            // Optionally create a larger platform
            // world.getBlockAt(1, 64, 0).setType(Material.OBSIDIAN);
            // world.getBlockAt(-1, 64, 0).setType(Material.OBSIDIAN);
            // world.getBlockAt(0, 64, 1).setType(Material.OBSIDIAN);
            // world.getBlockAt(0, 64, -1).setType(Material.OBSIDIAN);
        }
        // Return a location slightly above the platform
        return new Location(world, 0.5, 65.0, 0.5);
    }

    // Optional: Override other methods like generateBedrock, generateCaves etc. to ensure they do nothing
    // By default, they might not run if generateChunkData returns an empty chunk, but being explicit can help.

    @Override
    public boolean shouldGenerateNoise() {
        return false; // No need for noise generation
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false; // No surface generation
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false; // No bedrock
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false; // No caves
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false; // No decorations (trees, ores, etc.)
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false; // No mob spawning by default (can be overridden by plugins/server settings)
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false; // No structures (villages, temples, etc.)
    }
}

