package com.bergerkiller.bukkit.maplands;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A map zoom level that can be used
 */
public enum ZoomLevel {
    ZOOM2(16, 21, 0.805f,   1.0f, 3.0f,    -51.2f),
    ZOOM4(32, 43, 1.61f,    2.62f, 8.24f,   -51.2f);

    private final int width, height;
    private final int offset_x, offset_z;
    private final int cols, rows;
    private final float dx, dz;
    private final float scale;
    private final float pitch;
    private final MapTexture mask;

    private ZoomLevel(int width, int height, float scale, float dx, float dz, float pitch) {
        this.width = width;
        this.height = height;
        this.offset_x = -(width >> 1);
        this.offset_z = -width;
        this.cols = 2 + MathUtil.ceil(128.0 / (double) width * 2.0);
        this.rows = 3 + MathUtil.ceil(128.0 / (double) width * 3.0);
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
     * Gets the x-position of a certain sprite coordinate as drawn on the canvas
     * 
     * @param x - coordinate of the sprite
     * @return x - position on the canvas
     */
    public int getDrawX(int x) {
        // Take full-width steps for every 2, +half-width step for odd coordinates
        return this.width * (x >> 1) + (this.width >> 1) * (x & 0x1) + this.offset_x;
    }

    /**
     * Gets the z-position of a certain sprite coordinate as drawn on the canvas
     * 
     * @param z - coordinate of the sprite
     * @return z - position on the canvas
     */
    public int getDrawZ(int z) {
        // Take full height steps for every 3, +1/third step for steps in between
        int div3 = (z / 3);
        int mod3 = z - (3 * div3);
        return this.width * div3 + MathUtil.ceil((double) mod3 * (double) this.width / 3.0) + this.offset_z;
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
