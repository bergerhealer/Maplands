package com.bergerkiller.bukkit.maplands;

import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapResourcePack;

public class Maplands extends PluginBase {
    public static Maplands plugin;
    private static MapResourcePack resourcePack;
    private static int maxRenderTime = 50;
    private static byte backgroundColor = MapColorPalette.COLOR_TRANSPARENT;
    private MapCanvasCache cache;

    public static MapResourcePack getResourcePack() {
        if (resourcePack == null) {
            resourcePack = MapResourcePack.VANILLA; // fallback under test
            MapResourcePack.VANILLA.load(); // test! Make sure it is loaded.
        }
        return resourcePack;
    }

    public MapCanvasCache getCache() {
        return cache;
    }

    public static int getMaxRenderTime() {
        return maxRenderTime;
    }

    public static byte getBackgroundColor() {
        return backgroundColor;
    }

	@Override
	public void enable() {
	    plugin = this;

	    this.cache = new MapCanvasCache(this, this.getDataFile("cache"));

	    MapResourcePack.VANILLA.load();

	    FileConfiguration config = new FileConfiguration(this);
	    config.load();
	    config.setHeader("resourcePack", "Specifies a resource pack to use when rendering blocks");
	    config.addHeader("resourcePack", "When left empty the Vanilla Minecraft look is displayed");
	    resourcePack = new MapResourcePack(config.get("resourcePack", ""));

	    config.setHeader("maxRenderTime", "Specifies the maximum amount of time in milliseconds the plugin");
	    config.addHeader("maxRenderTime", "may spend rendering the map during a single tick, per map");
	    maxRenderTime = config.get("maxRenderTime", 50);

	    config.setHeader("enableCache", "Whether map data is written to disk and restored when viewed again");
	    config.addHeader("enableCache", "The rendered map data can be found inside the cache subdirectory");
	    config.addHeader("enableCache", "Having this enabled will reduce server lag when a large display is initialized");
	    cache.setEnabled(config.get("enableCache", true));

	    config.setHeader("backgroundColor", "The background color of maps showing the void in hexadecimal format, for example: '#1256FE'");
	    config.addHeader("backgroundColor", "You can use the constants: transparent, black, white, red, green, blue");
	    String backgroundColorName = config.get("backgroundColor", "transparent");
	    if (backgroundColorName.equalsIgnoreCase("transparent")) {
	        backgroundColor = MapColorPalette.COLOR_TRANSPARENT;
	    } else if (backgroundColorName.equalsIgnoreCase("black")) {
	        backgroundColor = MapColorPalette.COLOR_BLACK;
	    } else if (backgroundColorName.equalsIgnoreCase("white")) {
	        backgroundColor = MapColorPalette.COLOR_WHITE;
	    } else if (backgroundColorName.equalsIgnoreCase("red")) {
	        backgroundColor = MapColorPalette.COLOR_RED;
	    } else if (backgroundColorName.equalsIgnoreCase("green")) {
	        backgroundColor = MapColorPalette.COLOR_GREEN;
	    } else if (backgroundColorName.equalsIgnoreCase("blue")) {
	        backgroundColor = MapColorPalette.COLOR_BLUE;
	    } else {
	        while (backgroundColorName.startsWith("#")) {
	            backgroundColorName = backgroundColorName.substring(1);
	        }
	        if (backgroundColorName.startsWith("0x") || backgroundColorName.startsWith("0X")) {
	            backgroundColorName = backgroundColorName.substring(2);
	        }
	        try {
	            if (backgroundColorName.length() == 6) {
	                int rgb_color = Integer.parseInt(backgroundColorName.toUpperCase(Locale.ENGLISH), 16);
	                backgroundColor = MapColorPalette.getColor(new java.awt.Color(rgb_color));
	            } else {
	                int map_color = Integer.parseInt(backgroundColorName.toUpperCase(Locale.ENGLISH));
	                if (map_color < 0 || map_color >= 256) {
	                    this.log(Level.WARNING, "Background color '" + backgroundColorName + "' is out of range");
	                } else {
	                    backgroundColor = (byte) (map_color & 0xFF);
	                }
	            }
	        } catch (NumberFormatException ex) {
	            this.log(Level.WARNING, "Background color '" + backgroundColorName + "' can not be decoded");
	        }
	    }

	    config.save();

	    this.register(new MaplandsListener());

	    try {
	        resourcePack.load();
	    } catch (NoSuchMethodError e) {
	        // eh.
	    }
	}

	@Override
	public void disable() {
	    plugin = null;
	    resourcePack = null;
	}

    @Override
    public void permissions() {
        this.loadPermissions(Permission.class);
    }

    @Override
    public int getMinimumLibVersion() {
        return Common.VERSION;
    }

    @Override
    public boolean command(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        if (!Permission.COMMAND_MAP.has(player)) {
            player.sendMessage("No permission to use this!");
            return true;
        }

        ItemStack item = MapDisplay.createMapItem(MaplandsDisplay.class);
        player.getInventory().addItem(item);

        sender.sendMessage("Given Maplands map item");
        return true;
    }
}
