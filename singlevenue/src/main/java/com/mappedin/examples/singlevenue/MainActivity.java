package com.mappedin.examples.singlevenue;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.mappedin.jpct.Logger;
import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Directions;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.MapViewDelegate;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.Overlay;
import com.mappedin.sdk.Overlay2DLabel;
import com.mappedin.sdk.Path;
import com.mappedin.sdk.Polygon;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
    private ImageView compass = null;

//    private HashMap<Polygon, Integer> originalColors = new HashMap<Polygon, Integer>();
    private ArrayList<Polygon> originalColors = new ArrayList<>();
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

        mappedIn = new MappedIn(getApplication());

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
        compass = (ImageView) findViewById(R.id.compass_image);
        compass.bringToFront();
        compass.setImageDrawable(getResources().getDrawable(R.drawable.compass));

        showLocationsButton = (Button) findViewById(R.id.showLocationButton);
        showLocationsButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showLocations();}});

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
            LocationGenerator amenity = new LocationGenerator() {
                @Override
                public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                    return new Amenity(data, _index, venue);
                }
            };
            LocationGenerator tenant = new LocationGenerator() {
                @Override
                public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                    return new Tenant(data, _index, venue);
                }
            };
            final LocationGenerator[] locationGenerators = {tenant, amenity};
            mappedIn.getVenue(activeVenue, accessibleDirections, locationGenerators, new GetVenueCallback());
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
            activeVenue = venue;
            Map[] maps = venue.getMaps();
            if (maps.length == 0) {
                Logger.log("No maps! Make sure your venue is set up correctly!");
                return;
            }

            Arrays.sort(maps, new Comparator<Map>() {
                @Override
                public int compare(Map a, Map b) {
                    return (int) (a.getFloor() - b.getFloor());
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

    public boolean didTapPolygon(Polygon polygon) {
        if (navigationMode) {
            if (path != null) {
                didTapNothing();
                return true;
            }
            if (polygon.getLocations().length == 0) {
                return false;
            }

            Directions directions = destinationPolygon.directionsFrom(activeVenue, polygon, destinationPolygon.getLocations()[0], polygon.getLocations()[0]);
            if (directions != null) {
                path = new Path(directions.getPath(), 1f, 3f, 0x4ca1fc, 1);
                mapView.addElement(path);
                mapView.setMap(directions.getPath()[0].getMap());
                mapView.frame(directions.getPath(), 0, 0.246f, 0.5f);
            }

            highlightPolygon(polygon, 0x007afb);
            highlightPolygon(destinationPolygon, 0xff834c);
            runOnUiThread(new Runnable() {
                public void run() {
                    selectOriginTextView.setVisibility(View.INVISIBLE);
                }
            });
            return true;
        }
        clearHighlightedColours();
        if (polygon.getLocations().length == 0) {
            return false;
        }
        destinationPolygon = polygon;
        highlightPolygon(polygon, 0x4ca1fc);

        showLocationDetails(destinationPolygon.getLocations()[0]);
        return true;
    }

    public boolean didTapOverlay(Overlay overlay) {
        LocationLabelClicker clicker = overlays.get(overlay);
        if (clicker != null) {
            clicker.click();
            return true;
        } else {
            Logger.log("No click");
            return false;
        }
    }

    public void didTapNothing() {
        clearHighlightedColours();
        clearLocationDetails();
        stopNavigation();
        clearMarkers();

    }

    @Override
    public void onCameraBearingChange(final double bearing) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                compass.setRotation((float)(bearing/Math.PI*180));
            }
        });
    }

    @Override
    public void manipulatedCamera() {

    }

    private void highlightPolygon(Polygon polygon, int color) {
        if (!originalColors.contains(polygon)) {
            originalColors.add(polygon);
        }
        mapView.setColor(polygon, color, 1);
    }

    private void clearHighlightedColours() {
        for  (Polygon polygon: originalColors) {
            mapView.resetColor(polygon);
        }
        originalColors.clear();
    }

    private void showLocationDetails(Location location) {
        clearLocationDetails();
        titleLabel.setText(location.getName());
        goButton.setVisibility(View.VISIBLE);

        // This sample is using the Ion framework for easy image loading/cacheing. You can use what you like
        // https://github.com/koush/ion
        if (location.getClass() == Tenant.class) {
            Tenant tenant = (Tenant)location;
            descriptionLabel.setText(tenant.description);
            if (tenant.logo != null) {
                String url = tenant.logo.getImage(logoImageView.getWidth()).toString();
                if (url != null) {
                    Ion.with(logoImageView)
                            //.placeholder(R.drawable.placeholder_image)
                            //.error(R.drawable.error_image)
                            //.animateLoad(Animation)
                            .load(tenant.logo.getImage(logoImageView.getWidth()).toString());
                }
            }
        } else if (location.getClass() == Amenity.class) {
            Amenity amenity = (Amenity)location;
            descriptionLabel.setText(amenity.description);
            if (amenity.logo != null) {
                String url = amenity.logo.getImage(logoImageView.getWidth()).toString();
                if (url != null) {
                    Ion.with(logoImageView)
                            //.placeholder(R.drawable.placeholder_image)
                            //.error(R.drawable.error_image)
                            //.animateLoad(Animation)
                            .load(amenity.logo.getImage(logoImageView.getWidth()).toString());
                }
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
        mapView.removeAllElements();
    }

    private void startNavigation() {
        stopNavigation();
        navigationMode = true;
        selectOriginTextView.setVisibility(View.VISIBLE);
    }

    private void stopNavigation() {
        selectOriginTextView.setVisibility(View.INVISIBLE);
        mapView.removeAllElements();
        navigationMode = false;
        path = null;
    }

    private void showLocations() {
        for (Location location : activeVenue.getLocations()) {
            Coordinate[] coords = location.getNavigatableCoordinates();
            if (coords.length > 0) {
                Overlay2DLabel label = new Overlay2DLabel(location.getName(), 36, Typeface.DEFAULT);
                label.setPosition(coords[0]);
                LocationLabelClicker clicker = new LocationLabelClicker();
                clicker.location = location;
                overlays.put(label, clicker);
                mapView.addElement(label);
            }
        }
    }

    private class LocationLabelClicker {
        public Location location = null;
        public void click() {
            didTapNothing();
            showLocationDetails(location);
            if (location != null){
                Polygon[] polygons = location.getPolygons();
                if (polygons.length>0){
                    clearHighlightedColours();
                    destinationPolygon = polygons[0];
                    highlightPolygon(destinationPolygon, 0x4ca1fc);
                    showLocationDetails(location);
                }
            }
        }
    }
}