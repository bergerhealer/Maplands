package com.bergerkiller.bukkit.maplands.menu;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.common.resources.SoundEffect;
import com.bergerkiller.bukkit.maplands.MaplandsDisplay;

/**
 * Main menu of the map display
 */
public class SettingsMenu extends MapWidgetWindow {

    public SettingsMenu() {
        this.setFocusable(true);
        this.setBounds(7, 7, 114, 114);
        this.setDepthOffset(2);
        this.getTitle().setText("Settings");
    }

    @Override
    public void onAttached() {
        super.onAttached();
        this.activate();

        final MaplandsDisplay maplands_display = (MaplandsDisplay) this.getDisplay();

        // Configure markers
        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                SettingsMenu.this.removeWidget();
                maplands_display.getMapMarkers().showMenu();
            }
        }).setText("Markers")
          .setBounds(7, 19, 100, 15);

        // Turn auto-updates on or off
        this.addWidget(new MapWidgetButton() {
            @Override
            public void onAttached() {
                super.onAttached();
                updateText();
            }

            @Override
            public void onActivate() {
                maplands_display.setLiveRefreshing(!maplands_display.isLiveRefreshing());
                updateText();
                display.playSound(SoundEffect.CLICK);
            }

            private void updateText() {
                this.setText("Live Refresh: " + (maplands_display.isLiveRefreshing() ? "ON" : "OFF"));
            }
        }).setBounds(7, 36, 100, 15);

        // Sends player a command to refresh the map
        this.addWidget(new MapWidgetButton() {
            @Override
            public void onKeyPressed(MapKeyEvent event) {
                if (event.getKey() == MapPlayerInput.Key.ENTER) {
                    maplands_display.sendRenderCommand(event.getPlayer());
                    display.playSound(SoundEffect.CLICK);
                } else {
                    super.onKeyPressed(event);
                }
            }
        }).setText("Render Command")
          .setBounds(7, 53, 100, 15);
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (event.getKey() == Key.BACK && this.isActivated()) {
            this.removeWidget();
            return;
        }
        super.onKeyPressed(event);
    }
}
