package com.mappedin.sdkv4_examples

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private var sortedMaps: List<MPIMap>? = null
    private var blueDot: MPIBlueDotPositionUpdate? = null
    private var selectedPolygon: MPINavigatable.MPIPolygon? = null
    private var presentMarkerId: String? = null
    private var markerId: String = ""
    private var defaultRotation: Double? = null
    private var defaultTilt: Double? = null

    private val connectionTemplateString: String by lazy { readFileContentFromAssets("connectionTemplate.html") }
    private val venueDataString: String by lazy { readFileContentFromAssets("mappedin-demo-mall.json") }
    private val markerString: String by lazy { readFileContentFromAssets("marker.html") }
    private val positionsString: String by lazy { readFileContentFromAssets("positions.json").replace("\n", "") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        increaseFloor.setOnClickListener {
            changeMap(true)
        }

        decreaseFloor.setOnClickListener {
            changeMap(false)
        }

        closeButton.setOnClickListener {
            clearPolygon()
        }

        directionsButton.setOnClickListener {
            if (selectedPolygon != null && blueDot?.nearestNode != null) {
                // Get directions to selected polygon from users nearest node
                mapView.getDirections(selectedPolygon!!, blueDot?.nearestNode!!, true) {
                    it?.let { directions ->
                        // Remove Marker before DrawJourney
                        mapView.removeMarker(presentMarkerId!!)
                        mapView.journeyManager.draw(
                            directions,
                            MPIOptions.Journey(
                                connectionTemplateString = connectionTemplateString,
                                destinationMarkerTemplateString = "",
                                departureMarkerTemplateString = "",
                                pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0),
                                polygonHighlightColor = "green"
                            )
                        )
                    }
                }
            }
        }

        centerDirectionsButton.setOnClickListener {
            mapView?.getNearestNodeByScreenCoordinates(mapView?.width?.div(2) ?: 0, mapView?.height?.div(2) ?: 0) { node ->
                if (node != null && selectedPolygon != null) {
                    mapView.getDirections(selectedPolygon!!, node, true) {
                        it?.let { directions ->
                            // Remove Marker before DrawJourney
                            mapView.removeMarker(presentMarkerId!!)
                            mapView.journeyManager.draw(
                                directions,
                                MPIOptions.Journey(
                                    destinationMarkerTemplateString = "<div>Destination</div>",
                                    departureMarkerTemplateString = "<div>Departure</div>",
                                    pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0),
                                    polygonHighlightColor = "orange"
                                )
                            )
                        }
                    }
                }
            }
        }

        followMode.setOnClickListener {
            mapView?.blueDotManager?.setState(MPIState.FOLLOW)
        }

        resetCamera.setOnClickListener {
            mapView.cameraControlsManager.setRotation(defaultRotation ?: 0.0) { _, error ->
                if (error == null) {
                    // access rotation here
                    Log.d("Camera", mapView.cameraControlsManager.rotation.toString())
                }
            }
            mapView.cameraControlsManager.setTilt(defaultTilt ?: 0.0) { _, error ->
                if (error == null) {
                    // access rotation here
                    Log.d("Camera", mapView.cameraControlsManager.tilt.toString())
                }
            }
        }

        // Set up MPIMapViewListener for MPIMapView events
        mapView.listener = object : MPIMapViewListener {
            override fun onDataLoaded(data: MPIData) {
                println("MPIData: " + Json.encodeToString(data))
                sortedMaps = data.maps.sortedBy { it.elevation }

                // Enable blueDot, does not appear until updatePosition is called with proper coordinates
                mapView.blueDotManager.enable(MPIOptions.BlueDot(smoothing = false, showBearing = true, baseColor = "#2266ff"))

                mapView.venueData?.polygons?.forEach {
                    if (it.locations.isNullOrEmpty()) {
                        mapView.addInteractivePolygon(it)
                    }
                }
            }

            override fun onMapChanged(map: MPIMap) {
                runOnUiThread {
                    supportActionBar?.title = map.name
                }
                println("MPIMap Changed: " + Json.encodeToString(map))

                // Create an MPICoordinate from Latitude and Longitude
                val coord = map.createCoordinate(43.5214, -80.5369)
                println("MPICoordinate: $coord")

                // Find Distance between Location and Nearest Node
                val distance = distanceLocationToNode(map, 43.5214, -80.5369)
                println("Distance: $distance")
            }

            override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
                println("MPIPolygon Clicked:" + Json.encodeToString(polygon))
                selectPolygon(polygon)
            }

            override fun onStateChanged(state: MPIState) {
                runOnUiThread {
                    if (state == MPIState.FOLLOW) {
                        followMode.visibility = View.GONE
                    } else {
                        followMode.visibility = View.VISIBLE
                    }
                }
            }

            override fun onBlueDotUpdated(blueDot: MPIBlueDot) {
//                this@MainActivity.blueDot = blueDot
//                nearestNode.text = "BlueDot Nearest Node: " + (blueDot.nearestNode?.id ?: "N/A")
            }

            override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
                this@MainActivity.blueDot = update
                runOnUiThread {
                    nearestNode.text = getString(R.string.blueDotNearestNode, update.nearestNode?.id ?: "N/A")
                }
            }

            override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
                println("State change: ${stateChange.name} ${stateChange.markerVisibility} ${stateChange.reason}")
            }

            override fun onNothingClicked() {
                clearPolygon()
            }

            override fun onFirstMapLoaded() {
                defaultRotation = defaultRotation ?: mapView.cameraControlsManager.rotation
                defaultTilt = defaultTilt ?: mapView.cameraControlsManager.tilt

                mapView.cameraControlsManager.setRotation(180.0)
                mapView.cameraControlsManager.setTilt(0.0)

                // label all locations to be light on dark
                mapView.labelAllLocations(
                    options = MPIOptions.FloatingLabelAllLocations(
                        appearance = MPIOptions.FloatingLabelAppearance.lightOnDark
                    )
                )

                // create a multi-destination journey between 4 sample locations
                mapView.venueData?.locations?.let { locations ->
                    if (locations.size < 8) return@let

                    mapView.getDirections(
                        to = MPIDestinationSet(destinations = listOf(locations[4], locations[5], locations[6])),
                        from = locations[7],
                        accessible = false
                    ) {
                        it?.let { directions ->
                            // draw the journey
                            mapView.journeyManager.draw(
                                directions = directions,
                                options = MPIOptions.Journey(connectionTemplateString = connectionTemplateString)
                            )

                            val maxSteps = 3
                            val startDelay = 15
                            val stepDelay = 5
                            val timer = Timer("Step Setter", false)

                            for (step in 0..maxSteps) {
                                // manipulate journey after a delay
                                timer.schedule((startDelay + stepDelay * step) * 1000L) {
                                    if (step == maxSteps) {
                                        // change the journey step
                                        mapView.journeyManager.clear()
                                    } else {
                                        // clear journey
                                        mapView.journeyManager.setStep(step)
                                    }
                                }
                            }
                        }
                    }
                }

                val positions = Json.decodeFromString<List<MPIPosition>>(positionsString)
                val timer = Timer("Position Updater", false)
                positions.forEachIndexed { index, position ->
                    timer.schedule(3000L * index) {
                        mapView.blueDotManager.updatePosition(position)
                    }
                }
            }
        }

        // Load venue with credentials, if using proxy pass in MPIOptions.Init(noAuth = true, venue="venue_name", baseUrl="proxy_url")
