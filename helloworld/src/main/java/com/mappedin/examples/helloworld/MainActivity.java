package com.mappedin.examples.helloworld;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

import com.mappedin.source.mappedin_model.MapView;
import com.mappedin.source.mappedin_model.MappedinCallback;
import com.mappedin.source.mappedin_model.MappedIn;
import com.mappedin.source.mappedin_model.Venue;
import com.mappedin.source.jpct.Logger;

public class MainActivity extends AppCompatActivity {

    private MappedIn mappedIn = null;
    private Venue activeVenue = null;
    private MapView mapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mappedIn = new MappedIn(getApplicationContext());
        mappedIn.getVenues(new MappedinCallback<List<Venue>>() {
            @Override
            public void onCompleted(final List<Venue> venues) {
                if (venues.size() == 0 ) {
                    Logger.log("No venues available! Are you using the right credentials? Talk to your mappedin representative.");
                    return;
                }
                 mappedIn.getVenue(venues.get(0), false, null, new MappedinCallback<Venue>() {
                    @Override
                    public void onCompleted(Venue venue) {
                        Logger.log(venue.getName() + " loaded!");
                        mapView = (MapView) getFragmentManager().findFragmentById(R.id.mapFragment);
                        mapView.setMap(venue.getMaps().get(0));
                    }

                    @Override
                    public void onError() {
                        Logger.log("Error loading venue.");
                    }
                });
            }

            @Override
            public void onError() {
                Logger.log("Error loading any venues. Did you set your credentials?");
            }
        });
    }
}
