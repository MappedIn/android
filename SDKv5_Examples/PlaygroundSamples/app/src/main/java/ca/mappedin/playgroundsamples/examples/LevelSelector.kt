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

class LevelSelector : AppCompatActivity(), MPIMapViewListener {
    private lateinit var mapView: MPIMapView
    private lateinit var mapSpinner: Spinner //The spinner that contains the map (level) names.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Level Selector"

        mapSpinner = Spinner(this)
        mapSpinner.setPadding(12,16,12,16)

        mapView = findViewById<MPIMapView>(R.id.mapView)
        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/api-keys/
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-campus"
            ),
            showVenueOptions = MPIOptions.ShowVenue(labelAllLocationsOnInit = false)
        ) { Log.e(javaClass.simpleName, "Error loading map view") }
        mapView.listener = this
    }

    //Populate the spinner with all map groups (buildings). When changed populate the level spinner with all maps in that group.
    private fun setupBuildingSpinner() {
        val controlLayout = findViewById<LinearLayout>(R.id.linearLayout)
        val buildingSpinner = Spinner(this)
        buildingSpinner.setPadding(12,16,12,16)
        controlLayout.addView(buildingSpinner)
        val mapGroupNames = mapView.venueData?.mapGroups?.map { it.name }
        mapGroupNames?.let { buildingSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, it) }

        buildingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            //When a new map group (building) is selected, update the level spinner with the list of maps for that building.
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMapGroupName = parent?.getItemAtPosition(position).toString()
                val mapNames = mapView.venueData?.maps?.filter{ it.group?.name.contentEquals(selectedMapGroupName)}?.map { it.name }
                mapNames?.let { mapSpinner.adapter = parent?.context?.let { it1 ->
                    ArrayAdapter<String>(
                        it1, android.R.layout.simple_spinner_item, it)
                } }
            }
        }
    }

    //Populate the spinner with the first map group on load. Change the map when the user selects a map.
    private fun setupLevelSpinner() {
        val controlLayout = findViewById<LinearLayout>(R.id.linearLayout)
        controlLayout.addView(mapSpinner)
        //On first load, the spinner shows the list of maps from the first map group.
        val firstMapGroupName = mapView.venueData?.mapGroups?.get(0)?.name.toString()
        val mapNames = mapView.venueData?.maps?.filter{ it.group?.name.contentEquals(firstMapGroupName)}?.map { it.name }
        mapNames?.let { mapSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, it) }

        mapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.getItemAtPosition(position)?.let { mapName ->
                    mapView.venueData?.maps?.first { it.name == mapName }?.let { map ->
                        mapView.setMap(map) { err ->
                            err?.message?.let { message ->
                                Log.e("setMap", message)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
    }

    override fun onFirstMapLoaded() {
        //Populate the spinners once the data has loaded.
        runOnUiThread {
            setupBuildingSpinner()
            setupLevelSpinner()
        }
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
