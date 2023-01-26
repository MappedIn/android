package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class LevelSelector : AppCompatActivity(), AdapterView.OnItemSelectedListener, MPIMapViewListener {
    private lateinit var mapView: MPIMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Level Selector"

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall"
            ),
            showVenueOptions = MPIOptions.ShowVenue(labelAllLocationsOnInit = false)
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
    }

    private fun setupSpinner() {
        val controlLayout = findViewById<LinearLayout>(R.id.linearLayout)
        val spinner = Spinner(this)
        controlLayout.addView(spinner)
        val mapNames = mapView.venueData?.maps?.map { it.name }
        mapNames?.let { spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, it) }
        spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        parent?.getItemAtPosition(pos)?.let { mapName ->
            mapView.venueData?.maps?.first { it.name == mapName }?.let { map ->
                mapView.setMap(map) { err ->
                    err?.message?.let { message ->
                        Log.e("setMap", message)
                    }
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d("Nothing", "nothing")
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
        runOnUiThread {
            setupSpinner()
        }
    }

    override fun onFirstMapLoaded() {
    }

    override fun onMapChanged(map: MPIMap) {
    }

    override fun onNothingClicked() {
    }

    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
    }

    override fun onStateChanged(state: MPIState) {
    }
}
