package com.bergerkiller.bukkit.maplands.markers;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapMarker;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetSubmitText;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetText;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;

/**
 * Configures a single static map marker
 */
public class MapStaticMarkerMenu extends MapWidgetWindow {
    private final MapStaticMarker marker;
    private MapWidgetSubmitText captionSubmitTextWidget;
    private MapMarker previewMarker;

    public MapStaticMarkerMenu(MapStaticMarker marker) {
        this.marker = marker;

        this.setFocusable(true);
        this.setBounds(4, 4, 106, 106);
        this.setDepthOffset(2);
        this.setBackgroundColor(MapColorPalette.COLOR_BLUE);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.activate();

        // Label showing the (block) coordinates of the marker
        this.getTitle().setFont(MapFont.TINY)
                       .setColor(MapColorPalette.COLOR_BLACK)
                       .setText("Marker [" + marker.position.getBlockX() + "/" +
                marker.position.getBlockY() + "/" + marker.position.getBlockZ() + "]");

        // Marker displays a preview of how the static marker would look
        this.previewMarker = display.createMarker();
        this.previewMarker.setPosition(64, 80);
        this.marker.applyProperties(this.previewMarker);

        // Coordinates of the bar of options
        int bar_y = 18;
        int x_offset = 5;
        int x_step = 11;

        // Add a label next to the marker to warn it isn't displayed on item frames
        final MapWidget notVisibleWarningWidget = this.addWidget(new MapWidgetText())
          .setFont(MapFont.TINY)
          .setText("Not shown on item frames!")
          .setColor(MapColorPalette.COLOR_RED)
          .setPosition(x_offset-1, 94)
          .setVisible(!marker.isVisibleOnItemFrames());

        // Marker type selector
        this.addWidget(new MapMarkerTypeSelector() {
            @Override
            public void onTypeChanged() {
                marker.type = getType();
                notVisibleWarningWidget.setVisible(!marker.isVisibleOnItemFrames());
                updateMarker();
            }
        }).setType(marker.type)
          .setTooltip("Marker type", true)
          .setPosition(x_offset + x_step * 0, bar_y);

        // Marker rotation selector

        // Marker color selector
        this.addWidget(new MapMarkerColorSelectWidget() {
            @Override
            public void onColorChanged() {
                marker.color = getColor();
                updateMarker();
            }
        }).setColor(marker.color)
          .setTooltip("Text color", true)
          .setPosition(x_offset + x_step * 1, bar_y);

        // Bold
        this.addWidget(new MapWidgetIconToggleButton() {
            @Override
            public void onToggled() {
                marker.bold = isOn();
                updateMarker();
            }
        }).setOnTexture("bold_enabled.png")
          .setOffTexture("bold_disabled.png")
          .setTooltip("Bold text", true)
          .setOn(marker.bold)
          .setPosition(x_offset + x_step * 2, bar_y);

        // Italic
        this.addWidget(new MapWidgetIconToggleButton() {
            @Override
            public void onToggled() {
                marker.italic = isOn();
                updateMarker();
            }
        }).setOnTexture("italic_enabled.png")
          .setOffTexture("italic_disabled.png")
          .setTooltip("Italic text", true)
          .setOn(marker.italic)
          .setPosition(x_offset + x_step * 3, bar_y);

        // Underline
        this.addWidget(new MapWidgetIconToggleButton() {
            @Override
            public void onToggled() {
                marker.underline = isOn();
                updateMarker();
            }
        }).setOnTexture("underline_enabled.png")
          .setOffTexture("underline_disabled.png")
          .setTooltip("Underline text", true)
          .setOn(marker.underline)
          .setPosition(x_offset + x_step * 4, bar_y);

        // Strikethrough
        this.addWidget(new MapWidgetIconToggleButton() {
            @Override
            public void onToggled() {
                marker.strikethrough = isOn();
                updateMarker();
            }
        }).setOnTexture("strikethrough_enabled.png")
          .setOffTexture("strikethrough_disabled.png")
          .setTooltip("Strike-through text", true)
          .setOn(marker.strikethrough)
          .setPosition(x_offset + x_step * 5, bar_y);

        // Configure actual text on the marker
        // Show the text in a textbox that can be activated
        // to edit it in an anvil menu.
        MapMarkerCaptionWidget captionWidget = this.addWidget(new MapMarkerCaptionWidget() {
            @Override
            public void onActivate() {
                captionSubmitTextWidget.activate();
            }
        }).setCaption(marker.caption);
        captionWidget.setBounds(x_offset, bar_y + 12, 95, 11);

        captionSubmitTextWidget = this.addWidget(new MapWidgetSubmitText() {
            @Override
            public void onAccept(String text) {
                captionWidget.setCaption(text);
                marker.caption = text;
                updateMarker();
            }

            @Override
            public void onCancel() {
            }
        }).setDescription("Enter the new marker caption");

        // Remove button
        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                marker.remove();
                close();
            }
        }).setText("Remove")
          .setBounds(30, 43, 42, 11);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        this.previewMarker.remove();
    }

    private void updateMarker() {
        marker.applyProperties(this.previewMarker);
        marker.update();
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
