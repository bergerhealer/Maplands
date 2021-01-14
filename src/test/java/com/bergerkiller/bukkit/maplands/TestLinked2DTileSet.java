package com.bergerkiller.bukkit.maplands;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import com.bergerkiller.bukkit.maplands.util.Linked2DTile;
import com.bergerkiller.bukkit.maplands.util.Linked2DTileList;
import com.bergerkiller.bukkit.maplands.util.Linked2DTileSet;

/**
 * Tests the {@link Linked2DTileSet}
 */
public class TestLinked2DTileSet {

    @Test
    public void testConstruction() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);
        assertTrue(set.isEmpty());
        for (int x = -100; x <= 100; x++) {
            for (int y = -100; y <= 100; y++) {
                assertFalse(set.contains(x, y));
            }
        }

        assertTrue(set.getValidTiles(0).isEmpty());
        assertTrue(set.getValidTiles(1).isEmpty());
        assertTrue(set.getValidTiles(2).isEmpty());
        assertTrue(set.getValidTiles(-1).isEmpty());
        assertTrue(set.getValidTiles(3).isEmpty());
    }

    @Test
    public void testBoundsCheck() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);
        set.setAll();

        Consumer<Runnable> verifyThrows = (runnable) -> {
            try {
                runnable.run();
                fail("Should throw IllegalArgumentException");
            } catch (IllegalArgumentException ex) {}
        };
        Consumer<BiConsumer<Integer, Integer>> checkRange = (consumer) -> {
            verifyThrows.accept(() -> consumer.accept(-101, 0));
            verifyThrows.accept(() -> consumer.accept(101, 0));
            verifyThrows.accept(() -> consumer.accept(0, -101));
            verifyThrows.accept(() -> consumer.accept(0, 101));
        };

        checkRange.accept((x, y) -> set.contains(x, y));
        checkRange.accept((x, y) -> set.set(x, y));
        checkRange.accept((x, y) -> set.clear(x, y));
    }

    @Test
    public void test2DTileGetDepthModThree() {
        for (int x = -100; x <= 100; x++) {
            for (int y = -100; y <= 100; y++) {
                Linked2DTile tile = new Linked2DTile(x, y);
                int dmt = tile.getDepthModThree();
                boolean isDepth0 = MapUtil.isTile(x, y, 0);
                boolean isDepth1 = MapUtil.isTile(x, y, 1);
                boolean isDepth2 = MapUtil.isTile(x, y, 2);

                // Check for valid tiles, that it matches with what isTile reports
                assertEquals(isDepth0, dmt == 0);
                assertEquals(isDepth1, dmt == 1);
                assertEquals(isDepth2, dmt == 2);

                // Not a valid tile, should be -1
                if (!isDepth0 && !isDepth1 && !isDepth2) {
                    assertEquals(-1, dmt);
                }
            }
        }
    }

    private static boolean isValidTile(int x, int y) {
        return MapUtil.isTile(x, y, 0) || MapUtil.isTile(x, y, 1) || MapUtil.isTile(x, y, 2);
    }

    @Test
    public void testSetAndClearAll() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);

        // Set all
        set.setAll();

        // Verify the set contains all elements
        {
            assertFalse(set.isEmpty());

            for (int x = -100; x <= 100; x++) {
                for (int y = -100; y <= 100; y++) {
                    if (isValidTile(x, y)) {
                        assertTrue(set.contains(x, y));
                    }
                }
            }

            for (int depth : new int[] {0, 1, 2}) {
                Linked2DTileList list = set.getValidTiles(depth);
                Linked2DTile tile = list.head;
                for (int y = -100; y <= 100; y++) {
                    for (int x = -100; x <= 100; x++) {
                        if (!MapUtil.isTile(x, y, depth)) {
                            continue;
                        }
  
                        assertFalse(tile.next == list.tail);
                        tile = tile.next;
                        assertEquals(x, tile.x);
                        assertEquals(y, tile.y);
                    }
                }
            }
        }

        // Clear all
        set.clearAll();

        // Verify set contains no elements
        {
            assertTrue(set.isEmpty());

            for (int x = -100; x <= 100; x++) {
                for (int y = -100; y <= 100; y++) {
                    assertFalse(set.contains(x, y));
                }
            }

            for (int depth : new int[] {0, 1, 2}) {
                Linked2DTileList list = set.getValidTiles(depth);
                assertTrue(list.isEmpty());
                assertTrue(list.head.next == list.tail);
            }
        }
    }

    @Test
    public void testSetManyTiles() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);

        for (int y = -100; y <= 100; y++) {
            for (int x = -100; x <= 100; x++) {
                if (isValidTile(x, y)) {
                    assertTrue(set.set(x, y));
                } else {
                    assertFalse(set.set(x, y));
                }
            }
        }
        for (int y = -100; y <= 100; y++) {
            for (int x = -100; x <= 100; x++) {
                assertFalse(set.set(x, y));
                if (isValidTile(x, y)) {
                    assertTrue(set.contains(x, y));
                } else {
                    assertFalse(set.contains(x, y));
                }
            }
        }
    }

    @Test
    public void testSetOneTile() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);

        // Set the tile at [0, 0] which has (depth % 3) == 2
        assertTrue(set.set(0, 0));
        assertTrue(set.contains(0, 0));
        assertTrue(set.getValidTiles(0).isEmpty());
        assertTrue(set.getValidTiles(1).isEmpty());
        assertFalse(set.getValidTiles(2).isEmpty());
        {
            Linked2DTile tile = set.getValidTiles(2).head.next;
            assertEquals(0, tile.x);
            assertEquals(0, tile.y);
            assertEquals(set.getValidTiles(2).tail, tile.next);
        }
    }

    @Test
    public void testInverseIterator() {
        Linked2DTileSet set = new Linked2DTileSet(-100, 100, -100, 100);

        // Verify the inverse set contains all elements when empty
        {
            Iterator<Linked2DTile> iter = set.iterateInverse().iterator();
            for (int y = -100; y <= 100; y++) {
                for (int x = -100; x <= 100; x++) {
                    if (isValidTile(x, y)) {
                        assertTrue(iter.hasNext());
                        Linked2DTile tile = iter.next();
                        assertEquals(x, tile.x);
                        assertEquals(y, tile.y);
                    }
                }
            }
            assertFalse(iter.hasNext());
            try {
                iter.next();
                fail("next() should throw");
            } catch (NoSuchElementException ex) {}
        }

        // Set a single element, verify that iterator will skip it
        set.set(0, 0);
        {
            Iterator<Linked2DTile> iter = set.iterateInverse().iterator();
            for (int y = -100; y <= 100; y++) {
                for (int x = -100; x <= 100; x++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    if (isValidTile(x, y)) {
                        assertTrue(iter.hasNext());
                        Linked2DTile tile = iter.next();
                        assertEquals(x, tile.x);
                        assertEquals(y, tile.y);
                    }
                }
            }
            assertFalse(iter.hasNext());
            try {
                iter.next();
                fail("next() should throw");
            } catch (NoSuchElementException ex) {}
        }

        // Set presume next element in chain, should skip both
        set.set(1, 0);
        {
            Iterator<Linked2DTile> iter = set.iterateInverse().iterator();
            for (int y = -100; y <= 100; y++) {
                for (int x = -100; x <= 100; x++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    if (x == 1 && y == 0) {
                        continue;
                    }
                    if (isValidTile(x, y)) {
                        assertTrue(iter.hasNext());
                        Linked2DTile tile = iter.next();
                        assertEquals(x, tile.x);
                        assertEquals(y, tile.y);
                    }
                }
            }
            assertFalse(iter.hasNext());
            try {
                iter.next();
                fail("next() should throw");
            } catch (NoSuchElementException ex) {}
        }

        // Set one with a gap in-between
        set.set(3, 0);
        {
            Iterator<Linked2DTile> iter = set.iterateInverse().iterator();
            for (int y = -100; y <= 100; y++) {
                for (int x = -100; x <= 100; x++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    if (x == 1 && y == 0) {
                        continue;
                    }
                    if (x == 3 && y == 0) {
                        continue;
                    }
                    if (isValidTile(x, y)) {
                        assertTrue(iter.hasNext());
                        Linked2DTile tile = iter.next();
                        assertEquals(x, tile.x);
                        assertEquals(y, tile.y);
                    }
                }
            }
            assertFalse(iter.hasNext());
            try {
                iter.next();
                fail("next() should throw");
            } catch (NoSuchElementException ex) {}
        }

        // Verify the inverse set contains no elements when filled
        set.setAll();

        {
            Iterator<Linked2DTile> iter = set.iterateInverse().iterator();

            assertFalse(iter.hasNext());
            try {
                iter.next();
                fail("next() should throw");
            } catch (NoSuchElementException ex) {}
        }
    }
}
