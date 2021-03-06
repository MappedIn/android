package com.mappedin.sdkv3_examples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.bumptech.glide.Glide
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*
import com.mappedin.sdk.web.MPIOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    var sortedMaps: List<MPIMap>? = null
    var blueDot: MPIBlueDot? = null
    var selectedPolygon: MPINavigatable.MPIPolygon? = null

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
                        mapView.drawJourney(directions,
                            MPIOptions.Journey(
                                connectionTemplateString = """<div style=\"font-size: 13px;display: flex; align-items: center; justify-content: center;\"><div style=\"margin: 10px;\">{{capitalize type}} {{#if isEntering}}to{{else}}from{{/if}} {{toMapName}}</div><div style=\"width: 40px; height: 40px; border-radius: 50%;background: green;display: flex;align-items: center;margin: 5px;margin-left: 0px;justify-content: center;\"><svg height=\"16\" viewBox=\"0 0 36 36\" width=\"16\"><g fill=\"white\">{{{icon}}}</g></svg></div></div>""",
                                pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0)))
                    }
                }
            }
        }

        centerDirectionsButton.setOnClickListener {
            mapView?.getNearestNodeByScreenCoordinates(mapView?.width?.div(2) ?: 0, mapView?.height?.div(2) ?: 0) { node ->
                if (node != null && selectedPolygon != null) {
                    mapView.getDirections(selectedPolygon!!, node, true) { directions ->
                        directions?.path?.let { path ->
                            mapView.drawJourney(directions,
                                MPIOptions.Journey(pathOptions = MPIOptions.Path(drawDuration = 0.0, pulseIterations = 0.0)))
                        }
                    }
                }
            }
        }

        followButton.setOnClickListener {
            mapView?.blueDotManager?.setState(MPIState.FOLLOW)
        }

        //Set up MPIMapViewListener for MPIMapView events
        mapView.listener = object : MPIMapViewListener {
            override fun onDataLoaded(data: MPIData) {
                println("MPIData: " + Json.encodeToString(data))
                sortedMaps = data.maps.sortedBy{it.elevation}

                //Enable blue dot, does not appear until updatePosition is called with proper coordinates
                mapView.enableBlueDot(MPIOptions.BlueDot(smoothing = false, showBearing = true))

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
                        followButton.visibility = View.GONE
                    } else {
                        followButton.visibility = View.VISIBLE
                    }
                }
            }

            override fun onBlueDotUpdated(blueDot: MPIBlueDot) {
                this@MainActivity.blueDot = blueDot
                nearestNode.text = "BlueDot Nearest Node: " + (blueDot.nearestNode?.id ?: "N/A")
            }

            override fun onNothingClicked() {
                runOnUiThread {
                    clearPolygon()
                }
            }

            override fun onFirstMapLoaded() {
                runOnUiThread {
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
        mapView.loadVenue(MPIOptions.Init("5eab30aa91b055001a68e996", "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1", "mappedin-demo-mall", headers = listOf(MPIHeader("testName", "testValue"))), MPIOptions.ShowVenue(labelAllLocationsOnInit = true, backgroundColor = "#CDCDCD"))
    }

    fun clearPolygon() {
        selectedPolygon = null
        locationView.visibility = View.GONE
        mapView.clearAllPolygonColors()
    }

    fun selectPolygon(polygon: MPINavigatable.MPIPolygon) {
        polygon.locations?.firstOrNull()?.let {

            selectedPolygon = polygon
            locationView.visibility = View.VISIBLE

            mapView.clearAllPolygonColors {
                mapView.setPolygonColor(polygon.id, "blue")
            }
            mapView.focusOn(MPIOptions.Focus(polygons=listOf(polygon)))

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