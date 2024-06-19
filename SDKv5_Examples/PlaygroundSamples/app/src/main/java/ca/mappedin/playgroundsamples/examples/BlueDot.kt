package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.concurrent.schedule

class BlueDot : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private val positionsString: String by lazy { readFileContentFromAssets("blue-dot-positions.json").replace("\n", "") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Blue Dot"

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
                labelAllLocationsOnInit = true,
                shadingAndOutlines = true,
                multiBufferRendering = true,
                outdoorView = MPIOptions.OutdoorView(enabled = true),
            ),
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
        progressBar.visibility = ProgressBar.INVISIBLE

        mapView.blueDotManager.enable(options = MPIOptions.BlueDot(smoothing = false, showBearing = true))
        mapView.blueDotManager.setState(MPIState.FOLLOW)
        mapView.cameraManager.set(MPIOptions.CameraTransformCoordinate(zoom = 700.0))
        // Load positions from blue-dot-positions.json
        val positions = Json.decodeFromString<List<MPIPosition>>(positionsString)
        val timer = Timer("Position Updater", false)
        positions.forEachIndexed { index, position ->
            timer.schedule(3000L * index) {
                mapView.blueDotManager.updatePosition(position)
            }
        }
    }

    override fun onMapChanged(map: MPIMap) {
    }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onNothingClicked() {
    }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
    }

    override fun onStateChanged(state: MPIState) {
    }

    private fun readFileContentFromAssets(file: String): String {
        return application.assets.open(file).bufferedReader().use {
            it.readText()
        }
    }
}
