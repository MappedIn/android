package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class CameraControls : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private var defaultTilt: Double? = null
    private var defaultZoom: Double? = null
    private var defaultRotation: Double? = null
    private var defaultPosition: MPIMap.MPICoordinate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Blue Dot"

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall"
            )
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
        TODO("Not yet implemented")
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
        TODO("Not yet implemented")
    }

    override fun onDataLoaded(data: MPIData) {
        TODO("Not yet implemented")
    }

    override fun onFirstMapLoaded() {
        defaultTilt = mapView.cameraManager.tilt
        defaultZoom = mapView.cameraManager.zoom
        defaultRotation = mapView.cameraManager.rotation
        defaultPosition = mapView.cameraManager.position
    }

    override fun onMapChanged(map: MPIMap) {
        TODO("Not yet implemented")
    }

    override fun onNothingClicked() {
        TODO("Not yet implemented")
    }

    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
        TODO("Not yet implemented")
    }

    override fun onStateChanged(state: MPIState) {
        TODO("Not yet implemented")
    }
}
