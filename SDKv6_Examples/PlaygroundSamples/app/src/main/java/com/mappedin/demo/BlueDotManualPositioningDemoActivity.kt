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
import com.mappedin.models.BlueDotEvents
import com.mappedin.models.BlueDotOptions
import com.mappedin.models.ForcePositionTarget
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.ManualPositionOptions
import com.mappedin.models.Show3DMapOptions

/**
 * Demonstrates BlueDot manual positioning APIs.
 *
 * This demo showcases three ways to programmatically control the BlueDot position:
 *
 * 1. **forcePosition** — Overrides all sensor input and places the BlueDot at exact coordinates
 *    for a specified duration. Useful for VPS calibration or manual position correction.
 *
 * 2. **reportPosition** — Reports a position with a confidence score to the fusion engine.
 *    Unlike forcePosition, this is blended with other active sensors. Useful for integrating
 *    external positioning systems (IPS, beacons, WiFi RTT).
 *
 * 3. **enableSensor / disableSensor** — Manages individual sensors in the fusion engine.
 *    The "manual" sensor must be enabled before reportPosition updates are accepted.
 *
 * The demo also subscribes to anchor lifecycle events (AnchorSet, AnchorExpired) to show
 * when forced positions become active and when they expire.
 */
class BlueDotManualPositioningDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var statusLabel: TextView
	private lateinit var eventLogLabel: TextView
	private lateinit var sensorToggleButton: Button

	private var isManualSensorEnabled = false
	private var mapCenterLatitude: Double = 0.0
	private var mapCenterLongitude: Double = 0.0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Manual Positioning"

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
					val insets =
						windowInsets.getInsets(
							WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
						)
					view.setPadding(dp(16), insets.top + dp(12), dp(16), dp(8))
					windowInsets
				}
			}

		statusLabel =
			TextView(this).apply {
				text = "Loading map..."
				textSize = 16f
				setTextColor(Color.DKGRAY)
				gravity = Gravity.CENTER
			}
		headerLayout.addView(statusLabel)

		eventLogLabel =
			TextView(this).apply {
				text = "Event: --"
				textSize = 12f
				setTextColor(Color.parseColor("#2266ff"))
				gravity = Gravity.START
				isSingleLine = false
				maxLines = 3
			}
		headerLayout.addView(eventLogLabel)

		mainLayout.addView(headerLayout)

		// Row 1: Force Position and Report Position buttons
		val positionRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(8), dp(16), dp(8))
			}

		val forceButton = createButton("Force Position") { forcePosition() }
		val reportButton = createButton("Report Position") { reportPosition() }

		positionRow.addView(
			forceButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				marginEnd = dp(8)
			},
		)
		positionRow.addView(
			reportButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
		)
		mainLayout.addView(positionRow)

		// Row 2: Manual sensor toggle button
		val sensorRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(4), dp(16), dp(8))
			}

		sensorToggleButton = createButton("Enable Manual Sensor") { toggleManualSensor() }
		sensorRow.addView(
			sensorToggleButton,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
		)
		mainLayout.addView(sensorRow)

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

	// See Demo API Key Terms and Conditions
	// https://developer.mappedin.com/docs/demo-keys-and-maps
	private fun loadMap() {
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "64ef49e662fd90fe020bee61",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d(TAG, "getMapData success")
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
							Log.e(TAG, "show3dMap error: $error")
						}
					}
				}.onFailure { error ->
					runOnUiThread {
						loadingIndicator.visibility = android.view.View.GONE
						statusLabel.text = "Error: ${error.message}"
					}
					Log.e(TAG, "getMapData error: $error")
				}
		}
	}

	/**
	 * Called when the map is rendered and ready for interaction.
	 * Enables the BlueDot and subscribes to anchor lifecycle events.
	 */
	private fun onMapReady() {
		runOnUiThread {
			statusLabel.text = "Map loaded — enabling BlueDot..."
		}

		mapView.mapData.mapCenter { result ->
			result.onSuccess { center ->
				if (center != null) {
					mapCenterLatitude = center.latitude
					mapCenterLongitude = center.longitude
					Log.d(TAG, "Map center: ${center.latitude}, ${center.longitude}")
				}
			}
		}

		enableBlueDot()
		subscribeToAnchorEvents()
	}

	/**
	 * Enables the BlueDot with manual positioning configuration.
	 *
	 * Setting [BlueDotOptions.watchDevicePosition] to false prevents the BlueDot from using
	 * the device's GPS, allowing full manual control over position updates.
	 */
	private fun enableBlueDot() {
		val options =
			BlueDotOptions(
				color = "#2266ff",
				radius = 12.0,
				watchDevicePosition = false,
				initialState = BlueDotOptions.InitialState.INACTIVE,
			)

		mapView.blueDot.enable(options) { result ->
			result.fold(
				onSuccess = {
					runOnUiThread {
						statusLabel.text = "BlueDot enabled — use buttons above"
					}
					Log.d(TAG, "BlueDot enabled")
				},
				onFailure = { error ->
					runOnUiThread {
						statusLabel.text = "Error enabling BlueDot: ${error.message}"
					}
					Log.e(TAG, "BlueDot enable error: $error")
				},
			)
		}
	}

	/**
	 * Subscribes to anchor lifecycle events.
	 *
	 * - **AnchorSet** fires when [BlueDot.forcePosition] creates a new calibration anchor.
	 * - **AnchorExpired** fires when that anchor's TTL expires and it is removed.
	 *
	 * These events let you track when forced positions are active and when they expire.
	 */
	private fun subscribeToAnchorEvents() {
		mapView.blueDot.on(BlueDotEvents.AnchorSet) { payload ->
			payload?.let {
				val msg =
					"Anchor set — lat: ${String.format("%.5f", it.anchor.latitude)}, " +
						"lng: ${String.format("%.5f", it.anchor.longitude)}"
				Log.d(TAG, msg)
				runOnUiThread {
					eventLogLabel.text = "Event: $msg"
				}
			}
		}

		mapView.blueDot.on(BlueDotEvents.AnchorExpired) { payload ->
			payload?.let {
				val msg =
					"Anchor expired — lat: ${String.format("%.5f", it.anchor.latitude)}, " +
						"lng: ${String.format("%.5f", it.anchor.longitude)}"
				Log.d(TAG, msg)
				runOnUiThread {
					eventLogLabel.text = "Event: $msg"
				}
			}
		}
	}

	/**
	 * Forces the BlueDot to a hardcoded position for 30 seconds.
	 *
	 * [BlueDot.forcePosition] places a high-confidence anchor that overrides all other sensors
	 * for the specified duration. After the duration elapses, the anchor expires and an
	 * [BlueDotEvents.AnchorExpired] event is emitted.
	 */
	private fun forcePosition() {
		val target =
			ForcePositionTarget(
				latitude = mapCenterLatitude,
				longitude = mapCenterLongitude,
				heading = 90.0,
				floorLevel = null,
			)

		mapView.blueDot.forcePosition(target, durationMs = 30_000) { result ->
			result.fold(
				onSuccess = {
					runOnUiThread {
						statusLabel.text = "Position forced for 30s"
					}
					Log.d(TAG, "forcePosition success")
				},
				onFailure = { error ->
					runOnUiThread {
						statusLabel.text = "forcePosition error: ${error.message}"
					}
					Log.e(TAG, "forcePosition error: $error")
				},
			)
		}
	}

	/**
	 * Reports a confidence-weighted position to the BlueDot fusion engine.
	 *
	 * [BlueDot.reportPosition] does not override other sensors. Instead, the position is
	 * blended into the fused result based on the [ManualPositionOptions.confidence] value.
	 * A confidence of 0.8 means this reading carries significant weight.
	 *
	 * The "manual" sensor must be enabled via [BlueDot.enableSensor] before
	 * reportPosition updates are accepted by the fusion engine.
	 */
	private fun reportPosition() {
		val options =
			ManualPositionOptions(
				latitude = mapCenterLatitude,
				longitude = mapCenterLongitude,
				confidence = 0.8,
			)

		mapView.blueDot.reportPosition(options) { result ->
			result.fold(
				onSuccess = {
					runOnUiThread {
						statusLabel.text = "Position reported (confidence: 0.8)"
					}
					Log.d(TAG, "reportPosition success")
				},
				onFailure = { error ->
					runOnUiThread {
						statusLabel.text = "reportPosition error: ${error.message}"
					}
					Log.e(TAG, "reportPosition error: $error")
				},
			)
		}
	}

	/**
	 * Toggles the "manual" sensor on or off.
	 *
	 * Enabling the manual sensor allows [BlueDot.reportPosition] updates to be accepted
	 * by the fusion engine. Disabling it stops the manual sensor from contributing to
	 * the fused position.
	 */
	private fun toggleManualSensor() {
		if (isManualSensorEnabled) {
			mapView.blueDot.disableSensor("manual") { result ->
				result.fold(
					onSuccess = {
						isManualSensorEnabled = false
						runOnUiThread {
							sensorToggleButton.text = "Enable Manual Sensor"
							statusLabel.text = "Manual sensor disabled"
						}
						Log.d(TAG, "Manual sensor disabled")
					},
					onFailure = { error ->
						runOnUiThread {
							statusLabel.text = "disableSensor error: ${error.message}"
						}
						Log.e(TAG, "disableSensor error: $error")
					},
				)
			}
		} else {
			mapView.blueDot.enableSensor("manual") { result ->
				result.fold(
					onSuccess = { state ->
						isManualSensorEnabled = true
						runOnUiThread {
							sensorToggleButton.text = "Disable Manual Sensor"
							statusLabel.text = "Manual sensor enabled (${state.value})"
						}
						Log.d(TAG, "Manual sensor enabled: ${state.value}")
					},
					onFailure = { error ->
						runOnUiThread {
							statusLabel.text = "enableSensor error: ${error.message}"
						}
						Log.e(TAG, "enableSensor error: $error")
					},
				)
			}
		}
	}

	companion object {
		private const val TAG = "ManualPositioningDemo"
	}
}
