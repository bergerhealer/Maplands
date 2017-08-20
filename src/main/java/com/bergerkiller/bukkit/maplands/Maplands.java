package com.bergerkiller.bukkit.maplands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.map.MapDisplay;

public class Maplands extends PluginBase {
    public static Maplands plugin;
    
	@Override
	public void enable() {
	    plugin = this;
	    //this.register(new TCMapListener());
	}

	@Override
	public void disable() {
	    plugin = null;
	}

    @Override
    public int getMinimumLibVersion() {
        return Common.VERSION;
    }

    @Override
    public boolean command(CommandSender sender, String command, String[] args) {
        Player player = (Player) sender;

        ItemStack item = MapDisplay.createMapItem(TestFrameMap.class);
        player.getInventory().addItem(item);
        
        sender.sendMessage("Given item");
        return true;
    }
}