//        mapView.loadVenue(
//            MPIOptions.Init(
//                "5eab30aa91b055001a68e996",
//                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
//                "mappedin-demo-mall",
//                headers = listOf(MPIHeader("testName", "testValue"))
//            ),
//            MPIOptions.ShowVenue(
//                labelAllLocationsOnInit = true,
//                backgroundColor = "#CDCDCD"
//            )
//        )
        mapView.showVenue(venueDataString) {
            it?.let {
                println(it.errorMessage)
            }
        }
    }

    fun clearPolygon() {
        selectedPolygon = null
        mapView.clearAllPolygonColors()
        runOnUiThread {
            locationView.visibility = View.GONE
        }
    }

    fun distanceLocationToNode(map: MPIMap, latitude: Double, longitude: Double): Double? {
        // Create an MPICoordinate from Latitude and Longitude
        map.createCoordinate(latitude, longitude)?.let { coordinate ->
            // Calculate Distance Between Coordinate and the Nearest Node
            val p1_x = coordinate.x
            val p1_y = coordinate.y
            if ((blueDot?.nearestNode?.y != null) && (blueDot?.nearestNode?.x != null)) {
                val p2_x = blueDot?.nearestNode!!.x
                val p2_y = blueDot?.nearestNode!!.y
                val xDist = (p2_x!! - p1_x)
                val yDist = (p2_y!! - p1_y)
                val mappedinDistance = sqrt(xDist * xDist + yDist * yDist)
                // Convert the Distance from Mappedin Units to Meters
                return mappedinDistance * map.x_scale!!
            }
        }
        return null
    }

    fun selectPolygon(polygon: MPINavigatable.MPIPolygon) {
        polygon.locations.firstOrNull()?.let {
            selectedPolygon = polygon

            mapView.clearAllPolygonColors {
                mapView.setPolygonColor(polygon.id, "blue")
            }
            mapView.focusOn(MPIOptions.Focus(polygons = listOf(polygon)))

            if (markerId == presentMarkerId) {
                mapView?.removeMarker(markerId)
            }

            // Add a Marker on the node of the polygon being clicked
            val node = polygon.entrances[0]
            markerId = mapView.createMarker(node, markerString, MPIOptions.Marker(anchor = MPIOptions.MARKER_ANCHOR.TOP))
            presentMarkerId = markerId

            runOnUiThread {
                locationView.visibility = View.VISIBLE
                locationTitle.text = it.name
                locationDescription.text = it.description
                Glide
                    .with(this@MainActivity)
                    .load(it.logo?.original)
                    .centerCrop()
                    .into(locationImage)
            }
        }
    }

    private fun changeMap(isIncrementing: Boolean = true) {
        mapView.currentMap?.let {
            val currentIndex = sortedMaps?.indexOf(it) ?: 0
            val nextIndex = if (isIncrementing) currentIndex + 1 else currentIndex - 1
            sortedMaps?.getOrNull(nextIndex)?.let { nextMap ->
                mapView.setMap(nextMap)
            }
        }
    }

    private fun readFileContentFromAssets(file: String): String {
        return application.assets.open(file).bufferedReader().use {
            it.readText()
        }
    }
}
