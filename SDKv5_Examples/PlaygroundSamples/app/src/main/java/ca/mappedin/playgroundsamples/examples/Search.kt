package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import ca.mappedin.playgroundsamples.adapter.LocationAdapter
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.MPIBlueDotPositionUpdate
import com.mappedin.sdk.models.MPIBlueDotStateChange
import com.mappedin.sdk.models.MPIData
import com.mappedin.sdk.models.MPIMap
import com.mappedin.sdk.models.MPINavigatable
import com.mappedin.sdk.models.MPIOfflineSearchResultLocation
import com.mappedin.sdk.models.MPIState
import com.mappedin.sdk.web.MPIOptions

class Search : AppCompatActivity(), MPIMapViewListener, SearchView.OnQueryTextListener {
    private lateinit var mapView: MPIMapView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private var searchResults: MutableList<MPINavigatable.MPILocation> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Search"

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

        val linearLayout = findViewById<LinearLayout>(R.id.controlsLinearLayout)
        val searchView = SearchView(this)
        linearLayout.addView(searchView)
        searchView.queryHint = "Search..."
        searchView.isIconifiedByDefault = false
        searchView.setOnQueryTextListener(this)

        recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        linearLayout.addView(recyclerView)
    }

    private fun reloadAdapter() {
        recyclerView.adapter = LocationAdapter(searchResults) { index -> onLocationSelected(index) }
        recyclerView.setHasFixedSize(true)
    }

    private fun onLocationSelected(index: Int) {
        val polygons = searchResults[index].polygons
        val floor = polygons[0].map!!
        // Move to location floor and focus on
        mapView.setMap(floor) { err ->
            err?.message?.let { message ->
                Log.e("setMap", message)
            }
            mapView.setPolygonColor(polygons[0], "#BF4320")
            mapView.cameraManager.focusOn(MPIOptions.CameraTargets(polygons = polygons))
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query == null || query == "") {
            searchResults = (mapView.venueData?.locations ?: listOf()).toMutableList()
            runOnUiThread {
                reloadAdapter()
            }
        } else {
            mapView.searchManager.search(query) { results ->
                // This sample only shows locations of type tenant.
                // The filters below filter results that are MPIOfflineSearchResultLocation (removing categories MPIOfflineSearchResultCategory)
                // and where the location type is tenant (removing amenities it.location.type = "amenities"). Location types can be unique
                // to each venue. The list is then sorted, with the highest score first.
                val searchLocations = results
                    ?.filterIsInstance<MPIOfflineSearchResultLocation>()
                    ?.filter { it.location.type == "tenant" }
                    ?.sortedByDescending { it.score }
                searchResults.clear()

                searchLocations?.forEach { searchResultLocation ->
                    searchResults.add(searchResultLocation.location)
                    // Print out the MPIOfflineSearchMatch to logcat to show search match justification.
                    Log.d("Search Matches On: ", searchResultLocation.matches.toString())
                }
                runOnUiThread {
                    recyclerView.adapter =
                        LocationAdapter(searchResults) { index -> onLocationSelected(index) }
                }
            }
        }
        return false
    }

    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
        searchResults = (mapView.venueData?.locations ?: listOf()).toMutableList()
        runOnUiThread {
            reloadAdapter()
        }
    }

    override fun onFirstMapLoaded() {
        progressBar.visibility = ProgressBar.INVISIBLE
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
