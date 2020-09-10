package com.bergerkiller.bukkit.maplands.markers;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;

public class MapMarkerMenu extends MapWidgetWindow {
    private final MapMarkers markers;

    public MapMarkerMenu(MapMarkers markers) {
        this.markers = markers;

        this.setFocusable(true);
        this.setBounds(7, 7, 114, 114);
        this.setDepthOffset(2);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.activate();

        this.addWidget(new MapMarkerTypeSelector() {
            @Override
            public void onTypeChanged() {
                markers.setMarkerTypeWhenHeld(getType());
            }
        }).setType(markers.getMarkerTypeWhenHeld()).setPosition(30, 30);
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
