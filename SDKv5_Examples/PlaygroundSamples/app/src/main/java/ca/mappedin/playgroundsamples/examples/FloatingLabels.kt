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

class FloatingLabels : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private var styleNames = arrayOf("Default", "Custom Colours", "SVG Icons", "Light on Dark", "Dark on Light")
    private val TAG = "FloatingLabels"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Floating Labels"

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

            // Change the Floating Label theme based on the user's selection.
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        Log.d(TAG, "Default Style Selected")
                        // Remove all floating labels and re add using the default style.
                        mapView.floatingLabelsManager.removeAll()
                        mapView.floatingLabelsManager.labelAllLocations(null)
                    }
                    1 -> {
                        Log.d(TAG, "Custom Colours Text Style Selected")
                        // Remove all floating labels and re-add with custom text color. Color is in RGB format.
                        val textAppearance: MPIOptions.FloatingLabelAppearance.Text = MPIOptions.FloatingLabelAppearance.Text(
                            numLines = 2,
                            foregroundColor = "#DAA520",
                            backgroundColor = "#000000",
                        )
                        val coloredTextTheme: MPIOptions.FloatingLabelAppearance = MPIOptions.FloatingLabelAppearance(text = textAppearance)
                        val themeOptions: MPIOptions.FloatingLabelAllLocations = MPIOptions.FloatingLabelAllLocations(coloredTextTheme)
                        mapView.floatingLabelsManager.removeAll()
                        mapView.floatingLabelsManager.labelAllLocations(themeOptions)
                    }
                    2 -> {
                        Log.d(TAG, "SVG Icons Style Selected")
                        // Remove all floating labels and re-add with a custom SVG icon.
                        // Note that SVG must be instantiated using triple " to avoid characters being double escaped.
                        val svgIcon: String = """
                            <svg width="92" height="92" viewBox="-17 0 92 92" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <g clip-path="url(#clip0)">
                            <path d="M53.99 28.0973H44.3274C41.8873 28.0973 40.7161 29.1789 40.7161 31.5387V61.1837L21.0491 30.7029C19.6827 28.5889 18.8042 28.1956 16.0714 28.0973H6.5551C4.01742 28.0973 2.84619 29.1789 2.84619 31.5387V87.8299C2.84619 90.1897 4.01742 91.2712 6.5551 91.2712H16.2178C18.7554 91.2712 19.9267 90.1897 19.9267 87.8299V58.3323L39.6912 88.6656C41.1553 90.878 41.9361 91.2712 44.669 91.2712H54.0388C56.5765 91.2712 57.7477 90.1897 57.7477 87.8299V31.5387C57.6501 29.1789 56.4789 28.0973 53.99 28.0973Z" fill="white"/>
                            <path d="M11.3863 21.7061C17.2618 21.7061 22.025 16.9078 22.025 10.9887C22.025 5.06961 17.2618 0.27124 11.3863 0.27124C5.51067 0.27124 0.747559 5.06961 0.747559 10.9887C0.747559 16.9078 5.51067 21.7061 11.3863 21.7061Z" fill="white"/>
                            </g>
                            <defs>
                            <clipPath id="clip0">
                            <rect width="57" height="91" fill="white" transform="translate(0.747559 0.27124)"/>
                            </clipPath>
                            </defs>
                            </svg>
                        """.trimIndent()

                        // Define colors using RGB values.
                        val foreGroundColor: MPIOptions.FloatingLabelAppearance.Color = MPIOptions.FloatingLabelAppearance.Color(active = "#BF4320", inactive = "#7E2D16")
                        val backgroundColor: MPIOptions.FloatingLabelAppearance.Color = MPIOptions.FloatingLabelAppearance.Color(active = "#FFFFFF", inactive = "#FAFAFA")

                        val markerAppearance: MPIOptions.FloatingLabelAppearance.Marker = MPIOptions.FloatingLabelAppearance.Marker(
                            foregroundColor = foreGroundColor,
                            backgroundColor = backgroundColor,
                            icon = svgIcon,
                        )

                        val markerTheme: MPIOptions.FloatingLabelAppearance = MPIOptions.FloatingLabelAppearance(marker = markerAppearance)
                        val themeOptions: MPIOptions.FloatingLabelAllLocations = MPIOptions.FloatingLabelAllLocations(markerTheme)

                        mapView.floatingLabelsManager.removeAll()
                        mapView.floatingLabelsManager.labelAllLocations(themeOptions)
                    }
                    3 -> {
                        Log.d(TAG, "Light on Dark Theme Selected")
                        // Remove all floating labels and re-add using the light on dark theme.
                        val themeOptions: MPIOptions.FloatingLabelAllLocations = MPIOptions.FloatingLabelAllLocations(MPIOptions.FloatingLabelAppearance.lightOnDark)
                        mapView.floatingLabelsManager.removeAll()
                        mapView.floatingLabelsManager.labelAllLocations(themeOptions)
                    }
                    4 -> {
                        Log.d(TAG, "Dark on Light Theme Selected")
                        // Remove all floating labels and re-add using the dark on light theme.
                        val themeOptions: MPIOptions.FloatingLabelAllLocations = MPIOptions.FloatingLabelAllLocations(MPIOptions.FloatingLabelAppearance.darkOnLight)
                        mapView.floatingLabelsManager.removeAll()
                        mapView.floatingLabelsManager.labelAllLocations(themeOptions)
                    }
                    else -> {
                        Log.d(TAG, "Unknown style selected")
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

        // Zoom in when the map loads to better show the Floating Labels.
        mapView.cameraManager.set(
            MPIOptions.CameraTransformCoordinate(
                zoom = 800.0,
                position = mapView.currentMap?.createCoordinate(
                    43.86181934825464,
                    -78.94672121994297,
                ),
            ),
        )
    }

    override fun onMapChanged(map: MPIMap) { }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onNothingClicked() { }

    @Deprecated("Use MPIMapClickListener instead")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) { }

    override fun onStateChanged(state: MPIState) { }
}
