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
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

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

		// Use MapView.getAssetUrl to generate a URL the WebView can fetch
		// This is more efficient than reading bytes and passing them through hydrateMapData
		val mvfUrl = MapView.getAssetUrl("school-demo-multifloor-mvfv3.zip")

		// Hydrate the map data from the local MVF file URL
		mapView.hydrateMapDataFromURL(mvfUrl) { result ->
			result
				.onSuccess {
					Log.d("OfflineModeDemo", "hydrateMapDataFromURL success")
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
					Log.e("OfflineModeDemo", "hydrateMapDataFromURL error: $error")
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
}
