package com.bergerkiller.bukkit.maplands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import org.bukkit.inventory.ItemStack;

public class MaplandsListener implements Listener {

    private void handleBlockChanges(Block... blocks) {
        for (TestFrameMap display : MapDisplay.getAllDisplays(TestFrameMap.class)) {
            for (Block block : blocks) {
                display.onBlockChange(block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block[] blocks = new Block[7];
        blocks[blocks.length - 1] = event.getBlock();
        for (int i = 0; i < 6; i++) {
            blocks[i] = event.getBlock().getRelative(FaceUtil.BLOCK_SIDES[i]);
        }
        handleBlockChanges(blocks);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        handleBlockChanges(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockInteracted(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            handleBlockChanges(event.getClickedBlock());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event){
        if(!MapUtil.isMaplandMap(event.getRightClicked())) return;

        event.setCancelled(true);
    }
}
