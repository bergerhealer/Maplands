package com.bergerkiller.bukkit.maplands;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.mountiplex.reflection.SafeMethod;

public class MapCanvasCache {
    private final Maplands _plugin;
    private boolean _enabled = true;
    private final File _cacheFolder;
    private final Map<UUID, Item> _cache = new ConcurrentHashMap<UUID, Item>();
    private final AsyncTask _saveTask = new AsyncTask() {
        @Override
        public void run() {
            while (true) {
                boolean savedSomething = false;
                long expireTime = System.currentTimeMillis() - 10*60*1000; // 10 minutes
                Iterator<Map.Entry<UUID, Item>> iter = _cache.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, Item> entry = iter.next();
                    Item item = entry.getValue();
                    if (item.saved.compareAndSet(false, true)) {
                        UUID mapUUID = entry.getKey();

                        savedSomething = true;
                        _cacheFolder.mkdirs();

                        boolean success = true;
                        try {
                            success &= ImageIO.write(item.color, "gif", getColorFile(mapUUID));
                        } catch (IOException e) {
                            _plugin.getLogger().log(Level.SEVERE, "Failed to save color data of {" + mapUUID.toString() + "} to cache", e);
                            success = false;
                        }
                        try {
                            success &= ImageIO.write(item.depth, "png", getDepthFile(mapUUID));
                        } catch (IOException e) {
                            _plugin.getLogger().log(Level.SEVERE, "Failed to save depth data of {" + mapUUID.toString() + "} to cache", e);
                            success = false;
                        }
                        if (!success) {
                            // Cleanup
                            getColorFile(mapUUID).delete();
                            getDepthFile(mapUUID).delete();
                        }
                    } else if (item.created < expireTime) {
                        iter.remove();
                    }
                }
                if (!savedSomething) {
                    synchronized (this) {
                        if (this.isStopRequested()) {
                            break;
                        } else {
                            try {
                                this.wait(5000);
                            } catch (InterruptedException e) {}
                        }
                    }
                }
            }
        }
    };

    public MapCanvasCache(Maplands plugin, File cacheFolder) {
        _plugin = plugin;
        _cacheFolder = cacheFolder;
    }

    public void setEnabled(boolean enabled) {
        _enabled = enabled;
        if (!enabled) {
            if (_saveTask.isRunning()) {
                synchronized (_saveTask) {
                    _saveTask.notify();
                    _saveTask.stop();
                }
                _saveTask.waitFinished();
            }
            _cache.clear();
        }
    }

    public void save(UUID mapUUID, MapCanvas canvas) {
        if (!_enabled) {
            return;
        }
        synchronized (_saveTask) {
            _cache.put(mapUUID, new Item(toJavaImageIndexed(canvas), depthToGrayscaleImage(canvas)));
            _saveTask.notify();
            if (!_saveTask.isRunning()) {
                _saveTask.start();
            }
        }
    }

    public boolean load(UUID mapUUID, MapCanvas canvas) {
        if (!_enabled) {
            return false;
        }
        Item item = _cache.get(mapUUID);
        if (item == null) {
            // Load from disk (sync), is slower!
            // Do not put it in cache
            File colorFile = getColorFile(mapUUID);
            File depthFile = getDepthFile(mapUUID);
            if (!colorFile.exists() || !depthFile.exists()) {
                return false;
            }

            BufferedImage color, depth;
            try {
                color = ImageIO.read(colorFile);
            } catch (IOException e) {
                _plugin.getLogger().log(Level.SEVERE, "Failed to load color data of {" + mapUUID.toString() + "} from cache", e);
                return false;
            }
            try {
                depth = ImageIO.read(depthFile);
            } catch (IOException e) {
                _plugin.getLogger().log(Level.SEVERE, "Failed to load depth data of {" + mapUUID.toString() + "} from cache", e);
                return false;
            }
            if (color.getWidth() != depth.getWidth() || color.getHeight() != depth.getHeight()) {
                _plugin.getLogger().log(Level.SEVERE, "Failed to load data of {" + mapUUID.toString() + "} from cache: image resolutions don't match!");
                return false;
            }
            item = new Item(color, depth);
        }

        // Verify the buffers are at all compatible. If the map was resized, ignore the data and regenerate.
        if (item.color.getWidth() != canvas.getWidth() || item.color.getHeight() != canvas.getHeight()) {
            return false;
        }

        // Decode the data in the images
        DataBufferByte color_buffer = CommonUtil.tryCast(item.color.getRaster().getDataBuffer(), DataBufferByte.class);
        DataBufferUShort depth_buffer = CommonUtil.tryCast(item.depth.getRaster().getDataBuffer(), DataBufferUShort.class);
        if (color_buffer == null) {
            _plugin.getLogger().log(Level.SEVERE, "Failed to load color data of {" + mapUUID.toString() + "} from cache: invalid bit format!");
        }
        if (depth_buffer == null) {
            _plugin.getLogger().log(Level.SEVERE, "Failed to load depth data of {" + mapUUID.toString() + "} from cache: invalid bit format!");
        }

        // Write it directly to the canvas layer
        try {
            canvas.setDrawDepth(MapCanvas.MAX_DEPTH);
            short[] canvas_depth = canvas.getDepthBuffer();
            System.arraycopy(depth_buffer.getData(), 0, canvas_depth, 0, canvas_depth.length);
            canvas.writePixels(0, 0, item.color.getWidth(), item.color.getHeight(), color_buffer.getData());
            return true;
        } catch (Throwable t) {
            _plugin.getLogger().log(Level.SEVERE, "Failed to load data of {" + mapUUID.toString() + "} from cache", t);
            return false;
        }
    }

    private File getColorFile(UUID mapUUID) {
        return new File(_cacheFolder, mapUUID.toString() + "_color.gif");
    }

    private File getDepthFile(UUID mapUUID) {
        return new File(_cacheFolder, mapUUID.toString() + "_depth.png");
    }

    // Turns the depth buffer into a 16-bit grayscale image
    public static BufferedImage depthToGrayscaleImage(MapCanvas canvas) {
        short[] depth = canvas.getDepthBuffer();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        DataBufferUShort dataBuffer = new DataBufferUShort(depth, depth.length);
        ColorModel c = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {16}, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT); 
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width, 1, new int[] {0}, null);
        return new BufferedImage(c, raster, c.isAlphaPremultiplied(), null);
    }

    //TODO: We no longer need this once we depend on a version of BKCommonLib that always has this method
    //      The fallback implementation can then be removed
    private static Function<MapCanvas, BufferedImage> toJavaImageIndexedMethod = null;
    public static BufferedImage toJavaImageIndexed(MapCanvas canvas) {
        if (toJavaImageIndexedMethod == null) {
            if (SafeMethod.contains(MapCanvas.class, "toJavaImageIndexed")) {
                final SafeMethod<BufferedImage> method = new SafeMethod<BufferedImage>(MapCanvas.class, "toJavaImageIndexed");
                toJavaImageIndexedMethod = (c) -> { return method.invoke(c); };
            } else {
                // Implement workaround
                final IndexColorModel colorModel;
                {
                    int num_colors = 0;
                    byte[] r = new byte[256];
                    byte[] g = new byte[256];
                    byte[] b = new byte[256];
                    byte[] a = new byte[256];
                    for (int i = 0; i < 256; i++) {
                        Color c = MapColorPalette.getRealColor(i);
                        if (c.getAlpha() >= 128) {
                            num_colors = (i + 1);
                            r[i] = (byte) c.getRed();
                            g[i] = (byte) c.getGreen();
                            b[i] = (byte) c.getBlue();
                            a[i] = (byte) c.getAlpha();
                        }
                    }
                    colorModel = new IndexColorModel(8, num_colors, r, g, b, a);
                }
                toJavaImageIndexedMethod = (c) -> {
                    byte[] buffer = c.getBuffer().clone(); //Note: readPixels() was bugged on an older version of BKCommonLib!
                    int width = c.getWidth();
                    int height = c.getHeight();
                    java.awt.image.DataBufferByte data = new java.awt.image.DataBufferByte(buffer, buffer.length);
                    WritableRaster raster = Raster.createInterleavedRaster(data, width, height, width, 1, new int[] {0}, null);
                    return new BufferedImage(colorModel, raster, false, null);
                };
            }
        }
        return toJavaImageIndexedMethod.apply(canvas);
    }

    public static class Item {
        public final BufferedImage color;
        public final BufferedImage depth;
        public final long created;
        public final AtomicBoolean saved;

        private Item(BufferedImage color, BufferedImage depth) {
            this.color = color;
            this.depth = depth;
            this.created = System.currentTimeMillis();
            this.saved = new AtomicBoolean(false);
        }
    }
}
