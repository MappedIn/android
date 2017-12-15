package com.mappedin.examples.singlevenue;

import android.graphics.Color;

import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Cylinder;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.Model;
import com.mappedin.sdk.Vector3;

public class IAmHere {
    Model arrow;
    Cylinder cylinder;
    Map map;
    Coordinate[] frame;
    public IAmHere(){
        Model.Triangle triangle1 = new Model.Triangle(
                new Vector3(0, 1, 3.21f), new Vector3(-1, -1, 3.21f), new Vector3(0, 0, 3.21f));
        Model.Triangle triangle2 = new Model.Triangle(
                new Vector3(0, 1, 3.21f), new Vector3(0, 0, 3.21f), new Vector3(1, -1, 3.21f));
        Model.Triangle[] triangles = {triangle1, triangle2};
        this.arrow = new Model(triangles, Color.RED);
        this.cylinder = new Cylinder(1.7f, 3.2f, Color.GREEN);
    }

    public void setPosition(Map map, Coordinate position, float over){
        arrow.setPosition(position, over);
        cylinder.setPosition(position, over);
        this.map = map;
    }

    public void setRotation(float angle, float over){
        arrow.setHeading(angle, over);
    }
    public void setColor(int arrowColor, int cylinderColor, float over){
        arrow.setColor(arrowColor, over);
        cylinder.setColor(cylinderColor, over);
    }
    public Coordinate[] getFrame(){
        if (map != null) {
            Vector3 position = arrow.getPosition();
            float x = position.getX();
            float y = position.getY();
            float z = position.getZ();
            float offset = 20;
            frame = new Coordinate[4];
            frame[0] = new Coordinate(new Vector3(x + offset, y + offset, z), map);
            frame[1] = new Coordinate(new Vector3(x + offset, y - offset, z), map);
            frame[2] = new Coordinate(new Vector3(x - offset, y + offset, z), map);
            frame[3] = new Coordinate(new Vector3(x - offset, y - offset, z), map);
        }
        return frame;
    }
    public void addIAmHere(MapView mapView){
        mapView.addElement(arrow);
        mapView.addElement(cylinder);
    }
    public void removeIAmHere(MapView mapView){
        mapView.removeElement(arrow);
        mapView.removeElement(cylinder);
    }
}