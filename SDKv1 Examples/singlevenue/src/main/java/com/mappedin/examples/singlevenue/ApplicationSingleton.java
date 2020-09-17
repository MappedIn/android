package com.mappedin.examples.singlevenue;

import android.app.Application;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.Venue;

public class ApplicationSingleton extends Application {
    private Venue activeVenue;
    private Location activeLocation;

    public Venue getActiveVenue() {
        return activeVenue;
    }

    public void setActiveVenue(Venue activeVenue) {
        this.activeVenue = activeVenue;
    }

    public Location getActiveLocation() {
        return activeLocation;
    }

    public void setActiveLocation(Location activeLocation) {
        this.activeLocation = activeLocation;
    }
}
