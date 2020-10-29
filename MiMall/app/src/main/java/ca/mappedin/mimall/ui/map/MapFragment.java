package ca.mappedin.mimall.ui.map;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mappedin.Mappedin;
import com.mappedin.MiGestureType;
import com.mappedin.MiMapView;
import com.mappedin.MiMapViewListener;
import com.mappedin.enums.MiMapStatus;
import com.mappedin.interfaces.MiMapViewCallback;
import com.mappedin.interfaces.VenueCallback;
import com.mappedin.models.MiLevel;
import com.mappedin.models.MiOverlay;
import com.mappedin.models.MiSpace;
import com.mappedin.models.MiVenue;

import org.jetbrains.annotations.NotNull;

import ca.mappedin.mimall.R;
import ca.mappedin.mimall.shared.Repository;

public class MapFragment extends Fragment {

    private ImageView locationLogo;
    private TextView locationName;
    private TextView locationLevel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_map, container, false);

        locationLogo = root.findViewById(R.id.locationLogo);
        locationName = root.findViewById(R.id.locationName);
        locationLevel = root.findViewById(R.id.locationLevel);

        final ProgressBar progressBar = root.findViewById(R.id.loadingProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        final MiMapView mapView = root.findViewById(R.id.mapView);
        final AutoCompleteTextView searchBar = root.findViewById(R.id.searchBar);

        ImageView searchButton = root.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchKeyword = searchBar.getText().toString().trim();
                if (searchKeyword.length() > 0) {
                    mapView.focusOn(Repository.getInstance().searchLocations(searchKeyword), 300);
//                    locationName.setText();
                }
            }
        });

        Mappedin.getVenue("mappedin-demo-mall", new VenueCallback() {
            @Override
            public void onVenueLoaded(final MiVenue miVenue) {
                mapView.loadMap(miVenue, new MiMapViewCallback() {
                    @Override
                    public void onMapLoaded(MiMapStatus miMapStatus) {
                        if (miMapStatus == MiMapStatus.LOADED) {
                            progressBar.setVisibility(View.GONE);

                            Log.i("MiMapView", "Map has loaded. Levels: " + miVenue.getLevels().size());

                            mapView.displayLocationLabels();
//                            mapView.setLabelProperties(PropertyFactory.textColor(Color.RED));
                            Repository.getInstance().setLocations(miVenue.getLocations());

                            ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(MapFragment.this.getContext(),
                                    android.R.layout.simple_dropdown_item_1line, Repository.getInstance().getLocationsArray());
                            searchBar.setAdapter(searchAdapter);

                        } else {
                            Log.e("MiMapView", "Map failed to load");
                        }
                    }
                });
            }
        });

        //Set an MiMapViewListener to run custom code on certain map events
        mapView.setListener(new MiMapViewListener() {
            @Override
            public void onTapNothing() {
                //Called when a point on the map is tapped that isn't a MiSpace or MiOverlay
            }

            @Override
            public boolean didTapSpace(MiSpace miSpace) {
                //Called when an MiSpace is tapped, return false to be called again if multiple MiSpaces were tapped
                Log.d("LOG", miSpace.getLocations().get(0).getName());
                return false;
            }

            @Override
            public void onTapCoordinates(@NotNull LatLng latLng) {
                //Called when any point is tapped on the map with the LatLng coordinates
            }

            @Override
            public boolean didTapOverlay(@NotNull MiOverlay miOverlay) {
                //Called when an MiOverlay is tapped, return false to be called again if multiple MiOverlays were tapped
                return false;
            }

            @Override
            public void onLevelChange(@NotNull MiLevel miLevel) {
                //Called when the level changes
            }

            @Override
            public void onManipulateCamera(@NotNull MiGestureType miGestureType) {
                //Called when the user pinches or pans the map
            }
        });


        return root;
    }
}