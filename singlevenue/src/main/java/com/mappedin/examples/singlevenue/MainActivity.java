package com.mappedin.examples.singlevenue;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import com.koushikdutta.ion.Ion;
import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Directions;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapViewCamera;
import com.mappedin.sdk.MapViewDelegate;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.Overlay;
import com.mappedin.sdk.Path;
import com.mappedin.sdk.Polygon;
import com.mappedin.sdk.RawData;
import com.mappedin.sdk.Venue;
import com.mappedin.sdk.Overlay2DLabel;
import com.mappedin.jpct.Logger;

public class MainActivity extends AppCompatActivity implements MapViewDelegate {

    private boolean accessibleDirections = false;

    private MapViewDelegate delegate = this;

    private MappedIn mappedIn = null;
    private MapView mapView = null;
    private Map[] maps = null;

    private Spinner mapSpinner = null;
    private TextView titleLabel = null;
    private TextView descriptionLabel = null;
    private ImageView logoImageView = null;
    private TextView selectOriginTextView = null;
    private Button goButton = null;
    private Button showLocationsButton = null;

    private HashMap<Polygon, Integer> originalColors = new HashMap<Polygon, Integer>();
    private HashMap<Overlay, LocationLabelClicker> overlays = new HashMap<Overlay, LocationLabelClicker>();

    private Venue activeVenue = null;

    private Context context;

    private boolean navigationMode = false;
    private Path path;

    private Polygon destinationPolygon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        mappedIn = new MappedIn(getApplicationContext());

