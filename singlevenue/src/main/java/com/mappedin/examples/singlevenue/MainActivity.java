package com.mappedin.examples.singlevenue;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapViewDelegate;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.Overlay;
import com.mappedin.sdk.Polygon;
import com.mappedin.sdk.RawData;
import com.mappedin.sdk.Venue;
import com.mappedin.jpct.Logger;

public class MainActivity extends AppCompatActivity implements MapViewDelegate {

    private boolean accessibleDirections = false;

    private MapViewDelegate delegate = this;

    private MappedIn mappedIn = null;
    private MapView mapView = null;
    private Map[] maps = null;

    private Venue activeVenue = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mappedIn = new MappedIn(getApplicationContext());
        mappedIn.getVenues(new GetVenuesCallback());
    }

    // Get the basic info for all Venues we have access to
    private class GetVenuesCallback implements MappedinCallback<List<Venue>> {
        @Override
        public void onCompleted(final List<Venue> venues) {
            if (venues.size() == 0 ) {
                Logger.log("No venues available! Are you using the right credentials? Talk to your mappedin representative.");
                return;
            }
            activeVenue = venues.get(0); // Grab the first venue, which is likely all you have
            setTitle(activeVenue.getName());
            mapView = (MapView) getFragmentManager().findFragmentById(R.id.mapFragment);
            mapView.setDelegate(delegate);
            mappedIn.getVenue(activeVenue, accessibleDirections, new CustomLocationGenerator(), new GetVenueCallback());
        }

        @Override
        public void onError(Exception e) {
            Logger.log("Error loading any venues. Did you set your credentials? Exception: " + e);
        }
    }

    // Get the full details on a single Venue
    private class GetVenueCallback implements MappedinCallback<Venue> {
        @Override
        public void onCompleted(final Venue venue) {
            List<Map> mapsList = venue.getMaps();
            if (mapsList.size() == 0) {
                Logger.log("No maps! Make sure your venue is set up correctly!");
                return;
            }
            maps = new Map[mapsList.size()];
            venue.getMaps().toArray(maps);

            Arrays.sort(maps, new Comparator<Map>() {
                @Override
                public int compare(Map a, Map b) {
                    return (int) (a.getElevation() - b.getElevation());
                }
            });
            mapView.setMap(maps[0]);
        }

        @Override
        public void onError(Exception e) {
            Logger.log("Error loading Venue: " + e);
        }

    }

    private class CustomLocationGenerator implements LocationGenerator {
        @Override
        public Location locationGenerator(RawData rawData) throws Exception {
            return new Location(rawData);
        }
    }

    public void didTapPolygon(Polygon polygon) {
        if (polygon.getLocations().size() > 0) {
            polygon.setColor(0x4ca1fc);
        }
    }

    public void didTapMarker() {

    }

    public void didTapOverlay(Overlay var1) {

    }

    public void didTapNothing() {

    }
}