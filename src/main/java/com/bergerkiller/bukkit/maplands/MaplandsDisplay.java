package com.bergerkiller.bukkit.maplands;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.events.map.MapClickEvent;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapBlendMode;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapFont.Alignment;
import com.bergerkiller.bukkit.common.math.Vector2;
import com.bergerkiller.bukkit.common.resources.SoundEffect;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.maplands.markers.MapMarkers;
import com.bergerkiller.bukkit.maplands.menu.MenuButton;
import com.bergerkiller.bukkit.maplands.menu.SettingsMenu;
import com.bergerkiller.bukkit.maplands.util.Linked2DTile;
import com.bergerkiller.bukkit.maplands.util.Linked2DTileList;
import com.bergerkiller.bukkit.maplands.util.Linked2DTileSet;

/**
 * Main display class of Maplands. Renders the world on the map and provides
 * a UI menu. Makes use of the Canvas depth buffer capabilities to do the sprite rendering.
 */
public class MaplandsDisplay extends MapDisplay {
    private final MapMarkers mapMarkers = new MapMarkers(this);
    private final MaplandsDisplayChunks chunks = new MaplandsDisplayChunks();
    private IsometricBlockSprites sprites;
    private ZoomLevel zoom;
    private BlockFace facing;
    private Block startBlock;
    private final MapBlockBounds blockBounds = new MapBlockBounds();
    private int menuShowTicks = 0;
    private int menuSelectIndex = 0;
    private int currentRenderZ;
    private int minimumRenderZ;
    private int maximumRenderZ;
    private int minCols, maxCols, minRows, maxRows;
    private Linked2DTileSet tilesThatNeedDrawing = new Linked2DTileSet();
    private final HashSet<IntVector3> dirtyTiles = new HashSet<IntVector3>();
    private MenuButton[] menuButtons;
    private MapTexture menu_bg;
    int rendertime = 0;
    private static final int MENU_DURATION = 200; // amount of ticks menu is kept open while idle

    @Override
    public void onAttached() {
        this.getLayer().fill(Maplands.getBackgroundColor());
        this.menuButtons = new MenuButton[] {
                new MenuButton("zoom_in", 1, 1) {
                    public void onPressed() {
                        zoom(1);
                    }
                },
                new MenuButton("zoom_out", 26, 1) {
                    public void onPressed() {
                        zoom(-1);
                    }
                },
                new MenuButton("rotate_left", 51, 1) {
                    public void onPressed() {
                        rotate(1);
                    }
                },
                new MenuButton("rotate_right", 76, 1) {
                    public void onPressed() {
                        rotate(-1);
                    }
                },
                new MenuButton("settings", 102, 1) {
                    public void onPressed() {
                        MaplandsDisplay.this.addWidget(new SettingsMenu());
                    }
                }
        };
        this.menu_bg = MapTexture.loadResource(MaplandsDisplay.class, "/com/bergerkiller/bukkit/maplands/textures/menu_bg.png");
        for (MenuButton button : this.menuButtons) {
            button.setDisplay(this);
        }

        this.mapMarkers.load();

        // Overwrite old sprites
        getLayer().setBlendMode(MapBlendMode.NONE);

        this.setSessionMode(MapSessionMode.FOREVER); // VIEWING for debug, FOREVER for release

        // Load from cache if possible
        if (Maplands.plugin.getCache().load(this.properties.getUniqueId(), this.getLayer())) {
            this.render(RenderMode.FROM_CACHE);
        } else {
            this.render(RenderMode.INITIALIZE);
        }

        refreshMapDisplayLookup();
    }

    @Override
    public void onDetached() {
        refreshMapDisplayLookup();

        // Save our current state to disk
        Maplands.plugin.getCache().save(this.properties.getUniqueId(), this.getLayer());

        // Release chunks we keep loaded
        chunks.clear();
    }

    public MapDisplayProperties getProperties() {
        return super.properties;
    }

