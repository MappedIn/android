package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapClickListener
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class Markers : AppCompatActivity(), MPIMapViewListener, MPIMapClickListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private val markerIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Markers"

        progressBar = findViewById(R.id.loadingIndicator)
        progressBar.bringToFront()

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall",
            ),
            MPIOptions.ShowVenue(
                labelAllLocationsOnInit = false,
                shadingAndOutlines = true,
                multiBufferRendering = true,
                outdoorView = MPIOptions.OutdoorView(enabled = true),
            ),
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
        mapView.mapClickListener = this
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
    }

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE

        mapView.flatLabelsManager.labelAllLocations(MPIOptions.FlatLabelAllLocations())
    }

    override fun onMapChanged(map: MPIMap) {
    }

    @Deprecated("Use MPIMapClickListener instead.")
    override fun onNothingClicked() {
    }

    override fun onClick(mapClickEvent: MPIMapClickEvent) {
        if (!mapClickEvent.polygons.isEmpty()) {
            mapView.markerManager.add(
                node = mapClickEvent.polygons.first().entrances[0],
                contentHtml = """
                    <div style="background-color:white; border: 2px solid black; padding: 0.4rem; border-radius: 0.4rem;">
                    ${mapClickEvent.polygons.first().locations[0].name}
                    </div>
            """,
                markerOptions = MPIOptions.Marker(rank = MPIOptions.COLLISION_RANK.MEDIUM, anchor = MPIOptions.MARKER_ANCHOR.CENTER),
            ) {
                if (it != null) {
                    markerIds.add(it)
                }
            }
        } else {
            markerIds.forEach {
                mapView.markerManager.remove(it)
            }
            markerIds.clear()
        }
    }

    @Deprecated("Use MPIMapClickListener instead.")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
    }

    override fun onStateChanged(state: MPIState) {
    }
}
