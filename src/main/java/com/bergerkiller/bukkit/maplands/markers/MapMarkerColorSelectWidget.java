package com.bergerkiller.bukkit.maplands.markers;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.resources.SoundEffect;

/**
 * For selecting the text color to use
 */
public abstract class MapMarkerColorSelectWidget extends MapWidget {
    private static final ColorOption[] COLORS = {
            ColorOption.create('0', 0x00, 0x00, 0x00), ColorOption.create('1', 0x00, 0x00, 0xAA),
            ColorOption.create('2', 0x00, 0xAA, 0x00), ColorOption.create('3', 0x00, 0xAA, 0xAA),
            ColorOption.create('4', 0xAA, 0x00, 0x00), ColorOption.create('5', 0xAA, 0x00, 0xAA),
            ColorOption.create('6', 0xFF, 0xAA, 0x00), ColorOption.create('7', 0xAA, 0xAA, 0xAA),
            ColorOption.create('8', 0x55, 0x55, 0x55), ColorOption.create('9', 0x55, 0x55, 0xFF),
            ColorOption.create('a', 0x55, 0xFF, 0x55), ColorOption.create('b', 0x55, 0xFF, 0xFF),
            ColorOption.create('c', 0xFF, 0x55, 0x55), ColorOption.create('d', 0xFF, 0x55, 0xFF),
            ColorOption.create('e', 0xFF, 0xFF, 0x55), ColorOption.create('f', 0xFF, 0xFF, 0xFF),
    };
    private int selectedIndex = 0;
    private ColorList list = null;
    public final MapWidgetTooltip tooltip = new MapWidgetTooltip();

    public MapMarkerColorSelectWidget() {
        this.setFocusable(true);
        this.setSize(10, 10);
        this.tooltip.setText("Text color");
    }

    public abstract void onColorChanged();

    public MapMarkerColorSelectWidget setTooltip(String tooltipText, boolean above) {
        this.tooltip.setText(tooltipText);
        this.tooltip.setPreferAbove(above);
        return this;
    }

    public ChatColor getColor() {
        return COLORS[selectedIndex].value();
    }

    public MapMarkerColorSelectWidget setColor(ChatColor color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].value() == color) {
                selectedIndex = i;
                this.invalidate();
                break;
            }
        }
        return this;
    }

    private static void drawColor(MapCanvas view, boolean focused, int index) {
        view.drawRectangle(0, 0, view.getWidth(), view.getHeight(),
                focused ? MapColorPalette.COLOR_RED : MapColorPalette.COLOR_BLACK);

        if (index >= 0 && index < COLORS.length) {
            view.drawRectangle(1, 1, view.getWidth()-2, view.getHeight()-2, MapColorPalette.getColor(80, 80, 80));
            view.fillRectangle(2, 2, view.getWidth()-4, view.getHeight()-4, COLORS[index].color());
        } else {
            view.fillRectangle(1, 1, view.getWidth()-2, view.getHeight()-2, MapColorPalette.getColor(80, 80, 80));
        }
    }

    @Override
    public void onDraw() {
        drawColor(view, isFocused(), selectedIndex);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (list == null) {
            list = this.addWidget(new ColorList());
            this.removeWidget(this.tooltip);
            display.playSound(SoundEffect.PISTON_EXTEND);
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

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (list == null) {
            super.onKeyPressed(event);
            return;
        }

        if (event.getKey() == MapPlayerInput.Key.BACK ||
            event.getKey() == MapPlayerInput.Key.ENTER)
        {
            closeList();
        } else if (event.getKey() == MapPlayerInput.Key.UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                this.invalidate();
                list.invalidate();
                onColorChanged();
                display.playSound(SoundEffect.CLICK);
            }
        } else if (event.getKey() == MapPlayerInput.Key.DOWN) {
            if (selectedIndex < (COLORS.length-1)) {
                selectedIndex++;
                this.invalidate();
                list.invalidate();
                onColorChanged();
                display.playSound(SoundEffect.CLICK);
            }
        } else {
            // Left/right pressed exits the menu and then allows for navigation
            closeList();
            super.onKeyPressed(event);
        }
    }

    private void closeList() {
        this.deactivate();
        this.removeWidget(list);
        this.addWidget(this.tooltip);
        list = null;
        display.playSound(SoundEffect.PISTON_CONTRACT);
    }

    private class ColorList extends MapWidget {
        public ColorList() {
            this.setBounds(0, -18, MapMarkerColorSelectWidget.this.getWidth(), 10 + 2 * 18);
        }

        @Override
        public void onDraw() {
            drawColor(view.getView(0, 0, 10, 10), false, selectedIndex-2);
            drawColor(view.getView(0, 9, 10, 10), false, selectedIndex-1);
            drawColor(view.getView(0, getHeight()-9-1, 10, 10), false, selectedIndex+1);
            drawColor(view.getView(0, getHeight()-18-1, 10, 10), false, selectedIndex+2);
        }
    }

    private static class ColorOption {
        private final ChatColor value;
        private final byte color;

        private ColorOption(char chatColorCode, int r, int g, int b) {
            this.value = ChatColor.getByChar(chatColorCode);
            this.color = MapColorPalette.getColor(r, g, b);
        }

        public ChatColor value() {
            return this.value;
        }

        public byte color() {
            return this.color;
        }

        public static ColorOption create(char chatColorCode, int r, int g, int b) {
            return new ColorOption(chatColorCode, r, g, b);
        }
    }
}
