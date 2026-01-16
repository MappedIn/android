package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.mappedin.MapView
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import org.json.JSONArray
import org.json.JSONObject

class OfflineModeDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Offline Mode"

		// Create a FrameLayout to hold both the map view and loading indicator
		val container = FrameLayout(this)

		mapView = MapView(this)
		container.addView(
			mapView.view,
			ViewGroup.LayoutParams(
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
		container.addView(loadingIndicator, loadingParams)

		setContentView(container)

		// Load the MVFv3 zip file from assets
		val zipBuffer = loadMvfFromAssets("school-demo-multifloor-mvfv3.zip")
		if (zipBuffer == null) {
			Log.e("OfflineModeDemo", "Failed to load MVF file from assets")
			runOnUiThread {
				loadingIndicator.visibility = View.GONE
			}
			return
		}

		// Create the backup object in the format expected by hydrateMapData
		// { type: "binary", main: <array of bytes> }
		val mainArray = JSONArray()
		for (byte in zipBuffer) {
			mainArray.put(byte.toInt() and 0xFF)
		}
		val backupObject =
			JSONObject().apply {
				put("type", "binary")
				put("main", mainArray)
			}

		// Hydrate the map data from the local MVF file
		mapView.hydrateMapData(backupObject) { result ->
			result
				.onSuccess {
					Log.d("OfflineModeDemo", "hydrateMapData success")
					// Display the map
					mapView.show3dMap(Show3DMapOptions()) { r ->
						r.onSuccess {
							runOnUiThread {
								loadingIndicator.visibility = View.GONE
							}
							onMapReady(mapView)
						}
						r.onFailure { error ->
							runOnUiThread {
								loadingIndicator.visibility = View.GONE
							}
							Log.e("OfflineModeDemo", "show3dMap error: $error")
						}
					}
				}.onFailure { error ->
					runOnUiThread {
						loadingIndicator.visibility = View.GONE
					}
					Log.e("OfflineModeDemo", "hydrateMapData error: $error")
				}
		}
	}

	private fun onMapReady(mapView: MapView) {
		Log.d("OfflineModeDemo", "show3dMap success - Map displayed from offline MVF")

		// Add labels to all named spaces to demonstrate the map is fully functional
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				spaces.filter { it.name.isNotEmpty() }.forEach { space ->
					mapView.labels.add(space, space.name, AddLabelOptions(interactive = true)) { }
				}
			}
		}
	}

	private fun loadMvfFromAssets(fileName: String): ByteArray? =
		try {
			assets.open(fileName).use { inputStream ->
				inputStream.readBytes()
			}
		} catch (e: Exception) {
			Log.e("OfflineModeDemo", "Error loading MVF file: ${e.message}")
			null
		}
}