        mapSpinner = (Spinner) findViewById(R.id.mapSpinner);
        logoImageView = (ImageView) findViewById(R.id.logoImageView);
        titleLabel = (TextView) findViewById(R.id.titleLabel);
        descriptionLabel = (TextView) findViewById(R.id.descriptionLabel);
        selectOriginTextView = (TextView) findViewById(R.id.selectOriginTextView);
        goButton = (Button) findViewById(R.id.goButton);
        goButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startNavigation();
            }
        });

        showLocationsButton = (Button) findViewById(R.id.showLocationButton);
        showLocationsButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showLocations();}});

        mappedIn.getVenues(new GetVenuesCallback());

    }

    // Get the basic info for all Venues we have access to
    private class GetVenuesCallback implements MappedinCallback<Venue[]> {
        @Override
        public void onCompleted(final Venue[] venues) {
            Logger.log("++++++ GetVenuesCallback");
            if (venues.length == 0 ) {
                Logger.log("No venues available! Are you using the right credentials? Talk to your mappedin representative.");
                return;
            }
            activeVenue = venues[0]; // Grab the first venue, which is likely all you have
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
            Map[] maps = venue.getMaps();
            if (maps.length == 0) {
                Logger.log("No maps! Make sure your venue is set up correctly!");
                return;
            }

            Arrays.sort(maps, new Comparator<Map>() {
                @Override
                public int compare(Map a, Map b) {
                    return (int) (a.getElevation() - b.getElevation());
                }
            });
            mapView.setMap(maps[0]);
            //mapSpinner.setAdapter(new ArrayAdapter<Map>());
        }

        @Override
        public void onError(Exception e) {
            Logger.log("Error loading Venue: " + e);
        }

    }

    private class CustomLocationGenerator implements LocationGenerator {
        @Override
        public Location locationGenerator(RawData rawData) throws Exception {
            return new CustomLocation(rawData);
        }
    }

    public void didTapPolygon(Polygon polygon) {
        if (navigationMode) {
            if (path != null) {
                didTapNothing();
                return;
            }
            if (polygon.getLocations().size() == 0) {
                return;
            }

            Directions directions = destinationPolygon.directionsFrom(activeVenue, polygon, destinationPolygon.getLocations().get(0).getName(), polygon.getLocations().get(0).getName());
            if (directions != null) {
                path = new Path(directions.getPath(), 5f, 5f, 0x4ca1fc);
                mapView.addPath(path);
                mapView.getCamera().focusOn(directions.getPath());
            }

            highlightPolygon(polygon, 0x007afb);
            highlightPolygon(destinationPolygon, 0xff834c);
            selectOriginTextView.setVisibility(View.INVISIBLE);
            return;
        }
        clearHighlightedColours();
        if (polygon.getLocations().size() == 0) {
            return;
        }
        destinationPolygon = polygon;
        highlightPolygon(polygon, 0x4ca1fc);
        showLocationDetails((CustomLocation) polygon.getLocations().get(0));
    }

    public void didTapMarker() {

    }

    public void didTapOverlay(Overlay overlay) {
        LocationLabelClicker clicker = overlays.get(overlay);
        if (clicker != null) {
            clicker.click();
        } else {
            Logger.log("No click");
        }
    }

    public void didTapNothing() {
        clearHighlightedColours();
        clearLocationDetails();
        stopNavigation();
        clearMarkers();

    }

    private void highlightPolygon(Polygon polygon, int color) {
        if (!originalColors.containsKey(polygon)) {
            originalColors.put(polygon, polygon.getColor());
        }
        polygon.setColor(color);
    }

    private void clearHighlightedColours() {
        Set<Entry<Polygon, Integer>> colours = originalColors.entrySet();
        for  (Entry<Polygon, Integer> pair : colours) {
            pair.getKey().setColor(pair.getValue());
        }

        originalColors.clear();
    }

    private void showLocationDetails(CustomLocation location) {
        clearLocationDetails();
        titleLabel.setText(location.getName());
        descriptionLabel.setText(location.description);
        goButton.setVisibility(View.VISIBLE);

        // This sample is using the Ion framework for easy image loading/cacheing. You can use what you like
        // https://github.com/koush/ion
        if (location.logo != null) {
            String url = location.logo.get(logoImageView.getWidth(), this).toString();
            Logger.log("++++ " + url);
            if (url != null) {
                Ion.with(logoImageView)
                        //.placeholder(R.drawable.placeholder_image)
                        //.error(R.drawable.error_image)
                        //.animateLoad(Animation)
                        .load(location.logo.get(logoImageView.getWidth(), this).toString());
            }
        }
    }

    private void clearLocationDetails() {
        titleLabel.setText("");
        descriptionLabel.setText("");
        logoImageView.setImageDrawable(null);
        goButton.setVisibility(View.INVISIBLE);
    }

    private void clearMarkers() {
        mapView.removeAllMarkers();
    }

    private void startNavigation() {
        stopNavigation();
        navigationMode = true;
        selectOriginTextView.setVisibility(View.VISIBLE);
    }

    private void stopNavigation() {
        selectOriginTextView.setVisibility(View.INVISIBLE);
        mapView.removeAllPaths();
        navigationMode = false;
        path = null;
    }

    private void showLocations() {
        for (Location location : activeVenue.getLocations()) {

            List<Coordinate> coords = location.getNavigatableCoordinates();
            if (coords.size() > 0) {
                Overlay2DLabel label = new Overlay2DLabel(location.getName(), 36, Typeface.DEFAULT);
                label.setPosition(coords.get(0));
                LocationLabelClicker clicker = new LocationLabelClicker();
                clicker.location = location;
                overlays.put(label, clicker);
                mapView.addMarker(label);
            }
        }
    }

    private class LocationLabelClicker {
        public Location location = null;
        public void click() {
            didTapNothing();
            Coordinate start = activeVenue.getLocations()[5].getNavigatableCoordinates().get(0);
            Directions directions = location.directionsFrom(activeVenue, start, null, null);
            if (directions != null) {
                path = new Path(directions.getPath(), 5f, 5f, 0x4ca1fc);
                mapView.addPath(path);
                mapView.getCamera().focusOn(directions.getPath());
            }
        };
    }
}