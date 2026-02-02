package com.mappedin.demo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.DynamicFocusOptions
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.Show3DMapOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Demonstrates the Dynamic Focus extension for automatic outdoor/indoor scene management.
 *
 * This demo uses the DynamicFocus extension directly with autoFocus enabled.
 * Zoom in and move the map around to observe Dynamic Focus auto focus behaviour.
 */
class DynamicFocusDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var statusLabel: TextView
	private lateinit var stateLabel: TextView
	private lateinit var eventLogLabel: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Dynamic Focus"

		val density = resources.displayMetrics.density

		fun dp(value: Int): Int = (value * density).toInt()

		val mainLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
			}
		setContentView(mainLayout)

		// Header section with window insets handling
		val headerLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(16), dp(12), dp(16), dp(8))
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
					val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
					view.setPadding(dp(16), insets.top + dp(12), dp(16), dp(8))
					windowInsets
				}
			}

		// Status label
		statusLabel =
			TextView(this).apply {
				text = "Loading map..."
				textSize = 16f
				setTextColor(Color.DKGRAY)
				gravity = Gravity.CENTER
			}
		headerLayout.addView(statusLabel)

		// State label for live getter values
		stateLabel =
			TextView(this).apply {
				text = "State: --"
				textSize = 12f
				setTextColor(Color.DKGRAY)
				gravity = Gravity.START
				isSingleLine = false
				maxLines = 3
			}
		headerLayout.addView(stateLabel)

		// Event log label to display DynamicFocus events
		eventLogLabel =
			TextView(this).apply {
				text = "Event: --"
				textSize = 12f
				setTextColor(Color.parseColor("#2266ff"))
				gravity = Gravity.START
				isSingleLine = false
				maxLines = 2
			}
		headerLayout.addView(eventLogLabel)

		mainLayout.addView(headerLayout)

		// Enable/Disable buttons
		val buttonRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(8), dp(16), dp(8))
			}

		val enableButton = createButton("Enable") { enableDynamicFocus() }
		val disableButton = createButton("Disable") { disableDynamicFocus() }
		buttonRow.addView(
			enableButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				marginEnd = dp(8)
			},
		)
		buttonRow.addView(disableButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		mainLayout.addView(buttonRow)

		// Map container
		mapView = MapView(this)
		val mapFrame =
			FrameLayout(this).apply {
				layoutParams =
					LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						0,
						1f,
					)
			}
		mapFrame.addView(
			mapView.view,
			ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)

		// Loading indicator centered in map
		loadingIndicator = ProgressBar(this)
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		loadingParams.gravity = Gravity.CENTER
		mapFrame.addView(loadingIndicator, loadingParams)

		mainLayout.addView(mapFrame)

		loadMap()
	}

	private fun createButton(
		text: String,
		onClick: () -> Unit,
	): Button =
		Button(this).apply {
			this.text = text
			textSize = 14f
			setBackgroundColor(Color.parseColor("#E0E0E0"))
			setOnClickListener { onClick() }
		}

	private fun loadMap() {
		// Trial API key - see https://developer.mappedin.com/docs/demo-keys-and-maps
		// Using the outdoor/indoor map for Dynamic Focus demo
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "682e13a2703478000b567b66",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("DynamicFocusDemo", "getMapData success")
					mapView.show3dMap(Show3DMapOptions()) { mapResult ->
						mapResult.onSuccess {
							runOnUiThread {
								loadingIndicator.visibility = android.view.View.GONE
							}
							onMapReady()
						}
						mapResult.onFailure { error ->
							runOnUiThread {
								loadingIndicator.visibility = android.view.View.GONE
								statusLabel.text = "Error: ${error.message}"
							}
							Log.e("DynamicFocusDemo", "show3dMap error: $error")
						}
					}
				}.onFailure { error ->
					runOnUiThread {
						loadingIndicator.visibility = android.view.View.GONE
						statusLabel.text = "Error: ${error.message}"
					}
					Log.e("DynamicFocusDemo", "getMapData error: $error")
				}
		}
	}

	private fun onMapReady() {
		runOnUiThread {
			statusLabel.text = "Map loaded - tap Enable to start Dynamic Focus"
		}

		// Label all spaces with names
		mapView.__EXPERIMENTAL__auto()
	}

	private fun logEvent(message: String) {
		val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
		val timestamp = timeFormat.format(Date())
		runOnUiThread {
			eventLogLabel.text = "Event [$timestamp]: $message"
		}
		Log.d("DynamicFocusDemo", "[DynamicFocus Event] $message")
	}

	private fun enableDynamicFocus() {
		val options =
			DynamicFocusOptions(
				autoFocus = true,
				setFloorOnFocus = true,
				indoorZoomThreshold = 17.0,
				outdoorZoomThreshold = 17.0,
			)

		mapView.dynamicFocus.enable(options) { result ->
			result.fold(
				onSuccess = {
					runOnUiThread {
						statusLabel.text = "Dynamic Focus enabled - zoom and pan to see auto focus"
					}
				},
				onFailure = { error ->
					runOnUiThread {
						statusLabel.text = "Error enabling: ${error.message}"
					}
				},
			)
			refreshStateDisplay()
		}
	}

	private fun disableDynamicFocus() {
		mapView.dynamicFocus.disable { _ ->
			runOnUiThread {
				statusLabel.text = "Dynamic Focus disabled"
			}
			refreshStateDisplay()
		}
	}

	private fun refreshStateDisplay() {
		mapView.dynamicFocus.getState { result ->
			result.fold(
				onSuccess = { state ->
					runOnUiThread {
						if (state != null) {
							val text =
								listOf(
									"autoFocus: ${state.autoFocus}",
									"mode: ${state.mode.value}",
								).joinToString(" | ")
							stateLabel.text = text
						} else {
							stateLabel.text = "State: not available"
						}
					}
				},
				onFailure = {
					runOnUiThread {
						stateLabel.text = "State: error"
					}
				},
			)
		}
	}
}
