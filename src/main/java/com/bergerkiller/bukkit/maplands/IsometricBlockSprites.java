package com.bergerkiller.bukkit.maplands;

import java.util.HashMap;
import java.util.Random;

import com.bergerkiller.bukkit.common.map.MapResourcePack;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.map.util.Vector3f;
import com.bergerkiller.bukkit.common.wrappers.BlockData;

/**
 * Renders and caches isometric block sprites
 */
public class IsometricBlockSprites {
    private MapResourcePack resources;
    private final HashMap<BlockData, MapTexture> spriteCache = new HashMap<BlockData, MapTexture>();
    private final MapTexture brushTexture;

    public IsometricBlockSprites() {
        this.resources = MapResourcePack.VANILLA;
        this.brushTexture = MapTexture.loadPluginResource(Maplands.plugin, "com/bergerkiller/bukkit/maplands/mask2.png");
    }

    /**
     * Gets the brush texture that is used to mask off the area when drawing a single block tile
     * 
     * @return sprite brush
     */
    public MapTexture getBrushTexture() {
        return this.brushTexture;
    }

    Random r = new Random();
    /**
     * Gets the texture sprite for a particular block
     * 
     * @param block to get the sprite
     * @return sprite texture
     */
    public MapTexture getSprite(BlockData block) {
        MapTexture result = spriteCache.get(block);
        if (result == null) {
            result = MapTexture.createEmpty(32, 43);

            Matrix4f transform = new Matrix4f();
            //transform.translate(result.getWidth(), 0.0f, result.getWidth() - 1);
            //transform.scale(1.45f, 1.0f, 1.71f);

            transform.translate(result.getWidth(), 0.0f, result.getWidth() - 2);
            transform.scale(1.45f, 1.0f, 1.71f);
            
            transform.rotateX(-45.0f);
            transform.rotateY(225.0f);

            //map.fill(MapColorPalette.COLOR_RED);
            result.setLightOptions(0.2f, 0.8f, new Vector3f(-1.0f, 1.0f, -1.0f));
            result.drawModel(this.resources.getBlockModel(block), transform);

            spriteCache.put(block, result);
        }
        return result;
    }

}
