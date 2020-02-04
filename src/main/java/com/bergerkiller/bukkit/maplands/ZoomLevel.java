package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A map zoom level that can be used
 */
public enum ZoomLevel {
    ZOOM4( 4,  0.00f,  19.8758f,  -70f),
    ZOOM8( 8,  0.5f,   19.8758f,  -55f),
    ZOOM16(16, 2.5f,   19.8758f,  -51.2f),
    ZOOM32(32, 5.4f,   19.8758f,  -50.4f),
    ZOOM64(64, 12.8f,  19.3f,  -51.2f);

    public static final ZoomLevel DEFAULT = ZOOM32;

    private final int width, height;
    private final int step_x, step_z;
    private final int screen_z_base;
    private final int draw_dx, draw_dz;
    private final float dz, sz;
    private final float pitch;
    private final MapTexture mask;

    private ZoomLevel(int width, float dz, float sz, float pitch) {
        this.step_x = (width >> 1);
        this.step_z = MathUtil.floor((double) width / 3.0);
        this.width = this.step_x * 2;
        this.height = this.step_z * 4;
        this.draw_dx = -(this.width >> 1);
        this.draw_dz = -(this.height >> 1);
        this.screen_z_base = -width - this.draw_dz;
        this.dz = dz;
        this.sz = sz;
        this.pitch = pitch;
        this.mask = createMask(width, height);
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public final int getNumberOfColumns(int width) {
        return MathUtil.ceil((double) width / (double) this.step_x / 2.0);
    }

    public final int getNumberOfRows(int height) {
        return 3 + MathUtil.ceil((double) height / (double) this.step_z / 2.0);
    }

    /*
    public final int getColumns() {
        return this.cols;
    }

    public final int getRows() {
        return this.rows;
    }
    */

    /**
     * Gets the x-position of the middle of a certain tile as drawn on the screen
     * 
     * @param tx - coordinate of the tile
     * @return x screen position of the tile (middle)
     */
    public final int getScreenX(int tx) {
        return tx * this.step_x;
    }

    /**
     * Gets the z-position of the middle of a certain tile as drawn on the screen
     * 
     * @param tz - coordinate of the tile
     * @return z screen position of the tile (middle)
     */
    public final int getScreenZ(int tz) {
        return tz * this.step_z + this.screen_z_base;
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

    public Matrix4x4 getTransform(BlockFace facing) {
        Matrix4x4 transform = new Matrix4x4();
        transform.translate((float) (width / (16.0 * MathUtil.HALFROOTOFTWO)), 0, this.dz);

        float scale_w = (float) width / 20.0f;
        float scale_h = (float) width / this.sz;
        transform.scale(scale_w, scale_w, scale_h);

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

        int ratio_error = MathUtil.floor(((double) width / 3.0 * 4.0) - (double) height);
        int half_width = width >> 1;
        int edge_y = height - width + ratio_error - 1;

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
