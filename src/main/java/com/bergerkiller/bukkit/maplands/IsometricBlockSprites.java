package com.bergerkiller.bukkit.maplands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.math.Vector3;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.common.wrappers.BlockRenderOptions;

/**
 * Renders and caches isometric block sprites
 */
public class IsometricBlockSprites {
    private final HashMap<BlockRenderOptions, Sprite> spriteCache = new HashMap<BlockRenderOptions, Sprite>();
    private final BlockFace facing;
    private final ZoomLevel zoom;
    private final Matrix4x4 transform;
    public final int width;
    public final int height;
    public final Sprite AIR;

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

    public Sprite getSprite(Material material) {
        return getSprite(BlockData.fromMaterial(material).getDefaultRenderOptions());
    }

    public Sprite getSprite(BlockRenderOptions options) {
        return spriteCache.computeIfAbsent(options, this::renderSprite);
    }

    private Sprite renderSprite(BlockRenderOptions options) {
        MapTexture texture = MapTexture.createEmpty(this.width, this.height);

        //map.fill(MapColorPalette.COLOR_RED);
        texture.setLightOptions(0.2f, 0.8f, new Vector3(-1.0f, 1.0f, -1.0f));
        texture.drawModel(Maplands.getResourcePack().getBlockModel(options), this.transform);

        // Compare texture with the sprite mask to see if all pixels are drawn
        // If they are, the sprite is fully opaque, and that flag can be set
        byte[] texture_buffer = texture.getBuffer();
        byte[] mask_buffer = this.zoom.getMask().getBuffer();
        int len = this.width * this.height;
        boolean isFullyOpaque = true;
        for (int i = 0; i < len; i++) {
            if (mask_buffer[i] != 0 && MapColorPalette.isTransparent(texture_buffer[i])) {
                isFullyOpaque = false;
                break;
            }
        }

        return new Sprite(texture, isFullyOpaque);
    }

    /**
     * Gets the texture sprite for a particular block
     * 
     * @param block to get the sprite
     * @return sprite texture
     */
    public Sprite getSprite(Block block) {
        return getSprite(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public Sprite getSprite(World world, int x, int y, int z) {
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

    /**
     * A single isometric block sprite. Stores the texture,
     * and metadata about the sprite which might be helpful
     * for rendering.
     */
    public static final class Sprite {
        public final MapTexture texture;
        public final boolean isFullyOpaque;

        public Sprite(MapTexture texture, boolean isFullyOpaque) {
            this.texture = texture;
            this.isFullyOpaque = isFullyOpaque;
        }
    }
}
