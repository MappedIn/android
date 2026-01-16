package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.mappedin.MapView
import com.mappedin.models.Doors
import com.mappedin.models.DoorsUpdateState
import com.mappedin.models.GeometryUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.MapObject
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import com.mappedin.models.Walls
import com.mappedin.models.WallsTexture
import com.mappedin.models.WallsUpdateState

class ColorsAndTexturesDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Colors & Textures"

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
				mapId = "64ef49e662fd90fe020bee61",
			)

		// Load the map data.
		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("MappedinDemo", "getMapData success")
					// Display the map with outdoor view and shadingAndOutlines disabled.
					val show3dOptions =
						Show3DMapOptions(
							outdoorView =
								Show3DMapOptions.OutdoorViewOptions(
									style = "https://tiles-cdn.mappedin.com/styles/midnightblue/style.json",
								),
							shadingAndOutlines = false,
						)
					mapView.show3dMap(show3dOptions) { r ->
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
					Log.e("MappedinDemo", "getMapData error: $it")
				}
		}
	}

	// Apply textures and colors when the map is ready.
	private fun onMapReady(mapView: MapView) {
		Log.d("MappedinDemo", "show3dMap success - Applying textures")

		// Get local asset URLs using the Android WebView asset URL scheme
		val exteriorWallURL = "https://appassets.androidplatform.net/assets/exterior-wall.jpg"
		val floorURL = "https://appassets.androidplatform.net/assets/floor.png"
		val objectSideURL = "https://appassets.androidplatform.net/assets/object-side.jpg"

		// Make interior doors visible, sides brown and top yellow
		mapView.updateState(
			Doors.INTERIOR,
			DoorsUpdateState(
				visible = true,
				color = "brown",
				topColor = "yellow",
				opacity = 0.6,
			),
		)

		// Make exterior doors visible, sides black and top blue
		mapView.updateState(
			Doors.EXTERIOR,
			DoorsUpdateState(
				visible = true,
				color = "black",
				topColor = "blue",
				opacity = 0.6,
			),
		)

		// Update all spaces with floor texture
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				for (space in spaces) {
					mapView.updateState(
						space,
						GeometryUpdateState(
							topTexture = GeometryUpdateState.Texture(url = floorURL),
						),
					)
				}
			}
			result.onFailure { error ->
				Log.e("MappedinDemo", "Error getting spaces: $error")
			}
		}

		// Update all objects with side texture and top color
		mapView.mapData.getByType<MapObject>(MapDataType.MAP_OBJECT) { result ->
			result.onSuccess { objects ->
				for (obj in objects) {
					mapView.updateState(
						obj,
						GeometryUpdateState(
							texture = GeometryUpdateState.Texture(url = objectSideURL),
							topColor = "#9DB2BF",
						),
					)
				}
			}
			result.onFailure { error ->
				Log.e("MappedinDemo", "Error getting objects: $error")
			}
		}

		// Update interior walls with colors
		mapView.updateState(
			Walls.INTERIOR,
			WallsUpdateState(
				color = "#526D82",
				topColor = "#27374D",
			),
		)

		// Update exterior walls with textures
		mapView.updateState(
			Walls.EXTERIOR,
			WallsUpdateState(
				texture = WallsTexture(url = exteriorWallURL),
				topTexture = WallsTexture(url = exteriorWallURL),
			),
		)
	}
}
