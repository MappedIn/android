package mappedin.com.wayfindingsample;

import com.mappedin.sdk.Directions;
import com.mappedin.sdk.Element;
import com.mappedin.sdk.Overlay2DImage;
import com.mappedin.sdk.Path;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by christinemaiolo on 2018-04-20.
 */

class NavigationPath {
    Directions directions;
    Path routePath;
    ArrayList<Element> pathElements = new ArrayList<>();
    HashMap<Overlay2DImage, Integer> vortexes;

    NavigationPath(Directions directions, Path routePath, ArrayList<Element> pathElements, HashMap<Overlay2DImage, Integer> vortexes){
        this.directions = directions;
        this.routePath = routePath;
        this.pathElements = pathElements;
        this.vortexes = vortexes;
    }
}
