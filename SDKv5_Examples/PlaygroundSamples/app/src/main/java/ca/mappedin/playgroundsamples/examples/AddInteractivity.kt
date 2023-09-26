package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapClickListener
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class AddInteractivity : AppCompatActivity(), MPIMapClickListener {
    private lateinit var mapView: MPIMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Add Interactivity"

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall",
            ),
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.mapClickListener = this
    }

    override fun onClick(mapClickEvent: MPIMapClickEvent) {
        if (!mapClickEvent.polygons.isEmpty()) {
            val polygon = mapClickEvent.polygons.first()
            mapView.setPolygonColor(polygon, "#BF4320")
        }
    }
}
