package mappedin.com.wayfindingsample;

import com.mappedin.sdk.ImageSet;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.Utils;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;

public class Elevator extends Location {
    String id;
    String externalId;
    String description;
    ImageSet logo;

    public Elevator(ByteBuffer data, int _index, Venue venue) {
        super(data, _index, venue);
        id = Utils.encodingString(data);
        externalId = Utils.encodingString(data);
        description = Utils.encodingString(data);
        logo = Utils.encodingImageSet(data);
    }
}
