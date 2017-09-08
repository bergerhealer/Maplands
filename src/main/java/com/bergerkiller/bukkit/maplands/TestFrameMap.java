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
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;

public class TestFrameMap extends MapDisplay {
    private IsometricBlockSprites sprites;
    private Block startBlock;
    private int menuShowTicks = 0;
    private int menuSelectIndex = 0;
    private MenuButton[] menuButtons;
    private MapTexture menu_bg;
    private static final int MENU_DURATION = 200; // amount of ticks menu is kept open while idle
    private static final int BACK_VIEW = 100; // amount of layers visible 'behind' the current block
    private static final int FORWARD_VIEW = 200; // amount of layers visible 'after' the current block

    @Override
    public void onAttached() {
        this.menuButtons = new MenuButton[] {
                new MenuButton("zoom_in", 0, 0) {
                    public void onPressed() {
                        zoom(1);
                    }
                },
                new MenuButton("zoom_out", 32, 0) {
                    public void onPressed() {
                        zoom(-1);
                    }
                },
                new MenuButton("rotate_left", 64, 0) {
                    public void onPressed() {
                        rotate(-1);
                    }
                },
                new MenuButton("rotate_right", 96, 0) {
                    public void onPressed() {
                        rotate(1);
                    }
                }
        };
        this.menu_bg = MapTexture.loadResource(TestFrameMap.class, "/com/bergerkiller/bukkit/maplands/textures/menu_bg.png");
        for (MenuButton button : this.menuButtons) {
            button.setDisplay(this);
        }

        this.setSessionMode(MapSessionMode.VIEWING); // VIEWING for debug, FOREVER for release
        this.setReceiveInputWhenHolding(true);
        this.render();
    }

    private void render() {
        int px = properties.get("px", 0);
        int py = properties.get("py", 0);
        int pz = properties.get("pz", 0);
        BlockFace facing = properties.get("facing", BlockFace.NORTH_EAST);
        ZoomLevel zoom = properties.get("zoom", ZoomLevel.ZOOM4);
        String worldName = properties.get("mapWorld", "");
        if (worldName.length() == 0) {
            Player player = this.getOwners().get(0);
            worldName = player.getWorld().getName();
            px = player.getLocation().getBlockX();
            py = player.getLocation().getBlockY();
            pz = player.getLocation().getBlockZ();
            properties.set("px", px);
            properties.set("py", py);
            properties.set("pz", pz);
            properties.set("mapWorld", worldName);
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

        int cols = (this.getWidth() / 128) * this.sprites.getZoom().getColumns();
        int rows = (this.getHeight() / 128) * this.sprites.getZoom().getRows();
        for (int dy = -BACK_VIEW; dy <= FORWARD_VIEW; dy++) {
            getLayer().setDrawDepth(dy);
            for (int dx = 0; dx < cols; dx++) {
                for (int dz = 0; dz < rows; dz++) {
                    drawBlock(facing, new IntVector3(dx, dy, dz), true);
                }
            }
            if (!getLayer().hasMoreDepth()) {
                break;
            }
        }
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (this.menuShowTicks > 0) {
            this.showMenu(); // keep on while interacted

            // Menu is shown. Intercepts keys.
            if (event.getKey() == Key.LEFT) {
                // Previous button
                this.setMenuIndex(this.menuSelectIndex - 1);
            } else if (event.getKey() == Key.RIGHT) {
                // Next button
                this.setMenuIndex(this.menuSelectIndex + 1);
            } else if (event.getKey() == Key.ENTER) {
                // Activate menu button
                this.menuButtons[this.menuSelectIndex].onPressed();
            } else if (event.getKey() == Key.BACK) {
                // Hide menu again
                this.hideMenu();
            }
            
        } else if (event.getKey() == Key.BACK) {
            // Show menu
            this.showMenu();
        } else {
            // No menu is shown. Simple navigation.
            int mdx = properties.get("px", 0);
            int mdz = properties.get("pz", 0);
            BlockFace facing = properties.get("facing", BlockFace.NORTH_EAST);

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
            }

            properties.set("px", mdx);
            properties.set("pz", mdz);
            properties.set("facing", facing);

            this.render();
        }
    }

    public void drawBlock(BlockFace facing, IntVector3 p, boolean isRedraw) {
        IntVector3 b = screenTileToBlock(facing, p);
        if (b != null) {
            int draw_x = sprites.getZoom().getDrawX(p.x); //((p.x - 1) * BLOCK_SIZE)/2;
            int draw_z = sprites.getZoom().getDrawZ(p.z); //mapping[p.z];
            drawBlock(b, draw_x, draw_z, isRedraw);
        }
    }

    public void drawBlock(IntVector3 d, int draw_x, int draw_y, boolean isRedraw) {
        int x = this.startBlock.getX() + d.x;
        int y = this.startBlock.getY() + d.y;
        int z = this.startBlock.getZ() + d.z;
        if (y >= 0 && y < 256) {
            MapTexture sprite = this.sprites.getSprite(this.startBlock.getWorld(), x, y, z);
            if (sprite != this.sprites.AIR) {
                getLayer().draw(sprite, draw_x, draw_y);
            }
        }
    }

    public void hideMenu() {
        menuShowTicks = 0;
        for (MenuButton button : this.menuButtons) {
            button.setVisible(false);
        }
        getLayer(1).clearRectangle(0, 0, this.menu_bg.getWidth(), this.menu_bg.getHeight());
    }

    public void showMenu() {
        menuShowTicks = MENU_DURATION;
        for (int i = 0; i < this.menuButtons.length; i++) {
            this.menuButtons[i].setVisible(true);
            this.menuButtons[i].setSelected(i == this.menuSelectIndex);
        }
        getLayer(1).draw(this.menu_bg, 0, 0);
    }

    public void setMenuIndex(int index) {
        while (index < 0) {
            index += this.menuButtons.length;
        }
        while (index >= this.menuButtons.length) {
            index -= this.menuButtons.length;
        }
        if (menuSelectIndex != index) {
            menuSelectIndex = index;
            for (int i = 0; i < this.menuButtons.length; i++) {
                this.menuButtons[i].setSelected(i == index);
            }
        }
    }

    public void zoom(int n) {
        ZoomLevel level = properties.get("zoom", ZoomLevel.ZOOM4);
        ZoomLevel[] values = ZoomLevel.values();
        int zoomLevelIdx = 0;
        while (values[zoomLevelIdx] != level) {
            zoomLevelIdx++;
        }
        zoomLevelIdx = MathUtil.clamp(zoomLevelIdx + n, 0, values.length - 1);
        properties.set("zoom", values[zoomLevelIdx]);
        this.render();
    }

    public void rotate(int n) {
        BlockFace facing = properties.get("facing", BlockFace.NORTH_EAST);
        facing = FaceUtil.rotate(facing, n * 2);
        properties.set("facing", facing);
        this.render();
    }
    
    @Override
    public void onTick() {
        if (menuShowTicks > 0 && --menuShowTicks == 0) {
            this.hideMenu();
        }
        for (MenuButton button : this.menuButtons) {
            button.onTick();
        }
        
        
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

    public static IntVector3 screenTileToBlock(BlockFace facing, IntVector3 p) {
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
