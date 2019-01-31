package mappedin.com.wayfindingsample;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by Peter on 2018-02-27.
 */

public class VenuesCallback implements MappedinCallback<List<Venue>> {

    private MappedIn mappedIn;
    private LocationGenerator[] customerLocations;
    private MappedinCallback<Venue> getVenueCallback;

    VenuesCallback(MappedIn mappedIn, MappedinCallback<Venue> getVenueCallback) {
        // Get Venue Data
        LocationGenerator customerLocation = new LocationGenerator() {
            @Override
            public Location locationGenerator(ByteBuffer data, int index, Venue venue) {
                return new CustomerLocation(data, index, venue);
            }
        };
        this.customerLocations = new LocationGenerator[]{customerLocation};
        this.mappedIn = mappedIn;
        this.getVenueCallback = getVenueCallback;
    }

    /**
     * Function that will be called when the Mappedin API call has finished successfully
     *
     * @param venues Data returned for the Mappedin API call
     */
    @Override
    public void onCompleted(List<Venue> venues) {
        if (venues != null && venues.size() > 0) {
            // Can be customized to load venue of your choice
            mappedIn.getVenue(venues.get(0), customerLocations, getVenueCallback);
        }
    }

    /**
     * Function that will be called if the Mappedin API call failed
     *
     * @param exception The error that occurred
     */
    @Override
    public void onError(Exception exception) {

    }
}
