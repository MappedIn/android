package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import ca.mappedin.playgroundsamples.adapter.InstructionAdapter
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class TurnByTurnDirections : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private var instructions = listOf<MPIDirections.MPIInstruction>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Turn by Turn Directions"

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
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
    }

    private fun setupRecyclerView() {
        val linearLayout = findViewById<LinearLayout>(R.id.controlsLinearLayout)
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setPadding(16, 16, 16, 16)
        linearLayout.addView(recyclerView)
        recyclerView.adapter = InstructionAdapter(instructions)
        recyclerView.setHasFixedSize(true)
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
    }

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE

        val departure = mapView.venueData?.locations?.first { it.name == "Apple" }
        val destination = mapView.venueData?.locations?.first { it.name == "Microsoft" }

        if (departure == null || destination == null) return

        mapView.getDirections(to = destination, from = departure) {
                directions ->
            directions?.let { mapView.journeyManager.draw(it) }
            directions?.instructions?.let { instructions = it }
            runOnUiThread {
                setupRecyclerView()
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
}
