package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class Markers : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private val markerIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Markers"

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall",
            ),
            showVenueOptions = MPIOptions.ShowVenue(labelAllLocationsOnInit = false),
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
    }

    override fun onFirstMapLoaded() {
        mapView.flatLabelsManager.labelAllLocations(MPIOptions.FlatLabelAllLocations())
    }

    override fun onMapChanged(map: MPIMap) {
    }

    override fun onNothingClicked() {
        markerIds.forEach {
            mapView.removeMarker(it)
        }
        markerIds.clear()
    }

    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
        val markerId = mapView.createMarker(
            node = polygon.entrances[0],
            contentHtml = """
                    <div style="background-color:white; border: 2px solid black; padding: 0.4rem; border-radius: 0.4rem;">
                    ${polygon.locations[0].name}
                    </div>
            """,
            options = MPIOptions.Marker(rank = 4.0, anchor = MPIOptions.MARKER_ANCHOR.CENTER),
        )
        markerId.let {
            markerIds.add(it)
        }
    }

    override fun onStateChanged(state: MPIState) {
    }
}
