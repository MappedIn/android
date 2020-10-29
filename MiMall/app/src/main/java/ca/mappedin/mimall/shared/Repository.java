package ca.mappedin.mimall.shared;

import android.util.Log;

import com.mappedin.models.MiLocation;
import com.mappedin.models.MiSpace;

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

    public MiSpace searchLocations(String keyword) {
        for (MiLocation location :
                locations) {
            if (location.getName().equals(keyword)) {
                return location.getSpaces().get(0);
            }
        }
        return null;
    }

    public String[] getLocationsArray() {
        ArrayList<String> locations = new ArrayList<String>();
        for (MiLocation location :
                this.locations) {
            locations.add(location.getName());

            for (MiSpace space :
                    location.getSpaces()) {
                Log.d("LOG", "" + space.getExternalId());
            }
        }
        String[] searchResults = new String[locations.size()];
        return locations.toArray(searchResults);
    }

    public void setLocations(List<MiLocation> locations) {
        this.locations = locations;
    }
}
