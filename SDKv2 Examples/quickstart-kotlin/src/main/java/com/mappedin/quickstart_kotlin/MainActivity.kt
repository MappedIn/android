package com.mappedin.quickstart_kotlin

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mappedin.Mappedin
import com.mappedin.enums.MiMapStatus
import com.mappedin.interfaces.VenueCallback
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
    }
}
