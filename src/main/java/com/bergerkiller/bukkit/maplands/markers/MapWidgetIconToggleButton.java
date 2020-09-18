package com.bergerkiller.bukkit.maplands.markers;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.resources.SoundEffect;

/**
 * Simple on/off toggle button with 2 image textures
 */
public abstract class MapWidgetIconToggleButton extends MapWidget {
    private MapTexture on_texture, off_texture;
    private boolean on = false;
    private boolean attached = false;
    public final MapWidgetTooltip tooltip = new MapWidgetTooltip();

    public MapWidgetIconToggleButton() {
        this.setFocusable(true);
    }

    public abstract void onToggled();

    public MapWidgetIconToggleButton setTooltip(String tooltipText) {
        this.tooltip.setText(tooltipText);
        return this;
    }

    public boolean isOn() {
        return this.on;
    }

    public MapWidgetIconToggleButton setOn(boolean on) {
        if (this.on != on) {
            this.on = on;
            this.invalidate();
            if (this.attached) {
                onToggled();
            }
        }
        return this;
    }

    public MapWidgetIconToggleButton setOnTexture(String textureName) {
        this.on_texture = MapTexture.loadResource(MapWidgetIconToggleButton.class,
                "/com/bergerkiller/bukkit/maplands/textures/" + textureName);
        this.setSize(this.on_texture.getWidth()+2, this.on_texture.getHeight()+2);
        return this;
    }

    public MapWidgetIconToggleButton setOffTexture(String textureName) {
        this.off_texture = MapTexture.loadResource(MapWidgetIconToggleButton.class,
                "/com/bergerkiller/bukkit/maplands/textures/" + textureName);
        this.setSize(this.on_texture.getWidth()+2, this.on_texture.getHeight()+2);
        return this;
    }

    @Override
    public void onAttached() {
        attached = true;
    }

    @Override
    public void onDraw() {
        if (this.isFocused()) {
            view.drawRectangle(0, 0, getWidth(), getHeight(), MapColorPalette.COLOR_RED);
        } else {
            view.drawRectangle(0, 0, getWidth(), getHeight(), MapColorPalette.COLOR_BLACK);
        }
        view.draw(on ? on_texture : off_texture, 1, 1);
    }

    @Override
    public void onActivate() {
        this.setOn(!this.isOn());
        display.playSound(SoundEffect.CLICK);
    }

    @Override
    public void onFocus() {
        super.onFocus();

        this.addWidget(this.tooltip);

        // Click navigation sounds
        display.playSound(SoundEffect.CLICK_WOOD);
    }

    @Override
    public void onBlur() {
        super.onBlur();
        this.removeWidget(this.tooltip);
    }
}
