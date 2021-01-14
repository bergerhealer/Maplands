package com.bergerkiller.bukkit.maplands.util;

/**
 * A single linked list of tiles. There are three
 * depth levels per {@link Linked2DTileSet}, each represented
 * by a tile list.<br>
 * <br>
 * To iterate, the first element is the {@link Linked2DTile#next}
 * of {@link #head}, and once {@link #tail} is found, the end is
 * reached. The head and tail tiles are not actual tiles.
 */
public class Linked2DTileList {
    public final Linked2DTile head;
    public final Linked2DTile tail;

    public Linked2DTileList() {
        this.head = new Linked2DTile(Integer.MIN_VALUE, Integer.MIN_VALUE);
        this.tail = new Linked2DTile(Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.linkHeadToTail();
    }

    public void linkHeadToTail() {
        Linked2DTile.link(this.head, this.tail);
    }

    public boolean isEmpty() {
        return this.head.next == this.tail;
    }
}
