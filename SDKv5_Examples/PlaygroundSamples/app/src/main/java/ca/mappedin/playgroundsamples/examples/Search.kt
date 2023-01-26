package ca.mappedin.playgroundsamples.examples

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import ca.mappedin.playgroundsamples.adapter.LocationAdapter
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions

class Search : AppCompatActivity(), MPIMapViewListener, SearchView.OnQueryTextListener {
    private lateinit var mapView: MPIMapView
    private lateinit var recyclerView: RecyclerView
    private var searchResults = listOf<MPINavigatable.MPILocation>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_split)
        this.title = "Search"

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

        val linearLayout = findViewById<LinearLayout>(R.id.linearLayout)
        val searchView = SearchView(this)
        linearLayout.addView(searchView)
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
            searchResults = mapView.venueData?.locations ?: listOf()
            runOnUiThread {
                reloadAdapter()
            }
        } else {
            mapView.searchManager.search(query) { results ->
                val filteredSearchResults = mutableListOf<MPINavigatable.MPILocation>()
                results?.flatMap { result -> result.matches.filter { match -> match.matchesOn == "name" } }
                    ?.let { matches ->
                        for (match in matches) {
                            mapView.venueData?.locations?.first { it.name == match.value }
                                ?.let { location ->
                                    if (!filteredSearchResults.contains(location)) filteredSearchResults.add(
                                        location
                                    )
                                }
                        }
                        searchResults = filteredSearchResults
                        runOnUiThread {
                            recyclerView.adapter =
                                LocationAdapter(searchResults) { index -> onLocationSelected(index) }
                        }
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
        searchResults = mapView.venueData?.locations ?: listOf()
        runOnUiThread {
            reloadAdapter()
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
