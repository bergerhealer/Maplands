package com.bergerkiller.bukkit.maplands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.map.util.Vector3f;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.common.wrappers.BlockRenderOptions;

/**
 * Renders and caches isometric block sprites
 */
public class IsometricBlockSprites {
    private final HashMap<BlockRenderOptions, MapTexture> spriteCache = new HashMap<BlockRenderOptions, MapTexture>();
    private final BlockFace facing;
    private final ZoomLevel zoom;
    private final Matrix4f transform;
    public final int width;
    public final int height;
    public final MapTexture AIR;

    private IsometricBlockSprites(BlockFace facing, ZoomLevel zoom) {
        this.facing = facing;
        this.zoom = zoom;
        this.width = zoom.getWidth();
        this.height = zoom.getHeight();
        this.transform = zoom.getTransform(facing);
        this.AIR = this.getSprite(Material.AIR);
    }

    /**
     * Gets the zoom level of these sprites
     * 
     * @return zoom level
     */
    public ZoomLevel getZoom() {
        return this.zoom;
    }

    /**
     * Gets the brush texture that is used to mask off the area when drawing a single block tile
     * 
     * @return sprite brush
     */
    public MapTexture getBrushTexture() {
        return this.zoom.getMask();
    }

    public MapTexture getSprite(Material material) {
        return getSprite(BlockData.fromMaterial(material).getDefaultRenderOptions());
    }

    public MapTexture getSprite(BlockRenderOptions options) {
        MapTexture result = spriteCache.get(options);
        if (result == null) {
            result = MapTexture.createEmpty(this.width, this.height);

            //map.fill(MapColorPalette.COLOR_RED);
            result.setLightOptions(0.2f, 0.8f, new Vector3f(-1.0f, 1.0f, -1.0f));
            result.drawModel(Maplands.getResourcePack().getBlockModel(options), this.transform);

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
        return getSprite(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public MapTexture getSprite(World world, int x, int y, int z) {
        return getSprite(BlockRenderOptions.fromBlock(world, x, y, z));
    }

    // Static caches for different zoom levels and different yaw rotations

    private static List<IsometricBlockSprites> instances = new ArrayList<IsometricBlockSprites>();

    public static IsometricBlockSprites getSprites(BlockFace facing, ZoomLevel zoom) {
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
