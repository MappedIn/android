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
    private MappedinCallback<Venue> getVenueCallback;

    private LocationGenerator[] customerLocations;
    // Only use for keys that have these location types in the binary builder
    private LocationGenerator[] locationGenerators1;

    private Venue venue;

    VenuesCallback(MappedIn mappedIn, MappedinCallback<Venue> getVenueCallback) {
        // Get Venue Data
        LocationGenerator customerLocation = new LocationGenerator() {
            @Override
            public Location locationGenerator(ByteBuffer data, int index, Venue venue) {
                return new CustomerLocation(data, index, venue);
            }
        };
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
        LocationGenerator elevator = new LocationGenerator() {
            @Override
            public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                return new Elevator(data, _index, venue);
            }
        };
        LocationGenerator escalatorStairs = new LocationGenerator() {
            @Override
            public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                return new EscalatorStairs(data, _index, venue);
            }
        };

        this.customerLocations = new LocationGenerator[]{customerLocation};
        this.locationGenerators1 = new LocationGenerator[]{tenant, amenity, elevator, escalatorStairs};

        this.mappedIn = mappedIn;
        this.getVenueCallback = getVenueCallback;
    }

    public void setActiveVenue(Venue venue) {
        this.venue = venue;
    }

    /**
     * Function that will be called when the Mappedin API call has finished successfully
     *
     * @param venues Data returned for the Mappedin API call
     */
    @Override
    public void onCompleted(List<Venue> venues) {
        if (venues != null && venues.size() > 0 && this.venue != null) {
            // loads a set active venue
            mappedIn.getVenue(this.venue, this.customerLocations, this.getVenueCallback);
        }
        else if (venues != null && venues.size() > 0) {
            // loads the first venue your keys have access to
            mappedIn.getVenue(venues.get(0), this.customerLocations, this.getVenueCallback);
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
