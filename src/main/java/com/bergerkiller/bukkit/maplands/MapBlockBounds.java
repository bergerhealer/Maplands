package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;

/**
 * Stores and computes the range of x/y/z block coordinates
 * within which a tile could possibly be rendered on the map.
 * Acts as a shortcut for block physics handling.
 */
public final class MapBlockBounds {
    private int min_x, min_y, min_z;
    private int max_x, max_y, max_z;

    public boolean contains(int bx, int by, int bz) {
        return bx >= min_x && bx <= max_x &&
               by >= min_y && by <= max_y &&
               bz >= min_z && bz <= max_z;
    }

    public void update(BlockFace facing,
            int minTileX, int minTileY, int minTileZ,
            int maxTileX, int maxTileY, int maxTileZ)
    {
        boolean is_first = true;
        int[] x_range = sign_range(minTileX, maxTileX);
        int[] y_range = sign_range(minTileY, maxTileY);
        int[] z_range = sign_range(minTileZ, maxTileZ);
        for (int mx : x_range) {
            for (int my : y_range) {
                for (int mz : z_range) {
                    IntVector3 b = MapUtil.screenTileToBlock(facing, mx, my, mz);
                    if (b == null) {
                        continue;
                    }
                    if (is_first) {
                        is_first = false;
                        min_x = max_x = b.x;
                        min_y = max_y = b.y;
                        min_z = max_z = b.z;
                    } else {
                        if (b.x < min_x) min_x = b.x;
                        if (b.y < min_y) min_y = b.y;
                        if (b.z < min_z) min_z = b.z;
                        if (b.x > max_x) max_x = b.x;
                        if (b.y > max_y) max_y = b.y;
                        if (b.z > max_z) max_z = b.z;
                    }
                }
            }
        }
        if (is_first) {
            // Invalid?!
            min_x = max_x = 0;
            min_y = max_y = 0;
            min_z = max_z = 0;
        }
    }

    // Old update function that looped all tiles in the cuboid area
    // It was very slow for large displays
    public void update_old_slow(BlockFace facing,
            int minTileX, int minTileY, int minTileZ,
            int maxTileX, int maxTileY, int maxTileZ)
    {
        boolean is_first = true;
        for (int mx = minTileX; mx <= maxTileX; mx++) {
            for (int my = minTileY; my <= maxTileY; my++) {
                for (int mz = minTileZ; mz <= maxTileZ; mz++) {
                    IntVector3 b = MapUtil.screenTileToBlock(facing, mx, my, mz);
                    if (b == null) {
                        continue;
                    }
                    if (is_first) {
                        is_first = false;
                        min_x = max_x = b.x;
                        min_y = max_y = b.y;
                        min_z = max_z = b.z;
                    } else {
                        if (b.x < min_x) min_x = b.x;
                        if (b.y < min_y) min_y = b.y;
                        if (b.z < min_z) min_z = b.z;
                        if (b.x > max_x) max_x = b.x;
                        if (b.y > max_y) max_y = b.y;
                        if (b.z > max_z) max_z = b.z;
                    }
                }
            }
        }
        if (is_first) {
            // Invalid?!
            min_x = max_x = 0;
            min_y = max_y = 0;
            min_z = max_z = 0;
        }
    }

    public void offset(Block startBlock) {
        min_x += startBlock.getX();
        min_y += startBlock.getY();
        min_z += startBlock.getZ();
        max_x += startBlock.getX();
        max_y += startBlock.getY();
        max_z += startBlock.getZ();
    }

    // Creates an array that contains the first two and last two significant values in a range
    // Speeds up the for loops a bit
    private static int[] sign_range(int a, int b) {
        int diff = (b-a);
        if (diff < 0) {
            return new int[0];
        } else {
            switch (diff) {
            case 0:
                return new int[] {a};
            case 1:
                return new int[] {a, b};
            case 2:
                return new int[] {a, a+1, b};
            default:
                return new int[] {a, a+1, b-1, b};
            }
        }
    }

    @Override
    public String toString() {
        return "[" + min_x + "/" + min_y + "/" + min_z + "] -> [" + max_x + "/" + max_y + "/" + max_z + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof MapBlockBounds) {
            MapBlockBounds other = (MapBlockBounds) o;
            return this.min_x == other.min_x &&
                   this.min_y == other.min_y &&
                   this.min_z == other.min_z &&
                   this.max_x == other.max_x &&
                   this.max_y == other.max_y &&
                   this.max_z == other.max_z;
        } else {
            return false;
        }
    }
}
