package com.bergerkiller.bukkit.maplands.markers;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;

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
    public boolean strikethrough;
    public ChatColor color;
    public String caption;

    public MapStaticMarker(MapMarkers markers, Vector position) {
        this.markers = markers;
        this.id = "static_" + UUID.randomUUID().toString();
        this.position = position;
        this.type = MapMarker.Type.BANNER_WHITE;
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.strikethrough = false;
        this.color = ChatColor.WHITE;
        this.caption = "[" + position.getBlockX() + "/" + position.getBlockY() +
                "/" + position.getBlockZ() + "]";
    }

    public boolean isVisibleOnItemFrames() {
        if (this.type != null) {
            try {
                return this.type.isVisibleOnItemFrames();
            } catch (Throwable t) {}
        }
        return true;
    }

    /**
     * Applies display properties such as marker type and caption to a
     * marker. Position is not updated.
     * 
     * @param marker
     */
    public void applyProperties(MapMarker marker) {
        marker.setVisible(this.type != null);
        if (this.type != null) {
            marker.setType(this.type);
        }

        if (this.caption.isEmpty()) {
            marker.setCaption(null);
        } else {
            StringBuilder str = new StringBuilder();
            str.append(this.color);
            if (this.bold) {
                str.append(ChatColor.BOLD);
            }
            if (this.italic) {
                str.append(ChatColor.ITALIC);
            }
            if (this.underline) {
                str.append(ChatColor.UNDERLINE);
            }
            if (this.strikethrough) {
                str.append(ChatColor.STRIKETHROUGH);
            }
            str.append(this.caption);
            marker.setCaption(str.toString());
        }
    }

    public void update() {
        MapMarker marker = markers.getDisplay().getMarker(this.id);
        if (marker == null) {
            marker = markers.getDisplay().createMarker(this.id);
        }
        Vector position = markers.getDisplay().getScreenCoordinates(this.position);
        marker.setPosition(position.getX(), position.getY());

        if (markers.isHiddenInFirstTile() && position.getX() < 128.0 && position.getY() < 128.0) {
            marker.setVisible(false);
        } else {
            applyProperties(marker);
        }

        markers.save();
    }

    public void remove() {
        markers.removeStaticMarker(this);
    }

    public CommonTagCompound save() {
        CommonTagCompound data = new CommonTagCompound();
        data.putValue("x", position.getX());
        data.putValue("y", position.getY());
        data.putValue("z", position.getZ());
        if (this.type != null) {
            data.putValue("type", this.type.name());
        }
        data.putValue("bold", this.bold ? Boolean.TRUE : null);
        data.putValue("italic", this.italic ? Boolean.TRUE : null);
        data.putValue("underline", this.underline ? Boolean.TRUE : null);
        data.putValue("strikethrough", this.strikethrough ? Boolean.TRUE : null);
        data.putValue("color", this.color.name());
        data.putValue("caption", this.caption);
        return data;
    }

    public static MapStaticMarker load(MapMarkers markers, CommonTagCompound data) {
        double x = data.getValue("x", 0.0);
        double y = data.getValue("y", 0.0);
        double z = data.getValue("z", 0.0);
        MapStaticMarker marker = new MapStaticMarker(markers, new Vector(x, y, z));
        if (data.containsKey("type")) {
            marker.type = MapMarker.Type.byName(data.getValue("type", MapMarker.Type.BANNER_BLACK.name()));
        } else {
            marker.type = null;
        }
        marker.bold = data.containsKey("bold") && data.getValue("bold", Boolean.FALSE);
        marker.italic = data.containsKey("italic") && data.getValue("italic", Boolean.FALSE);
        marker.underline = data.containsKey("underline") && data.getValue("underline", Boolean.FALSE);
        marker.strikethrough = data.containsKey("strikethrough") && data.getValue("strikethrough", Boolean.FALSE);

        String colorName = data.getValue("color", String.class);
        if (colorName != null) {
            for (ChatColor color : ChatColor.values()) {
                if (color.name().equals(colorName)) {
                    marker.color = color;
                    break;
                }
            }
        }

        marker.caption = data.getValue("caption", "ERROR");
        return marker;
    }
}
