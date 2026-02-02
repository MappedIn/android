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
import com.mappedin.models.BlueDotPositionUpdate
import com.mappedin.models.BlueDotUpdateOptions
import com.mappedin.models.Coordinate
import com.mappedin.models.Events
import com.mappedin.models.Floor
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.Show3DMapOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Demonstrates the BlueDot indoor positioning visualization.
 */
class BlueDotDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var statusLabel: TextView
	private lateinit var stateLabel: TextView
	private lateinit var eventLogLabel: TextView

	private var currentHeading: Double = 0.0
	private var currentAccuracy: Double = 5.0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Blue Dot"

		// Helper to convert dp to pixels
		val density = resources.displayMetrics.density

		fun dp(value: Int): Int = (value * density).toInt()

		// Create vertical layout for controls and map
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
				// Handle window insets to avoid status bar, camera cutout, etc.
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

		// Event log label to display BlueDot events
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

		// Add header to main layout
		mainLayout.addView(headerLayout)

		// Enable/Disable buttons
		val buttonRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(8), dp(16), dp(8))
			}

		val enableButton = createButton("Enable") { enableBlueDot() }
		val disableButton = createButton("Disable") { disableBlueDot() }
		buttonRow.addView(
			enableButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				marginEnd = dp(8)
			},
		)
		buttonRow.addView(disableButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		mainLayout.addView(buttonRow)

		// Color buttons
		val colorRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(4), dp(16), dp(8))
			}

		val blueButton = createColorButton(Color.parseColor("#2266ff"), "#2266ff")
		val greenButton = createColorButton(Color.parseColor("#22cc44"), "#22cc44")
		val purpleButton = createColorButton(Color.parseColor("#9922cc"), "#9922cc")
		val orangeButton = createColorButton(Color.parseColor("#ff8800"), "#ff8800")

		colorRow.addView(blueButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(8) })
		colorRow.addView(greenButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(8) })
		colorRow.addView(purpleButton, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(8) })
		colorRow.addView(orangeButton, LinearLayout.LayoutParams(0, dp(40), 1f))
		mainLayout.addView(colorRow)

		// Heading buttons
		val headingRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(4), dp(16), dp(8))
			}

		listOf("0°" to 0.0, "90°" to 90.0, "180°" to 180.0, "270°" to 270.0).forEachIndexed { index, (label, heading) ->
			val button = createButton(label) { setHeading(heading) }
			headingRow.addView(
				button,
				LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
					if (index < 3) marginEnd = dp(8)
				},
			)
		}
		mainLayout.addView(headingRow)

		// Accuracy buttons
		val accuracyRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(16), dp(4), dp(16), dp(8))
			}

		listOf("5m" to 5.0, "10m" to 10.0, "25m" to 25.0, "50m" to 50.0).forEachIndexed { index, (label, accuracy) ->
			val button = createButton(label) { setAccuracy(accuracy) }
			accuracyRow.addView(
				button,
				LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
					if (index < 3) marginEnd = dp(8)
				},
			)
		}
		mainLayout.addView(accuracyRow)

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

	private fun createColorButton(
		color: Int,
		hexColor: String,
	): Button =
		Button(this).apply {
			setBackgroundColor(color)
			setOnClickListener { changeColor(hexColor) }
		}

	private fun loadMap() {
		// Trial API key - see https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "64ef49e662fd90fe020bee61",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("BlueDotDemo", "getMapData success")
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
							Log.e("BlueDotDemo", "show3dMap error: $error")
						}
					}
				}.onFailure { error ->
					runOnUiThread {
						loadingIndicator.visibility = android.view.View.GONE
						statusLabel.text = "Error: ${error.message}"
					}
					Log.e("BlueDotDemo", "getMapData error: $error")
				}
		}
	}

	private fun onMapReady() {
		runOnUiThread {
			statusLabel.text = "Map loaded - tap Enable to start"
		}
		setupClickHandler()
		setupBlueDotEventListeners()
	}

	private fun setupClickHandler() {
		mapView.on(Events.Click) { clickPayload ->
			clickPayload?.let { click ->
				moveBlueDot(click.coordinate, click.floors)
			}
		}
	}

	/**
	 * Sets up event listeners for BlueDot events to demonstrate the on() API.
	 */
	private fun setupBlueDotEventListeners() {
		// Listen for position updates
		mapView.blueDot.on(BlueDotEvents.PositionUpdate) { payload ->
			payload?.let {
				val floorName = it.floor?.name ?: "nil"
				val heading = it.heading?.let { h -> String.format("%.0f°", h) } ?: "nil"
				logEvent(
					"position-update: (${String.format(
						"%.4f",
						it.coordinate.latitude,
					)}, ${String.format("%.4f", it.coordinate.longitude)}) floor=$floorName heading=$heading",
				)
			}
		}

		// Listen for status changes
		mapView.blueDot.on(BlueDotEvents.StatusChange) { payload ->
			payload?.let {
				logEvent("status-change: ${it.status.value} (action: ${it.action.value})")
			}
		}

		// Listen for follow state changes
		mapView.blueDot.on(BlueDotEvents.FollowChange) { payload ->
			payload?.let {
				val mode = it.mode?.value ?: "none"
				logEvent("follow-change: following=${it.following} mode=$mode")
			}
		}

		// Listen for BlueDot clicks
		mapView.blueDot.on(BlueDotEvents.Click) { payload ->
			payload?.let {
				logEvent("click: (${String.format("%.4f", it.coordinate.latitude)}, ${String.format("%.4f", it.coordinate.longitude)})")
			}
		}

		// Listen for errors
		mapView.blueDot.on(BlueDotEvents.Error) { payload ->
			payload?.let {
				logEvent("error: [${it.code}] ${it.message}")
			}
		}
	}

	/**
	 * Logs an event to the event log label
	 */
	private fun logEvent(message: String) {
		val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
		val timestamp = timeFormat.format(Date())
		runOnUiThread {
			eventLogLabel.text = "Event [$timestamp]: $message"
		}
		Log.d("BlueDotDemo", "[BlueDot Event] $message")
	}

	private fun enableBlueDot() {
		val options =
			BlueDotOptions(
				accuracyRing = BlueDotOptions.AccuracyRing(color = "#2266ff", opacity = 0.25),
				color = "#2266ff",
				heading = BlueDotOptions.Heading(color = "#2266ff", opacity = 0.6),
				initialState = BlueDotOptions.InitialState.INACTIVE,
				radius = 12.0,
				watchDevicePosition = false,
			)

		mapView.blueDot.enable(options) { result ->
			result.fold(
				onSuccess = {
					runOnUiThread {
						statusLabel.text = "BlueDot enabled - tap map to place"
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

	private fun moveBlueDot(
		coordinate: Coordinate,
		floors: List<Floor>?,
	) {
		val floorId: BlueDotPositionUpdate.FloorId? =
			if (!floors.isNullOrEmpty()) {
				BlueDotPositionUpdate.FloorId.Id(floors.first().id)
			} else if (coordinate.floorId != null) {
				BlueDotPositionUpdate.FloorId.Id(coordinate.floorId!!)
			} else {
				null
			}

		val position =
			BlueDotPositionUpdate(
				accuracy = BlueDotPositionUpdate.Accuracy.Value(currentAccuracy),
				floorId = floorId,
				heading = BlueDotPositionUpdate.Heading.Value(currentHeading),
				latitude = BlueDotPositionUpdate.Latitude.Value(coordinate.latitude),
				longitude = BlueDotPositionUpdate.Longitude.Value(coordinate.longitude),
			)

		mapView.blueDot.update(position, BlueDotUpdateOptions(animate = true)) { _ ->
			runOnUiThread {
				statusLabel.text = "BlueDot placed"
			}
			refreshStateDisplay()
		}
	}

	private fun disableBlueDot() {
		mapView.blueDot.disable { _ ->
			runOnUiThread {
				statusLabel.text = "BlueDot disabled"
			}
			refreshStateDisplay()
		}
	}

	private fun setHeading(heading: Double) {
		currentHeading = heading

		mapView.blueDot.update(
			BlueDotPositionUpdate(heading = BlueDotPositionUpdate.Heading.Value(heading)),
			BlueDotUpdateOptions(animate = true),
		) { _ ->
			runOnUiThread {
				statusLabel.text = "Heading set to ${heading.toInt()}°"
			}
			refreshStateDisplay()
		}
	}

	private fun setAccuracy(accuracy: Double) {
		currentAccuracy = accuracy

		mapView.blueDot.update(
			BlueDotPositionUpdate(accuracy = BlueDotPositionUpdate.Accuracy.Value(accuracy)),
			BlueDotUpdateOptions(animate = true),
		) { _ ->
			runOnUiThread {
				statusLabel.text = "Accuracy set to ${accuracy.toInt()}m"
			}
			refreshStateDisplay()
		}
	}

	private fun changeColor(hexColor: String) {
		val options =
			BlueDotOptions(
				accuracyRing = BlueDotOptions.AccuracyRing(color = hexColor, opacity = 0.25),
				color = hexColor,
				heading = BlueDotOptions.Heading(color = hexColor, opacity = 0.6),
			)

		mapView.blueDot.updateState(options) { _ ->
			runOnUiThread {
				statusLabel.text = "BlueDot color changed to $hexColor"
			}
			refreshStateDisplay()
		}
	}

	/**
	 * Fetches all getter values and updates the state label
	 */
	private fun refreshStateDisplay() {
		val stateInfo = ConcurrentHashMap<String, String>()
		val pendingCalls = AtomicInteger(7)

		fun checkComplete() {
			if (pendingCalls.decrementAndGet() == 0) {
				runOnUiThread {
					val text =
						listOf(
							"enabled: ${stateInfo["enabled"] ?: "?"}",
							"status: ${stateInfo["status"] ?: "?"}",
							"following: ${stateInfo["following"] ?: "?"}",
							"heading: ${stateInfo["heading"] ?: "?"}",
							"accuracy: ${stateInfo["accuracy"] ?: "?"}",
							"coord: ${stateInfo["coord"] ?: "?"}",
							"floor: ${stateInfo["floor"] ?: "?"}",
						).joinToString(" | ")
					stateLabel.text = text
				}
			}
		}

		mapView.blueDot.getIsEnabled { result ->
			result.fold(
				onSuccess = { enabled -> stateInfo["enabled"] = if (enabled) "true" else "false" },
				onFailure = { stateInfo["enabled"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getStatus { result ->
			result.fold(
				onSuccess = { status -> stateInfo["status"] = status.value },
				onFailure = { stateInfo["status"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getIsFollowing { result ->
			result.fold(
				onSuccess = { following -> stateInfo["following"] = if (following) "true" else "false" },
				onFailure = { stateInfo["following"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getHeading { result ->
			result.fold(
				onSuccess = { heading -> stateInfo["heading"] = heading?.let { String.format("%.1f", it) } ?: "nil" },
				onFailure = { stateInfo["heading"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getAccuracy { result ->
			result.fold(
				onSuccess = { accuracy -> stateInfo["accuracy"] = accuracy?.let { String.format("%.1fm", it) } ?: "nil" },
				onFailure = { stateInfo["accuracy"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getCoordinate { result ->
			result.fold(
				onSuccess = { coord ->
					stateInfo["coord"] = coord?.let {
						String.format("%.4f, %.4f", it.latitude, it.longitude)
					} ?: "nil"
				},
				onFailure = { stateInfo["coord"] = "?" },
			)
			checkComplete()
		}

		mapView.blueDot.getFloor(mapView.mapData) { result ->
			result.fold(
				onSuccess = { floor -> stateInfo["floor"] = floor?.name ?: "nil" },
				onFailure = { stateInfo["floor"] = "?" },
			)
			checkComplete()
		}
	}
}
