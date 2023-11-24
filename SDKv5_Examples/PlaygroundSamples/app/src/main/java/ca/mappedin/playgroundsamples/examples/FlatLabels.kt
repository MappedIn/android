package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.MPIBlueDotPositionUpdate
import com.mappedin.sdk.models.MPIBlueDotStateChange
import com.mappedin.sdk.models.MPIData
import com.mappedin.sdk.models.MPIMap
import com.mappedin.sdk.models.MPINavigatable
import com.mappedin.sdk.models.MPIState
import com.mappedin.sdk.web.MPIOptions

class FlatLabels : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private var styleNames = arrayOf("Default", "Small Red", "Medium Blue", "Large Purple")
    private var fontSizes = arrayOf(12f, 4f, 8f, 16f) // The font sizes used for flat labels.
    private var textColours = arrayOf("#000000", "#e31a0b", "#0a0dbf", "#7c08d4") // The colours used for flat labels.
    private val TAG = "FlatLabels"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Flat Labels"

        progressBar = findViewById(R.id.splitLoadingIndicator)
        progressBar.bringToFront()

        mapView = findViewById(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall",
            ),
            MPIOptions.ShowVenue(labelAllLocationsOnInit = false), // Disable Floating Labels from loading on start.
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this

        val controlLayout = findViewById<LinearLayout>(R.id.controlsLinearLayout)

        val spinnerLabel = TextView(this)
        spinnerLabel.setPadding(12, 16, 12, 16)
        spinnerLabel.setText("Select a Theme", TextView.BufferType.NORMAL)
        controlLayout.addView(spinnerLabel)

        val labelStyleSpinner = Spinner(this)
        labelStyleSpinner.setPadding(12, 16, 12, 16)
        controlLayout.addView(labelStyleSpinner)
        styleNames.let { labelStyleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, it) }

        labelStyleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        Log.d(TAG, "Default Style Selected")
                        // Remove all flat labels and re-add with the default style.
                        mapView.flatLabelsManager.removeAll()
                        mapView.flatLabelsManager.labelAllLocations(null)
                    }
                    in 1..3 -> {
                        Log.d(TAG, styleNames[position] + " Style Selected")
                        // Remove all flat labels and re-add with the chosen style.
                        val flatLabelAppearance: MPIOptions.FlatLabelAppearance = MPIOptions.FlatLabelAppearance(
                            fontSize = fontSizes[position],
                            color = textColours[position],
                        )
                        mapView.flatLabelsManager.removeAll()

                        val flatLabelLocations: MPIOptions.FlatLabelAllLocations = MPIOptions.FlatLabelAllLocations(appearance = flatLabelAppearance)

                        mapView.flatLabelsManager.labelAllLocations(flatLabelLocations)
                    }
                    else -> {
                        Log.d(TAG, "Unknown Style Selected")
                    }
                }
            }
        }
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) { }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) { }

    override fun onDataLoaded(data: MPIData) { }

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE

        // Zoom in when the map loads to better show the flat labels.
        mapView.cameraManager.set(
            MPIOptions.CameraTransformCoordinate(
                zoom = 800.0,
                position = mapView.currentMap?.createCoordinate(
                    43.86181934825464,
                    -78.94672121994297,
                ),
            ),
        )
        // Enable all flat labels with the default style.
        mapView.flatLabelsManager.labelAllLocations(null)
    }
    override fun onMapChanged(map: MPIMap) { }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onNothingClicked() { }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) { }

    override fun onStateChanged(state: MPIState) { }
}
