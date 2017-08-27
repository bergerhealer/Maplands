package com.bergerkiller.bukkit.maplands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;

public class TestFrameMap extends MapDisplay {
    private IsometricBlockSprites sprites;
    private final int BLOCK_SIZE = 32;
    BlockFace facing_fwd = BlockFace.NORTH_EAST;
    BlockFace facing_rgt = FaceUtil.rotate(facing_fwd, 2);
    BlockFace facing_bwd = FaceUtil.rotate(facing_rgt, 2);
    BlockFace facing_lft = FaceUtil.rotate(facing_bwd, 2);
    Block startBlock;
    
    private int[] mapping = {
            -32,   -21,   -10,
            0,     11,     22,
            32,    43,     54,
            64,    75,     86,
            96,    107,    118
    };

    private void render() {
        CommonTagCompound nbt = ItemUtil.getMetaTag(this.getMapItem());
        int mdx = nbt.getValue("dx", 0);
        int mdz = nbt.getValue("dz", 0);
        
        // Start coordinates for the view
        startBlock = Bukkit.getWorld("testworld_flat").getBlockAt(-200 + mdx, 25, -132 + mdz);

        this.getLayer().setRelativeBrushMask(null);
        this.getLayer().setDrawDepth(0);
        this.getLayer().fill(MapColorPalette.COLOR_RED);
        this.getLayer().clearDepthBuffer();
        this.getLayer().setRelativeBrushMask(sprites.getBrushTexture());

        for (int dy = 0; dy < 100; dy++) {
            getLayer().setDrawDepth(dy);
            for (int dx = 0; dx < 10; dx++) {
                for (int dz = 0; dz < 10; dz++) {
                    drawBlock(dx, dy, dz);
                }
            }
            if (!getLayer().hasMoreDepth()) {
                break;
            }
        }
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        CommonTagCompound nbt = ItemUtil.getMetaTag(this.getMapItem());
        int mdx = nbt.getValue("dx", 0);
        int mdz = nbt.getValue("dz", 0);
        //mdx += event.getKey().dx();
        //mdz += event.getKey().dy();
        if (event.getKey() == Key.UP) {
            mdx += facing_fwd.getModX();
            mdz += facing_fwd.getModZ();
        } else if (event.getKey() == Key.RIGHT) {
            mdx += facing_rgt.getModX();
            mdz += facing_rgt.getModZ();
        } else if (event.getKey() == Key.DOWN) {
            mdx += facing_bwd.getModX();
            mdz += facing_bwd.getModZ();
        } else if (event.getKey() == Key.LEFT) {
            mdx += facing_lft.getModX();
            mdz += facing_lft.getModZ();
        }
        
        nbt.putValue("dx", mdx);
        nbt.putValue("dz", mdz);
        this.setMapItem(this.getMapItem());
        this.render();
    }

    @Override
    public void onAttached() {
        this.setSessionMode(MapSessionMode.VIEWING);
        this.setReceiveInputWhenHolding(true);
        
        this.sprites = new IsometricBlockSprites();
        
        this.render();
    }

    public void drawBlock(int px, int py, int pz) {
        // This state is impossible and never occurs (diagonal relationship)
        if ((px & 0x1) != ((pz + py) & 0x1)) {
            return;
        }

        int dx = ((px + (py & 0x1)) >> 1) + -(pz >> 1);
        int dy = -pz - (py & ~0x1);
        int dz = (dx + -dy) - py;

        // repeating pattern of 6, increasing every loop
        // 0, 0, 0, 0, 1, 1 ... 2, 2, 2, 2, 3, 3 ... etc.
        int v = py / 6;
        int k = (py - 6 * v);
        int n = 2 * v + k / 4;

        // repeating pattern of 6, looping
        // 4, 5, 0, 1, 2, 3 ... 4, 5, 0, 1, 2, 3 ... etc
        int p = (py + 4) % 6;

        drawBlock(dx + n, dy + 2 * n, dz - n, px, py, (pz * 3) - p);
    }

    public void drawBlock(int dx, int dy, int dz, int px, int py, int pz) {
        if (pz < 0 || pz >= mapping.length) {
            return;
        }
        
        Block block = this.startBlock.getRelative(dx, dy, dz);
        MapTexture sprite = this.sprites.getSprite((dy == Integer.MIN_VALUE) ? BlockData.fromMaterial(Material.GLOWSTONE) : WorldUtil.getBlockData(block));

        int x = ((px - 1) * BLOCK_SIZE)/2;
        //int y = ((pz - 3) * (sprite.getHeight()) - 1) / 4;
        int y = mapping[pz];
        
        getLayer().draw(sprite, x, y);
    }

    @Override
    public void onTick() {
        //getLayer(1).clear();
        
        /*
        Location loc = this.getOwners().get(0).getEyeLocation();

        Matrix4f modeloffset = new Matrix4f();
        modeloffset.set(new Vector3f(-8.0f, -8.0f, -8.0f));
        
        Matrix4f translation = new Matrix4f();
        translation.set(4.0f, new Vector3f(64, 0, 70));

        Matrix4f rotationPitch = new Matrix4f();
        rotationPitch.rotateX(loc.getPitch() - 90.0f);

        Matrix4f rotationYaw = new Matrix4f();
        rotationYaw.rotateY(loc.getYaw());

        Matrix4f transform = new Matrix4f();
        transform.setIdentity();
        transform.multiply(translation);
        transform.multiply(rotationPitch);
        transform.multiply(rotationYaw);
        transform.multiply(modeloffset);
        
        getLayer(1).drawModel(resources.getModel("block/repeater_on_4tick"), transform);
        */
    }
}
