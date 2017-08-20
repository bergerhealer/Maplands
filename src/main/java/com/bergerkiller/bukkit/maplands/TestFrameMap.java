package com.bergerkiller.bukkit.maplands;

import org.bukkit.Location;

import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapResourcePack;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.util.Matrix4f;
import com.bergerkiller.bukkit.common.map.util.Vector3f;

public class TestFrameMap extends MapDisplay {
    private MapResourcePack resources;

    @Override
    public void onAttached() {
        this.setSessionMode(MapSessionMode.VIEWING);
        
        resources = new MapResourcePack("C:\\Users\\QT\\Desktop\\TexturePack\\1.12.1.jar");
        
        //getLayer(0).draw(this.loadTexture("com/bergerkiller/bukkit/maplands/paws.png"), 0, 0);
    }

    @Override
    public void onTick() {
        getLayer(1).clear();
        
        Location loc = this.getOwners().get(0).getEyeLocation();

        Matrix4f modeloffset = new Matrix4f();
        modeloffset.set(new Vector3f(-8.0f, -8.0f, -8.0f));
        
        Matrix4f translation = new Matrix4f();
        translation.set(4.0f, new Vector3f(64, 0, 70));

        Matrix4f rotationPitch = new Matrix4f();
        rotationPitch.rotateX(loc.getPitch() - 90.0f);

        Matrix4f rotationYaw = new Matrix4f();
        rotationYaw.rotateY(loc.getYaw());

        Matrix4f transform = new Matrix4f();
        transform.setIdentity();
        transform.multiply(translation);
        transform.multiply(rotationPitch);
        transform.multiply(rotationYaw);
        transform.multiply(modeloffset);
        
        getLayer(1).drawModel(resources.getModel("block/repeater_on_4tick"), transform);
    }
}
