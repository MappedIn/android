package mappedin.com.wayfindingsample;

import android.graphics.Color;

import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Cylinder;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.Model;

import com.mappedin.sdk.Vector3;

/**
 * Created by Peter on 2017-07-04.
 */

public class IAmHere {
    private Model arrow;
    private Cylinder cylinder;

    IAmHere(){
        Model.Triangle triangle1 = new Model.Triangle(
                new Vector3(0, 1, 10.01f), new Vector3(-1, -1, 10.01f), new Vector3(0, 0, 10.01f));
        Model.Triangle triangle2 = new Model.Triangle(
                new Vector3(0, 1, 10.01f), new Vector3(0, 0, 10.01f), new Vector3(1, -1, 10.01f));
        Model.Triangle[] triangles = {triangle1, triangle2};
        this.arrow = new Model(triangles, Color.BLUE);
        this.cylinder = new Cylinder(1.7f, 10f, Color.GRAY);
    }

    void setPosition(Coordinate position, float over){
        arrow.setPosition(position, over);
        cylinder.setPosition(position, over);
    }

    void setRotation(float angle, float over){
        arrow.setHeading(angle, over);
    }
    void setColor(int arrowColor, int cylinderColor, float over){
        arrow.setColor(arrowColor, over);
        cylinder.setColor(cylinderColor, over);
    }
    void addIAmHere(MapView mapView){
        mapView.addElement(arrow);
        mapView.addElement(cylinder);
    }
    void removeIAmHere(MapView mapView){
        mapView.removeElement(arrow);
        mapView.removeElement(cylinder);
    }
}