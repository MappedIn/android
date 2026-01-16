package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.mappedin.MapView
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.Show3DMapOptions

class StackedMapsDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	private var animate = true
	private var distanceBetweenFloors = 25.0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Stacked Maps"

		// Create a FrameLayout to hold both the map view and controls
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

		// Add control panel
		val controlPanel = createControlPanel()
		val controlParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		controlParams.gravity = Gravity.TOP or Gravity.START
		// Account for action bar height to prevent overlap
		val typedValue = TypedValue()
		var actionBarHeight = 0
		if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
			actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
		}
		controlParams.setMargins(32, actionBarHeight + 32, 32, 32)
		container.addView(controlPanel, controlParams)

		setContentView(container)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "666ca6a48dd908000bf47803",
			)

		// Load the map data.
		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("MappedinDemo", "getMapData success")

					// Display the map with higher pitch for better stacked view.
					val show3dMapOptions =
						Show3DMapOptions(
							pitch = 80.0,
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
					Log.e("MappedinDemo", "getMapData error: $it")
				}
		}
	}

	private fun createControlPanel(): CardView {
		val card = CardView(this)
		card.radius = 16f
		card.cardElevation = 8f
		card.setCardBackgroundColor(0xFFFFFFFF.toInt())

		val layout = LinearLayout(this)
		layout.orientation = LinearLayout.VERTICAL
		layout.setPadding(32, 32, 32, 32)

		// Expand button
		val expandButton =
			Button(this).apply {
				text = "Expand"
				setOnClickListener {
					StackedMapsUtils.expandFloors(
						mapView,
						ExpandOptions(
							distanceBetweenFloors = distanceBetweenFloors,
							animate = animate,
						),
					)
				}
			}
		layout.addView(expandButton)

		// Collapse button
		val collapseButton =
			Button(this).apply {
				text = "Collapse"
				setOnClickListener {
					StackedMapsUtils.collapseFloors(
						mapView,
						CollapseOptions(
							animate = animate,
						),
					)
				}
			}
		layout.addView(collapseButton)

		// Animate checkbox
		val animateCheckbox =
			CheckBox(this).apply {
				text = "Animate"
				isChecked = animate
				setOnCheckedChangeListener { _, isChecked ->
					animate = isChecked
				}
			}
		layout.addView(animateCheckbox)

		// Floor gap label
		val floorGapLabel =
			TextView(this).apply {
				text = "Floor Gap:"
				setPadding(0, 24, 0, 8)
			}
		layout.addView(floorGapLabel)

		// Floor gap value display
		val gapValueDisplay =
			TextView(this).apply {
				text = "${distanceBetweenFloors.toInt()}m"
			}

		// Floor gap slider
		val floorGapSlider =
			SeekBar(this).apply {
				max = 50
				progress = distanceBetweenFloors.toInt()
				setOnSeekBarChangeListener(
					object : SeekBar.OnSeekBarChangeListener {
						override fun onProgressChanged(
							seekBar: SeekBar?,
							progress: Int,
							fromUser: Boolean,
						) {
							distanceBetweenFloors = progress.toDouble()
							gapValueDisplay.text = "${progress}m"

							// Automatically expand floors with new gap value
							if (fromUser) {
								StackedMapsUtils.expandFloors(
									mapView,
									ExpandOptions(
										distanceBetweenFloors = distanceBetweenFloors,
										animate = animate,
									),
								)
							}
						}

						override fun onStartTrackingTouch(seekBar: SeekBar?) {}

						override fun onStopTrackingTouch(seekBar: SeekBar?) {}
					},
				)
			}
		layout.addView(floorGapSlider)
		layout.addView(gapValueDisplay)

		card.addView(layout)
		return card
	}

	// Place your code to be called when the map is ready here.
	private fun onMapReady(mapView: MapView) {
		Log.d("MappedinDemo", "show3dMap success - Map displayed")

		// Hide the outdoor map and configure camera for stacked view.
		mapView.outdoor.hide()
		mapView.camera.setMaxPitch(88.0)
		mapView.camera.set(
			com.mappedin.models.CameraTarget(
				pitch = 75.0,
			),
		)

		// Expand floors.
		StackedMapsUtils.expandFloors(
			mapView,
			ExpandOptions(
				distanceBetweenFloors = distanceBetweenFloors,
				animate = animate,
			),
		)
	}
}
