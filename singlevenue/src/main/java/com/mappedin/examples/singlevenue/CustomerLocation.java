package com.mappedin.examples.singlevenue;

import com.mappedin.sdk.*;

import java.nio.ByteBuffer;

/**
 * Created by Peter on 2017-11-03.
 */

class CustomerLocation extends Location {
    String externalId;
    public CustomerLocation(ByteBuffer data, int index, Venue venue) {
        super(data, index, venue);
        externalId = com.mappedin.sdk.Utils.encodingString(data);
    }
}
