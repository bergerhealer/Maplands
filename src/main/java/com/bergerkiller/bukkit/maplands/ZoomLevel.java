package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A map zoom level that can be used
 */
public enum ZoomLevel {
    ZOOM8( 8,  11,  0.4f,  -51.2f),
    ZOOM16(16, 21,  1.0f,  -51.2f),
    ZOOM32(32, 43,  2.5f,  -51.2f),
    ZOOM64(64, 86,  5.4f, -51.2f);

    public static final ZoomLevel DEFAULT = ZOOM32;

    private final int width, height;
    private final int screen_z_base;
    private final int draw_dx, draw_dz;
    private final int cols, rows;
    private final float dx, dz;
    private final float scale;
    private final float pitch;
    private final MapTexture mask;

    private ZoomLevel(int width, int height, float d, float pitch) {
        this(width, height, d, 3.0f * d, pitch);
    }
    
    private ZoomLevel(int width, int height, float dx, float dz, float pitch) {
        this(width, height, (float) width / 19.8758f, dx, dz, pitch);
    }

    private ZoomLevel(int width, int height, float scale, float dx, float dz, float pitch) {
        this.width = width;
        this.height = height;
        this.draw_dx = -(width >> 1);
        this.draw_dz = -(height >> 1);
        this.screen_z_base = -width - this.draw_dz;
        this.cols = MathUtil.ceil(128.0 / (double) width * 2.0);
        this.rows = 2 + MathUtil.ceil(128.0 / (double) width * 3.0);
        this.dx = dx;
        this.dz = dz;
        this.scale = scale;
        this.pitch = pitch;
        this.mask = createMask(width, height);
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public final int getColumns() {
        return this.cols;
    }

    public final int getRows() {
        return this.rows;
    }

    /**
     * Gets the x-position of the middle of a certain tile as drawn on the screen
     * 
     * @param tx - coordinate of the tile
     * @return x screen position of the tile (middle)
     */
    public final int getScreenX(int tx) {
        // Take full-width steps for every 2, +half-width step for odd coordinates
        return this.width * (tx >> 1) + (this.width >> 1) * (tx & 0x1);
    }

    /**
     * Gets the z-position of the middle of a certain tile as drawn on the screen
     * 
     * @param tz - coordinate of the tile
     * @return z screen position of the tile (middle)
     */
    public final int getScreenZ(int tz) {
        // Take full height steps for every 3, +1/third step for steps in between
        int div3 = (tz / 3);
        int mod3 = tz - (3 * div3);
        return this.width * div3 + MathUtil.ceil((double) mod3 * (double) this.width / 3.0) + this.screen_z_base;
    }

    /**
     * Gets the x-position of a certain sprite coordinate as drawn on the canvas
     * 
     * @param x - coordinate of the sprite
     * @return x - position on the canvas (top-left)
     */
    public final int getDrawX(int x) {
        return this.getScreenX(x) + this.draw_dx;
    }

    /**
     * Gets the z-position of a certain sprite coordinate as drawn on the canvas
     * 
     * @param z - coordinate of the sprite
     * @return z - position on the canvas (top-left)
     */
    public final int getDrawZ(int z) {
        return this.getScreenZ(z) + this.draw_dz;
    }

    /**
     * Takes a coordinate of a tile and returns the coordinate in the middle of the tile on the screen.
     * 
     * @param p coordinate of the tile (y is depth)
     * @return screen coordinate of the middle of the tile
     */
    public final IntVector3 tileToScreen(IntVector3 p) {
        return new IntVector3(getScreenX(p.x), p.y, getScreenZ(p.z));
    }

    /**
     * Takes a coordinate on the screen and returns the coordinate of the tile it represents.
     * This function is incredibly slow right now and really needs optimization!
     * 
     * @param p coordinate on the screen (y is depth)
     * @return tile coordinate at this screen coordinate
     */
    public final IntVector3 screenToTile(IntVector3 p) {
        int lastDistSqX = Integer.MAX_VALUE;
        int foundTileX = 0;
        int foundTileZ = 0;
        int higherThanCtr = 0;
        for (int tx = 0;;tx++) {
            int foundTileX_X = 0;
            int foundTileZ_X = 0;
            int lastDistSqZ = Integer.MAX_VALUE;
            boolean foundAny = false;
            for (int tz = 0;;tz++) {
                if (!MapUtil.isTile(tx, p.y, tz)) {
                    continue;
                }
                foundAny = true;
                int pdx = getScreenX(tx) - p.x;
                int pdz = getScreenZ(tz) - p.z;
                int distSq = (pdx * pdx) + (pdz * pdz);
                if (distSq <= lastDistSqZ) {
                    lastDistSqZ = distSq;
                    foundTileX_X = tx;
                    foundTileZ_X = tz;
                } else {
                    break;
                }
            }
            if (!foundAny) {
                continue;
            }
            if (lastDistSqZ <= lastDistSqX) {
                higherThanCtr = 0;
                lastDistSqX = lastDistSqZ;
                foundTileX = foundTileX_X;
                foundTileZ = foundTileZ_X;
            } else if (++higherThanCtr > 1) {
                break;
            }
        }
        return new IntVector3(foundTileX, p.y, foundTileZ);
    }

    public MapTexture getMask() {
        return this.mask;
    }

    public Matrix4f getTransform(BlockFace facing) {
        Matrix4f transform = new Matrix4f();
        transform.translate(this.dx, 0, this.dz);
        transform.scale(this.scale);

        transform.translate(8, 8, 8);
        transform.rotateX(this.pitch);
        transform.rotateY(FaceUtil.faceToYaw(facing) - 90);
        transform.translate(-8, -8, -8);

        return transform;
    }

    private static MapTexture createMask(int width, int height) {
        // This creates a tileable block-shaped mask (looks like a hexagon)
        MapTexture mask = MapTexture.createEmpty(width, height);
        mask.fill(MapColorPalette.COLOR_WHITE);

        int half_width = width >> 1;
        int edge_y = height - width - 1;

        for (int x = 0; x < width; x++) {
            int x1, x2;
            if (x < half_width) {
                x1 = width - x - 1;
                x2 = half_width - x - 1;
            } else {
                x1 = x;
                x2 = width - x + half_width - 1;
            }

            float a = (float) (x1) / (float) (half_width);
            int f1 = MathUtil.ceil((float) edge_y * a) - edge_y - 1;
            for (int y = 0; y <= f1; y++) {
                mask.writePixel(x, y, MapColorPalette.COLOR_TRANSPARENT);
            }

            int f2 = edge_y - f1;
            for (int y = 0; y <= f2; y++) {
                mask.writePixel(x2, height - y, MapColorPalette.COLOR_TRANSPARENT);
            }
        }

        return mask;
    }
}
