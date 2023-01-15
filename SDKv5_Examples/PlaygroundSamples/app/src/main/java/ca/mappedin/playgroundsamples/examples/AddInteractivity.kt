package ca.mappedin.playgroundsamples.examples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class AddInteractivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Add interactivity"

        val mapView = findViewById<MPIMapView>(R.id.mapView)

        mapView.loadVenue(
            MPIOptions.Init("5eab30aa91b055001a68e996", "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall"),
            MPIOptions.ShowVenue(labelAllLocationsOnInit = true, backgroundColor = "white")
        ) { Log.e(javaClass.simpleName, "Error loading map view")}

        mapView.listener = object : MPIMapViewListener {
            override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
            }

            override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
            }

            override fun onDataLoaded(data: MPIData) {
            }

            override fun onFirstMapLoaded() {
            }

            override fun onMapChanged(map: MPIMap) {
            }

            override fun onNothingClicked() {
            }

            override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
                mapView.setPolygonColor(polygon, "#BF4320")
            }

            override fun onStateChanged(state: MPIState) {
            }
        }
    }


}