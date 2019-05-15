package mappedin.com.wayfindingsample;

import android.os.Parcelable;
import android.os.Parcel;

public class VenueParcelable implements Parcelable {
    private int venueIndex;

    /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(venueIndex);
    }


    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<VenueParcelable> CREATOR = new Parcelable.Creator<VenueParcelable>() {
        public VenueParcelable createFromParcel(Parcel in) {
            return new VenueParcelable(in);
        }

        public VenueParcelable[] newArray(int size) {
            return new VenueParcelable[size];
        }
    };

    public VenueParcelable(int index) {
        venueIndex = index;
    }

    // example constructor that takes a Parcel and gives you an object populated with it's values
    protected VenueParcelable(Parcel in) {
        venueIndex = in.readInt();
    }
}