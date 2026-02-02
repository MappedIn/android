package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.mappedin.MapView
import com.mappedin.models.Floor
import com.mappedin.models.FloorUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions

class MultiFloorViewDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Multi-Floor View"

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

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "67a6641530e940000bac3c1a",
			)

		// Load the map data.
		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("MappedinDemo", "getMapData success")

					// Display the map with multi-floor view enabled.
					val show3dMapOptions =
						Show3DMapOptions(
							multiFloorView =
								Show3DMapOptions.MultiFloorViewOptions(
									enabled = true,
									floorGap = 10.0,
									updateCameraElevationOnFloorChange = true,
								),
						)

					mapView.show3dMap(show3dMapOptions) { r ->
						r.onSuccess {
							runOnUiThread {
								loadingIndicator.visibility = android.view.View.GONE
							}
							onMapReady(mapView)
						}
						r.onFailure {
							runOnUiThread {
								loadingIndicator.visibility = android.view.View.GONE
							}
							Log.e("MappedinDemo", "show3dMap error: $it")
						}
					}
				}.onFailure {
					runOnUiThread {
						loadingIndicator.visibility = android.view.View.GONE
					}
					Log.e("MappedinDemo", "getMapData error: $it")
				}
		}
	}

	// Place your code to be called when the map is ready here.
	private fun onMapReady(mapView: MapView) {
		Log.d("MappedinDemo", "show3dMap success - Map displayed with multi-floor view")

		// Get all floors and find the ones needed.
		mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { result ->
			result.onSuccess { floors ->
				// Set the current floor to the one with elevation 9.
				val floor9 = floors.find { it.elevation == 9.0 }
				if (floor9 != null) {
					mapView.setFloor(floor9.id) {
						Log.d("MappedinDemo", "Set floor to elevation 9: ${floor9.name}")
					}
				}

				// Show the 6th floor (elevation 6) as well.
				val floor6 = floors.find { it.elevation == 6.0 }
				if (floor6 != null) {
					mapView.updateState(
						floor6,
						FloorUpdateState(
							geometry = FloorUpdateState.Geometry(visible = true),
						),
					) {
						Log.d("MappedinDemo", "Made floor with elevation 6 visible: ${floor6.name}")
					}
				}
			}
			result.onFailure {
				Log.e("MappedinDemo", "Failed to get floors: $it")
			}
		}
	}
}
