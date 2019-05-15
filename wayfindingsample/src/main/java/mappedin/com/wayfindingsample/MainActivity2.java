package mappedin.com.wayfindingsample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.Venue;
import com.mappedin.jpct.Logger;

import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity2 extends AppCompatActivity{

    private VenueListAdapter venueListAdapter;
    private ListView venueList;

    private MappedIn mappedIn = null;
    private Venue activeVenue = null;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        mappedIn = new MappedIn(getApplication());
        context = this;
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
        //Only use for keys that have these location types in the binary builder
        final LocationGenerator[] locationGenerators1 = {tenant, amenity, elevator, escalatorStairs};

        LocationGenerator customerLocation = new LocationGenerator() {
            @Override
            public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                return new CustomerLocation(data, _index, venue);
            }
        };
        final LocationGenerator[] locationGenerators2 = {customerLocation};

        mappedIn.getVenues(new MappedinCallback<List<Venue>>() {
            @Override
            public void onCompleted(final List<Venue> venues) {
                venueList = (ListView) findViewById(R.id.venue_list_view);
                venueListAdapter = new VenueListAdapter(context, venues);
                venueList.setAdapter(venueListAdapter);
                venueList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
//                        loadingUI();
                        activeVenue = venues.get(position);
                        mappedIn.getVenue(activeVenue, locationGenerators2, new MappedinCallback<Venue>() {
                            @Override
                            public void onCompleted(Venue venue) {
//                                showVenueUI(venue);
//                                ((ApplicationSingleton) getApplication()).setActiveVenue(venue);
                                Logger.log("onComplete went through!");
                                Intent i = new Intent(MainActivity2.this, MainActivity.class);
                                i.putExtra("venue_selected", position);
                                startActivity(i);
                            }

                            @Override
                            public void onError(Exception error) {

                            }
                        });
//                        drawer.closeDrawer(GravityCompat.START);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                Logger.log("get venues for mappedin failed");
            }
        });

    }
}
