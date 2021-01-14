package com.bergerkiller.bukkit.maplands.util;

import java.util.NoSuchElementException;

/**
 * Stores a 2D rectangular set of tiles that can efficiently
 * be iterated and modified by linking the available tiles
 * together.
 */
public class Linked2DTileSet {
    private final int _offX;
    private final int _offY;
    private final int _width;
    private final int _height;
    private final Linked2DTileList[] _depths;
    private final Linked2DTile[] _tiles;

    /**
     * Default constructor for a 0x0 no-element set
     */
    public Linked2DTileSet() {
        this(0, -1, 0, -1);
    }

    /**
     * Creates a new Tile2DSet using the specified range
     * of x/y values. Initially all tiles are set.
     * 
     * @param minX Minimum x-coordinate (inclusive)
     * @param maxX Maximum x-coordinate (inclusive)
     * @param minY Minimum y-coordinate (inclusive)
     * @param maxY Maximum y-coordinate (inclusive)
     */
    public Linked2DTileSet(int minX, int maxX, int minY, int maxY) {
        this._offX = minX;
        this._offY = minY;
        this._width = maxX - minX + 1;
        this._height = maxY - minY + 1;
        this._depths = new Linked2DTileList[] {new Linked2DTileList(),
                                               new Linked2DTileList(),
                                               new Linked2DTileList()};

        // Create all tiles (that exist). Initially, do not store in the
        // linked list, which makes it 'empty'.
        this._tiles = new Linked2DTile[this._width * this._height];
        int index = 0;
        for (int y = 0; y < this._height; y++) {
            for (int x = 0; x < this._width; x++) {
                Linked2DTile tile = new Linked2DTile(this._offX + x, this._offY + y);
                if (tile.getDepthModThree() != -1) {
                    this._tiles[index] = tile;
                }
                index++;
            }
        }
    }

    public int getMinX() {
        return this._offX;
    }

    public int getMaxX() {
        return this._offX + this._width - 1;
    }

    public int getMinY() {
        return this._offY;
    }

    public int getMaxY() {
        return this._offY + this._height - 1;
    }

    /**
     * Gets whether this tile set is empty, indicating no
     * tiles are set
     * 
     * @return True if empty
     */
    public boolean isEmpty() {
        return this._depths[0].isEmpty()
                && this._depths[1].isEmpty()
                && this._depths[2].isEmpty();
    }

    /**
     * Fills this entire set, setting it to contain all tiles
     */
    public void setAll() {
        // Start at the head of all three depth levels
        Linked2DTile[] current = new Linked2DTile[] {
                this._depths[0].head,
                this._depths[1].head,
                this._depths[2].head
        };

        // Go by all tiles and connect them together
        // in the right depth-specific linked list.
        for (Linked2DTile tile : this._tiles) {
            if (tile != null) {
                int depth = tile.getDepthModThree();
                Linked2DTile.link(current[depth], tile);
                current[depth] = tile;
            }
        }

        // Connect to tail
        Linked2DTile.link(current[0], this._depths[0].tail);
        Linked2DTile.link(current[1], this._depths[1].tail);
        Linked2DTile.link(current[2], this._depths[2].tail);
    }

    /**
     * Clears this entire set, setting it to contain no tiles at all (empty)
     */
    public void clearAll() {
        if (!isEmpty()) {
            for (Linked2DTile tile : this._tiles) {
                if (tile != null) {
                    tile.prev = null;
                    tile.next = null;
                }
            }

            this._depths[0].linkHeadToTail();
            this._depths[1].linkHeadToTail();
            this._depths[2].linkHeadToTail();
        }
    }

    /**
     * Gets whether a tile is set
     * @param x X-coordinate of the tile
     * @param y Y-coordinate of the tile
     * @return True if the tile at this coordinate was set
     * @throws IllegalArgumentException If coordinate is out of range
     */
    public boolean contains(int x, int y) {
        Linked2DTile tile = this._tiles[getIndex(x, y)];
        return tile != null && tile.isSet();
    }

