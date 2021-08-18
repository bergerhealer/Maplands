package com.bergerkiller.bukkit.maplands;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.bergerkiller.bukkit.common.map.MapTexture;

public class TestDepthBufferEncoding {

    @Test
    public void testDepthEncoding() throws IOException {
        byte[] encoded_data;

        // Encode depth buffer
        {
            MapTexture texture = MapTexture.createEmpty(1024, 512);
            texture.setDrawDepth(0);
            short[] buf = texture.getDepthBuffer();
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (short) (i % 65521);
            }

            BufferedImage image = MapCanvasCache.depthToGrayscaleImage(texture);

            // Verify image itself is valid
            verifyDepthImage(image);

            // Encode as png
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", stream);
                encoded_data = stream.toByteArray();
            }
        }

        // Decode png image as short[] array and verify data matches
        {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded_data));
            verifyDepthImage(image);
        }
    }

    private void verifyDepthImage(BufferedImage image) {
        DataBufferUShort depth_buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        short[] buf = depth_buffer.getData();
        for (int i = 0; i < buf.length; i++) {
            assertEquals((short) (i % 65521), buf[i]);
        }
    }
}
