package com.travelplus.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class LocationUtils {

    private static Set<Material> unsafeFloorMaterials;

    public static void setupUnsafeMaterials() {
        unsafeFloorMaterials = new HashSet<>();
        unsafeFloorMaterials.add(Material.LAVA);
        unsafeFloorMaterials.add(Material.WATER);
        unsafeFloorMaterials.add(Material.MAGMA_BLOCK);
        unsafeFloorMaterials.add(Material.POINTED_DRIPSTONE);
        // unsafeFloorMaterials.add(Material.BEDROCK); // Bedrock is usually safe to stand on
        unsafeFloorMaterials.add(Material.AIR);
        unsafeFloorMaterials.add(Material.CAVE_AIR);
        unsafeFloorMaterials.add(Material.VOID_AIR);
        unsafeFloorMaterials.add(Material.FIRE);
        unsafeFloorMaterials.add(Material.SOUL_FIRE);
        unsafeFloorMaterials.add(Material.CACTUS);
        unsafeFloorMaterials.add(Material.SWEET_BERRY_BUSH);
        unsafeFloorMaterials.add(Material.WITHER_ROSE);
        unsafeFloorMaterials.add(Material.POWDER_SNOW);
        // Ores might be undesirable but not strictly unsafe to spawn on
        // for (Material mat : Material.values()) {
        //     if (mat.name().endsWith("_ORE") || mat.name().startsWith("DEEPSLATE_") || mat.name().equals("ANCIENT_DEBRIS")) {
        //         unsafeFloorMaterials.add(mat);
        //     }
        // }
    }

    @Nullable
    public static Location findTrulySafeLocation(Location center, int radius) {
        if (unsafeFloorMaterials == null) {
            setupUnsafeMaterials(); // Ensure initialized
        }

        World world = center.getWorld();
        if (world == null) return null;

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int startY = center.getBlockY();

        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 1;

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) < r && Math.abs(dz) < r) continue;

                    int currentX = centerX + dx;
                    int currentZ = centerZ + dz;

                    for (int yOffset = 0; yOffset <= 10; yOffset++) {
                        int checkYUp = startY + yOffset;
                        int checkYDown = startY - yOffset;

                        if (checkYUp <= maxY) {
                            Location potentialLocUp = checkLocationSafety(world, currentX, checkYUp, currentZ);
                            if (potentialLocUp != null) {
                                potentialLocUp.setYaw(center.getYaw());
                                potentialLocUp.setPitch(center.getPitch());
                                return potentialLocUp;
                            }
                        }

                        if (yOffset > 0 && checkYDown >= minY) {
                            Location potentialLocDown = checkLocationSafety(world, currentX, checkYDown, currentZ);
                            if (potentialLocDown != null) {
                                potentialLocDown.setYaw(center.getYaw());
                                potentialLocDown.setPitch(center.getPitch());
                                return potentialLocDown;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Location checkLocationSafety(World world, int x, int y, int z) {
        Block floorBlock = world.getBlockAt(x, y - 1, z);
        Block feetBlock = world.getBlockAt(x, y, z);
        Block headBlock = world.getBlockAt(x, y + 1, z);

        if (!floorBlock.getType().isSolid() || unsafeFloorMaterials.contains(floorBlock.getType())) {
            return null;
        }
        if (!feetBlock.isPassable() || feetBlock.isLiquid() ||
                !headBlock.isPassable() || headBlock.isLiquid()) {
            return null;
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}

