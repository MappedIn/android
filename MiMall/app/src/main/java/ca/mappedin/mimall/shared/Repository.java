package ca.mappedin.mimall.shared;

import com.mappedin.models.MiLocation;

import java.util.ArrayList;
import java.util.List;

public class Repository {

    private List<MiLocation> locations = new ArrayList<MiLocation>();

    private static Repository instance = null;

    private Repository() {
    }


    public static Repository getInstance() {
        if (instance == null) {
            instance = new Repository();
        }

        return instance;
    }

    public List<MiLocation> getLocations() {
        return locations;
    }

    public List<MiLocation> searchLocations(String keyword) {
        ArrayList<MiLocation> matchingLocations = new ArrayList<MiLocation>();
        //TODO Perform Search Operation
        return matchingLocations;
    }

    public void setLocations(List<MiLocation> locations) {
        this.locations = locations;
    }
}
