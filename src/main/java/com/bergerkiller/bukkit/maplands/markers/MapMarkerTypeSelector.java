package com.bergerkiller.bukkit.maplands.markers;

import java.util.List;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.resources.SoundEffect;

/**
 * Shows the marker currently selected, and when activated, shows
 * a list of markers which can be navigated to change the selection.
 */
public abstract class MapMarkerTypeSelector extends MapWidget {
    private static final MapTexture NO_MARKER = MapTexture.loadResource(MapMarkerTypeSelector.class,
            "/com/bergerkiller/bukkit/maplands/textures/no_marker.png");

    private MapMarker.Type type = null;
    private MapMarker previewMarker = null;
    private final List<MapMarker.Type> types = MapMarker.Type.values();
    private int typeIndex = -1;
    private MarkerList list = null;
    private boolean markersHidden = false;
    public final MapWidgetTooltip tooltip = new MapWidgetTooltip();

    /**
     * Called when the type is changed inside the selector
     */
    public abstract void onTypeChanged();

    public MapMarkerTypeSelector() {
        this.setSize(10, 10);
        this.setFocusable(true);
        this.tooltip.setText("Marker Type");
    }

    public MapMarker.Type getType() {
        return type;
    }

    public MapMarkerTypeSelector setTooltip(String tooltipText) {
        this.tooltip.setText(tooltipText);
        return this;
    }

    public MapMarkerTypeSelector setType(MapMarker.Type type) {
        this.type = type;
        this.typeIndex = types.indexOf(type);
        this.invalidate();
        if (previewMarker != null) {
            if (type == null) {
                previewMarker.setVisible(false);
            } else {
                previewMarker.setType(type);
                previewMarker.setVisible(true);
            }
        }
        if (list != null) {
            list.updateMarkers();
            list.invalidate();
        }
        if (previewMarker != null) {
            onTypeChanged();
        }
        return this;
    }

    public void setMarkersHidden(boolean hidden) {
        if (markersHidden != hidden) {
            markersHidden = hidden;
            if (hidden) {
                onDetached();
            } else {
                onAttached();
            }
        }
    }

    @Override
    public void onAttached() {
        previewMarker = display.createMarker();
        if (this.type != null) {
            previewMarker.setType(this.type);
        } else {
            previewMarker.setVisible(false);
        }
        previewMarker.setPosition((double) this.getAbsoluteX() + 0.5 * this.getWidth() + 0.5, getMarkerBaseY());
    }

    @Override
    public void onDetached() {
        if (previewMarker != null) {
            previewMarker.remove();
        }
    }

    @Override
    public void onDraw() {
        if (this.isFocused() || this.isActivated()) {
            view.fillRectangle(1, 1, getWidth()-2, getHeight()-2, MapColorPalette.getColor(150, 64, 64));
            view.drawRectangle(0, 0, getWidth(), getHeight(), MapColorPalette.COLOR_RED);
        } else {
            view.fillRectangle(1, 1, getWidth()-2, getHeight()-2, MapColorPalette.getColor(100, 100, 100));
            view.drawRectangle(0, 0, getWidth(), getHeight(), MapColorPalette.COLOR_BLACK);
        }
        if (this.type == null) {
            view.draw(NO_MARKER, 1, 1);
        }
    }

    private double getMarkerBaseY() {
        return (double) this.getAbsoluteY() + 0.5 * this.getHeight() - 0.5;
    }

    @Override
    public void onActivate() {
        if (list == null) {
            list = this.addWidget(new MarkerList());
            this.removeWidget(this.tooltip);
            display.playSound(SoundEffect.PISTON_CONTRACT);
        }
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (list == null) {
            super.onKeyPressed(event);
            return;
        }

        if (event.getKey() == MapPlayerInput.Key.BACK ||
            event.getKey() == MapPlayerInput.Key.ENTER)
        {
            this.removeWidget(list);
            this.addWidget(this.tooltip);
            display.playSound(SoundEffect.PISTON_EXTEND);
            list = null;
        } else if (event.getKey() == MapPlayerInput.Key.UP) {
            if (typeIndex == 0) {
                setType(null);
                display.playSound(SoundEffect.CLICK);
            } else if (typeIndex > 0) {
                setType(types.get(typeIndex-1));
                display.playSound(SoundEffect.CLICK);
            }
        } else if (event.getKey() == MapPlayerInput.Key.DOWN) {
            if (typeIndex < (types.size()-1)) {
                setType(types.get(typeIndex+1));
                display.playSound(SoundEffect.CLICK);
            }
        } else {
            // Left/right pressed exits the menu and then allows for navigation
            this.removeWidget(list);
            this.addWidget(this.tooltip);
            list = null;
            super.onKeyPressed(event);
        }
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

    // Shows 2 additional markers above and below the marker
    private class MarkerList extends MapWidget {

        public MarkerList() {
            this.setBounds(0, -18, MapMarkerTypeSelector.this.getWidth(), 10 + 2 * 18);
        }

        @Override
        public void onAttached() {
            double mx = MapMarkerTypeSelector.this.getAbsoluteX() + (this.getWidth() / 2) + 0.5;
            double my = MapMarkerTypeSelector.this.getMarkerBaseY();
            double dy = 9.0;
            display.createMarker("typeselector_-2").setPosition(mx, my - 2.0 * dy);
            display.createMarker("typeselector_-1").setPosition(mx, my - dy);
            display.createMarker("typeselector_1").setPosition(mx, my + dy);
            display.createMarker("typeselector_2").setPosition(mx, my + 2.0 * dy);
            updateMarkers();
        }

        public void updateMarkers() {
            for (int i = -2; i <= 2; i++) {
                if (i == 0) {
                    continue;
                }

                int index = typeIndex + i;
                MapMarker marker = display.getMarker("typeselector_" + Integer.toString(i));
                if (index >= 0 && index < types.size()) {
                    marker.setType(types.get(index));
                    marker.setVisible(true);
                } else {
                    marker.setVisible(false);
                }
            }
        }

        @Override
        public void onDetached() {
            display.removeMarker("typeselector_-2");
            display.removeMarker("typeselector_-1");
            display.removeMarker("typeselector_1");
            display.removeMarker("typeselector_2");
        }

        @Override
        public void onDraw() {
            byte bg_color = MapColorPalette.getColor(80, 80, 80);
            int half = getHeight()/2;

            view.drawRectangle(0, 0, getWidth(), getHeight(), MapColorPalette.COLOR_BLACK);
            view.fillRectangle(1, 1, getWidth()-2, half - 6, bg_color);
            view.fillRectangle(1, half+5, getWidth()-2, half - 6, bg_color);

            if (typeIndex == 0) {
                view.draw(NO_MARKER, 1, 10);
            } else if (typeIndex == 1) {
                view.draw(NO_MARKER, 1, 1);
            }
        }
    }
}
