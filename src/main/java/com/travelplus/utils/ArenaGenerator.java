package com.travelplus.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable; // Added missing import

import java.util.Random;

/**
 * Generates a void world with a 35x35 platform centered at 0,0.
 * Platform consists of bedrock at Y=64 and obsidian at Y=65.
 */
public class ArenaGenerator extends ChunkGenerator {

    private static final int PLATFORM_RADIUS = 17; // (35 - 1) / 2
    private static final int BEDROCK_Y = 64;
    private static final int OBSIDIAN_Y = 65;
    private static final int SPAWN_Y = 66;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // No noise needed for a void/platform world
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Generate the platform surface if the chunk is within range
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                if (Math.abs(worldX) <= PLATFORM_RADIUS && Math.abs(worldZ) <= PLATFORM_RADIUS) {
                    chunkData.setBlock(x, BEDROCK_Y, z, Material.BEDROCK);
                    chunkData.setBlock(x, OBSIDIAN_Y, z, Material.OBSIDIAN);
                }
            }
        }
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Bedrock is handled by generateSurface for the platform
        // No additional bedrock needed for void
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true; // We need this to generate the platform
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false; // Typically no mobs in arena void unless intended
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Nullable
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        // Spawn players slightly above the center of the platform
        return new Location(world, 0.5, SPAWN_Y, 0.5);
    }
}

