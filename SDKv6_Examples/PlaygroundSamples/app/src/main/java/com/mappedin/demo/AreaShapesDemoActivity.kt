package com.mappedin.demo

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddPathOptions
import com.mappedin.models.Area
import com.mappedin.models.CameraAnimationOptions
import com.mappedin.models.CameraTarget
import com.mappedin.models.Coordinate
import com.mappedin.models.DirectionZone
import com.mappedin.models.EasingFunction
import com.mappedin.models.Feature
import com.mappedin.models.Floor
import com.mappedin.models.Geometry
import com.mappedin.models.GetDirectionsOptions
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.NavigationTarget
import com.mappedin.models.PaintStyle
import com.mappedin.models.Show3DMapOptions
import org.json.JSONArray
import org.json.JSONObject

class AreaShapesDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var pathToggle: SwitchCompat
	private lateinit var loadingIndicator: ProgressBar
	private var forkLiftArea: Area? = null
	private var maintenanceArea: Area? = null
	private var currentFloor: Floor? = null
	private var origin: Any? = null
	private var destination: Any? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		setContentView(root)

		// Header section with title and description.
		val header =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(16), dp(12), dp(16), dp(12))
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
					val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
					view.setPadding(
						dp(16),
						systemBars.top + dp(12),
						dp(16),
						dp(12),
					)
					insets
				}
			}

		val titleView =
			TextView(this).apply {
				text = "Areas & Shapes"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}

		val descriptionView =
			TextView(this).apply {
				text = "Demonstrates drawing shapes from areas, labeling them, and routing with zone avoidance."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}

		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Toggle section for path type
		val toggleContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(dp(16), dp(8), dp(16), dp(8))
				setBackgroundColor("#F3F4F6".toColorInt())
				gravity = Gravity.CENTER_VERTICAL
			}

		val toggleLabel =
			TextView(this).apply {
				text = "Human Safe Path"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}

		pathToggle =
			SwitchCompat(this).apply {
				isChecked = false
			}

		pathToggle.setOnCheckedChangeListener { _, isChecked ->
			drawPath(isChecked) // when ON, avoid zone for human safety
		}

		toggleContainer.addView(toggleLabel)
		toggleContainer.addView(pathToggle)
		root.addView(toggleContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Map view container with loading indicator
		val mapContainer = FrameLayout(this)
		mapView = MapView(this)
		mapContainer.addView(
			mapView.view,
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)

		// Add loading indicator
		loadingIndicator = ProgressBar(this)
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		loadingParams.gravity = Gravity.CENTER
		mapContainer.addView(loadingIndicator, loadingParams)

		mapContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
		root.addView(mapContainer)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "667b26b38298d5000b85eeb0",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { r2 ->
					r2.onSuccess {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
						onMapReady()
					}
					r2.onFailure {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
					}
				}
			}
		}
	}

	private fun onMapReady() {
		// Set camera position
		val cameraTarget =
			CameraTarget(
				center = Coordinate(latitude = 43.49109852349488, longitude = -79.61573677603003),
				zoomLevel = 18.283635634745174,
				pitch = 49.274370381250826,
				bearing = 0.3680689187522478,
			)

		mapView.camera.set(cameraTarget) {
			// Animate camera to closer view
			val animationTarget =
				CameraTarget(
					center = Coordinate(latitude = 43.49109852349488, longitude = -79.61573677603003),
					zoomLevel = 19.999995755401297,
					pitch = 49.274370381250826,
					bearing = 0.3680689187522478,
				)

			mapView.camera.animateTo(
				animationTarget,
				CameraAnimationOptions(duration = 2000, easing = EasingFunction.EASE_IN_OUT),
			) {
				// Camera animation complete, now load areas and create shapes
				loadAreasAndShapes()
			}
		}
	}

	private fun loadAreasAndShapes() {
		// Get current floor for zone avoidance
		mapView.currentFloor { floorResult ->
			floorResult.onSuccess { floor ->
				currentFloor = floor

				// Get all areas
				mapView.mapData.getByType<Area>(MapDataType.AREA) { result ->
					result.onSuccess { areas ->
						// Find the Forklift Area
						forkLiftArea = areas.find { it.name == "Forklift Area" }
						forkLiftArea?.let { area ->
							createShapeFromArea(
								area,
								"Maintenance Area",
								"red",
								0.7,
								0.2,
								0.1,
							)
						}

						// Find the Maintenance Area
						maintenanceArea = areas.find { it.name == "Maintenance Area" }
						maintenanceArea?.let { area ->
							createShapeFromArea(
								area,
								"Forklift Area",
								"orange",
								0.7,
								0.2,
								1.0,
							)
						}

						// Get origin and destination for paths
						setupPathEndpoints()
					}
				}
			}
		}
	}

	private fun createShapeFromArea(
		area: Area,
		labelText: String,
		color: String,
		opacity: Double,
		altitude: Double,
		height: Double,
	) {
		// Get the GeoJSON from the area
		val areaGeoJSON = area.geoJSON ?: return

		// Create a FeatureCollection containing the Feature of the Area
		val shapeFeatureCollection =
			JSONObject().apply {
				put("type", "FeatureCollection")
				put(
					"features",
					JSONArray().apply {
						put(
							JSONObject().apply {
								put("type", areaGeoJSON.optString("type"))
								areaGeoJSON.optJSONObject("properties")?.let {
									put("properties", it)
								}
								areaGeoJSON.optJSONObject("geometry")?.let {
									put("geometry", it)
								}
							},
						)
					},
				)
			}

		// Draw the shape
		mapView.shapes.add(
			shapeFeatureCollection,
			PaintStyle(color = color, opacity = opacity, altitude = altitude, height = height),
		) {}

		// Label the area
		mapView.labels.add(area, labelText) {}
	}

	private fun setupPathEndpoints() {
		// Get objects for origin and destination
		mapView.mapData.getByType<com.mappedin.models.MapObject>(MapDataType.MAP_OBJECT) { objResult ->
			objResult.onSuccess { objects ->
				origin = objects.find { it.name == "I3" }

				// Get doors for destination
				mapView.mapData.getByType<com.mappedin.models.Door>(MapDataType.DOOR) { doorResult ->
					doorResult.onSuccess { doors ->
						destination = doors.find { it.name == "Outbound Shipments 1" }

						// Draw initial path (forklift path, not avoiding zone)
						if (origin != null && destination != null) {
							drawPath(false)
						}
					}
				}
			}
		}
	}

	private fun drawPath(avoidZone: Boolean) {
		// Remove existing paths
		mapView.paths.removeAll()

		val originTarget = origin
		val destinationTarget = destination

		if (originTarget == null || destinationTarget == null || maintenanceArea == null) {
			return
		}

		// Create NavigationTargets
		val from =
			when (originTarget) {
				is com.mappedin.models.MapObject -> NavigationTarget.MapObjectTarget(originTarget)
				else -> return
			}

		val to =
			when (destinationTarget) {
				is com.mappedin.models.Door -> NavigationTarget.DoorTarget(destinationTarget)
				else -> return
			}

		// Create zone for avoidance if needed
		val options =
			if (avoidZone && maintenanceArea?.geoJSON != null && currentFloor != null) {
				val areaGeoJSON = maintenanceArea!!.geoJSON!!

				// Extract geometry from the area's GeoJSON and create Feature
				// Following the TypeScript pattern of manually extracting parts
				val geometryJson = areaGeoJSON.optJSONObject("geometry")
				val propertiesJson = areaGeoJSON.opt("properties")

				if (geometryJson != null) {
					val geometry = Geometry.fromJson(geometryJson)
					val properties: Map<String, Any?>? =
						when (propertiesJson) {
							is JSONObject -> {
								val map = mutableMapOf<String, Any?>()
								propertiesJson.keys().forEach { key ->
									map[key] = propertiesJson.opt(key)
								}
								map
							}
							else -> null
						}

					if (geometry != null) {
						val feature =
							Feature(
								geometry = geometry,
								properties = properties,
							)
						val zone =
							DirectionZone(
								cost = Double.MAX_VALUE,
								floor = currentFloor,
								geometry = feature,
							)
						GetDirectionsOptions(zones = listOf(zone))
					} else {
						null
					}
				} else {
					null
				}
			} else {
				null
			}

		// Get directions
		mapView.mapData.getDirections(from, to, options) { result ->
			result.onSuccess { directions ->
				if (directions != null) {
					android.util.Log.d("AreaShapes", "Got directions! Distance: ${directions.distance}m")
					// Draw the path
					val pathColor = if (avoidZone) "cornflowerblue" else "green"
					mapView.paths.add(
						directions.coordinates,
						AddPathOptions(color = pathColor),
					) {}
				} else {
					android.util.Log.w("AreaShapes", "getDirections returned null - no path found!")
				}
			}
			result.onFailure { error ->
				android.util.Log.e("AreaShapes", "getDirections failed: ${error.message}", error)
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
