package mappedin.com.wayfindingsample;

import android.app.Application;


import com.mappedin.sdk.Venue;

/**
 * Created by Peter on 2016-03-24.
 */
public class ApplicationSingleton extends Application {
    private Venue activeVenue;

    public Venue getActiveVenue() {
        return activeVenue;
    }

    public void setActiveVenue(Venue activeVenue) {
        this.activeVenue = activeVenue;
    }
}
