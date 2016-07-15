package com.mappedin.examples.helloworld;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import java.util.List;

import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.Venue;
import com.mappedin.jpct.Logger;

public class MainActivity extends AppCompatActivity {

    private MappedIn mappedIn = null;
    private MapView mapView = null;

    private ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("Start");
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        mappedIn = new MappedIn(getApplicationContext());
        mappedIn.getVenues(new MappedinCallback<Venue[]>() {
            @Override
            public void onCompleted(final Venue[] venues) {
                Logger.log("Got venues");
                if (venues.length == 0) {
                    Logger.log("No venues! Did you set up your keys correctly?");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mappedIn.getVenue(venues[0], false, null, new MappedinCallback<Venue>() {
                    @Override
                    public void onCompleted(Venue venue) {
                        Logger.log(venue.getName() + " loaded!");
                        mapView = (MapView) getFragmentManager().findFragmentById(R.id.mapFragment);
                        Map[] maps = venue.getMaps();
                        if (maps.length == 0) {
                            Logger.log("No maps on venue! Talk to your mappedin representative to make sure your venue (" + venue.getName() + ") is set up correctly.");
                            progressBar.setVisibility(View.GONE);
                            return;
                        }
                        mapView.setMap(maps[0]);
                        progressBar.setVisibility(View.GONE);
                        Logger.log("Map loaded!");
                    }

                    @Override
                    public void onError(Exception e) {
                        Logger.log("Error loading venue: " + e);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Logger.log("Error loading any venues: " + e);
                Logger.log("Did you set your credentials?");
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
