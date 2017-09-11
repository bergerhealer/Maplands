package com.bergerkiller.bukkit.maplands;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class TestFrameMap extends MapDisplay {
    private IsometricBlockSprites sprites;
    private ZoomLevel zoom;
    private BlockFace facing;
    private Block startBlock;
    private int menuShowTicks = 0;
    private int menuSelectIndex = 0;
    private int currentRenderY;
    private boolean forwardRenderNeeded;
    private int tile_offset_x, tile_offset_z;
    private int minCols, maxCols, minRows, maxRows;
    private boolean enableLight = false;
    private boolean[] drawnTiles; //TODO: Bitset may be faster/more memory efficient?
    private MenuButton[] menuButtons;
    private MapTexture menu_bg;
    int rendertime = 0;
    private static final int MENU_DURATION = 200; // amount of ticks menu is kept open while idle
    private static final int VIEW_RANGE = (256*3); // amount of layers visible backwards and forwards the current block

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
                        rotate(1);
                    }
                },
                new MenuButton("rotate_right", 96, 0) {
                    public void onPressed() {
                        rotate(-1);
                    }
                }
        };
        this.menu_bg = MapTexture.loadResource(TestFrameMap.class, "/com/bergerkiller/bukkit/maplands/textures/menu_bg.png");
        for (MenuButton button : this.menuButtons) {
            button.setDisplay(this);
        }

        this.setSessionMode(MapSessionMode.FOREVER); // VIEWING for debug, FOREVER for release
        this.setReceiveInputWhenHolding(true);
        this.render(true);
    }

    private void render(boolean clear) {
        int px = properties.get("px", 0);
        int py = properties.get("py", 0);
        int pz = properties.get("pz", 0);
        this.facing = properties.get("facing", BlockFace.NORTH_EAST);
        this.zoom = properties.get("zoom", ZoomLevel.DEFAULT);
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
        this.sprites = IsometricBlockSprites.getSprites(facing, this.zoom);

        this.testSprite = MapTexture.createEmpty(zoom.getWidth(), zoom.getHeight());

        // Start coordinates for the view
        this.startBlock = world.getBlockAt(px, py, pz);

        this.getLayer().setRelativeBrushMask(null);
        //this.getLayer().setDrawDepth(-VIEW_RANGE);
        //this.getLayer().fill(MapColorPalette.COLOR_RED);
        if (clear) {
            this.getLayer().clearDepthBuffer();
        }
        this.getLayer().setRelativeBrushMask(this.sprites.getBrushTexture());

        this.currentRenderY = -VIEW_RANGE;
        this.forwardRenderNeeded = true;

        int nrColumns = this.sprites.getZoom().getNumberOfColumns(this.getWidth());
        int nrRows = this.sprites.getZoom().getNumberOfRows(this.getHeight());
        this.minCols = -nrColumns;
        this.maxCols = nrColumns;
        this.minRows = -nrRows;
        this.maxRows = nrRows + 3;

        // Clear drawn tiles state
        if (clear) {
            int len = (this.maxRows - this.minRows + 1) * (this.maxCols - this.minCols + 1);
            if (this.drawnTiles == null || len != this.drawnTiles.length) {
                this.drawnTiles = new boolean[len];
            } else {
                Arrays.fill(this.drawnTiles, false);
            }
        }

        rendertime = 0;
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
            if (event.getKey() == Key.UP) {
                BlockFace facing_fwd = facing;
                moveTiles(0, 2, facing_fwd);
            } else if (event.getKey() == Key.RIGHT) {
                BlockFace facing_rgt = FaceUtil.rotate(facing, 2);
                moveTiles(-2, 0, facing_rgt);
            } else if (event.getKey() == Key.DOWN) {
                BlockFace facing_bwd = FaceUtil.rotate(facing, 4);
                moveTiles(0, -2, facing_bwd);
            } else if (event.getKey() == Key.LEFT) {
                BlockFace facing_lft = FaceUtil.rotate(facing, 6);
                moveTiles(2, 0, facing_lft);
            }

            if (event.getKey() == Key.ENTER) {
                //rotate(1);
                this.render(true);
            }
        }
    }

    public void moveTiles(int dtx, int dtz, BlockFace blockChange) {
        // Move all pixels
        getLayer().movePixels(
                zoom.getScreenX(dtx) - zoom.getScreenX(0),
                zoom.getScreenZ(dtz) - zoom.getScreenZ(0)
        );

        // Transform the "have we drawn this tile?" buffer with the same movement
        // Shorten the direction moved away from, since some of those blocks were only partially drawn
        // They will have to be fully re-drawn to draw the cut-off portion
        int fMinCols = this.minCols;
        int fMaxCols = this.maxCols;
        int fMinRows = this.minRows;
        int fMaxRows = this.maxRows;
        if (dtx > 0) {
            fMinCols += dtx + 1;
        } else if (dtx < 0) {
            fMaxCols += dtx - 1;
        }
        if (dtz > 0) {
            fMinRows += dtz + 5;
        } else if (dtz < 0) {
            fMaxRows += dtz - 5;
        }
        boolean[] newDrawnTiles = new boolean[this.drawnTiles.length];
        int tileIdx = -1;
        for (int y = this.minRows; y <= this.maxRows; y++) {
            for (int x = this.minCols; x <= this.maxCols; x++) {
                tileIdx++;
                boolean value = this.drawnTiles[tileIdx];
                int mx = x + dtx;
                int mz = y + dtz;
                if (mx >= fMinCols && mx <= fMaxCols && mz >= fMinRows && mz <= fMaxRows) {
                    int index = tileIdx;
                    index += dtz * (this.maxCols - this.minCols + 1) + dtx;
                    newDrawnTiles[index] = value;
                }
            }
        }
        this.drawnTiles = newDrawnTiles;

        // When moving up or down, the depth buffer values are incremented/decremented
        if (dtz != 0) {
            short[] buffer = this.getLayer().getDepthBuffer();
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] != MapCanvas.MAX_DEPTH) {
                    buffer[i] -= dtz;
                }
            }
        }

        // Re-render with the changed block position
        properties.set("px", properties.get("px", 0) + blockChange.getModX());
        properties.set("pz", properties.get("pz", 0) + blockChange.getModZ());
        render(false);
    }

    public boolean drawBlock(int x, int y, int z, boolean isRedraw) {
        IntVector3 b = getBlockAtTile(x, y, z);
        if (b != null) {
            int draw_x = sprites.getZoom().getDrawX(x);
            int draw_z = sprites.getZoom().getDrawZ(z);
            return drawBlock(b, draw_x, draw_z, isRedraw);
        } else {
            return true;
        }
    }

    private MapTexture testSprite;
    
    private int getLight(int x, int y, int z) {
        return WorldUtil.getSkyLight(this.startBlock.getWorld().getChunkAt(x >> 4, z >> 4), x, y, z);
    }
    
    public boolean drawBlock(IntVector3 d, int draw_x, int draw_y, boolean isRedraw) {
        int x = this.startBlock.getX() + d.x;
        int y = this.startBlock.getY() + d.y;
        int z = this.startBlock.getZ() + d.z;
        if (y >= 0 && y < 256) {
            MapTexture sprite = this.sprites.getSprite(this.startBlock.getWorld(), x, y, z);
            if (sprite != this.sprites.AIR) {
                draw_x += (this.getWidth() >> 1);
                draw_y += (this.getHeight() >> 1);

                if (enableLight) {
                    int light = getLight(x, y + 1, z);
                    if (light < 15) {
                        light = Math.max(light, getLight(x - 1, y, z));
                        light = Math.max(light, getLight(x + 1, y, z));
                        light = Math.max(light, getLight(x, y, z - 1));
                        light = Math.max(light, getLight(x, y, z + 1));
                    }
                    
                    float lightf = (float) light / 15.0f;
                    lightf *= lightf;
                    
                    byte[] in = sprite.getBuffer();
                    byte[] buff = testSprite.getBuffer();
                    for (int i = 0; i < buff.length; i++) {
                        buff[i] = MapColorPalette.getSpecular(in[i], lightf);
                    }

                    getLayer().draw(testSprite, draw_x, draw_y);
                } else {
                    getLayer().draw(sprite, draw_x, draw_y);
                }

                return getLayer().hasMoreDepth(draw_x, draw_y, sprite.getWidth(), sprite.getHeight());
            } else {
                return true;
            }
        } else {
            return true;
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
        ZoomLevel level = properties.get("zoom", ZoomLevel.DEFAULT);
        ZoomLevel[] values = ZoomLevel.values();
        int zoomLevelIdx = 0;
        while (values[zoomLevelIdx] != level) {
            zoomLevelIdx++;
        }
        zoomLevelIdx = MathUtil.clamp(zoomLevelIdx + n, 0, values.length - 1);
        properties.set("zoom", values[zoomLevelIdx]);
        this.render(true);
    }

    public void rotate(int n) {
        BlockFace facing = properties.get("facing", BlockFace.NORTH_EAST);
        facing = FaceUtil.rotate(facing, n * 2);
        properties.set("facing", facing);
        this.render(true);
    }

    /**
     * Gets the tile coordinates of a particular point on the canvas
     * 
     * @param x - pixel coordinate
     * @param z - pixel coordinate
     * @return tile coordinates. Null if there is no tile here.
     */
    public IntVector3 getTileAt(int x, int z) {
        int y = getLayer().getDepth(x, z);
        if (y == MapCanvas.MAX_DEPTH) {
            // No tile drawn here
            return null;
        }
        return this.zoom.screenToTile(new IntVector3(x, y, z));
    }

    /**
     * Gets the relative block coordinates of a particular block on the canvas
     * 
     * @param x - pixel coordinate
     * @param z - pixel coordinate
     * @return relative block coordinates at this position. Null if there is no tile here.
     */
    public IntVector3 getBlockAt(int x, int z) {
        IntVector3 tile = this.getTileAt(x, z);
        if (tile == null) {
            return null;
        } else {
            return getBlockAtTile(tile.x, tile.y, tile.z);
        }
    }

    public IntVector3 getBlockAtTile(int tx, int ty, int tz) {
        return MapUtil.screenTileToBlock(this.facing, tx + this.tile_offset_x, ty, tz + this.tile_offset_z);
    }

    private void renderSlice(int y) {
        getLayer().setDrawDepth(y);
        int tileIdx = -1;
        for (int dz = minRows; dz <= maxRows; dz++) {
            for (int dx = minCols; dx <= maxCols; dx++) {
                tileIdx++;
                if (!this.drawnTiles[tileIdx] && !drawBlock(dx, y, dz, true)) {

                    // Fully covered. No longer render this tile!
                    this.drawnTiles[tileIdx] = true;
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (menuShowTicks > 0 && --menuShowTicks == 0) {
            this.hideMenu();
        }
        for (MenuButton button : this.menuButtons) {
            button.onTick();
        }

        // Render at most 50 ms / map / tick
        long startTime = System.currentTimeMillis();
        if (this.currentRenderY <= VIEW_RANGE) {
            rendertime++;
            do {
                if (forwardRenderNeeded) {
                    renderSlice(currentRenderY);
                    forwardRenderNeeded = this.getLayer().hasMoreDepth();
                }
            } while (++this.currentRenderY <= VIEW_RANGE && (System.currentTimeMillis() - startTime) < 50);

            if (this.currentRenderY > VIEW_RANGE) {
                //System.out.println("RENDER TIME: " + rendertime);
            }
        }
    }

}
