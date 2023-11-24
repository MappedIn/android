package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapClickListener
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.MPIBlueDotPositionUpdate
import com.mappedin.sdk.models.MPIBlueDotStateChange
import com.mappedin.sdk.models.MPIData
import com.mappedin.sdk.models.MPIMap
import com.mappedin.sdk.models.MPIMapClickEvent
import com.mappedin.sdk.models.MPINavigatable
import com.mappedin.sdk.models.MPIState
import com.mappedin.sdk.web.MPIOptions

class Tooltips : AppCompatActivity(), MPIMapViewListener, MPIMapClickListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private val toolTipIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Tooltips"

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
            showVenueOptions = MPIOptions.ShowVenue(labelAllLocationsOnInit = false),
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
            mapView.createTooltip(
                node = mapClickEvent.polygons.first().entrances[0],
                """<div tabindex="0" style="padding: 10px; background-color: gray;">${mapClickEvent.polygons.first().locations[0].name}</div>""",
                MPIOptions.Tooltip(
                    collisionRank = MPIOptions.COLLISION_RANK.MEDIUM,
                ),
            ) {
                    id ->
                if (id != null) {
                    toolTipIds.add(id)
                }
            }
        } else {
            toolTipIds.forEach {
                mapView.removeTooltip(it)
            }
            toolTipIds.clear()
        }
    }

    @Deprecated("Use MPIMapClickListener instead.")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
    }

    override fun onStateChanged(state: MPIState) {
    }
}
