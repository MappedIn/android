package com.mappedin.sdkv3_examples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    var sortedMaps: List<MPIMap>? = null
    var blueDot: MPIBlueDotPositionUpdate? = null
    var selectedPolygon: MPINavigatable.MPIPolygon? = null
    var presentMarkerId: String? = null
    var markerId: String = ""
    var defaultRotation: Double? = null
    var defaultTilt: Double? = null

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
                //Get directions to selected polygon from users nearest node
                mapView.getDirections(selectedPolygon!!, blueDot?.nearestNode!!, true) { directions ->
                    directions?.path?.let { path ->
                        // Remove Marker before DrawJourney
                        mapView.removeMarker(presentMarkerId!!)
                        mapView.drawJourney(directions,
                            MPIOptions.Journey(
                                connectionTemplateString = "<div style=\"font-size: 13px;display: flex; align-items: center; justify-content: center;\"><div style=\"margin: 10px;\">{{capitalize type}} {{#if isEntering}}to{{else}}from{{/if}} {{toMapName}}</div><div style=\"width: 40px; height: 40px; border-radius: 50%;background: green;display: flex;align-items: center;margin: 5px;margin-left: 0px;justify-content: center;\"><svg height=\"16\" viewBox=\"0 0 36 36\" width=\"16\"><g fill=\"white\">{{{icon}}}</g></svg></div></div>",
                                destinationMarkerTemplateString = "",
                                departureMarkerTemplateString = "",
                                pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0),
                                polygonHighlightColor = "green"))
                    }
                }
            }
        }

        centerDirectionsButton.setOnClickListener {
            mapView?.getNearestNodeByScreenCoordinates(mapView?.width?.div(2) ?: 0, mapView?.height?.div(2) ?: 0) { node ->
                if (node != null && selectedPolygon != null) {
                    mapView.getDirections(selectedPolygon!!, node, true) { directions ->
                        directions?.path?.let { path ->
                            // Remove Marker before DrawJourney
                            mapView.removeMarker(presentMarkerId!!)
                            mapView.drawJourney(directions,
                                MPIOptions.Journey(destinationMarkerTemplateString = "<div>Destination</div>",
                                        departureMarkerTemplateString = "<div>Departure</div>",
                                        pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0),
                                        polygonHighlightColor = "orange"))
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

        //Set up MPIMapViewListener for MPIMapView events
        mapView.listener = object : MPIMapViewListener {
            override fun onDataLoaded(data: MPIData) {
                println("MPIData: " + Json.encodeToString(data))
                sortedMaps = data.maps.sortedBy{it.elevation}

                //Enable blueDot, does not appear until updatePosition is called with proper coordinates
                mapView.enableBlueDot(MPIOptions.BlueDot(smoothing = false, showBearing = true, baseColor = "#2266ff"))

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
                val coord = map.createCoordinate(43.5214,-80.5369)

                // Find Distance between Location and Nearest Node
                val distance = distanceLocationToNode(map, 43.5214, -80.5369)
            }
            override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
                println("MPIPolygon Clicked:" + Json.encodeToString(polygon))
                runOnUiThread {
                    selectPolygon(polygon)
                }
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
                nearestNode.text = "BlueDot Nearest Node: " + (update.nearestNode?.id ?: "N/A")
            }

            override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
//                println(stateChange.name)
                println(stateChange.markerVisibility)
//                println(stateChange.reason)
            }

            override fun onNothingClicked() {
                runOnUiThread {
                    clearPolygon()
                }
            }

            override fun onFirstMapLoaded() {
                runOnUiThread {
                    defaultRotation = defaultRotation ?: mapView.cameraControlsManager.rotation
                    defaultTilt = defaultTilt ?: mapView.cameraControlsManager.tilt

                    mapView.cameraControlsManager.setRotation(180.0)
                    mapView.cameraControlsManager.setTilt(0.0)

                    val fileName = "position.json"
                    val string = application.assets.open(fileName).bufferedReader().use {
                        it.readText()
                    }.toString()
                    println(string.replace("\n", ""))

                    val positions = Json.decodeFromString<List<MPIPosition>>(string)
                    val handler = Handler()
                    positions.forEachIndexed { index, position ->
                        handler.postDelayed({
                            mapView.updatePosition(position)
                        }, (3000*index).toLong())
                    }
                }
            }
        }

        //Load venue with credentials, if using proxy pass in MPIOptions.Init(noAuth = true, venue="venue_name", baseUrl="proxy_url")
        //mapView.loadVenue(MPIOptions.Init("5eab30aa91b055001a68e996", "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1", "mappedin-demo-mall", headers = listOf(MPIHeader("testName", "testValue"))), MPIOptions.ShowVenue(labelAllLocationsOnInit = true, backgroundColor = "#CDCDCD"))
        val venueDataJson = application.assets.open("mappedin-demo-mall.json").bufferedReader().use{
            it.readText()
        }
        mapView.showVenue(venueDataJson) {
            it?.let {
                println(it.errorMessage)
            }
        }
    }

    fun clearPolygon() {
        selectedPolygon = null
        locationView.visibility = View.GONE
        mapView.clearAllPolygonColors()
    }

    fun distanceLocationToNode(map: MPIMap, latitude: Double, longitude: Double): Double? {
        // Create an MPICoordinate from Latitude and Longitude
        val coordinate = map.createCoordinate(latitude, longitude)
        // Calculate Distance Between Coordinate and the Nearest Node
        if ((coordinate?.x != null) && (coordinate?.y != null)) {
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
        polygon.locations?.firstOrNull()?.let {

            selectedPolygon = polygon
            locationView.visibility = View.VISIBLE

            mapView.clearAllPolygonColors {
                mapView.setPolygonColor(polygon.id, "blue")
            }
            mapView.focusOn(MPIOptions.Focus(polygons=listOf(polygon)))

            if (markerId == presentMarkerId) {
                mapView?.removeMarker(markerId)
            }

            //Add a Marker on the node of the polygon being clicked
            var node = polygon.entrances.get(0)
            markerId = mapView.createMarker(node,
                    "<div style=\"width: 32px; height: 32px;\"><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 293.334 293.334\"><g fill=\"#010002\">" +
                            "<path d=\"M146.667 0C94.903 0 52.946 41.957 52.946 93.721c0 22.322 7.849 42.789 20.891 58.878 4.204 5.178 11.237 13.331 14.903 18.906 21.109 32.069 " +
                            "48.19 78.643 56.082 116.864 1.354 6.527 2.986 6.641 4.743.212 5.629-20.609 20.228-65.639 50.377-112.757 3.595-5.619 10.884-13.483 15.409-18.379a94.561 " +
                            "94.561 0 0016.154-24.084c5.651-12.086 8.882-25.466 8.882-39.629C240.387 41.962 198.43 0 146.667 0zm0 144.358c-28.892 0-52.313-23.421-52.313-52.313 0-28.887 " +
                            "23.421-52.307 52.313-52.307s52.313 23.421 52.313 52.307c0 28.893-23.421 52.313-52.313 52.313z\"/><circle cx=\"146.667\" cy=\"90.196\" r=\"21.756\"/></g></svg></div>",
                    MPIOptions.Marker(anchor = MPIOptions.MARKER_ANCHOR.TOP))
            presentMarkerId = markerId

            locationTitle.text = it.name
            locationDescription.text = it.description
            Glide
                .with(this@MainActivity)
                .load(it.logo?.original)
                .centerCrop()
                .into(locationImage);
        }
    }

    fun changeMap(isIncrementing: Boolean = true) {
        mapView.currentMap?.let {
            val currentIndex = sortedMaps?.indexOf(it) ?: 0
            val nextIndex = if(isIncrementing) currentIndex+1 else currentIndex-1
            sortedMaps?.getOrNull(nextIndex)?.let { nextMap ->
                mapView.setMap(nextMap)
            }
        }
    }

}