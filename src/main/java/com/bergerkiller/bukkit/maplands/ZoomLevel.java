package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.math.Vector2;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A map zoom level that can be used
 */
public enum ZoomLevel {
    ZOOM2( 2,  -0.50f,  19.8758f,  -70f),
    ZOOM4( 4,  0.00f,  19.8758f,  -70f),
    ZOOM8( 8,  0.5f,   19.8758f,  -55f),
    ZOOM16(16, 2.5f,   19.8758f,  -51.2f),
    ZOOM32(32, 5.4f,   19.8758f,  -50.4f),
    ZOOM64(64, 12.8f,  19.3f,  -51.2f);

    public static final ZoomLevel DEFAULT = ZOOM32;

    private final int width, height;
    private final int step_x, step_y;
    private final int screen_y_base;
    private final int draw_dx, draw_dy;
    private final float dy, sy;
    private final float pitch;
    private final MapTexture mask;

    private ZoomLevel(int width, float dy, float sy, float pitch) {
        if (width == 2) {
            // It's just too small to auto-compute it :(
            this.step_x = 1;
            this.step_y = 1;
            this.width = 2;
            this.height = 2;
            this.draw_dx = -1;
            this.draw_dy = -1;
            this.screen_y_base = -3;
            this.dy = dy;
            this.sy = sy;
            this.pitch = pitch;
            this.mask = MapTexture.createEmpty(2, 2);
            this.mask.fill(MapColorPalette.COLOR_WHITE);
        } else {
            this.step_x = (width >> 1);
            this.step_y = MathUtil.floor((double) width / 3.0);
            this.width = this.step_x * 2;
            this.height = this.step_y * 4;
            this.draw_dx = -(this.width >> 1);
            this.draw_dy = -(this.height >> 1);
            this.screen_y_base = -width - this.draw_dy;
            this.dy = dy;
            this.sy = sy;
            this.pitch = pitch;
            this.mask = createMask(width, height);
        }
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
        return 3 + MathUtil.ceil((double) height / (double) this.step_y / 2.0);
    }

    /**
     * Gets the delta change in screen x-coordinates with every tile step
     * 
     * @return tile x step
     */
    public final double getTileStepX() {
        return this.step_x;
    }

    /**
     * Gets the delta change in screen y-coordinates with every tile step
     * 
     * @return tile y step
     */
    public final double getTileStepY() {
        return (double) this.step_y;
    }

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
     * Gets the y-position of the middle of a certain tile as drawn on the screen
     * 
     * @param ty - coordinate of the tile
     * @return y screen position of the tile (middle)
     */
    public final int getScreenY(int ty) {
        return ty * this.step_y + this.screen_y_base;
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
     * Gets the y-position of a certain sprite coordinate as drawn on the canvas
     * 
     * @param y - coordinate of the sprite
     * @return y - position on the canvas (top-left)
     */
    public final int getDrawY(int y) {
        return this.getScreenY(y) + this.draw_dy;
    }

    /**
     * Takes a coordinate of a tile and returns the coordinate in the middle of the tile on the screen.
     * 
     * @param p coordinate of the tile (z is depth)
     * @return screen coordinate of the middle of the tile
     */
    public final IntVector3 tileToScreen(IntVector3 p) {
        return new IntVector3(getScreenX(p.x), getScreenY(p.y), p.z);
    }

    /**
     * Takes a coordinate on the screen and returns the coordinate of the tile it represents.
     * This function is incredibly slow right now and really needs optimization!
     * 
     * @param p coordinate on the screen (z is depth)
     * @return tile coordinate at this screen coordinate
     */
    public final IntVector3 screenToTile(IntVector3 p) {
        int lastDistSqX = Integer.MAX_VALUE;
        int foundTileX = 0;
        int foundTileY = 0;
        int higherThanCtr = 0;
        int tx_incr = (p.x >= 0) ? 1 : -1;
        int ty_incr = (p.y >= 0) ? 1 : -1;
        for (int tx = -tx_incr;;tx += tx_incr) {
            int foundTileX_X = 0;
            int foundTileY_X = 0;
            int lastDistSqZ = Integer.MAX_VALUE;
            boolean foundAny = false;
            for (int ty = -ty_incr;;ty += ty_incr) {
                if (!MapUtil.isTile(tx, ty, p.z)) {
                    continue;
                }
                foundAny = true;
                int pdx = getScreenX(tx) - p.x;
                int pdy = getScreenY(ty) - p.y;
                int distSq = (pdx * pdx) + (pdy * pdy);
                if (distSq <= lastDistSqZ) {
                    lastDistSqZ = distSq;
                    foundTileX_X = tx;
                    foundTileY_X = ty;
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
                foundTileY = foundTileY_X;
            } else if (++higherThanCtr > 1) {
                break;
            }
        }
        return new IntVector3(foundTileX, foundTileY, p.z);
    }

    public MapTexture getMask() {
        return this.mask;
    }

    public Matrix4x4 getTransform(BlockFace facing) {
        Matrix4x4 transform = new Matrix4x4();
        transform.translate((float) (width / (16.0 * MathUtil.HALFROOTOFTWO)), 0, this.dy);

        float scale_w = (float) width / 20.0f;
        float scale_h = (float) width / this.sy;
        transform.scale(scale_w, scale_w, scale_h);

        transform.translate(8, 8, 8);
        transform.rotateX(this.pitch);
        transform.rotateY(FaceUtil.faceToYaw(facing) - 90);
        transform.translate(-8, -8, -8);

        return transform;
    }

    /**
     * Calculates the pixel coordinates of an area inside a 1x1x1 block.
     * 
     * @param facing
     * @param dx - Delta x-coordinate within the block, relative to 0.5
     * @param dy - Delta y-coordinate within the block, relative to 0.5
     * @param dz - Delta z-coordinate within the block, relative to 0.5
     * @return exact pixel coordinates
     */
    public Vector2 getBlockPixelCoordinates(BlockFace facing, double dx, double dy, double dz) {
        double x = 0.0;
        double y = 0.0;

        // Adjust for the y-coordinate, is always the same offset regardless of facing
        y -= 2.0 * getTileStepY() * dy;

        // Adjust for the x-coordinate, which has a factor for both x and y
        x -= (double) facing.getModZ() * getTileStepX() * dx;
        y -= (double) facing.getModX() * getTileStepY() * dx;

        // Adjust for the z-coordinate, which has a factor for both x and y
        x += (double) facing.getModX() * getTileStepX() * dz;
        y -= (double) facing.getModZ() * getTileStepY() * dz;

        return new Vector2(x, y);
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
