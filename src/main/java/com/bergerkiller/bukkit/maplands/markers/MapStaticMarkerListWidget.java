package com.bergerkiller.bukkit.maplands.markers;

import java.util.List;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.resources.SoundEffect;
import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * Shows a List of static map markers that the player can
 * scroll through.
 */
public abstract class MapStaticMarkerListWidget extends MapWidget implements MapWidgetWithMarkers {
    private static final int NUM_ROWS = 6;
    private static final int ROW_WIDTH = 91;
    private static final int ROW_HEIGHT = 8;
    private final MapMarkers markers;
    private MapMarker[] previewMarkers;
    private int scrollOffset = 0;
    private int selectedIndex = 0;
    private boolean markersHidden = false;

    public MapStaticMarkerListWidget(MapMarkers markers) {
        this.markers = markers;
        this.setSize(ROW_WIDTH, NUM_ROWS * ROW_HEIGHT + 2);
        this.setFocusable(true);
    }

    public abstract void onItemActivated();

    @Override
    public void setMarkersHidden(boolean hidden) {
        if (markersHidden != hidden) {
            markersHidden = hidden;
            updateMarkers();
        }
    }

    public MapStaticMarker getSelectedItem() {
        List<MapStaticMarker> staticMarkers = markers.getStaticMarkers();
        return staticMarkers.get(MathUtil.clamp(selectedIndex, 0, staticMarkers.size()));
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        List<MapStaticMarker> staticMarkers = markers.getStaticMarkers();
        if (staticMarkers.isEmpty()) {
            selectedIndex = 0;
            scrollOffset = 0;
            invalidate();
            return;
        }

        if (index < 0) {
            index = 0;
        } else if (index >= staticMarkers.size()) {
            index = staticMarkers.size() - 1;
        }

        if (index != selectedIndex) {
            selectedIndex = index;
            scrollToSelection();
            invalidate();
            display.playSound(SoundEffect.CLICK);
        }
    }

    public void scrollToSelection() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
            invalidate();
        } else if (selectedIndex >= (scrollOffset + NUM_ROWS)) {
            scrollOffset = selectedIndex - NUM_ROWS + 1;
            invalidate();
        }
    }

    private void updateMarkers() {
        List<MapStaticMarker> staticMarkers = markers.getStaticMarkers();
        for (int i = 0; i < previewMarkers.length; i++) {
            int index = scrollOffset + i;
            MapMarker previewMarker = previewMarkers[i];
            if (!markersHidden && index >= 0 && index < staticMarkers.size()) {
                previewMarker.setType(staticMarkers.get(index).type);
                previewMarker.setVisible(true);
            } else {
                previewMarker.setVisible(false);
            }
        }
    }

    @Override
    public void onAttached() {
        previewMarkers = new MapMarker[NUM_ROWS];
        selectedIndex = 0;
        double x = (double) this.getAbsoluteX() + 4.5;
        double y = (double) this.getAbsoluteY() + 4.0;
        for (int i = 0; i < previewMarkers.length; i++) {
            previewMarkers[i] = display.createMarker();
            previewMarkers[i].setPosition(x, y + i * ROW_HEIGHT);
        }
        updateMarkers();
    }

    @Override
    public void onDetached() {
        if (previewMarkers != null) {
            for (MapMarker marker : previewMarkers) {
                marker.remove();
            }
            previewMarkers = null;
        }
    }

    @Override
    public void onActivate() {
        if (!this.markers.getStaticMarkers().isEmpty()) {
            super.onActivate();
        }
    }

    @Override
    public void onDraw() {
        // Draw background of the list
        view.fill(MapColorPalette.getColor(80, 80, 80));

        // Draw the frame around the items
        byte frame_color = this.isFocused() ? MapColorPalette.COLOR_YELLOW : MapColorPalette.COLOR_BLACK;
        view.drawRectangle(0, 0, getWidth(), getHeight(), frame_color);
        for (int n = 1; n < NUM_ROWS; n++) {
            int y = n * ROW_HEIGHT;
            view.drawLine(1, y, getWidth()-2, y, frame_color);
        }

        // Draw contents
        List<MapStaticMarker> staticMarkers = markers.getStaticMarkers();
        int endIndex = Math.min(scrollOffset + NUM_ROWS, staticMarkers.size());
        int item_y = 1;
        for (int i = scrollOffset; i < endIndex; i++) {
            MapStaticMarker marker = staticMarkers.get(i);
            MapCanvas itemArea = view.getView(1, item_y, getWidth()-2, ROW_HEIGHT-1);

            if (this.isActivated() && i == selectedIndex) {
                itemArea.fill(MapColorPalette.COLOR_YELLOW);
            }

            itemArea.draw(MapFont.TINY, 8, 1, MapColorPalette.COLOR_BLUE, marker.caption);
            item_y += ROW_HEIGHT;
        }

        // Just to make sure
        updateMarkers();
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (!this.isActivated()) {
            super.onKeyPressed(event);
        } else if (event.getKey() == MapPlayerInput.Key.BACK) {
            this.deactivate();
        } else if (event.getKey() == MapPlayerInput.Key.ENTER) {
            if (!this.markers.getStaticMarkers().isEmpty()) {
                this.onItemActivated();
            }
        } else if (event.getKey() == MapPlayerInput.Key.UP) {
            this.setSelectedIndex(this.getSelectedIndex()-1);
        } else if (event.getKey() == MapPlayerInput.Key.DOWN) {
            this.setSelectedIndex(this.getSelectedIndex()+1);
        }
    }
}
