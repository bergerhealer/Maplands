package com.bergerkiller.bukkit.maplands;

import static org.junit.Assert.*;

import org.bukkit.block.BlockFace;
import org.junit.Ignore;
import org.junit.Test;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.LogicUtil;

public class TestCoordinates {

    @Test
    public void testTileScreenConversion() {
        ZoomLevel zoom = ZoomLevel.DEFAULT;
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 100; y++) {
                for (int z = 0; z < 50; z++) {
                    if (!MapUtil.isTile(x, y, z)) {
                        continue;
                    }

                    IntVector3 tilePosIn = new IntVector3(x, y, z);
                    IntVector3 screenPos = zoom.tileToScreen(tilePosIn);
                    IntVector3 tilePos = zoom.screenToTile(screenPos);
                    assertEquals(tilePosIn, tilePos);
                }
            }
        }
    }

    @Ignore
    @Test
    public void testCoordinates() {
        for (int dy = 0; dy < 256; dy++) {
            for (int dx = 0; dx < 10; dx++) {
                for (int dz = 0; dz < 15; dz++) {
                    IntVector3 in = new IntVector3(dx, dy, dz);
                    IntVector3 a = MapUtil.screenTileToBlock(BlockFace.NORTH_EAST, in);
                    IntVector3 b = MapUtil.screenTileToBlock(BlockFace.NORTH_EAST, in);
                    if (!LogicUtil.bothNullOrEqual(a, b)) {
                        System.out.println("FAIL " + in + " != " + a + " but was " + b);
                    }
                }
            }
        }
    }

    @Ignore
    @Test
    public void testNegativePY() {
        for (int dy = 8; dy >= -12; dy -= 3) {
            IntVector3 p = new IntVector3(0, dy, 0);
            System.out.println(p + " = " + MapUtil.screenTileToBlock(BlockFace.NORTH_EAST, p));
        }
    }
}
