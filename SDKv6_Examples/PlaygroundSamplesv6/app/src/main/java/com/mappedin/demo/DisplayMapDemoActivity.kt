package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.mappedin.MapView
import com.mappedin.models.Annotation
import com.mappedin.models.Area
import com.mappedin.models.Connection
import com.mappedin.models.Door
import com.mappedin.models.EnterpriseCategory
import com.mappedin.models.EnterpriseLocation
import com.mappedin.models.EnterpriseVenue
import com.mappedin.models.Facade
import com.mappedin.models.Floor
import com.mappedin.models.FloorStack
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.LocationCategory
import com.mappedin.models.LocationProfile
import com.mappedin.models.MapDataType
import com.mappedin.models.MapObject
import com.mappedin.models.Node
import com.mappedin.models.PointOfInterest
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import kotlin.math.floor

class DisplayMapDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Display a Map"

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
		val loadingParams = FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		loadingParams.gravity = Gravity.CENTER
		container.addView(loadingIndicator, loadingParams)

		setContentView(container)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-mall",
			)

		// Load the map data.
		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("MappedinDemo", "getMapData success")
				// Display the map.
				mapView.show3dMap(Show3DMapOptions()) { r ->
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

	// Place your code to be called when the map is ready here.
	private fun onMapReady(mapView: MapView) {
		Log.d("MappedinDemo", "show3dMap success - Map displayed")
	}
}

