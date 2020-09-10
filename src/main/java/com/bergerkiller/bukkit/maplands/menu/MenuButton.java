package com.bergerkiller.bukkit.maplands.menu;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapTexture;

public class MenuButton {
    private final int x, y;
    private final MapTexture texture_on;
    private final MapTexture texture_off;
    private boolean selected;
    private boolean visible;
    private boolean changed;
    private int blinkCtr;
    private MapDisplay display;

    public MenuButton(String textureName, int x, int y) {
        this.x = x;
        this.y = y;
        this.selected = false;
        this.visible = false;
        this.changed = false;
        this.blinkCtr = 0;

        MapTexture texture_base = MapTexture.loadResource(MenuButton.class, "/com/bergerkiller/bukkit/maplands/textures/" + textureName + ".png");

        this.texture_off = MapTexture.createEmpty(texture_base.getWidth(), texture_base.getHeight());
        this.texture_on = MapTexture.createEmpty(texture_base.getWidth(), texture_base.getHeight());
        this.texture_on.draw(texture_base, 0, 0, MapColorPalette.COLOR_YELLOW);
        this.texture_off.draw(texture_base, 0, 0, MapColorPalette.COLOR_BLACK);
    }

    public void setDisplay(MapDisplay display) {
        this.display = display;
    }

    public void onTick() {
        if (this.changed) {
            this.changed = false;
            if (this.visible) {
                if (this.selected) {
                    display.getLayer(2).draw(texture_on, x, y);
                } else {
                    display.getLayer(2).draw(texture_off, x, y);
                }
            } else {
                display.getLayer(2).clearRectangle(x, y, texture_on.getWidth(), texture_on.getHeight());
            }
        }
        if (this.visible && this.selected) {
            if (this.blinkCtr == 0) {
                display.getLayer(2).draw(texture_on, x, y);
            } else if (this.blinkCtr == 5) {
                display.getLayer(2).draw(texture_off, x, y);
            }
            if (++this.blinkCtr == 10) {
                this.blinkCtr = 0;
            }
        }
    }

    public void onPressed() {
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            this.changed = true;
            this.blinkCtr = 0;
        }
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            this.changed = true;
            this.blinkCtr = 0;
        }
    }

}
