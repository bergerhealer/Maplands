package com.bergerkiller.bukkit.maplands;

import java.util.Locale;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A map zoom level that can be used
 */
public enum ZoomLevel {
    ZOOM4(32, 43, 2.62f, 8.25f, 1.61f, -51.2f);

    private final int width, height;
    private final int offset_x, offset_z;
    private final float dx, dz;
    private final float scale;
    private final float pitch;
    private final MapTexture mask;

    private ZoomLevel(int width, int height, float dx, float dz, float scale, float pitch) {
        this.width = width;
        this.height = height;
        this.offset_x = -(width >> 1);
        this.offset_z = -width;
        this.dx = dx;
        this.dz = dz;
        this.scale = scale;
        this.pitch = pitch;
        this.mask = MapTexture.loadResource(ZoomLevel.class, "/com/bergerkiller/bukkit/maplands/textures/" + this.name().toLowerCase(Locale.ENGLISH) + "_mask.png");
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
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
}
