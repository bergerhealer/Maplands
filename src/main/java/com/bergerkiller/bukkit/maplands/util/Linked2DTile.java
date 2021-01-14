package com.bergerkiller.bukkit.maplands.util;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.maplands.MapUtil;

/**
 * A single tile of a {@link Linked2DTileSet}
 * which has references to the previous and next element
 * for iteration and insertion purposes.
 */
public final class Linked2DTile {
    public final int x;
    public final int y;
    private final int depthModThree;
    private final int toBlock_pxy;
    public Linked2DTile prev = null;
    public Linked2DTile next = null;

    public Linked2DTile(int x, int y) {
        this.x = x;
        this.y = y;
        this.toBlock_pxy = MapUtil.getTilePXY(x, y) + 1;

        // Compute depth % 3 that this tile is drawn on
        // Is -1 when this tile is invalid
        this.depthModThree = MapUtil.getTileDepthModThree(x, y);
    }

    public boolean isSet() {
        return this.next != null;
    }

    /**
     * Gets the depth at which this tile is drawn, modulus 3.
     * Since tiles repeat every 3 depth levels, this allows for
     * categorizing the tile in one of 3 depth positions.<br>
     * <br>
     * Some tile x/y coordinates are invalid hexagons that will
     * never be drawn. For these tiles, -1 will be returned.
     *
     * @return depth % 3 at which the tile is drawn, -1 if not
     *         a valid tile.
     * @see MapUtil#isTile(int, int, int)
     */
    public int getDepthModThree() {
        return this.depthModThree;
    }

    /**
     * Gets the world block coordinates that displays the block drawn
     * at this tile for a given depth level. Before calling, make
     * sure to check the depth is correct using {@link #getDepthModThree()}.
     *
     * @param facing View facing
     * @param depth Depth z-coordinate
     * @return Block coordinates
     * @see MapUtil#screenTileToBlock(BlockFace, int, int, int)
     */
    public IntVector3 toBlock(BlockFace facing, int depth) {
        int py_div3 = Math.floorDiv(this.y, 3);
        int pz_div3 = Math.floorDiv(depth, 3);

        int dx = pz_div3 + this.toBlock_pxy;
        int dy = -py_div3 - pz_div3;
        int dz = dx - dy - depth;

        switch (facing) {
        case NORTH_EAST:
            return new IntVector3(dx, dy, dz);
        case SOUTH_WEST:
            return new IntVector3(-dx, dy, -dz);
        case NORTH_WEST:
            return new IntVector3(dz, dy, -dx);
        case SOUTH_EAST:
            return new IntVector3(-dz, dy, dx);
        default:
            throw new IllegalArgumentException("Unsupported facing: " + facing);
        }
    }

    /**
     * Removes this tile in the linked list. The previous
     * tile of this tile will link up with the next tile of this
     * tile, and vice-versa. Future iteration will exclude
     * this tile.
     *
     * @return The previous tile
     */
    public Linked2DTile remove() {
        Linked2DTile prev = this.prev;

        this.prev.next = this.next;
        this.next.prev = this.prev;
        this.prev = null;
        this.next = null;

        return prev;
    }

    /**
     * Links two tiles together so that {@link #next} of <i>a</i> refers
     * to b, and {@link #prev} of <i>b</i> refers to a.
     *
     * @param a First tile
     * @param b Second tile
     */
    public static void link(Linked2DTile a, Linked2DTile b) {
        a.next = b;
        b.prev = a;
    }

    @Override
    public String toString() {
        return "Linked2DTile{x=" + x + ", y=" + y + "}";
    }
}
