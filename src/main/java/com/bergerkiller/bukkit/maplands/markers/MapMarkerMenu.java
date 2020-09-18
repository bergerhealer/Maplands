package com.bergerkiller.bukkit.maplands.markers;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetText;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.common.resources.SoundEffect;

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

        // "Wielder"
        {
            int y = 8;

            // Label
            this.addWidget(new MapWidgetText())
              .setText("Wielder:")
              .setColor(MapColorPalette.COLOR_BLACK)
              .setPosition(11, y+1);

            // Marker type
            this.addWidget(new MapMarkerTypeSelector() {
                @Override
                public void onTypeChanged() {
                    markers.setMarkerTypeWhenHeld(getType());
                }
            }).setType(markers.getMarkerTypeWhenHeld())
              .setPosition(52, y);

            // Show coordinates on/off
            this.addWidget(new MapWidgetIconToggleButton() {
                @Override
                public void onToggled() {
                    markers.setShowCoordinatesWhenHeld(isOn());
                }
            }).setOnTexture("coords_enabled.png")
              .setOffTexture("coords_disabled.png")
              .setTooltip("Show coordinates")
              .setOn(markers.showCoordinatesWhenHeld())
              .setPosition(65, y);
        }

        // "Players"
        {
            int y = 20;

            // Label
            this.addWidget(new MapWidgetText())
              .setText("Players:")
              .setColor(MapColorPalette.COLOR_BLACK)
              .setPosition(11, y+1);

            // Marker type
            this.addWidget(new MapMarkerTypeSelector() {
                @Override
                public void onTypeChanged() {
                    markers.setMarkerTypeForPlayers(getType());
                }
            }).setType(markers.getMarkerTypeForPlayers())
              .setPosition(52, y);

            // Show coordinates on/off
            this.addWidget(new MapWidgetIconToggleButton() {
                @Override
                public void onToggled() {
                    markers.setShowCoordinatesForPlayers(isOn());
                }
            }).setOnTexture("coords_enabled.png")
              .setOffTexture("coords_disabled.png")
              .setTooltip("Show coordinates")
              .setOn(markers.showCoordinatesForPlayers())
              .setPosition(65, y);

            // Show name on/off
            this.addWidget(new MapWidgetIconToggleButton() {
                @Override
                public void onToggled() {
                    markers.setShowNameForPlayers(isOn());
                }
            }).setOnTexture("names_enabled.png")
              .setOffTexture("names_disabled.png")
              .setTooltip("Show names")
              .setOn(markers.showNameForPlayers())
              .setPosition(78, y);
        }

        // Static markers
        {
            // Label
            this.addWidget(new MapWidgetText())
              .setText("Static markers")
              .setColor(MapColorPalette.COLOR_BLACK)
              .setPosition(11, 35);

            // List of markers with options + text
            // When activated, opens up a menu to modify/delete it
            

            // Button to add new static markers
            this.addWidget(new MapWidgetButton() {
                @Override
                public void onActivate() {
                    /*
                    for (Player owner : display.getOwners()) {
                        if (display.isControlling(owner)) {
                            Vector position = owner.getLocation().toVector();
                            showStaticMarkerMenu(markers.addStaticMarker(position));
                            display.playSound(SoundEffect.WALK_CLOTH);
                            return;
                        }
                    }
                    display.playSound(SoundEffect.EXTINGUISH);
                    */
                }
            }).setText("Add static marker")
              .setBounds(11,  80, 50, 11);
        }
    }

    public void showStaticMarkerMenu(MapStaticMarker marker) {
        setMarkersHidden(true);
        this.addWidget(new MapStaticMarkerMenu(marker));
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (event.getKey() == Key.BACK && this.isActivated()) {
            this.close();
            return;
        }
        super.onKeyPressed(event);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        setMarkersHidden(false);
    }

    private void setMarkersHidden(boolean hidden) {
        for (MapWidget widget : this.getWidgets()) {
            if (widget instanceof MapMarkerTypeSelector) {
                ((MapMarkerTypeSelector) widget).setMarkersHidden(hidden);
            }
        }
    }
    
    /**
     * Closes this menu, removing this window
     */
    public void close() {
        this.removeWidget();
    }
}
