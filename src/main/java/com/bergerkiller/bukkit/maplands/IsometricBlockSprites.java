package com.bergerkiller.bukkit.maplands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.map.util.Vector3f;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.common.wrappers.BlockRenderOptions;

/**
 * Renders and caches isometric block sprites
 */
public class IsometricBlockSprites {
    private final HashMap<BlockRenderOptions, MapTexture> spriteCache = new HashMap<BlockRenderOptions, MapTexture>();
    private final MapTexture brushTexture;
    private final BlockFace facing;
    private final int zoom;
    public final int width;
    public final int height;

    private IsometricBlockSprites(BlockFace facing, int zoom) {
        this.facing = facing;
        this.zoom = zoom;
        this.width = MathUtil.ceil(128.0 / zoom);
        this.height = MathUtil.ceil(4.0 * 128.0 / ((double) zoom * 3.0));
        this.brushTexture = MapTexture.loadResource(IsometricBlockSprites.class, "/com/bergerkiller/bukkit/maplands/mask2.png");
    }

    /**
     * Gets the brush texture that is used to mask off the area when drawing a single block tile
     * 
     * @return sprite brush
     */
    public MapTexture getBrushTexture() {
        return this.brushTexture;
    }

    public MapTexture getSprite(Material material) {
        return getSprite(BlockData.fromMaterial(material).getDefaultRenderOptions());
    }

    public MapTexture getSprite(BlockRenderOptions options) {
        MapTexture result = spriteCache.get(options);
        if (result == null) {
            result = MapTexture.createEmpty(this.width, this.height);

            Matrix4f transform = new Matrix4f();

            transform.translate(2.62f, 0, 8.25f);
            transform.scale(1.61f);

            transform.translate(8, 8, 8);
            transform.rotateX(-51.2f);
            transform.rotateY(FaceUtil.faceToYaw(this.facing) - 90);
            transform.translate(-8, -8, -8);

            //map.fill(MapColorPalette.COLOR_RED);
            result.setLightOptions(0.2f, 0.8f, new Vector3f(-1.0f, 1.0f, -1.0f));
            result.drawModel(Maplands.plugin.resourcePack.getBlockModel(options), transform);

            spriteCache.put(options, result);
        }
        return result;
    }
    
    /**
     * Gets the texture sprite for a particular block
     * 
     * @param block to get the sprite
     * @return sprite texture
     */
    public MapTexture getSprite(Block block) {
        return getSprite(BlockRenderOptions.fromBlock(block));
    }

    // Static caches for different zoom levels and different yaw rotations

    private static List<IsometricBlockSprites> instances = new ArrayList<IsometricBlockSprites>();

    public static IsometricBlockSprites getSprites(BlockFace facing, int zoom) {
        for (IsometricBlockSprites sprites : instances) {
            if (sprites.facing == facing && sprites.zoom == zoom) {
                return sprites;
            }
        }
        IsometricBlockSprites sprites = new IsometricBlockSprites(facing, zoom);
        instances.add(sprites);
        return sprites;
    }

}
