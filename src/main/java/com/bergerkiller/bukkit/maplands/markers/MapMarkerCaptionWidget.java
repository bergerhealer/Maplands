package com.bergerkiller.bukkit.maplands.markers;

import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;

/**
 * Shows the caption of a marker
 */
public class MapMarkerCaptionWidget extends MapWidget {
    private String _caption;

    public MapMarkerCaptionWidget() {
        this.setFocusable(true);
        this._caption = "";
    }

    public MapMarkerCaptionWidget setCaption(String caption) {
        this._caption = caption;
        this.invalidate();
        return this;
    }

    @Override
    public void onDraw() {
        view.drawRectangle(0, 0, getWidth(), getHeight(),
                isFocused() ? MapColorPalette.COLOR_YELLOW : MapColorPalette.COLOR_BLACK);

        MapCanvas textArea = view.getView(1, 1, getWidth()-2, getHeight()-2);
        textArea.fill(MapColorPalette.getColor(80, 80, 80));
        textArea.draw(MapFont.MINECRAFT, 1, 1, MapColorPalette.COLOR_WHITE, this._caption);
    }
}
