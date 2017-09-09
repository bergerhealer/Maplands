package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.MathUtil;

public class MapUtil {
    /**
     * Checks whether a particular set of tile coordinates if a valid tile
     * 
     * @param tx
     * @param ty
     * @param tz
     * @return True if the coordinates represent a valid tile
     */
    public static boolean isTile(int tx, int ty, int tz) {
        return MathUtil.floorMod((tx * 3) + (ty * 2) + (-tz * 1), 6) == 4;
    }

    /**
     * Performs a mathematical operation to turn tile coordinates into block coordinates
     * 
     * @param facing view
     * @param p tile coordinates
     * @return block coordinates. Null if not a valid tile.
     */
    public static IntVector3 screenTileToBlock(BlockFace facing, IntVector3 p) {
        // Checks whether the tile coordinates are valid
        if (!MapUtil.isTile(p.x, p.y, p.z)) {
            return null;
        }

        int px_div2 = MathUtil.floorDiv(p.x, 2);
        int py_div3 = MathUtil.floorDiv(p.y, 3);
        int pz_div3 = MathUtil.floorDiv(p.z, 3);

        int dxz_fact;
        if ((p.x & 0x1) == 0x1) {
            dxz_fact = MathUtil.floorDiv((p.z + 2), 6);            
        } else {
            dxz_fact = MathUtil.floorDiv((p.z + 5), 6);
        }

        int dx = py_div3 + px_div2 - dxz_fact;
        int dy = -pz_div3 - py_div3;
        int dz = dx - dy - p.y;

        // Move to center block
        //dx += 1;
        //dz -= 3;
        dx += 1;
        dz += 1;

        if (facing == BlockFace.NORTH_EAST) {
            return new IntVector3(dx, dy, dz);
        } else if (facing == BlockFace.SOUTH_WEST) {
            return new IntVector3(-dx, dy, -dz);
        } else if (facing == BlockFace.NORTH_WEST) {
            return new IntVector3(dz, dy, -dx);
        } else if (facing == BlockFace.SOUTH_EAST) {
            return new IntVector3(-dz, dy, dx);
        } else {
            return null;
        }
    }
}