    private static enum RenderMode {
        /** Renders the entire map from scratch */
        INITIALIZE,
        /** Renders assuming the map already contains color and depth info */
        FROM_CACHE,
        /** Renders assuming the map was translated, but has missing areas */
        TRANSLATION
    }

    private void render(RenderMode renderMode) {
        // If no start block is initialized yet, always switch to mode INITIALIZE
        // This is used if a world is unloaded, but is then loaded again
        if (startBlock == null && renderMode != RenderMode.FROM_CACHE) {
            renderMode = RenderMode.INITIALIZE;
        }

        // Read properties of the map
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

        // If not loaded, display a 'not loaded' message
        if (world == null) {
            this.renderWorldUnloaded(worldName);
            return;
        }

        // Get the correct sprites
        this.sprites = IsometricBlockSprites.getSprites(facing, this.zoom);

        // Start coordinates for the view
        this.startBlock = world.getBlockAt(px, py, pz);
        this.getLayer().setRelativeBrushMask(null);
        //this.getLayer().setDrawDepth(-VIEW_RANGE);
        //this.getLayer().fill(MapColorPalette.COLOR_RED);
        if (renderMode == RenderMode.INITIALIZE) {
            this.getLayer().clearDepthBuffer();
        }
        this.getLayer().setRelativeBrushMask(this.sprites.getBrushTexture());

        // Requires updated facing/startblock/zoom info to work right
        mapMarkers.viewChanged();

        int nrColumns = this.sprites.getZoom().getNumberOfColumns(this.getWidth());
        int nrRows = this.sprites.getZoom().getNumberOfRows(this.getHeight());
        this.minCols = -nrColumns;
        this.maxCols = nrColumns;
        this.minRows = -nrRows;
        this.maxRows = nrRows;

        this.minimumRenderZ = -nrRows - 2*(Maplands.getMaxRenderY() - py);
        this.maximumRenderZ = nrRows + 2*(py - Maplands.getMinRenderY());

        this.blockBounds.update(this.facing,
                this.minCols, this.minimumRenderZ, this.minRows,
                this.maxCols, this.maximumRenderZ, this.maxRows);
        this.blockBounds.offset(this.startBlock);

        if (renderMode == RenderMode.FROM_CACHE && this.properties.get("finishedRendering", false)) {
            this.currentRenderZ = this.maximumRenderZ + 1;
        } else {
            this.currentRenderZ = this.minimumRenderZ;
            this.properties.set("finishedRendering", false);
        }

        this.dirtyTiles.clear();

        // Reset drawn tiles state when initializing / from cache
        // This will cause everything to render again
        if (renderMode != RenderMode.TRANSLATION) {
            if (this.minCols != this.tilesThatNeedDrawing.getMinX() ||
                this.maxCols != this.tilesThatNeedDrawing.getMaxX() ||
                this.minRows != this.tilesThatNeedDrawing.getMinY() ||
                this.maxRows != this.tilesThatNeedDrawing.getMaxY())
            {
                this.tilesThatNeedDrawing = new Linked2DTileSet(this.minCols, this.maxCols,
                                                                this.minRows, this.maxRows);
            }
            this.tilesThatNeedDrawing.setAll();
        }

        rendertime = 0;
    }

    /**
     * Gets whether the world is loaded, and this map is
     * able to display it
     *
     * @return True if the world is loaded, and this display is initialized
     */
    public boolean isLoaded() {
        return this.startBlock != null;
    }

    public boolean isRenderingWorld(World world) {
        return this.startBlock != null && this.startBlock.getWorld() == world;
    }

    public boolean isBlockWithinBounds(int bx, int by, int bz) {
        return this.blockBounds.contains(bx, by, bz);
    }

