package com.mappedin.demo

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
import android.widget.RadioButton
import android.widget.RadioGroup
import com.mappedin.MapView
import com.mappedin.models.Events
import com.mappedin.models.FindNearestResult
import com.mappedin.models.GeometryUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.QueryAtResult
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import java.util.Locale

class QueryDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private var highlightedSpace: Space? = null
	private var originalColor: String? = null
	private lateinit var instructionText: TextView
	private var useAtQuery = false

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
				text = "Click on the map to query. Use the toggle to switch between nearest and at."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		val queryModeGroup =
			RadioGroup(this).apply {
				orientation = RadioGroup.HORIZONTAL
				setPadding(0, dp(8), 0, 0)
			}
		val nearestRadio =
			RadioButton(this).apply {
				text = "Nearest"
				id = View.generateViewId()
				setPadding(dp(16), dp(8), dp(16), dp(8))
			}
		val atRadio =
			RadioButton(this).apply {
				text = "At"
				id = View.generateViewId()
				setPadding(dp(16), dp(8), dp(16), dp(8))
			}
		queryModeGroup.addView(nearestRadio)
		queryModeGroup.addView(atRadio)
		queryModeGroup.check(nearestRadio.id)
		queryModeGroup.setOnCheckedChangeListener { _, checkedId ->
			useAtQuery = checkedId == atRadio.id
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
		header.addView(queryModeGroup)
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
			result.onFailure {
				runOnUiThread {
					loadingIndicator.visibility = View.GONE
				}
				android.util.Log.e("QueryDemoActivity", "getMapData error: $it")
			}
		}
	}

	private fun onMapReady() {
		// Handle click events
		mapView.on(Events.Click) { clickPayload ->
			clickPayload ?: return@on
			val coordinate = clickPayload.coordinate

			Log.d("Query", "Click coordinate: lat=${coordinate.latitude}, lon=${coordinate.longitude}, floor=${coordinate.floorId}")

			// Reset previously highlighted space to its original color
			highlightedSpace?.let { space ->
				originalColor?.let { color ->
					mapView.updateState(space, GeometryUpdateState(color = color)) { }
				}
			}

			if (useAtQuery) {
				// Query.at: find all geometry at the clicked coordinate
				Log.d("Query", "Calling at with coordinate")
				mapView.mapData.query.at(coordinate) { result ->
					Log.d("Query", "at callback received: success=${result.isSuccess}, failure=${result.isFailure}")
					result.onSuccess { atResults ->
						Log.d("Query", "atResults size: ${atResults?.size}")
						val firstSpace =
							atResults?.firstOrNull { it is QueryAtResult.SpaceResult } as? QueryAtResult.SpaceResult
						if (firstSpace != null) {
							val space = firstSpace.space
							Log.d("Query", "Space at point: ${space.name} (${atResults.size} geometry at point)")

							mapView.getState(space) { stateResult ->
								stateResult.onSuccess { state ->
									originalColor = state?.color
									mapView.updateState(space, GeometryUpdateState(color = "#FF6B35")) { }
									highlightedSpace = space
									runOnUiThread {
										instructionText.text = "Highlighted: ${space.name} (${atResults?.size ?: 0} geometry at point)"
									}
								}
							}
						} else {
							Log.d("Query", "No space at click location")
							runOnUiThread {
								instructionText.text = "No space at click. ${atResults?.size ?: 0} geometry found."
							}
						}
					}
					result.onFailure { error ->
						Log.e("Query", "at failed: ${error.message}", error)
						runOnUiThread {
							instructionText.text = "Error: ${error.message}"
						}
					}
				}
			} else {
				// Query.nearest: find the nearest space to the clicked coordinate
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

								mapView.getState(space) { stateResult ->
									stateResult.onSuccess { state ->
										originalColor = state?.color
										mapView.updateState(space, GeometryUpdateState(color = "#FF6B35")) { }
										highlightedSpace = space
										runOnUiThread {
											instructionText.text = "Highlighted: ${space.name} (${String.format(Locale.getDefault(), "%.1f", nearestResult.distance)}m away)"
										}
									}
								}
							}
							else -> {
								Log.d("Query", "No space feature found")
								runOnUiThread {
									instructionText.text = "No space found near click location."
								}
							}
						}
					}
					result.onFailure { error ->
						Log.e("Query", "nearest failed: ${error.message}", error)
						runOnUiThread {
							instructionText.text = "Error: ${error.message}"
						}
					}
				}
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
