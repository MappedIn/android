package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapClickListener
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class AddInteractivity : AppCompatActivity(), MPIMapViewListener, MPIMapClickListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Add Interactivity"

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
                shadingAndOutlines = true,
                multiBufferRendering = true,
                outdoorView = MPIOptions.OutdoorView(enabled = true),
            ),
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.mapClickListener = this
        mapView.listener = this
    }

    override fun onClick(mapClickEvent: MPIMapClickEvent) {
        var title = String()
        val message = StringBuilder()

        // Use the map name as the title.
        if (mapClickEvent.maps.isNotEmpty()) {
            title = mapClickEvent.maps.first().name
        }

        // If a floating label was clicked, add its text to the message.
        if (mapClickEvent.floatingLabels.isNotEmpty()) {
            message.append("Floating Label Clicked: ")
            message.append(mapClickEvent.floatingLabels.first().text)
            message.append("\n")
        }

        // If a polygon was clicked, add it's location name to the message.
        if (mapClickEvent.polygons.isNotEmpty()) {
            message.append("Polygon clicked: ")
            message.append(mapClickEvent.polygons.first().locations.first().name)
            message.append("\n")
        }

        // If a path was clicked, add it to the message.
        if (mapClickEvent.paths.isNotEmpty()) {
            message.append("You clicked a path.\n")
        }

        // Add the coordinates clicked to the message.
        message.append("Coordinate Clicked: \nLatitude: ")
        message.append(mapClickEvent.position?.latitude)
        message.append("\nLongitude: ")
        message.append(mapClickEvent.position?.longitude)

        showMessage(title, message.toString())
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {}

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {}

    override fun onDataLoaded(data: MPIData) {}

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE

        // Make the floating labels interactive.
        mapView.floatingLabelsManager.labelAllLocations(MPIOptions.FloatingLabelAllLocations(interactive = true))

        // Draw an interactive journey.
        var departure = mapView.venueData?.locations?.first { it.name == "Microsoft" }
        var destination = mapView.venueData?.locations?.first { it.name == "Apple" }

        if (departure == null || destination == null) return

        val journeyOpts = MPIOptions.Journey(pathOptions = MPIOptions.Path(interactive = true))

        mapView.getDirections(to = destination, from = departure) {
            if (it != null) {
                mapView.journeyManager.draw(directions = it, options = journeyOpts)
            }
        }

        // Draw an interactive path.
        departure = mapView.venueData?.locations?.first { it.name == "Uniqlo" }
        destination = mapView.venueData?.locations?.first { it.name == "Nespresso" }

        if (departure == null || destination == null) return

        mapView.getDirections(to = destination, from = departure) {
            if (it != null) {
                mapView.pathManager.add(it.path, MPIOptions.Path(interactive = true))
            }
        }
    }

    override fun onMapChanged(map: MPIMap) {}

    @Deprecated("Use MPIMapClickListener instead")
    override fun onNothingClicked() {}

    @Deprecated("Use MPIMapClickListener instead")
    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {}

    override fun onStateChanged(state: MPIState) {}

    private fun showMessage(
        title: String,
        message: String,
    ) {
        runOnUiThread(
            Runnable {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder
                    .setMessage(message)
                    .setTitle(title)

                val dialog: AlertDialog = builder.create()
                dialog.show()
            },
        )
    }
}
