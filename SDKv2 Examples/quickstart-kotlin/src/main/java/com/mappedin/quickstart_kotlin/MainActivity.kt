package com.mappedin.quickstart_kotlin

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mappedin.Mappedin
import com.mappedin.MiGestureType
import com.mappedin.MiMapViewListener
import com.mappedin.enums.MiMapStatus
import com.mappedin.interfaces.VenueCallback
import com.mappedin.models.MiLevel
import com.mappedin.models.MiOverlay
import com.mappedin.models.MiSpace
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initialize the Mappedin singleton with the application and credentials
        Mappedin.init(application) //Mapbox token is optional

        Mappedin.setCredentials("5f4e59bb91b055001a68e9d9", "gmwQbwuNv7cvDYggcYl4cMa5c7n0vh4vqNQEkoyLRuJ4vU42")

        setContentView(R.layout.activity_main)

        Mappedin.getVenue("mappedin-demo-mall", VenueCallback { miVenue ->
            mapView.loadMap(miVenue, { miMapStatus ->
                if (miMapStatus == MiMapStatus.LOADED) {
                    Log.i("MiMapView", "Map has loaded")
                } else {
                    Log.e("MiMapView", "Map failed to load")
                }
            })
        })

        //Set an MiMapViewListener to run custom code on certain map events
        mapView.setListener(object : MiMapViewListener {
            override fun onTapNothing() {
                //Called when a point on the map is tapped that isn't a MiSpace or MiOverlay
            }

            override fun didTapSpace(miSpace: MiSpace?): Boolean {
                //Called when an MiSpace is tapped, return false to be called again if multiple MiSpaces were tapped
                return false
            }

            override fun onTapCoordinates(latLng: LatLng) {
                //Called when any point is tapped on the map with the LatLng coordinates
            }

            override fun didTapOverlay(miOverlay: MiOverlay): Boolean {
                //Called when an MiOverlay is tapped, return false to be called again if multiple MiOverlays were tapped
                return false
            }

            override fun onLevelChange(miLevel: MiLevel) {
                //Called when the level changes
            }

            override fun onManipulateCamera(miGestureType: MiGestureType) {
                //Called when the user pinches or pans the map
            }
        })
    }
}
