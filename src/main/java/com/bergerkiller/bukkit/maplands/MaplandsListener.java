package com.bergerkiller.bukkit.maplands;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MaplandsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        World world = event.getBlock().getWorld();
        int bx = event.getBlock().getX();
        int by = event.getBlock().getY();
        int bz = event.getBlock().getZ();
        for (MaplandsDisplay display : MaplandsDisplay.getAllDisplays()) {
            if (display.isRenderingWorld(world)) {
                display.onBlockChange(world, bx+1, by, bz);
                display.onBlockChange(world, bx, by+1, bz);
                display.onBlockChange(world, bx, by, bz+1);
                display.onBlockChange(world, bx-1, by, bz);
                display.onBlockChange(world, bx, by-1, bz);
                display.onBlockChange(world, bx, by, bz-1);
                display.onBlockChange(world, bx, by, bz);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        for (MaplandsDisplay display : MaplandsDisplay.getAllDisplays()) {
            display.onBlockChange(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockInteracted(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            for (MaplandsDisplay display : MaplandsDisplay.getAllDisplays()) {
                display.onBlockChange(event.getClickedBlock());
            }
        }
    }
}
