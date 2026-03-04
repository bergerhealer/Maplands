package com.bergerkiller.bukkit.maplands;

import com.bergerkiller.bukkit.common.inventory.CommonItemStack;

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
        CommonItemStack commonItem = CommonItemStack.copyOf(this.getMapItem());
        commonItem.updateCustomData(tag -> tag.putValue("mapDisplayClass", MaplandsDisplay.class.getName()));
        this.setMapItem(commonItem.toBukkit());
    }
}
