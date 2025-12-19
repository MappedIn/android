package com.mappedin.demo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddModelOptions
import com.mappedin.models.ClickPayload
import com.mappedin.models.Coordinate
import com.mappedin.models.Floor
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import org.json.JSONObject

class ModelsDemoActivity : AppCompatActivity() {
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		setContentView(root)

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
						dp(12)
					)
					insets
				}
			}
		val titleView =
			TextView(this).apply {
				text = "Models"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text =
					"Loads and displays 3D models from JSON configuration with custom positions, rotations, scales, and colors that was created using Mappedin 3D Model Mapper. Click on a model to remove it."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Map view container with loading indicator
		val mapContainer = FrameLayout(this)
		val mapView = MapView(this)
		mapContainer.addView(
			mapView.view,
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)

		// Add loading indicator
		loadingIndicator = ProgressBar(this)
		val loadingParams = FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
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
				mapId = "64ef49e662fd90fe020bee61",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { r2 ->
					r2.onSuccess {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
						onMapReady(mapView)
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

	private fun onMapReady(mapView: MapView) {
		// Load model positions from JSON file in assets.
		// The JSON file was created using Mappedin 3D Model Mapper:  https://developer.mappedin.com/docs/tools/3d-model-mapper
		val modelsJson = loadJsonFromAssets("model_positions.json")
		if (modelsJson == null) {
			Log.e("ModelsDemoActivity", "Failed to load model_positions.json")
			return
		}

		// Get all floors for lookup
		mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { floorsResult ->
			floorsResult.onSuccess { floors ->
				val floorMap = floors.associateBy { it.id }

				// Parse and add each model
				val modelsArray = modelsJson.getJSONArray("models")
				for (i in 0 until modelsArray.length()) {
					val modelData = modelsArray.getJSONObject(i)
					addModelFromJson(mapView, modelData, floorMap)
				}
			}
		}

		// Remove models that are clicked on.
		mapView.on("click") { payload ->
			val click = payload as? ClickPayload
			val clickedModel = click?.models?.firstOrNull() ?: return@on
			mapView.models.remove(clickedModel) { }
		}
	}

	private fun addModelFromJson(
		mapView: MapView,
		modelData: JSONObject,
		floorMap: Map<String, Floor>,
	) {
		try {
			val modelId = modelData.getString("modelId")
			val coordinateObj = modelData.getJSONObject("coordinate")
			val floorId = coordinateObj.getString("floorId")
			val latitude = coordinateObj.getDouble("latitude")
			val longitude = coordinateObj.getDouble("longitude")

			// Get rotation array
			val rotationArray = modelData.getJSONArray("rotation")
			val rotation =
				listOf(
					rotationArray.getDouble(0),
					rotationArray.getDouble(1),
					rotationArray.getDouble(2),
				)

			// Get scale array
			val scaleArray = modelData.getJSONArray("scale")
			val scale =
				AddModelOptions.Scale.PerAxis(
					scaleArray.getDouble(0),
					scaleArray.getDouble(1),
					scaleArray.getDouble(2),
				)

			val color = modelData.getString("color")
			val verticalOffset = coordinateObj.getDouble("verticalOffset")

			// Find the floor
			val floor = floorMap[floorId]
			if (floor == null) {
				Log.e("ModelsDemoActivity", "Floor not found: $floorId")
				return
			}

			// Create coordinate
			val coordinate = Coordinate(latitude, longitude)

			// Get model URL from assets
			val modelUrl = getModelUrl(modelId)
			if (modelUrl == null) {
				Log.e("ModelsDemoActivity", "Model not found: $modelId")
				return
			}

			// Create options with material colors
			val materialColors =
				mapOf(
					"Default" to AddModelOptions.MaterialStyle(color),
					"Fabric" to AddModelOptions.MaterialStyle(color),
					"Mpdn_Logo" to AddModelOptions.MaterialStyle(color),
				)

			val opts =
				AddModelOptions(
					interactive = true,
					rotation = rotation,
					scale = scale,
					verticalOffset = verticalOffset,
					visibleThroughGeometry = false,
					material = materialColors,
				)

			// Add the model
			mapView.models.add(coordinate, modelUrl, opts) { result ->
				result.onSuccess {
					Log.d("ModelsDemoActivity", "Successfully added model: $modelId")
				}
				result.onFailure { error ->
					Log.e("ModelsDemoActivity", "Failed to add model: $modelId - ${error.message}")
				}
			}
		} catch (e: Exception) {
			Log.e("ModelsDemoActivity", "Error parsing model data: ${e.message}")
		}
	}

	private fun getModelUrl(modelId: String): String? {
		// Map model IDs to their GLB filenames in assets/3d_assets
		return "https://appassets.androidplatform.net/assets/3d_assets/$modelId.glb"
	}

	private fun loadJsonFromAssets(fileName: String): JSONObject? =
		try {
			val jsonString = assets.open(fileName).bufferedReader().use { it.readText() }
			JSONObject(jsonString)
		} catch (e: Exception) {
			Log.e("ModelsDemoActivity", "Error loading JSON: ${e.message}")
			null
		}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
