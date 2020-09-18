package com.bergerkiller.bukkit.maplands.markers;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.map.MapMarker;

/**
 * A single static map marker
 */
public class MapStaticMarker {
    private final MapMarkers markers;
    public final String id;
    public final Vector position;
    public MapMarker.Type type;
    public boolean bold;
    public boolean italic;
    public boolean underline;
    public ChatColor color;
    public String caption;

    public MapStaticMarker(MapMarkers markers, Vector position) {
        this.markers = markers;
        this.id = "static_" + UUID.randomUUID().toString();
        this.position = position;
        this.type = MapMarker.Type.WHITE_CIRCLE;
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.color = ChatColor.WHITE;
        this.caption = "[" + position.getBlockX() + "/" + position.getBlockY() +
                "/" + position.getBlockZ() + "]";
    }

    public void update() {
        MapMarker marker = markers.getDisplay().getMarker(this.id);
        if (marker == null) {
            marker = markers.getDisplay().createMarker(this.id);
        }
        Vector position = markers.getDisplay().getScreenCoordinates(this.position);
        marker.setType(this.type);
        marker.setPosition(position.getX(), position.getY());

        if (this.caption.isEmpty()) {
            marker.setCaption(null);
        } else {
            StringBuilder str = new StringBuilder();
            if (this.bold) {
                str.append(ChatColor.BOLD);
            }
            if (this.italic) {
                str.append(ChatColor.ITALIC);
            }
            if (this.underline) {
                str.append(ChatColor.UNDERLINE);
            }
            str.append(this.color);
            str.append(this.caption);
            marker.setCaption(str.toString());
        }

        markers.save();
    }

    public void remove() {
        markers.removeStaticMarker(this);
    }
}
