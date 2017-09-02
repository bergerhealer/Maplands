package com.bergerkiller.bukkit.maplands;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.Ignore;
import org.junit.Test;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.MapDebugWindow;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class TestSprites {

    static {
        CommonUtil.bootstrap();
    }

    @Ignore
    @Test
    public void testSprites() {
        MapTexture map = MapTexture.createEmpty(200, 200);
        IsometricBlockSprites sprites_a = IsometricBlockSprites.getSprites(BlockFace.NORTH_EAST, 4);
        IsometricBlockSprites sprites_b = IsometricBlockSprites.getSprites(BlockFace.NORTH_WEST, 4);
        IsometricBlockSprites sprites_c = IsometricBlockSprites.getSprites(BlockFace.SOUTH_EAST, 4);
        IsometricBlockSprites sprites_d = IsometricBlockSprites.getSprites(BlockFace.SOUTH_WEST, 4);
        
        map.fill(MapColorPalette.COLOR_RED);
        
        map.setRelativeBrushMask(sprites_a.getBrushTexture());

        map.draw(sprites_a.getSprite(Material.GRASS), 0, 0);
        map.draw(sprites_b.getSprite(Material.GRASS), 16, 32);
        map.draw(sprites_c.getSprite(Material.GRASS), 32, 64);
        map.draw(sprites_d.getSprite(Material.GRASS), 48, 96);
        
        map.draw(sprites_b.getSprite(Material.GRASS), 16 + 32, 32);
        
        map.draw(sprites_b.getSprite(Material.GRASS), 16 + 32 + 16, 32 + 12);
        
        map.setRelativeBrushMask(null);
        
        map.fillRectangle(16 - 1, 21, 2, 2, MapColorPalette.COLOR_BLUE);
        map.fillRectangle(32 - 1, 21 + 32, 2, 2, MapColorPalette.COLOR_BLUE);
        map.fillRectangle(48 - 1, 21 + 64, 2, 2, MapColorPalette.COLOR_BLUE);
        map.fillRectangle(64 - 1, 21 + 96, 2, 2, MapColorPalette.COLOR_BLUE);
        
        MapDebugWindow.showMapForever(map, 4);
    }
}
