package com.bergerkiller.bukkit.maplands;

import static org.junit.Assert.*;

import org.bukkit.block.BlockFace;
import org.junit.Ignore;
import org.junit.Test;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.LogicUtil;

public class TestCoordinates {

    @Test
    public void testTileToBlockCoordinates() {
        // Tests conversion from/to block coordinates and tile coordinates
        for (int x = -100; x < 100; x++) {
            for (int y = -100; y < 100; y++) {
                for (int z = -100; z < 100; z++) {
                    if (!MapUtil.isTile(x, y, z)) {
                        continue;
                    }

                    IntVector3 tilePosIn = new IntVector3(x, y, z);
                    IntVector3 blockPos = MapUtil.screenTileToBlock(BlockFace.NORTH_EAST, tilePosIn);
                    assertNotNull(blockPos);
                    IntVector3 tilePosOut = MapUtil.blockToScreenTile(BlockFace.NORTH_EAST, blockPos);
                    if (tilePosOut == null) {
                        fail("Tile position was expected to be " + tilePosIn + ", but was null");
                    }
                    assertEquals(tilePosIn, tilePosOut);
                }
            }
        }
    }

    @Test
    public void testTileScreenConversion() {
        ZoomLevel zoom = ZoomLevel.DEFAULT;
        for (int x = -40; x < 40; x++) {
            for (int y = -40; y < 40; y++) {
                for (int z = -100; z < 100; z++) {
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