    /**
     * Sets a tile in this set
     * 
     * @param x X-coordinate of the tile
     * @param y Y-coordinate of the tile
     * @return True if the tile was set, False if it was already set
     * @throws IllegalArgumentException If coordinate is out of range
     */
    public boolean set(int x, int y) {
        int index = getIndex(x, y);
        Linked2DTile tile = this._tiles[index];
        if (tile == null || tile.isSet()) {
            return false; // Already set
        }

        // The linked list of this depth level stores the list
        final int depth = tile.getDepthModThree();

        // Try to find the nearest set tile from this tile
        // back and forwards. As soon we hit something, the
        // prev/next allows us to insert it.
        Linked2DTile prev, next;
        int prev_index = index;
        int next_index = index;
        int num_tiles = this._tiles.length;
        while (true) {
            if (--prev_index < 0) {
                prev = this._depths[depth].head;
                next = this._depths[depth].head.next;
                break;
            }
            if (++next_index >= num_tiles) {
                prev = this._depths[depth].tail.prev;
                next = this._depths[depth].tail;
                break;
            }
            prev = this._tiles[prev_index];
            if (prev != null && prev.isSet() && prev.getDepthModThree() == depth) {
                next = prev.next;
                break;
            }
            next = this._tiles[next_index];
            if (next != null && next.isSet() && next.getDepthModThree() == depth) {
                prev = next.prev;
                break;
            }
        }

        Linked2DTile.link(prev, tile);
        Linked2DTile.link(tile, next);
        return true;
    }

    /**
     * Clears a tile in this set
     * 
     * @param x X-coordinate of the tile
     * @param y Y-coordinate of the tile
     * @return True if the tile was cleared, False if it was already cleared
     * @throws IllegalArgumentException If coordinate is out of range
     */
    public boolean clear(int x, int y) {
        int index = getIndex(x, y);
        Linked2DTile tile = this._tiles[index];
        if (tile != null && tile.isSet()) {
            Linked2DTile.link(tile.prev, tile.next);
            tile.prev = null;
            tile.next = null;
            return true;
        } else {
            return false;
        }
    }

    private int getIndex(int x, int y) {
        x -= this._offX;
        y -= this._offY;
        if (x < 0 || x >= this._width || y < 0 || y >= this._height) {
            throw new IllegalArgumentException("Coordinate {" + x + ", " + y + "} is out of range");
        }
        return (y * this._width) + x;
    }

    /**
     * Gets the linked list of tiles that are valid and drawn
     * at the given depth (z) level.
     *
     * @param depth Depth level
     * @return tiles drawn at this depth level
     */
    public Linked2DTileList getValidTiles(int depth) {
        return this._depths[Math.floorMod(depth, 3)];
    }

    /**
     * Creates an iterable that when iterated, iterates
     * all the tiles **not** set in this set.
     * 
     * @return inverse iterable view
     */
    public Iterable<Linked2DTile> iterateInverse() {
        return () -> new InverseIterator(this);
    }

    /**
     * Iterator for all the tiles NOT set
     */
    private static final class InverseIterator implements java.util.Iterator<Linked2DTile> {
        private final Linked2DTile[] _tiles;
        private int _index;

        public InverseIterator(Linked2DTileSet set) {
            this._tiles = set._tiles;
            this._index = 0;
        }

        private Linked2DTile advance() {
            if (this._index >= this._tiles.length) {
                return null; // end of iteration
            }

            do {
                Linked2DTile tile = this._tiles[this._index];
                if (tile != null && !tile.isSet()) {
                    return tile; // Found one!
                }
            } while (++this._index < this._tiles.length);

            return null;
        }

        @Override
        public boolean hasNext() {
            return advance() != null;
        }

        @Override
        public Linked2DTile next() {
            Linked2DTile result = advance();
            if (result == null) {
                throw new NoSuchElementException();
            } else {
                this._index++;
                return result;
            }
        }
    }
}