    public void onBlockChange(Block block) {
        onBlockChange(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public void onBlockChange(World world, int bx, int by, int bz) {
        // Check possibly in range before doing computationally expensive stuff
        if (this.startBlock != null
                && world == this.startBlock.getWorld()
                && this.blockBounds.contains(bx, by, bz)
                && this.isLiveRefreshing()
        ) {
            int dx = bx - this.startBlock.getX();
            int dy = by - this.startBlock.getY();
            int dz = bz - this.startBlock.getZ();
            IntVector3 tile = MapUtil.blockToScreenTile(this.facing, dx, dy, dz);
            if (tile == null || tile.x < this.minCols || tile.x > this.maxCols || tile.y < this.minRows || tile.y > this.maxRows) {
                return;
            }
            this.dirtyTiles.add(tile);
        }
    }

    private void invalidateTile(int tx, int ty, int tz) {
        if (tx >= this.minCols && tx <= this.maxCols && ty >= this.minRows && ty <= this.maxRows) {
            this.tilesThatNeedDrawing.set(tx, ty);
            if (this.currentRenderZ > tz) {
                this.currentRenderZ = tz;
            }
        }
    }

    private void updateCheckHolding() {
        for (Player owner : this.getOwners()) {
            this.setReceiveInput(owner, this.isControlling(owner)  && !owner.isSneaking() && Permission.CHANGE_MAP.has(owner));
        }
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (this.getRootWidget().getWidgetCount() > 0) {
            // Menu widget is opened, do nothing with these keys
            super.onKeyPressed(event);
        } else if (this.menuShowTicks > 0) {
            menuShowTicks = MENU_DURATION; // keep on while interacted

            // Menu is shown. Intercepts keys.
            if (event.getKey() == Key.LEFT) {
                // Previous button
                this.setMenuIndex(this.menuSelectIndex - 1);
            } else if (event.getKey() == Key.RIGHT) {
                // Next button
                this.setMenuIndex(this.menuSelectIndex + 1);
            } else if (event.getKey() == Key.ENTER) {
                // Activate menu button
                this.playSound(SoundEffect.CLICK);
                this.menuButtons[this.menuSelectIndex].onPressed();
            } else if (event.getKey() == Key.BACK) {
                // Hide menu again
                this.hideMenu();
            }

        } else if (event.getKey() == Key.BACK) {
            // Show menu
            this.showMenu();
        } else if (event.getKey() == Key.ENTER) {
            // Re-render display. Also re-renders it if the world wasn't loaded, but is now.
            this.playSound(SoundEffect.EXTINGUISH);
            //this.getLayer().clear();
            this.renderAll();
        } else if (this.isLoaded()) {
            // No menu is shown. Simple navigation.
            if (event.getKey() == Key.UP) {
                moveStartBlock(facing, 1);
            } else if (event.getKey() == Key.RIGHT) {
                moveStartBlock(FaceUtil.rotate(facing, 2), 1);
            } else if (event.getKey() == Key.DOWN) {
                moveStartBlock(FaceUtil.rotate(facing, 4), 1);
            } else if (event.getKey() == Key.LEFT) {
                moveStartBlock(FaceUtil.rotate(facing, 6), 1);
            }
        }
    }

    /**
     * Draws a block at particular tile coordinates. The draw depth must have been set to
     * the tile coordinate z (depth) before drawing.
     * 
     * @param tx Tile x-coordinate (horizontal)
     * @param ty Tile y-coordinate (vertical)
     * @param tz Tile depth
     * @param isRedraw Whether to redraw the block entirely, instead of on top the current contents
     * @return result of the drawing operation
     */
    public DrawResult drawBlockTile(int tx, int ty, int tz, boolean isRedraw) {
        IntVector3 b = MapUtil.screenTileToBlock(this.facing, tx, ty, tz);
        if (b != null) {
            return drawBlockAtTile(b, tx, ty, isRedraw);
        } else {
            return DrawResult.PARTIALLY_DRAWN;
        }
    }

    /**
     * Draws a block at particular tile coordinates. The draw depth must have been set to
     * the tile coordinate z (depth) before drawing.
     * 
     * @param relativeBlockCoords Coordinates relative to start block to draw
     * @param tx Tile x-coordinate (horizontal)
     * @param ty Tile y-coordinate (vertical)
     * @param isRedraw Whether to redraw the block entirely, instead of on top the current contents
     * @return result of the drawing operation
     */
    public DrawResult drawBlockAtTile(IntVector3 relativeBlockCoords, int tx, int ty, boolean isRedraw) {
        int x = this.startBlock.getX() + relativeBlockCoords.x;
        int y = this.startBlock.getY() + relativeBlockCoords.y;
        int z = this.startBlock.getZ() + relativeBlockCoords.z;
        if (y < Maplands.getMinRenderY()) {
            return DrawResult.FULLY_DRAWN;
        } else if (y >= Maplands.getMaxRenderY()) {
            return DrawResult.PARTIALLY_DRAWN;
        } else {
            if (!this.chunks.cacheBlock(this.startBlock.getWorld(), x, z)) {
                return DrawResult.NOT_DRAWN;
            }

            IsometricBlockSprites.Sprite sprite = this.sprites.getSprite(this.startBlock.getWorld(), x, y, z);
            if (sprite != this.sprites.AIR || !isRedraw) {
                int draw_x = sprites.getZoom().getDrawX(tx) + (this.getWidth() >> 1);
                int draw_y = sprites.getZoom().getDrawY(ty) + (this.getHeight() >> 1);

                MapTexture texture = sprite.texture;
                getLayer().draw(texture, draw_x, draw_y);
                if (sprite.isFullyOpaque) {
                    // Fully opaque sprite, no need to check
                    return DrawResult.FULLY_DRAWN;
                } else {
                    // Ask canvas whether any more pixels remain to be drawn
                    return getLayer().hasMoreDepth(draw_x, draw_y, texture.getWidth(), texture.getHeight()) ?
                            DrawResult.PARTIALLY_DRAWN : DrawResult.FULLY_DRAWN;
                }
            } else {
                return DrawResult.PARTIALLY_DRAWN;
            }
        }
    }

    public void hideMenu() {
        this.playSound(SoundEffect.PISTON_CONTRACT);
        menuShowTicks = 0;
        for (MenuButton button : this.menuButtons) {
            button.setVisible(false);
        }
        getLayer(1).clearRectangle(0, 0, this.menu_bg.getWidth(), this.menu_bg.getHeight());
    }

    public void showMenu() {
        this.playSound(SoundEffect.PISTON_EXTEND);
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
        this.renderAll();
    }

    public void rotate(int n) {
        BlockFace facing = properties.get("facing", BlockFace.NORTH_EAST);
        facing = FaceUtil.rotate(facing, n * 2);
        properties.set("facing", facing);
        this.renderAll();
    }

    /**
     * Sends the render command to use to render this map, to the player
     *
     * @param recipient
     */
    public void sendRenderCommand(Player recipient) {
        recipient.sendMessage("To render the map you are holding, execute:");
        String rcmd = "/map render " + this.getProperties().getUniqueId().toString();
        ChatText.fromClickableSuggestedCommand(ChatColor.UNDERLINE + rcmd, rcmd)
                .setHoverText("Type the command into the chat")
                .sendTo(recipient);
    }

    /**
     * Gets the map marker configuration of this maplands display
     *
     * @return map markers
     */
    public MapMarkers getMapMarkers() {
        return this.mapMarkers;
    }

    /**
     * Whether block changes cause the display to automatically re-render
     * the changed blocks.
     *
     * @return True if refreshing live
     */
    public boolean isLiveRefreshing() {
        return properties.get("liveRefresh", true);
    }

    /**
     * Sets whether block changes cause the display to automatically re-render
     * the changed blocks.
     *
     * @param live Whether live refreshing is turned on
     */
    public void setLiveRefreshing(boolean live) {
        properties.set("liveRefresh", live);
    }

    /**
     * Re-renders the display, causing all blocks shown to be re-drawn
     * onto the map.
     */
    public void renderAll() {
        this.render(RenderMode.INITIALIZE);
    }

    /**
     * Gets the start block, which is the block the player was at when activating this maplands map.
     * 
     * @return start block
     */
    public Block getStartBlock() {
        return this.startBlock;
    }

    /**
     * Sets start block coordinates and world. Performs a translation of the map if possible, otherwise
     * re-renders the entire map.
     * 
     * @param bx Start block X-coordinate
     * @param by Start block Y-coordinate
     * @param bz Start block Z-coordinate
     */
    public void setStartBlock(Block block) {
        String oldWorldName = this.properties.get("mapWorld", String.class);
        if (oldWorldName == null || !oldWorldName.equals(block.getWorld().getName())) {
            this.properties.set("mapWorld", block.getWorld().getName());
            this.properties.set("px", block.getX());
            this.properties.set("py", block.getY());
            this.properties.set("pz", block.getZ());
            this.renderAll();
        } else {
            this.setStartBlock(block.getX(), block.getY(), block.getZ());
        }
    }

    /**
     * Sets start block coordinates. Performs a translation of the map if possible, otherwise
     * re-renders the entire map.
     * 
     * @param bx Start block X-coordinate
     * @param by Start block Y-coordinate
     * @param bz Start block Z-coordinate
     */
    public void setStartBlock(int bx, int by, int bz) {
        int old_x = this.properties.get("px", 0);
        int old_y = this.properties.get("py", 0);
        int old_z = this.properties.get("pz", 0);
        moveStartBlock(bx - old_x, by - old_y, bz - old_z);
    }

    /**
     * Moves the start block, translating the map accordingly
     * 
     * @param direction The direction to translate the start block
     * @param amount Number of times to translate by direction
     */
    public void moveStartBlock(BlockFace direction, int amount) {
        moveStartBlock(direction.getModX()*amount,
                       direction.getModY()*amount,
                       direction.getModZ()*amount);
    }

    /**
     * Moves the start block, translating the map accordingly
     * 
     * @param dx Delta in start block X-coordinate
     * @param dy Delta in start block Y-coordinate
     * @param dz Delta in start block Z-coordinate
     */
    public void moveStartBlock(int dx, int dy, int dz) {
        int old_x = this.properties.get("px", 0);
        int old_y = this.properties.get("py", 0);
        int old_z = this.properties.get("pz", 0);

        // Update
        this.properties.set("px", old_x + dx);
        this.properties.set("py", old_y + dy);
        this.properties.set("pz", old_z + dz);

        IntVector3 old_tile = MapUtil.blockToScreenTile(this.facing, 0, 0, 0);
        IntVector3 new_tile = MapUtil.blockToScreenTile(this.facing, dx, dy, dz);

        int pixels_dx = zoom.getScreenX(old_tile.x) - zoom.getScreenX(new_tile.x);
        int pixels_dy = zoom.getScreenY(old_tile.y) - zoom.getScreenY(new_tile.y);
        if (Math.abs(pixels_dx) >= this.getWidth() || Math.abs(pixels_dy) >= this.getHeight()) {
            // Change is so large the entire map blanks out. Just do a re-render of everything
            this.renderAll();
        } else {
            // Move the pixels already drawn
            getLayer().movePixels(pixels_dx, pixels_dy);

            // Delta in tiles
            int dtx = old_tile.x - new_tile.x;
            int dty = old_tile.y - new_tile.y;

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
            if (dty > 0) {
                fMinRows += dty + 7;
            } else if (dty < 0) {
                fMaxRows += dty - 5;
            }

            // Move the drawn tile state buffer
            {
                Linked2DTileSet newTilesThatNeedDrawing = new Linked2DTileSet(this.minCols, this.maxCols,
                                                                              this.minRows, this.maxRows);
                newTilesThatNeedDrawing.setAll();
                for (Linked2DTile tile : this.tilesThatNeedDrawing.iterateInverse()) {
                    int mx = tile.x + dtx;
                    int my = tile.y + dty;
                    if (mx >= fMinCols && mx <= fMaxCols && my >= fMinRows && my <= fMaxRows) {
                        newTilesThatNeedDrawing.clear(mx, my);
                    }
                 }
                 this.tilesThatNeedDrawing = newTilesThatNeedDrawing;
            }

            // When moving up or down, the depth buffer values are incremented/decremented
            if (dty != 0) {
                short[] buffer = this.getLayer().getDepthBuffer();
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] != MapCanvas.MAX_DEPTH) {
                        buffer[i] -= dty;
                    }
                }
            }

            this.render(RenderMode.TRANSLATION);
        }
    }

    /**
     * Gets the facing direction when rendering the map. Is always either NORTH_EAST,
     * NORTH_WEST, SOUTH_EAST or SOUTH_WEST.
     * 
     * @return facing direction
     */
    public BlockFace getFacing() {
        return this.facing;
    }

    /**
     * Gets the exact tile coordinates of particular pixel coordinates
     * 
     * @param x - pixel coordinate
     * @param y - pixel coordinate
     * @return tile coordinates. Null if there is no tile here.
     */
    public IntVector3 getTileAt(int x, int y) {
        int z = getLayer().getDepth(x, y);
        if (z == MapCanvas.MAX_DEPTH) {
            // No tile drawn here
            return null;
        }
        return this.zoom.screenToTile(new IntVector3(x - (this.getWidth() >> 1), y - (this.getHeight() >> 1), z));
    }

    /**
     * Looks up the tile of the block at a given position, and using this tile what the exact
     * pixel coordinates on the screen are for this position. Sub-pixel information is
     * retained.
     * 
     * @param position World coordinates of the position to compute
     * @return pixel coordinates on the screen, z is depth level
     */
    public Vector getScreenCoordinates(Vector position) {
        int bx = position.getBlockX() - this.startBlock.getX();
        int by = position.getBlockY() - this.startBlock.getY();
        int bz = position.getBlockZ() - this.startBlock.getZ();
        IntVector3 tile = MapUtil.blockToScreenTile(this.facing, bx, by, bz);
        if (tile == null) {
            return new Vector();
        }
        double sx = sprites.getZoom().getScreenX(tile.x) + (this.getWidth() >> 1);
        double sy = sprites.getZoom().getScreenY(tile.y) + (this.getHeight() >> 1);

        // Adjust for within-block coordinates
        Vector2 inBlock = sprites.getZoom().getBlockPixelCoordinates(this.facing,
                position.getX() - position.getBlockX() - 0.5,
                position.getY() - position.getBlockY() - 0.5,
                position.getZ() - position.getBlockZ() - 0.5);

        return new Vector(sx + inBlock.x, sy + inBlock.y, tile.z);
    }

    /**
     * Gets the exact block displayed at particular pixel coordinates
     * 
     * @param x - pixel coordinate (horizontal)
     * @param y - pixel coordinate (vertical)
     * @return block at these coordinates. Null when clicking outside the canvas or in the void.
     */
    public Block getBlockAt(int x, int y) {
        IntVector3 tile = this.getTileAt(x, y);
        if (tile == null) {
            return null;
        } else {
            IntVector3 relativeBlockCoords = MapUtil.screenTileToBlock(this.facing, tile.x, tile.y, tile.z);
            return this.startBlock.getRelative(relativeBlockCoords.x, relativeBlockCoords.y, relativeBlockCoords.z);
        }
    }

    /**
     * Renders a 'world not loaded' screen
     *
     * @param worldName
     */
    private void renderWorldUnloaded(String worldName) {
        this.startBlock = null;
        this.chunks.clear();
        this.hideMenu();
        this.clearMarkers();
        this.clearWidgets();
        this.getLayer().clearDepthBuffer();
        this.getLayer(1).clear();

        this.getLayer().setBlendMode(MapBlendMode.NONE);
        this.getLayer().fill(MapColorPalette.getColor(64, 64, 64));

        MapTexture text = MapTexture.createEmpty(128, 32);
        text.setAlignment(Alignment.MIDDLE);
        text.draw(MapFont.MINECRAFT, text.getWidth()/2, text.getHeight()/2 - 10,
                MapColorPalette.COLOR_RED, "World is not loaded");
        text.draw(MapFont.MINECRAFT, text.getWidth()/2, text.getHeight()/2 + 9,
                MapColorPalette.COLOR_ORANGE, worldName);

        // Scale it up to fit
        // TODO: More efficient, or something in BKCommonLib?
        int scale = Math.min(this.getWidth()/text.getWidth(), this.getHeight()/text.getHeight());
        if (scale > 1) {
            MapTexture scaled = MapTexture.createEmpty(text.getWidth()*scale, text.getHeight()*scale);
            for (int y = 0; y < text.getHeight(); y++) {
                for (int x = 0; x < text.getWidth(); x++) {
                    scaled.fillRectangle(x*scale, y*scale, scale, scale, text.readPixel(x, y));
                }
            }
            text = scaled;
        }

        int x = (this.getWidth() - text.getWidth()) / 2;
        int y = (this.getHeight() - text.getHeight()) / 2;
        this.getLayer(1).setBlendMode(MapBlendMode.NONE);
        this.getLayer(1).draw(text, x, y);
    }

    /**
     * Renders a single depth level onto the canvas
     * 
     * @param depth The depth to render (same as z-coordinate of the tile)
     * @return result of drawing the slice
     */
    private DrawResult renderSlice(int depth) {
        getLayer().setDrawDepth(depth);
        boolean mapIsFullyDrawn = true;
        boolean sliceHasNotDrawnTiles = false;

        {
            Linked2DTileList list = this.tilesThatNeedDrawing.getValidTiles(depth);
            Linked2DTile current = list.head;
            while ((current = current.next) != list.tail) {
                switch (drawBlockAtTile(current.toBlock(this.facing, depth), current.x, current.y, true)) {
                case NOT_DRAWN:
                    sliceHasNotDrawnTiles = true;
                    break;
                case PARTIALLY_DRAWN:
                    mapIsFullyDrawn = false;
                    break;
                case FULLY_DRAWN:
                    // Fully covered. No longer render this tile!
                    current = current.remove();
                    break;
                }
            }
        }

        if (sliceHasNotDrawnTiles) {
            return DrawResult.NOT_DRAWN;
        } else if (mapIsFullyDrawn) {
            return DrawResult.FULLY_DRAWN;
        } else {
            return DrawResult.PARTIALLY_DRAWN;
        }
    }

    @Override
    public void onRightClick(MapClickEvent event) {
        if (!event.getPlayer().isSneaking()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onTick() {
        // Receive player input from players holding the map in the main hand,
        // that are not sneaking and have permission to edit
        updateCheckHolding();

        // If not loaded, do nothing
        if (this.startBlock == null) {
            return;
        }

        // If startBlock refers to a now-unloaded world, unload the map
        if (Bukkit.getWorld(this.startBlock.getWorld().getName()) == null) {
            this.renderAll();
            return;
        }

        if (this.getRootWidget().getWidgetCount() == 0) {
            if (menuShowTicks > 0 && --menuShowTicks == 0) {
                this.hideMenu();
            }
            for (MenuButton button : this.menuButtons) {
                button.onTick();
            }
        } else {
            // While menu is active, show last selected button on continuously
            for (MenuButton button : this.menuButtons) {
                button.setBlinkOn();
            }
        }

        // Refresh displayed markers
        this.mapMarkers.update();

        // Unload chunks we haven't used in a while
        this.chunks.update();

        // Re-render all dirty tiles
        // If they result in holes, schedule the area behind for re-rendering
        if (!dirtyTiles.isEmpty()) {
            Iterator<IntVector3> iter = this.dirtyTiles.iterator();
            while (iter.hasNext()) {
                IntVector3 tile = (IntVector3) iter.next();
                getLayer().setDrawDepth(tile.z);
                DrawResult tileResult = drawBlockTile(tile.x, tile.y, tile.z, false);
                if (tileResult == DrawResult.NOT_DRAWN) {
                    continue; // Try again next tick
                }

                // Processed, remove the coordinate
                iter.remove();

                // Redraw neighbours too
                if (tileResult == DrawResult.PARTIALLY_DRAWN) {
                    for (int dtx = -1; dtx <= 1; dtx++) {
                        for (int dty = -2; dty <= 2; dty++) {
                            invalidateTile(tile.x + dtx, tile.y + dty, tile.z);
                        }
                    }
                }
            }
        }

        // Render at most 50 ms / map / tick
        long startTime = System.currentTimeMillis();
        if (this.currentRenderZ <= this.maximumRenderZ) {
            rendertime++;
            do {
                DrawResult sliceResult = renderSlice(currentRenderZ);
                if (sliceResult == DrawResult.FULLY_DRAWN) {
                    this.currentRenderZ = this.maximumRenderZ + 1;
                    break;
                } else if (sliceResult == DrawResult.NOT_DRAWN) {
                    break; // Try same slice again next tick
                }
            } while (++this.currentRenderZ <= this.maximumRenderZ && (System.currentTimeMillis() - startTime) < Maplands.getMaxRenderTime());

            if (this.currentRenderZ > this.maximumRenderZ) {
                // Fill all remaining holes with the desired background color
                for (int x = 0; x < this.getWidth(); x++) {
                    for (int y = 0; y < this.getHeight(); y++) {
                        if (this.getLayer().getDepth(x, y) == MapCanvas.MAX_DEPTH) {
                            this.getLayer().writePixel(x, y, Maplands.getBackgroundColor());
                        }
                    }
                }

                // Store in attributes that it has finished rendering
                if (!properties.get("finishedRendering", false)) {
                    properties.set("finishedRendering", true);
                    Maplands.plugin.getCache().save(this.properties.getUniqueId(), this.getLayer());
                }

                // CommonUtil.broadcast("Render time: " + rendertime + " ticks");
            }
        }
    }

    // Refreshed automatically and cached
    // It is used very often to handle block physics; this makes this faster
    private static Collection<MaplandsDisplay> all_maplands_displays = Collections.emptySet();
    private static Task refresh_mapdisplays_task = null;
    private static void refreshMapDisplayLookup() {
        // Refresh now, and again one tick delayed, to be sure it is updated.
        all_maplands_displays = MapDisplay.getAllDisplays(MaplandsDisplay.class);
        if (refresh_mapdisplays_task == null) {
            refresh_mapdisplays_task = new Task(Maplands.plugin) {
                @Override
                public void run() {
                    all_maplands_displays = MapDisplay.getAllDisplays(MaplandsDisplay.class);
                    refresh_mapdisplays_task = null;
                }
            };
        }
    }

    public static Collection<MaplandsDisplay> getAllDisplays() {
        return all_maplands_displays;
    }

    /**
     * Result of drawing a single block tile
     */
    public static enum DrawResult {
        /** Tile is fully drawn, and no further blocks behind this tile need drawing */
        FULLY_DRAWN,
        /** Tile is partially drawn, blocks behind the current block need drawing */
        PARTIALLY_DRAWN,
        /** Tile wasn't drawn because the chunk isn't loaded yet, and needs drawn again */
        NOT_DRAWN
    }
}
