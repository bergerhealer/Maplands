package com.bergerkiller.bukkit.maplands.markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.maplands.MaplandsDisplay;

/**
 * Manages all the markers displayed on a maplands map
 */
public class MapMarkers {
    private final MaplandsDisplay display;
    private MapMarker.Type markerTypeWhenHeld;
    private List<MapMarker> heldMarkers = new ArrayList<MapMarker>();

    public MapMarkers(MaplandsDisplay display) {
        this.display = display;
    }

    public MapMarker.Type getMarkerTypeWhenHeld() {
        return markerTypeWhenHeld;
    }

    public void setMarkerTypeWhenHeld(MapMarker.Type type) {
        if (markerTypeWhenHeld != type) {
            markerTypeWhenHeld = type;
            if (type == null) {
                for (MapMarker marker : heldMarkers) {
                    marker.remove();
                }
                heldMarkers.clear();
            } else {
                for (MapMarker marker : heldMarkers) {
                    marker.setType(type);
                }
            }
            save();
        }
    }

    public void load() {
        if (display.getProperties().containsKey("markerTypeWhenHeld", String.class)) {
            markerTypeWhenHeld = MapMarker.Type.byName(display.getProperties().get("markerTypeWhenHeld", String.class));
        } else {
            markerTypeWhenHeld = null;
        }
        display.clearMarkers();
    }

    public void save() {
        if (markerTypeWhenHeld == null) {
            display.getProperties().set("markerTypeWhenHeld", null);
        } else {
            display.getProperties().set("markerTypeWhenHeld", markerTypeWhenHeld.name());
        }
    }

    public void update() {
        if (markerTypeWhenHeld != null) {
            Set<MapMarker> updatedMarkers = new HashSet<MapMarker>();
            for (Player viewer : display.getViewers()) {
                if (!display.isHolding(viewer)) {
                    continue;
                }
                if (viewer.getWorld() != display.getStartBlock().getWorld()) {
                    continue;
                }

                String id = "held_" + viewer.getName();
                MapMarker marker = display.getMarker(id);
                if (marker == null) {
                    marker = display.createMarker(id);
                    marker.setType(markerTypeWhenHeld);
                }
                updatedMarkers.add(marker);

                // Refresh position of the marker, clamp to within the held map area (first tile)
                Vector position = display.getScreenCoordinates(viewer.getLocation().toVector());
                position.setX(MathUtil.clamp(position.getX(), 0.0, 127.5));
                position.setY(MathUtil.clamp(position.getY(), 0.0, 127.5));
                marker.setPosition(position.getX(), position.getY());
            }

            // Remove markers no longer matching a player
            // Re-add all newly updated markers for tracking
            for (Iterator<MapMarker> iter = heldMarkers.iterator(); iter.hasNext();) {
                MapMarker old = iter.next();
                if (!updatedMarkers.remove(old)) {
                    iter.remove();
                    old.remove();
                }
            }
            heldMarkers.addAll(updatedMarkers);
        }
    }

    public void showMenu() {
        display.addWidget(new MapMarkerMenu(this));
    }
}
