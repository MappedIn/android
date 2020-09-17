package com.mappedin.quickstart;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mappedin.Mappedin;
import com.mappedin.MiMapView;
import com.mappedin.enums.MiMapStatus;
import com.mappedin.interfaces.MiMapViewCallback;
import com.mappedin.interfaces.VenueCallback;
import com.mappedin.models.MiVenue;

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

    }
}
