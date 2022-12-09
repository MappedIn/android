package com.mappedin.sdkv5_examples

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import com.mappedin.sdkv5_examples.databinding.ActivityMainBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var sortedMaps: List<MPIMap>? = null
    private var blueDot: MPIBlueDotPositionUpdate? = null
    private var selectedPolygon: MPINavigatable.MPIPolygon? = null
    private var presentMarkerId: String? = null
    private var markerId: String = ""
    private var defaultRotation: Double? = null
    private var defaultTilt: Double? = null

    private val connectionTemplateString: String by lazy { readFileContentFromAssets("connectionTemplate.html") }
    private val markerString: String by lazy { readFileContentFromAssets("marker.html") }
    private val positionsString: String by lazy {
        readFileContentFromAssets("positions.json").replace(
            "\n",
            ""
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapView = binding.mapView
        binding.increaseFloor.setOnClickListener {
            changeMap(true)
        }

        binding.decreaseFloor.setOnClickListener {
            changeMap(false)
        }

        binding.closeButton.setOnClickListener {
            clearPolygon()
        }

        binding.directionsButton.setOnClickListener {
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
                                pathOptions = MPIOptions.Path(
                                    drawDuration = 0.0,
                                    pulseIterations = 0.0
                                ),
                                polygonHighlightColor = "green"
                            )
                        )
                    }
                }
            }
        }

        binding.centerDirectionsButton.setOnClickListener {
            mapView?.getNearestNodeByScreenCoordinates(
                mapView?.width?.div(2) ?: 0,
                mapView?.height?.div(2) ?: 0
            ) { node ->
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
                                    pathOptions = MPIOptions.Path(
                                        drawDuration = 0.0,
                                        pulseIterations = 0.0
                                    ),
                                    polygonHighlightColor = "orange"
                                )
                            )
                        }
                    }
                }
            }
        }

        binding.followMode.setOnClickListener {
            mapView?.blueDotManager?.setState(MPIState.FOLLOW)
        }

        binding.resetCamera.setOnClickListener {
            mapView.cameraManager.set(
                MPIOptions.CameraTransformNode(
                    rotation = defaultRotation,
                    tilt = defaultTilt
                )
            ) { error ->
                if (error == null) {
                    // access rotation here
                    Log.d("Camera", mapView.cameraManager.rotation.toString())
                    // access tilt here
                    Log.d("Camera", mapView.cameraManager.tilt.toString())
                }
            }
        }

        // Set up MPIMapViewListener for MPIMapView events
        mapView.listener = object : MPIMapViewListener {
            override fun onDataLoaded(data: MPIData) {
                println("MPIData: " + Json.encodeToString(data))
                sortedMaps = data.maps.sortedBy { it.elevation }

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
                        binding.followMode.visibility = View.GONE
                    } else {
                        binding.followMode.visibility = View.VISIBLE
                    }
                }
            }

            override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
                this@MainActivity.blueDot = update
                runOnUiThread {
                    binding.nearestNode.text =
                        getString(R.string.blueDotNearestNode, update.nearestNode?.id ?: "N/A")
                }
            }

            override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
                println("State change: ${stateChange.name} ${stateChange.markerVisibility} ${stateChange.reason}")
            }

            override fun onNothingClicked() {
                clearPolygon()
            }

            override fun onFirstMapLoaded() {
                // Enable blueDot, does not appear until updatePosition is called with proper coordinates
                mapView.blueDotManager.enable(
                    MPIOptions.BlueDot(
                        allowImplicitFloorLevel = true,
                        smoothing = false,
                        showBearing = true,
                        baseColor = "#2266ff"
                    )
                )

                val positions = Json.decodeFromString<List<MPIPosition>>(positionsString)
                val timer = Timer("Position Updater", false)
                positions.forEachIndexed { index, position ->
                    timer.schedule(3000L * index) {
                        mapView.blueDotManager.updatePosition(position)
                    }
                }

                defaultRotation = defaultRotation ?: mapView.cameraManager.rotation
                defaultTilt = defaultTilt ?: mapView.cameraManager.tilt

                mapView.cameraManager.set(
                    MPIOptions.CameraTransformNode(
                        rotation = 180.0,
                        tilt = 0.0
                    )
                ) {
                    it?.let {
                        println(it.message)
                    }
                }

                // label all locations to be dark on light
                mapView.floatingLabelsManager.labelAllLocations(
                    MPIOptions.FloatingLabelAllLocations(
                        appearance = MPIOptions.FloatingLabelAppearance.darkOnLight
                    )
                )

                // create a multi-destination journey between 4 sample locations
                mapView.venueData?.locations?.let { locations ->
                    if (locations.size < 8) return@let

                    mapView.getDirections(
                        to = MPIDestinationSet(
                            destinations = listOf(
                                locations[4],
                                locations[5],
                                locations[6]
                            )
                        ),
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
                                        mapView.journeyManager.setStep(step) {
                                            it?.let {
                                                println(it.message)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Load venue with credentials
        mapView.loadVenue(
            MPIOptions.Init(
                "5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall",
                headers = listOf(MPIHeader("language", "en-US"))
            ),
            // Locations are labeled separately onFirstMapLoaded where their style can be customized
            showVenueOptions = MPIOptions.ShowVenue(
                labelAllLocationsOnInit = false,
                backgroundColor = "#CDCDCD"
            )
        ) {
            it?.let {
                println(it.message)
            }
        }
    }

    fun clearPolygon() {
        selectedPolygon = null
        binding.mapView.clearAllPolygonColors() {
            it?.let {
                println(it.message)
            }
        }
        runOnUiThread {
            binding.locationView.visibility = View.GONE
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
            var mapView = binding.mapView

            mapView.clearAllPolygonColors {
                mapView.setPolygonColor(polygon, "blue")
            }
            mapView.cameraManager.focusOn(
                MPIOptions.CameraTargets(
                    polygons = listOf(
                        polygon
                    )
                )
            )

            if (markerId == presentMarkerId) {
                mapView?.removeMarker(markerId)
            }

            // Add a Marker on the node of the polygon being clicked
            val node = polygon.entrances[0]
            markerId = mapView.createMarker(
                node,
                markerString,
                MPIOptions.Marker(anchor = MPIOptions.MARKER_ANCHOR.TOP)
            )
            presentMarkerId = markerId

            runOnUiThread {
                binding.locationView.visibility = View.VISIBLE
                binding.locationTitle.text = it.name
                binding.locationDescription.text = it.description
                Glide
                    .with(this@MainActivity)
                    .load(it.logo?.original)
                    .centerCrop()
                    .into(binding.locationImage)
            }
        }
    }

    private fun changeMap(isIncrementing: Boolean = true) {
        var mapView = binding.mapView
        mapView.currentMap?.let {
            val currentIndex = sortedMaps?.indexOf(it) ?: 0
            val nextIndex = if (isIncrementing) currentIndex + 1 else currentIndex - 1
            sortedMaps?.getOrNull(nextIndex)?.let { nextMap ->
                mapView.setMap(nextMap) {
                    it?.let {
                        println(it.message)
                    }
                }
            }
        }
    }

    private fun readFileContentFromAssets(file: String): String {
        return application.assets.open(file).bufferedReader().use {
            it.readText()
        }
    }
}
