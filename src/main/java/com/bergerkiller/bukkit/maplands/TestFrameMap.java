package com.bergerkiller.bukkit.maplands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
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
    
    final int[] mapping = {
            -32,   -21,   -10,
            0,     11,     22,
            32,    43,     54,
            64,    75,     86,
            96,    107,    118
    };
    
    private void render() {
        CommonTagCompound nbt = ItemUtil.getMetaTag(this.getMapItem());
        int px = nbt.getValue("px", 0);
        int py = nbt.getValue("py", 0);
        int pz = nbt.getValue("pz", 0);
        String worldName = nbt.getValue("mapWorld", "");
        if (worldName.length() == 0) {
            Player player = this.getOwners().get(0);
            worldName = player.getWorld().getName();
            px = player.getLocation().getBlockX();
            py = player.getLocation().getBlockY() + 32;
            pz = player.getLocation().getBlockZ();
            nbt.putValue("px", px);
            nbt.putValue("py", py);
            nbt.putValue("pz", pz);
            nbt.putValue("mapWorld", worldName);
            this.setMapItem(this.getMapItem());
        }
        World world = Bukkit.getWorld(worldName);

        // Start coordinates for the view
        startBlock = world.getBlockAt(px, py, pz);
        
        this.getLayer().setRelativeBrushMask(null);
        this.getLayer().setDrawDepth(0);
        this.getLayer().fill(MapColorPalette.COLOR_RED);
        this.getLayer().clearDepthBuffer();
        this.getLayer().setRelativeBrushMask(sprites.getBrushTexture());

        for (int dy = 0; dy < 256; dy++) {
            getLayer().setDrawDepth(dy);
            for (int dx = 0; dx < 10; dx++) {
                for (int dz = 0; dz < mapping.length; dz++) {
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
        int mdx = nbt.getValue("px", 0);
        int mdz = nbt.getValue("pz", 0);
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
        
        nbt.putValue("px", mdx);
        nbt.putValue("pz", mdz);
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
        // Out of range on the map
        if (pz < 0 || pz >= mapping.length) {
            return;
        }

        // Checks whether the tile coordinates are valid
        if (MathUtil.floorMod((px * 3) + (py * 2) + (-pz * 1), 6) != 4) {
            return;
        }

        int pzl = (pz + ((py + 4) % 6)) / 3;

        int dx = ((px + (py & 0x1)) >> 1) + -(pzl >> 1);
        int dy = -pzl - (py & ~0x1);
        int dz = (dx + -dy) - py;

        // repeating pattern of 6, increasing every loop
        // 0, 0, 0, 0, 1, 1 ... 2, 2, 2, 2, 3, 3 ... etc.

        int v = 6 * (py / 6);
        int n = (v) / 3 + (py - v) / 4;

        //System.out.println("dy " + py + "  " + n);

        dx += n;
        dy += 2 * n;
        dz += -n;
        
        int draw_x = ((px - 1) * BLOCK_SIZE)/2;
        int draw_y = mapping[pz];
        
        drawBlock(dx, dy, dz, draw_x, draw_y);
    }

    public void drawBlock(int dx, int dy, int dz, int draw_x, int draw_y) {        
        Block block = this.startBlock.getRelative(dx, dy, dz);
        MapTexture sprite = this.sprites.getSprite((dy == Integer.MIN_VALUE) ? BlockData.fromMaterial(Material.GLOWSTONE) : WorldUtil.getBlockData(block));

        getLayer().draw(sprite, draw_x, draw_y);
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
