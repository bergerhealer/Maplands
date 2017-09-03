package com.bergerkiller.bukkit.maplands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.bases.IntVector3;
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

public class TestFrameMap extends MapDisplay {
    private IsometricBlockSprites sprites;
    private final int BLOCK_SIZE = 32;
    private Block startBlock;
    private static final int BACK_VIEW = 100; // amount of layers visible 'behind' the current block
    private static final int FORWARD_VIEW = 200; // amount of layers visible 'after' the current block

    static final int[] mapping = {
            -32,   -21,   -10,
            0,     11,     22,
            32,    43,     54,
            64,    75,     86,
            96,    107,    118
    };

    @Override
    public void onAttached() {
        this.setSessionMode(MapSessionMode.VIEWING); // VIEWING for debug, FOREVER for release
        this.setReceiveInputWhenHolding(true);
        this.render();
    }

    private void render() {
        CommonTagCompound nbt = ItemUtil.getMetaTag(this.getMapItem());
        int px = nbt.getValue("px", 0);
        int py = nbt.getValue("py", 0);
        int pz = nbt.getValue("pz", 0);
        BlockFace facing = nbt.getValue("facing", BlockFace.NORTH_EAST);
        int zoom = nbt.getValue("zoom", 4);
        String worldName = nbt.getValue("mapWorld", "");
        if (worldName.length() == 0) {
            Player player = this.getOwners().get(0);
            worldName = player.getWorld().getName();
            px = player.getLocation().getBlockX();
            py = player.getLocation().getBlockY();
            pz = player.getLocation().getBlockZ();
            nbt.putValue("px", px);
            nbt.putValue("py", py);
            nbt.putValue("pz", pz);
            nbt.putValue("mapWorld", worldName);
            this.setMapItem(this.getMapItem());
        }
        World world = Bukkit.getWorld(worldName);

        // Get the correct sprites
        this.sprites = IsometricBlockSprites.getSprites(facing, zoom);

        // Start coordinates for the view
        this.startBlock = world.getBlockAt(px, py, pz);

        this.getLayer().setRelativeBrushMask(null);
        this.getLayer().setDrawDepth(0);
        this.getLayer().fill(MapColorPalette.COLOR_RED);
        this.getLayer().clearDepthBuffer();
        this.getLayer().setRelativeBrushMask(this.sprites.getBrushTexture());

        for (int dy = -BACK_VIEW; dy <= FORWARD_VIEW; dy++) {
            getLayer().setDrawDepth(dy);
            for (int dx = 0; dx < 10; dx++) {
                for (int dz = 0; dz < mapping.length; dz++) {
                    drawBlock(facing, new IntVector3(dx, dy, dz));
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
        BlockFace facing = nbt.getValue("facing", BlockFace.NORTH_EAST);

        if (event.getKey() == Key.UP) {
            BlockFace facing_fwd = facing;            
            mdx += facing_fwd.getModX();
            mdz += facing_fwd.getModZ();
        } else if (event.getKey() == Key.RIGHT) {
            BlockFace facing_rgt = FaceUtil.rotate(facing, 2);
            mdx += facing_rgt.getModX();
            mdz += facing_rgt.getModZ();
        } else if (event.getKey() == Key.DOWN) {
            BlockFace facing_bwd = FaceUtil.rotate(facing, 4);
            mdx += facing_bwd.getModX();
            mdz += facing_bwd.getModZ();
        } else if (event.getKey() == Key.LEFT) {
            BlockFace facing_lft = FaceUtil.rotate(facing, 6);
            mdx += facing_lft.getModX();
            mdz += facing_lft.getModZ();
        }

        if (event.getKey() == Key.ENTER) {
            facing = FaceUtil.rotate(facing, 2);
        } else if (event.getKey() == Key.BACK) {
            mdx ^= 0x1;
        }

        nbt.putValue("px", mdx);
        nbt.putValue("pz", mdz);
        nbt.putValue("facing", facing.name());

        this.setMapItem(this.getMapItem());
        this.render();
    }

    public void drawBlock(BlockFace facing, IntVector3 p) {
        IntVector3 b = screenToBlock(facing, p);
        if (b != null) {
            int draw_x = ((p.x - 1) * BLOCK_SIZE)/2;
            int draw_y = mapping[p.z];
            drawBlock(b, draw_x, draw_y);
        }
    }

    public void drawBlock(IntVector3 d, int draw_x, int draw_y) {
        int x = this.startBlock.getX() + d.x;
        int y = this.startBlock.getY() + d.y;
        int z = this.startBlock.getZ() + d.z;
        if (y >= 0 && y < 256) {
            MapTexture sprite = this.sprites.getSprite(this.startBlock.getWorld(), x, y, z);
            getLayer().draw(sprite, draw_x, draw_y);
        }
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

    public static IntVector3 screenToBlock(BlockFace facing, IntVector3 p) {
        // Out of range on the map
        if (p.z < 0 || p.z >= mapping.length) {
            return null;
        }

        // Checks whether the tile coordinates are valid
        if (MathUtil.floorMod((p.x * 3) + (p.y * 2) + (-p.z * 1), 6) != 4) {
            return null;
        }

        int px_div2 = p.x / 2;
        int py_div3 = MathUtil.floorDiv(p.y, 3);
        int pz_div3 = p.z / 3;

        int dxz_fact;
        if ((p.x & 0x1) == 0x1) {
            dxz_fact = (p.z + 2) / 6;            
        } else {
            dxz_fact = (p.z + 5) / 6;
        }

        int dx = py_div3 + px_div2 - dxz_fact;
        int dy = -pz_div3 - py_div3;
        int dz = dx - dy - p.y;

        // Move to center block
        dx += 1;
        dz -= 3;

        if (facing == BlockFace.NORTH_EAST) {
            return new IntVector3(dx, dy, dz);
        } else if (facing == BlockFace.SOUTH_WEST) {
            return new IntVector3(-dx, dy, -dz);
        } else if (facing == BlockFace.NORTH_WEST) {
            return new IntVector3(dz, dy, -dx);
        } else if (facing == BlockFace.SOUTH_EAST) {
            return new IntVector3(-dz, dy, dx);
        } else {
            return null;
        }
    }

}
