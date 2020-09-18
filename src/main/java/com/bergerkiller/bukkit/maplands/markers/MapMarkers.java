package com.bergerkiller.bukkit.maplands.markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.maplands.MaplandsDisplay;

import net.md_5.bungee.api.ChatColor;

/**
 * Manages all the markers displayed on a maplands map
 */
public class MapMarkers {
    private final MaplandsDisplay display;
    private List<MapStaticMarker> staticMarkers = new ArrayList<MapStaticMarker>();
    private List<MapMarker> heldMarkers = new ArrayList<MapMarker>();
    private List<MapMarker> playerMarkers = new ArrayList<MapMarker>();
    private MapMarker.Type markerTypeWhenHeld;
    private boolean heldShowCoords = false;
    private MapMarker.Type markerTypeForPlayers;
    private boolean playersShowCoords = false;
    private boolean playersShowName = false;

    public MapMarkers(MaplandsDisplay display) {
        this.display = display;
    }

    public MaplandsDisplay getDisplay() {
        return display;
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

    public boolean showCoordinatesWhenHeld() {
        return heldShowCoords;
    }

    public void setShowCoordinatesWhenHeld(boolean show) {
        if (heldShowCoords != show) {
            heldShowCoords = show;
            save();
        }
    }

    public MapMarker.Type getMarkerTypeForPlayers() {
        return markerTypeForPlayers;
    }

    public void setMarkerTypeForPlayers(MapMarker.Type type) {
        if (markerTypeForPlayers != type) {
            markerTypeForPlayers = type;
            if (type == null) {
                for (MapMarker marker : playerMarkers) {
                    marker.remove();
                }
                playerMarkers.clear();
            } else {
                for (MapMarker marker : playerMarkers) {
                    marker.setType(type);
                }
            }
            save();
        }
    }

    public boolean showNameForPlayers() {
        return playersShowName;
    }

    public void setShowNameForPlayers(boolean show) {
        if (playersShowName != show) {
            playersShowName = show;
            save();
        }
    }

    public boolean showCoordinatesForPlayers() {
        return playersShowCoords;
    }

    public void setShowCoordinatesForPlayers(boolean show) {
        if (playersShowCoords != show) {
            playersShowCoords = show;
            save();
        }
    }

    public List<MapStaticMarker> getStaticMarkers() {
        return this.staticMarkers;
    }

    public MapStaticMarker addStaticMarker(Vector position) {
        MapStaticMarker marker = new MapStaticMarker(this, position);
        this.staticMarkers.add(marker);
        marker.update();
        return marker;
    }

    public void removeStaticMarker(MapStaticMarker marker) {
        if (this.staticMarkers.remove(marker)) {
            this.display.removeMarker(marker.id);
            this.save();
        }
    }

    public void load() {
        display.clearMarkers();

        if (display.getProperties().containsKey("markerTypeWhenHeld", String.class)) {
            markerTypeWhenHeld = MapMarker.Type.byName(display.getProperties().get("markerTypeWhenHeld", String.class));
        } else {
            markerTypeWhenHeld = null;
        }
        if (display.getProperties().containsKey("markerTypeForPlayers", String.class)) {
            markerTypeForPlayers = MapMarker.Type.byName(display.getProperties().get("markerTypeForPlayers", String.class));
        } else {
            markerTypeForPlayers = null;
        }
        heldShowCoords = display.getProperties().get("showCoordsWhenHeld", false);
        playersShowCoords = display.getProperties().get("playersShowCoords", false);
        playersShowName = display.getProperties().get("playersShowName", false);
    }

    public void save() {
        display.getProperties().set("markerTypeWhenHeld", (markerTypeWhenHeld==null) ? null : markerTypeWhenHeld.name());
        display.getProperties().set("markerTypeForPlayers", (markerTypeForPlayers==null) ? null : markerTypeForPlayers.name());
        display.getProperties().set("showCoordsWhenHeld", heldShowCoords);
        display.getProperties().set("playersShowCoords", playersShowCoords);
        display.getProperties().set("playersShowName", playersShowName);
    }

    public void update() {
        Set<MapMarker> updatedMarkers = new HashSet<MapMarker>();
        if (markerTypeWhenHeld != null) {
            updatedMarkers.clear();
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
                Vector playerPos = viewer.getLocation().toVector();
                Vector positionOnMap = display.getScreenCoordinates(playerPos);
                positionOnMap.setX(MathUtil.clamp(positionOnMap.getX(), 0.0, 127.5));
                positionOnMap.setY(MathUtil.clamp(positionOnMap.getY(), 0.0, 127.5));
                marker.setPosition(positionOnMap.getX(), positionOnMap.getY());

                // Show position (block) as label when enabled
                if (showCoordinatesWhenHeld()) {
                    marker.setCaption(ChatColor.WHITE + "[" + playerPos.getBlockX() + "/" +
                            playerPos.getBlockY() + "/" + playerPos.getBlockZ() + "]");
                } else {
                    marker.setCaption(null);
                }
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

        if (markerTypeForPlayers != null) {
            updatedMarkers.clear();
            for (Player player : display.getStartBlock().getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                if (markerTypeWhenHeld != null && display.isHolding(player)) {
                    continue;
                }

                // Refresh position of the marker, if outside of map area, don't show
                // Quick optimization: use block bounds to avoid getScreenCoordinates calc.
                Vector playerPos = player.getLocation().toVector();
                if (!display.isBlockWithinBounds(playerPos.getBlockX(), playerPos.getBlockY(), playerPos.getBlockZ())) {
                    continue;
                }
                Vector positionOnMap = display.getScreenCoordinates(playerPos);
                if (positionOnMap.getX() < 0.0 || positionOnMap.getY() < 0.0 ||
                    positionOnMap.getX() > display.getWidth() || positionOnMap.getY() > display.getHeight())
                {
                    continue;
                }
                
                // Create a marker
                String id = "player_" + player.getName();
                MapMarker marker = display.getMarker(id);
                if (marker == null) {
                    marker = display.createMarker(id);
                    marker.setType(markerTypeForPlayers);
                }
                updatedMarkers.add(marker);
                marker.setPosition(positionOnMap.getX(), positionOnMap.getY());

                // Show player display name / coordinates as configured
                StringBuilder caption = new StringBuilder();
                if (showNameForPlayers()) {
                    if (caption.length() > 0) caption.append(' ');
                    caption.append(player.getDisplayName());
                }
                if (showCoordinatesForPlayers()) {
                    if (caption.length() > 0) caption.append(' ');
                    caption.append(ChatColor.WHITE).append('[');
                    caption.append(playerPos.getBlockX()).append('/');
                    caption.append(playerPos.getBlockY()).append('/');
                    caption.append(playerPos.getBlockZ()).append(']');
                }
                if (caption.length() > 0) {
                    marker.setCaption(caption.toString());
                } else {
                    marker.setCaption(null);
                }
            }

            // Remove markers no longer matching a player
            // Re-add all newly updated markers for tracking
            for (Iterator<MapMarker> iter = playerMarkers.iterator(); iter.hasNext();) {
                MapMarker old = iter.next();
                if (!updatedMarkers.remove(old)) {
                    iter.remove();
                    old.remove();
                }
            }
            playerMarkers.addAll(updatedMarkers);
        }
    }

    /**
     * Called by the display when the view translation/rotation/zoom changes
     */
    public void viewChanged() {
        for (MapStaticMarker marker : this.staticMarkers) {
            marker.update();
        }
    }

    public void showMenu() {
        display.addWidget(new MapMarkerMenu(this));
    }
}
