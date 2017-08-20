package com.bergerkiller.bukkit.maplands;

import java.awt.GraphicsEnvironment;
import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bergerkiller.bukkit.common.events.map.MapClickEvent;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.events.map.MapShowEvent;
import com.bergerkiller.bukkit.common.map.MapBlendMode;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;

public class TCMapListener implements Listener {
    private final TestFramedMap globalDisplay = new TestFramedMap();
    private TestMap test = new TestMap();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMapViewed(MapShowEvent event) {
        if (!event.hasDisplay()) {
            event.setDisplay(Maplands.plugin, globalDisplay);
        }

        if (event.isHeldEvent() && !(event.getDisplay() instanceof TestMap)) {
            test = new TestMap();
            event.setDisplay(Maplands.plugin, test);
        }
    }

    public static class TestFramedMap extends MapDisplay {
        int ctr = 0;
        int hitCtr = 0;
        double hitEffect = 0.0;
        boolean state = false;
        MapTexture texture;
        MapTexture bloodText;
        MapTexture faceText;
        Random rand = new Random();
        Location loc = null;

        @Override
        public void onAttached() {
            faceText = loadTexture("com/bergerkiller/bukkit/maps/face1.png");
            bloodText = MapTexture.createEmpty(128, 128);
        }

        @Override
        public void onLeftClick(MapClickEvent event) {
            hitCtr++;
            hitEffect += 4.0;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 || dy == 0) {
                        bloodText.writePixel(event.getX() + dx, event.getY() + dy, MapColorPalette.getColor(255, rand.nextInt(100), rand.nextInt(100)));
                    }
                }
            }
            

            if (hitCtr == 0) {
                faceText = loadTexture("com/bergerkiller/bukkit/maps/face1.png");
            } else if (hitCtr < 5) {
                faceText = loadTexture("com/bergerkiller/bukkit/maps/face2.png");
            } else if (hitCtr < 10) {
                faceText = loadTexture("com/bergerkiller/bukkit/maps/face3.png");
            } else {
                faceText = loadTexture("com/bergerkiller/bukkit/maps/face4.png");
            }
            
            loc = event.getItemFrame().getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_HURT, 1.0f, 0.7f + 0.3f * rand.nextFloat());
            
            event.setCancelled(true);
        }

        @Override
        public void onTick() {
            int dx = (int) (hitEffect * (rand.nextDouble() - 0.5));
            int dy = (int) (hitEffect * (rand.nextDouble() - 0.5));
            if (hitCtr > 12) {
                hitEffect += hitCtr * 0.01;
            } else {
                hitEffect *= 0.8;
            }
            
            if (hitEffect > 10.0 && loc != null) {
                loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f + 0.3f * rand.nextFloat());
                loc.getWorld().playEffect(loc, Effect.SMOKE, 4);
            }
            if (hitEffect > 30.0) {
                loc.getWorld().createExplosion(loc, 20.0f);
                hitEffect = 0.0;
                hitCtr = 0;
                bloodText = MapTexture.createEmpty(128, 128);
            }
            
            this.getLayer(1).clear().draw(faceText, dx, dy);
            this.getLayer(2).clear().draw(bloodText, dx, dy);
        }
    }

    public static class TestMap extends MapDisplay {
        int alive = 0;
        public MapTexture background, cursor, tmp;
        public MapFont<Character> font = MapFont.fromJavaFont("Calibri", 12);
        public Random rand = new Random();
        public int mx, my;
        int ctr = 0;
        boolean vis = false;
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        int nameidx = 0;

        @Override
        public void onAttached() {
            this.setSessionMode(MapSessionMode.HOLDING);
            this.setReceiveInputWhenHolding(true);
            background = loadTexture("com/bergerkiller/bukkit/maps/background.png");
            cursor = loadTexture("com/bergerkiller/bukkit/maps/test.png");
            tmp = cursor.clone();

            getLayer().draw(background, 0, 0);
            mx = 50;
            my = 50;
        }

        @Override
        public void onKeyPressed(MapKeyEvent event) {
            mx += event.getKey().dx();
            my += event.getKey().dy();
            if (event.getKey() == Key.ENTER) {
                if (++nameidx == names.length) {
                    nameidx = 0;
                }
                this.font = MapFont.fromJavaFont(names[nameidx], 12);
            }
        }

        @Override
        public void onTick() {
            if (++ctr == 10) {
                ctr = 0;
                vis = !vis;
                tmp.setBlendMode(MapBlendMode.NONE).draw(cursor, 0, 0);
                if (vis) {
                    tmp.setBlendMode(MapBlendMode.ADD).fill((byte) 77);
                }
            }
            getLayer(1).clear().draw(tmp, mx, my);
            getLayer(2).clear().draw(font, 10, 70, MapColorPalette.COLOR_WHITE, "12345 abcdefg");
            getLayer(3).clear().draw(MapFont.MINECRAFT, 50, 20, MapColorPalette.COLOR_WHITE, "ALIVE: " + (++alive));

            //for (int i = 1; i <= 5; i++) {
            //    getLayer(i).clear().blend(cursor, rand.nextInt(128), rand.nextInt(128));
            //}
        }
    }

}
