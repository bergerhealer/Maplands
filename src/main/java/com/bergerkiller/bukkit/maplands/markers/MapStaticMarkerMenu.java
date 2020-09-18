package com.bergerkiller.bukkit.maplands.markers;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;

/**
 * Configures a single static map marker
 */
public class MapStaticMarkerMenu extends MapWidgetWindow {
    private final MapStaticMarker marker;

    public MapStaticMarkerMenu(MapStaticMarker marker) {
        this.marker = marker;

        this.setFocusable(true);
        this.setBounds(7, 7, 100, 100);
        this.setDepthOffset(2);
        this.setBackgroundColor(MapColorPalette.COLOR_BLUE);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.activate();

        // Remove button
        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                marker.remove();
                close();
            }
        }).setText("Remove")
          .setBounds(52, 80, 40, 11);
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (event.getKey() == Key.BACK && this.isActivated()) {
            this.close();
            return;
        }
        super.onKeyPressed(event);
    }

    /**
     * Closes this menu, removing this window
     */
    public void close() {
        this.removeWidget();
    }
}
