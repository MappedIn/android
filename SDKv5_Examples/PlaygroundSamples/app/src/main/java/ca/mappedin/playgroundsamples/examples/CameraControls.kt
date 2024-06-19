package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPICameraListener
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import kotlin.math.PI

class CameraControls : AppCompatActivity(), MPIMapViewListener, MPICameraListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private var defaultTilt: Double? = null
    private var defaultZoom: Double? = null
    private var defaultRotation: Double? = null
    private var defaultPosition: MPIMap.MPICoordinate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Camera Controls"

        progressBar = findViewById(R.id.splitLoadingIndicator)
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
                shadingAndOutlines = true,
                multiBufferRendering = true,
                outdoorView = MPIOptions.OutdoorView(enabled = true),
            ),
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this

        setupButtons()
    }

    private fun setupButtons() {
        val buttonsLinearLayout = findViewById<LinearLayout>(R.id.controlsLinearLayout)

        val tiltBtnLayout = LinearLayout(this)
        tiltBtnLayout.id = View.generateViewId()
        tiltBtnLayout.orientation = LinearLayout.HORIZONTAL
        tiltBtnLayout.gravity = Gravity.CENTER_HORIZONTAL
        buttonsLinearLayout?.addView(tiltBtnLayout)

        val plusTiltBtn = Button(this)
        plusTiltBtn.id = View.generateViewId()
        plusTiltBtn.text = "Increase Tilt"
        plusTiltBtn.setOnClickListener(
            View.OnClickListener {
                val currentTilt = mapView.cameraManager.tilt
                val delta = PI / 6.0
                mapView.cameraManager.set(MPIOptions.CameraTransformCoordinate(tilt = currentTilt + delta))
            },
        )
        tiltBtnLayout.addView(plusTiltBtn)

        val minusTiltBtn = Button(this)
        minusTiltBtn.id = View.generateViewId()
        minusTiltBtn.text = "Decrease Tilt"
        minusTiltBtn.setOnClickListener(
            View.OnClickListener {
                val currentTilt = mapView.cameraManager.tilt
                val delta = PI / 6.0
                mapView.cameraManager.set(MPIOptions.CameraTransformCoordinate(tilt = currentTilt - delta))
            },
        )
        tiltBtnLayout.addView(minusTiltBtn)

        val zoomBtnLayout = LinearLayout(this)
        zoomBtnLayout.id = View.generateViewId()
        zoomBtnLayout.orientation = LinearLayout.HORIZONTAL
        zoomBtnLayout.gravity = Gravity.CENTER_HORIZONTAL
        buttonsLinearLayout?.addView(zoomBtnLayout)

        val plusZoomBtn = Button(this)
        plusZoomBtn.id = View.generateViewId()
        plusZoomBtn.text = "Zoom In"
        plusZoomBtn.setOnClickListener(
            View.OnClickListener {
                val currentZoom = mapView.cameraManager.zoom
                val delta = 800.0
                mapView.cameraManager.set(MPIOptions.CameraTransformCoordinate(zoom = currentZoom - delta))
            },
        )
        zoomBtnLayout.addView(plusZoomBtn)

        val minusZoomBtn = Button(this)
        minusZoomBtn.id = View.generateViewId()
        minusZoomBtn.text = "Zoom Out"
        minusZoomBtn.setOnClickListener(
            View.OnClickListener {
                val currentZoom = mapView.cameraManager.zoom
                val delta = 800.0
                mapView.cameraManager.set(MPIOptions.CameraTransformCoordinate(zoom = currentZoom + delta))
            },
        )
        zoomBtnLayout.addView(minusZoomBtn)

        val resetBtnLayout = LinearLayout(this)
        resetBtnLayout.id = View.generateViewId()
        resetBtnLayout.orientation = LinearLayout.HORIZONTAL
        resetBtnLayout.gravity = Gravity.CENTER_HORIZONTAL
        buttonsLinearLayout?.addView(resetBtnLayout)

        val aniateBtn = Button(this)
        aniateBtn.id = View.generateViewId()
        aniateBtn.text = "Animate"
        aniateBtn.setOnClickListener(
            View.OnClickListener {
                val zoomTarget = mapView.venueData?.locations?.first { it.name == "Sunglass Hut" }
                val cameraTransform =
                    MPIOptions.CameraTransformNode(
                        zoom = 50.0,
                        tilt = 2.0,
                        rotation = 180.0,
                        position = zoomTarget?.nodes?.first(),
                    )
                val cameraAnimation = MPIOptions.CameraAnimation(duration = 3000.0, easing = MPIOptions.EASING_MODE.EASE_IN)
                mapView.cameraManager.animate(cameraTransform, cameraAnimation)
            },
        )
        resetBtnLayout.addView(aniateBtn)

        val resetBtn = Button(this)
        resetBtn.id = View.generateViewId()
        resetBtn.text = "Reset"
        resetBtn.setOnClickListener(
            View.OnClickListener {
                mapView.cameraManager.set(
                    MPIOptions.CameraTransformCoordinate(
                        zoom = defaultZoom,
                        tilt = defaultTilt,
                        rotation = defaultRotation,
                        position = defaultPosition,
                    ),
                )
            },
        )
        resetBtnLayout.addView(resetBtn)
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
    }

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE

        defaultTilt = mapView.cameraManager.tilt
        defaultZoom = mapView.cameraManager.zoom
        defaultRotation = mapView.cameraManager.rotation
        defaultPosition = mapView.cameraManager.position
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

    override fun onCameraChanged(cameraTransform: MPICameraTransform) {
        Log.d("Position", cameraTransform.position.toString())
        Log.d("Rotation", cameraTransform.rotation.toString())
        Log.d("Tilt", cameraTransform.tilt.toString())
        Log.d("Zoom", cameraTransform.zoom.toString())
    }
}
