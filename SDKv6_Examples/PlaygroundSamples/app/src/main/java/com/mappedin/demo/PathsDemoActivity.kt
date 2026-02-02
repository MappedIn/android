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
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddPathOptions
import com.mappedin.models.Events
import com.mappedin.models.GeometryUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.NavigationTarget
import com.mappedin.models.Path
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import com.mappedin.models.Width

class PathsDemoActivity : AppCompatActivity() {
	private var startSpace: Space? = null
	private var path: Path? = null
	private lateinit var instructionText: TextView
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
						dp(12),
					)
					insets
				}
			}
		val titleView =
			TextView(this).apply {
				text = "Paths"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Interactive path drawing between spaces using directions."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		instructionText =
			TextView(this).apply {
				text = "1. Click on a space to select it as the starting point."
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
				mapId = "65c0ff7430b94e3fabd5bb8c",
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
			result.onFailure {
				runOnUiThread {
					loadingIndicator.visibility = View.GONE
				}
				android.util.Log.e("PathsDemoActivity", "getMapData error: $it")
			}
		}
	}

	private fun onMapReady(mapView: MapView) {
		// Set all spaces to be interactive.
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				spaces.forEach { space ->
					mapView.updateState(space, GeometryUpdateState(interactive = true)) { }
				}

				// Handle click events
				mapView.on(Events.Click) { clickPayload ->
					clickPayload ?: return@on
					val spaces = clickPayload.spaces

					if (spaces == null || spaces.isEmpty()) {
						// Click on non-space area when path exists - reset
						if (path != null) {
							mapView.paths.removeAll()
							startSpace = null
							path = null
							setSpacesInteractive(mapView, true)
							instructionText.text = "1. Click on a space to select it as the starting point."
						}
						return@on
					}

					val clickedSpace = spaces[0]

					when {
						startSpace == null -> {
							// Step 1: Select starting space
							startSpace = clickedSpace
							instructionText.text = "2. Click on another space to select it as the end point."
						}
						path == null -> {
							// Step 2: Select ending space and create path
							val start = startSpace ?: return@on
							mapView.mapData.getDirections(
								NavigationTarget.SpaceTarget(start),
								NavigationTarget.SpaceTarget(clickedSpace),
							) { result ->
								result.onSuccess { directions ->
									if (directions != null) {
										val opts =
											AddPathOptions(
												width = Width.Value(1.0),
												color = "#1871fb",
											)
										mapView.paths.add(directions.coordinates, opts) { pathResult ->
											pathResult.onSuccess { createdPath ->
												path = createdPath
												setSpacesInteractive(mapView, false)
												instructionText.text = "3. Click anywhere to remove the path."
											}
										}
									}
								}
							}
						}
						else -> {
							// Step 3: Remove path and reset
							mapView.paths.removeAll()
							startSpace = null
							path = null
							setSpacesInteractive(mapView, true)
							instructionText.text = "1. Click on a space to select it as the starting point."
						}
					}
				}
			}
		}
	}

	private fun setSpacesInteractive(
		mapView: MapView,
		interactive: Boolean,
	) {
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				spaces.forEach { space ->
					mapView.updateState(space, GeometryUpdateState(interactive = interactive)) { }
				}
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
