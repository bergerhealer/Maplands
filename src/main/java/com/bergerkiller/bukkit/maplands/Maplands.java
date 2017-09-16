package com.bergerkiller.bukkit.maplands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapResourcePack;

public class Maplands extends PluginBase {
    public static Maplands plugin;
    private static MapResourcePack resourcePack;

    public static MapResourcePack getResourcePack() {
        if (resourcePack == null) {
            resourcePack = MapResourcePack.VANILLA; // fallback under test
            MapResourcePack.VANILLA.load(); // test! Make sure it is loaded.
        }
        return resourcePack;
    }

	@Override
	public void enable() {
	    plugin = this;

	    MapResourcePack.VANILLA.load();

	    FileConfiguration config = new FileConfiguration(this);
	    config.load();
	    config.setHeader("resourcePack", "Specifies a resource pack to use when rendering blocks");
	    config.addHeader("resourcePack", "When left empty the Vanilla Minecraft look is displayed");
	    resourcePack = new MapResourcePack(config.get("resourcePack", ""));
	    config.save();

	    this.register(new MaplandsListener());

	    this.loadPermissions(Permission.class);
	}

	@Override
	public void disable() {
	    plugin = null;
	    resourcePack = null;
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

        ItemStack item = MapDisplay.createMapItem(TestFrameMap.class);
        player.getInventory().addItem(item);

        sender.sendMessage("Given Maplands map item");
        return true;
    }
}
