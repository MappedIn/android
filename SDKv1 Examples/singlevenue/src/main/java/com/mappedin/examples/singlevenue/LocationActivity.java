package com.mappedin.examples.singlevenue;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.Polygon;

public class LocationActivity extends AppCompatActivity {

    private ImageView locationLogo;
    private Button takeMeThere;
    private TextView descriptionLabel;
    private TextView loading = null;
    private MapView mapView;
    private Location location;
    private Polygon polygon;
    private SetMapCallback setMapCallback = new SetMapCallback();

    @Override
    protected void onResume(){
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (polygon != null){
                mapView.setColor(polygon, Color.BLUE, 1);
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        locationLogo = (ImageView)findViewById(R.id.location_logo);
        descriptionLabel = (TextView)findViewById(R.id.description_text_view);
        mapView = (MapView) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        loading = (TextView) findViewById(R.id.location_loading_textview);
        location = ((ApplicationSingleton) getApplication()).getActiveLocation();
        if (location != null){
            if (location.getClass() == Tenant.class) {
                Tenant tenant = (Tenant)location;
                descriptionLabel.setText(tenant.description);
                if (tenant.logo != null) {
                    String url = tenant.logo.getImage(512).toString();
                    if (url != null) {
                        Ion.with(locationLogo)
                                .load(tenant.logo.getImage(512).toString());
                    }
                }
            } else if (location.getClass() == Amenity.class) {
                Amenity amenity = (Amenity)location;
                descriptionLabel.setText(amenity.description);
                if (amenity.logo != null) {
                    String url = amenity.logo.getImage(512).toString();
                    if (url != null) {
                        Ion.with(locationLogo)
                                .load(amenity.logo.getImage(512).toString());
                    }
                }
            }

            Polygon[] polygons = location.getPolygons();
            if (polygons.length > 0){
                polygon = polygons[0];
                if (polygon != null){
                    Map map = polygon.getMap();
                    if (map != null){
                        showLoadingLogo();
                        mapView.setMap(map, setMapCallback);
                        mapView.setColor(polygon, Color.BLUE, 1);
                        mapView.setHeight(polygon, 0.1f, 1);
                        mapView.frame(polygon, 0, 0, 0);
                    }
                }
            }
        }
        takeMeThere = (Button)findViewById(R.id.take_me_there);
        takeMeThere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (polygon != null) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("polygon_index", polygon.getIndex());
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
                else {
                    Intent returnIntent = new Intent();
                    setResult(Activity.RESULT_CANCELED, returnIntent);
                    finish();
                }
            }
        });
    }

    void showLoadingLogo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading != null){
                    loading.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    void hideLoadingLogo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading != null){
                    loading.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    class SetMapCallback implements MappedinCallback<Map> {

        /**
         * Function that will be called when the Mappedin API call has finished successfully
         *
         * @param map Data returned for the Mappedin API call
         */
        @Override
        public void onCompleted(Map map) {
            hideLoadingLogo();
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
}
