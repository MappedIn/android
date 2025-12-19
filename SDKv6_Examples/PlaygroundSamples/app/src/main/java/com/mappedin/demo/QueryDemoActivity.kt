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
import com.mappedin.models.*
import java.util.Locale

class QueryDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private var highlightedSpace: Space? = null
	private var originalColor: String? = null
	private lateinit var instructionText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Query"

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
						dp(12),
					)
					insets
				}
			}
		val titleView =
			TextView(this).apply {
				text = "Query"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Click on the map to find and highlight the nearest space."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		instructionText =
			TextView(this).apply {
				text = "Click anywhere on the map to start."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#374151".toColorInt())
				setPadding(0, dp(8), 0, 0)
			}
		header.addView(titleView)
		header.addView(descriptionView)
		header.addView(instructionText)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
				mapId = "660c0bb9ae0596d87766f2d9",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { r ->
					r.onSuccess {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
						onMapReady()
					}
					r.onFailure {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
					}
				}
			}
		}
	}

	private fun onMapReady() {
		// Handle click events
		mapView.on(Events.CLICK) { event ->
			val clickPayload = event as? ClickPayload ?: return@on
			val coordinate = clickPayload.coordinate

			Log.d("Query", "Click coordinate: lat=${coordinate.latitude}, lon=${coordinate.longitude}, floor=${coordinate.floorId}")

			// Reset previously highlighted space to its original color
			highlightedSpace?.let { space ->
				originalColor?.let { color ->
					mapView.updateState(space, GeometryUpdateState(color = color)) { }
				}
			}

			// Find the nearest space to the clicked coordinate
			Log.d("Query", "Calling nearest with coordinate")
			mapView.mapData.query.nearest(coordinate, listOf(MapDataType.SPACE)) { result ->
				Log.d("Query", "nearest callback received: success=${result.isSuccess}, failure=${result.isFailure}")
				result.onSuccess { queryResults ->
					Log.d("Query", "queryResults size: ${queryResults?.size}")
					val nearestResult = queryResults?.firstOrNull()
					when (val feature = nearestResult?.feature) {
						is FindNearestResult.Feature.SpaceFeature -> {
							val space = feature.space
							Log.d("Query", "Nearest space: ${space.name} at ${nearestResult.distance}m")

							// Get the current color of the space before highlighting
							mapView.getState(space) { stateResult ->
								stateResult.onSuccess { state ->
									originalColor = state?.color

									// Highlight the space with a new color
									mapView.updateState(space, GeometryUpdateState(color = "#FF6B35")) { }

									// Update the highlighted space reference
									highlightedSpace = space

									// Update instruction text
									instructionText.text = "Highlighted: ${space.name} (${String.format(Locale.getDefault(), "%.1f", nearestResult.distance)}m away)"
								}
							}
						}
						else -> {
							Log.d("Query", "No space feature found")
							instructionText.text = "No space found near click location."
						}
					}
				}
				result.onFailure { error ->
					Log.e("Query", "nearest failed: ${error.message}", error)
					instructionText.text = "Error: ${error.message}"
				}
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
