package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.MathUtil;

public class MapUtil {
    /**
     * Checks whether a particular set of tile coordinates if a valid tile
     * 
     * @param tx Tile x-coordinate
     * @param ty Tile y-coordinate
     * @param tz Tile depth
     * @return True if the coordinates represent a valid tile
     */
    public static boolean isTile(int tx, int ty, int tz) {
        return MathUtil.floorMod((tx * 3) + (tz * 2) + (-ty * 1), 6) == 4;
    }

    /**
     * Gets the depth at which this tile is drawn, modulus 3.
     * Since tiles repeat every 3 depth levels, this allows for
     * categorizing the tile in one of 3 depth positions.<br>
     * <br>
     * Some tile x/y coordinates are invalid hexagons that will
     * never be drawn. For these tiles, -1 will be returned.
     *
     * @param tx Tile x-coordinate
     * @param ty Tile y-coordinate
     * @return depth % 3, or -1 if the tile is invalid
     */
    public static int getTileDepthModThree(int tx, int ty) {
        int tmp = ((tx * 3) - ty) % 6;
        if ((tmp & 0x1) != 0) {
            return -1; // Must always be a multiple of 2
        } else if (tmp < 0) {
            return -1 - (tmp/2); // Account for negative modulus
        } else {
            return 2 - (tmp/2); // Normal check
        }
    }

    public static int getTilePXY(int px, int py) {
        int dxz_fact;
        if ((px & 0x1) == 0x1) {
            dxz_fact = MathUtil.floorDiv((py + 2), 6);
        } else {
            dxz_fact = MathUtil.floorDiv((py + 5), 6);
        }
        return MathUtil.floorDiv(px, 2) - dxz_fact;
    }

    /**
     * Performs a mathematical operation to turn tile coordinates into block coordinates
     * 
     * @param facing view
     * @param p tile coordinates
     * @return block coordinates. Null if not a valid tile.
     */
    public static IntVector3 screenTileToBlock(BlockFace facing, IntVector3 p) {
        return screenTileToBlock(facing, p.x, p.y, p.z);
    }

    /**
     * Performs a mathematical operation to turn tile coordinates into block coordinates
     * 
     * @param facing View direction
     * @param px tile x-coordinate
     * @param py tile y-coordinate
     * @param pz depth
     * @return block coordinates. Null if not a valid tile.
     */
    public static IntVector3 screenTileToBlock(BlockFace facing, int px, int py, int pz) {
        // Checks whether the tile coordinates are valid
        if (!MapUtil.isTile(px, py, pz)) {
            return null;
        }

        int py_div3 = MathUtil.floorDiv(py, 3);
        int pz_div3 = MathUtil.floorDiv(pz, 3);

        int dx = pz_div3 + getTilePXY(px, py);
        int dy = -py_div3 - pz_div3;
        int dz = dx - dy - pz;

        // Move to center block
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

    /**
     * Performs a mathematical operation to turn block coordinates into tile coordinates
     * 
     * @param facing view
     * @param blockPos relative block coordinates
     * @return output tile coordinates
     */
    public static IntVector3 blockToScreenTile(BlockFace facing, IntVector3 blockPos) {
        return blockToScreenTile(facing, blockPos.x, blockPos.y, blockPos.z);
    }

    /**
     * Performs a mathematical operation to turn block coordinates into tile coordinates
     * 
     * @param facing view
     * @param dx relative block coordinate x
     * @param dy relative block coordinate y
     * @param dz relative block coordinate z
     * @return tile coordinates (z is depth)
     */
    public static IntVector3 blockToScreenTile(BlockFace facing, int dx, int dy, int dz) {
        // Undo facing
        if (facing == BlockFace.NORTH_EAST) {
            // nothing
        } else if (facing == BlockFace.SOUTH_WEST) {
            dx = -dx;
            dz = -dz;
        } else if (facing == BlockFace.NORTH_WEST) {
            int dx_old = dx;
            dx = -dz;
            dz = dx_old;
        } else if (facing == BlockFace.SOUTH_EAST) {
            int dx_old = dx;
            dx = dz;
            dz = -dx_old;
        }

        // Undo move to center block
        dx -= 1;
        dz -= 1;

        // Find py using: dz = dx - dy - py
        int pz = dx - dy - dz;
        int py_div3 = MathUtil.floorDiv(pz, 3);

        // Find pz_div3 using: dy = -pz_div3 - py_div3
        int pz_div3 = -py_div3 - dy;

        // Find tilePXZ using: dx = py_div3 + tilePXZ;
        int tilePXZ = dx - py_div3;

        // Try values of px/pz until we find our result
        // We use the equation of getTilePXZ to limit our search space using the known pz_div3
        int px_start = 2 * (tilePXZ + (MathUtil.floorDiv(pz_div3, 2)));
        int px_end = px_start + 3;
        int pz_start = pz_div3 * 3;
        int pz_end = pz_start + 3;
        int px = px_start;
        int py = pz_start;
        searchloop:
        for (py = pz_start; py < pz_end; py++) {
            for (px = px_start; px <= px_end; px++) {
                if (isTile(px, py, pz) && getTilePXY(px, py) == tilePXZ) {
                    break searchloop;
                }
            }
        }
        return new IntVector3(px, py, pz);
    }
}
