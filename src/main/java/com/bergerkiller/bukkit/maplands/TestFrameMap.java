package com.bergerkiller.bukkit.maplands;

import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.ItemUtil;

/**
 * Used to be named like this. Is here so that maps can still be loaded from nbt
 */
@Deprecated
public class TestFrameMap extends MaplandsDisplay {
    @Override
    public void onAttached() {
        super.onAttached();

        // Change the display class to be the base class instead
        // This makes sure that in the future, displays use MaplandsDisplay instead of TestFrameMap
        ItemStack item = ItemUtil.cloneItem(this.getMapItem());
        ItemUtil.getMetaTag(item, true).putValue("mapDisplayClass", MaplandsDisplay.class.getName());
        this.setMapItem(item);
    }
}
