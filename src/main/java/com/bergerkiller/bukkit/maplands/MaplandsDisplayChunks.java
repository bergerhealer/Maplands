package com.bergerkiller.bukkit.maplands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

/**
 * Asynchronously loads chunks needed to render a map
 */
public class MaplandsDisplayChunks {
    private World world = null;
    private final Map<IntVector2, LoadedChunk> cache = new HashMap<IntVector2, LoadedChunk>();
    private LoadedChunk lastValue = null;
    private int ticks = 0;

    public void update() {
        lastValue = null;
        ticks++;

        Iterator<LoadedChunk> iter = cache.values().iterator();
        while (iter.hasNext()) {
            LoadedChunk chunk = iter.next();
            if (ticks >= chunk.timeout) {
                chunk.close();
                iter.remove();
            }
        }
    }

    public boolean cacheBlock(World world, int x, int z) {
        return cacheChunk(world, MathUtil.toChunk(x), MathUtil.toChunk(z));
    }

    public boolean cacheChunk(World world, int x, int z) {
        if (this.world != world) {
            this.world = world;
            clear();
        }

        // If same as last value, return that instantly
        // Saves a lookup while scanning blocks
        if (lastValue != null && lastValue.getX() == x && lastValue.getZ() == z) {
            return lastValue.checkLoaded(this.ticks);
        }

        lastValue = cache.computeIfAbsent(new IntVector2(x, z), c -> new LoadedChunk(this.world, c.x, c.z, this.ticks));
        return lastValue.checkLoaded(this.ticks);
    }

    public void clear() {
        for (LoadedChunk chunk : cache.values()) {
            chunk.close();
        }
        cache.clear();
        lastValue = null;
    }

    private static class LoadedChunk implements AutoCloseable {
        private static final int CLOSE_TIMEOUT = 20 * 60; // ~1 minute
        public final ForcedChunk chunk;
        public int timeout;

        public LoadedChunk(World world, int x, int z, int ticks) {
            this.chunk = WorldUtil.forceChunkLoaded(world, x, z);
            this.timeout = ticks + CLOSE_TIMEOUT;
        }

        public int getX() {
            return chunk.getX();
        }

        public int getZ() {
            return chunk.getZ();
        }

        public boolean checkLoaded(int ticks) {
            this.timeout = ticks + CLOSE_TIMEOUT;
            return chunk.getChunkAsync().isDone();
        }

        @Override
        public void close() {
            chunk.close();
        }
    }
}
