package com.mappedin.quickstart;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mappedin.Mappedin;
import com.mappedin.MiGestureType;
import com.mappedin.MiMapView;
import com.mappedin.MiMapViewListener;
import com.mappedin.enums.MiMapStatus;
import com.mappedin.interfaces.MiMapViewCallback;
import com.mappedin.interfaces.VenueCallback;
import com.mappedin.models.MiLevel;
import com.mappedin.models.MiOverlay;
import com.mappedin.models.MiSpace;
import com.mappedin.models.MiVenue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize the Mappedin singleton with the application and credentials
        Mappedin.init(getApplication()); //Mapbox token is optional
        Mappedin.setCredentials("5f4e59bb91b055001a68e9d9", "gmwQbwuNv7cvDYggcYl4cMa5c7n0vh4vqNQEkoyLRuJ4vU42");

        setContentView(R.layout.activity_main);

        final MiMapView mapView = findViewById(R.id.mapView);

        Mappedin.getVenue("mappedin-demo-mall", new VenueCallback() {
            @Override
            public void onVenueLoaded(MiVenue miVenue) {
                mapView.loadMap(miVenue, new MiMapViewCallback() {
                    @Override
                    public void onMapLoaded(MiMapStatus miMapStatus) {
                        if (miMapStatus == MiMapStatus.LOADED) {
                            Log.i("MiMapView", "Map has loaded");
                        } else {
                            Log.e("MiMapView", "Map failed to load");
                        }
                    }
                });
            }
        });

        //Set an MiMapViewListener to run custom code on certain map events
        mapView.setListener(new MiMapViewListener() {
            @Override
            public void onTapNothing() {
                //Called when a point on the map is tapped that isn't a MiSpace or MiOverlay
            }

            @Override
            public boolean didTapSpace(@Nullable MiSpace miSpace) {
                //Called when an MiSpace is tapped, return false to be called again if multiple MiSpaces were tapped
                return false;
            }

            @Override
            public void onTapCoordinates(@NotNull LatLng latLng) {
                //Called when any point is tapped on the map with the LatLng coordinates
            }

            @Override
            public boolean didTapOverlay(@NotNull MiOverlay miOverlay) {
                //Called when an MiOverlay is tapped, return false to be called again if multiple MiOverlays were tapped
                return false;
            }

            @Override
            public void onLevelChange(@NotNull MiLevel miLevel) {
                //Called when the level changes
            }

            @Override
            public void onManipulateCamera(@NotNull MiGestureType miGestureType) {
                //Called when the user pinches or pans the map
            }
        });

    }
}
