package mappedin.com.wayfindingsample;

import com.mappedin.sdk.ImageSet;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.Utils;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;

class CustomerLocation extends Location {
    // These properties depend on what properties you  have requested
    // Access to on location objects
    String externalId;
    //String description;
    //ImageSet logo;
    public CustomerLocation(ByteBuffer data, int index, Venue venue) {
        super(data, index, venue);
        externalId = Utils.encodingString(data);
        //description = Utils.encodingString(data);
        //logo = Utils.encodingImageSet(data);
    }

}
