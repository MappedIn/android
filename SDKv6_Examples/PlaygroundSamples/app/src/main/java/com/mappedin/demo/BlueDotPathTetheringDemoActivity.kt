package com.mappedin.demo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddPathOptions
import com.mappedin.models.BlueDotEvents
import com.mappedin.models.BlueDotOptions
import com.mappedin.models.ClickPayload
import com.mappedin.models.CoordinateOutsideThresholdMode
import com.mappedin.models.Directions
import com.mappedin.models.Events
import com.mappedin.models.FollowCameraOptions
import com.mappedin.models.FollowMode
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.ManualPositionOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.MultiDestinationTarget
import com.mappedin.models.NavigationOptions
import com.mappedin.models.NavigationTarget
import com.mappedin.models.OutsideThresholdPathStyle
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import com.mappedin.models.TetheredOptions
import com.mappedin.models.TrackCoordinateOptions
import com.mappedin.models.TrackingMode
import com.mappedin.models.TravelledOptions
import org.json.JSONArray

/**
 * Demonstrates Navigation.trackCoordinate
 * (both tethered and travelled modes) driven by a pre-recorded BlueDot walk through the
 * mappedin-demo-mall venue. Supports Single Path (entrance -> Aritzia) and Multi-Destination
 * Path (entrance -> Mucho Burrito -> Reebok -> Aritzia), with live controls for tether
 * threshold, outside-threshold style and mode, marker hiding, travelled color, active
 * destination, and full playback (play/pause + 1x/2x).
 *
 * Bridge callbacks come back on a background binder thread, so every UI mutation after a
 * bridge call MUST be marshalled to the main thread via [runOnUiThread].
 */
class BlueDotPathTetheringDemoActivity : AppCompatActivity() {
	// Demo mall space IDs
	private companion object {
		const val START_ENTRANCE_SPACE_ID = "s_6650b9a8cad393835892b92b"
		const val MUCHO_BURRITO_SPACE_ID = "s_62042759e325474a3000007b"
		const val REEBOK_SPACE_ID = "s_62042759e325474a30000079"
		const val ARITZIA_SPACE_ID = "s_62051551e325474a300010d5"
		const val LEVEL_2_FLOOR_NAME = "Level 2"

		const val SINGLE_POSITIONS_ASSET = "tethering-demo-mall.json"
		const val MULTI_POSITIONS_ASSET = "tethering-demo-mall-multi-dest.json"

		const val PLAYBACK_INTERVAL_MS = 1500L
	}

	private enum class NavigationDemoMode { NONE, SINGLE, MULTI_DESTINATION }

	private enum class PathTrackingMode { TETHERED, TRAVELLED }

	// === GUI-equivalent state ===
	private var trackingEnabled = true
	private var selectedDestination = 0
	private var tetherThreshold = 5.0
	private var hideMarkersOutsideThreshold = false
	private var coordinateOutsideThresholdMode = CoordinateOutsideThresholdMode.TETHER_AND_DASH
	private var outsideThresholdPathStyle = OutsideThresholdPathStyle.DASHED_BOXES
	private var pathTrackingMode = PathTrackingMode.TETHERED
	private var travelledColor = "#999999"
	private var navigationMode = NavigationDemoMode.NONE

	// === Playback state ===
	private var currentPositions: List<ManualPositionOptions> = emptyList()
	private var currentPositionIndex = 0
	private var isPaused = false
	private var playbackSpeed = 1
	private val playbackHandler = Handler(Looper.getMainLooper())
	private var playbackRunnable: Runnable? = null

	// === Cached directions ===
	private var singleDirections: Directions? = null
	private var multiDirections: List<Directions>? = null

	// === Tracking state ===
	private var lastBlueDotPosition: com.mappedin.models.Coordinate? = null

	// === Floor ID → elevation lookup (populated after map data loads) ===
	private val floorIdToLevel = mutableMapOf<String, Int>()

	// === Views ===
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	// Tracking status overlay
	private lateinit var statusIsTracking: TextView
	private lateinit var statusIsShowingOutside: TextView
	private lateinit var statusCurrentMode: TextView
	private lateinit var statusTravelledFraction: TextView
	private lateinit var statusPanel: LinearLayout

	// Controls panel (configuration that changes based on navigation state)
	private lateinit var controlsPanel: LinearLayout

	// Initial state controls (before path is drawn)
	private lateinit var initialControls: LinearLayout
	private lateinit var trackingModeGroup: RadioGroup

	// Navigation-active controls (after path is drawn)
	private lateinit var activeControls: LinearLayout
	private lateinit var trackingEnabledSwitch: Switch
	private lateinit var tetheredSettings: LinearLayout
	private lateinit var travelledSettings: LinearLayout
	private lateinit var destinationContainer: LinearLayout
	private lateinit var destinationSpinner: Spinner

	private lateinit var tetherThresholdLabel: TextView
	private lateinit var tetherThresholdSeek: SeekBar
	private lateinit var hideMarkersSwitch: Switch
	private lateinit var outsideModeSpinner: Spinner
	private lateinit var outsideStyleSpinner: Spinner
	private lateinit var travelledColorInput: EditText

	// Scrubber
	private lateinit var scrubberContainer: LinearLayout
	private lateinit var playPauseButton: Button
	private lateinit var speedButton: Button
	private lateinit var currentPositionLabel: TextView
	private lateinit var totalPositionsLabel: TextView
	private lateinit var positionSlider: SeekBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Blue Dot Path Tethering"

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
			}
		setContentView(root)

		buildStatusPanel()
		buildControlsPanel()

		// Wrap the status + controls section in a ScrollView with a weighted height
		// so the long set of tethered options fits on a mobile screen without crowding
		// the map. Weight 3 (top) : 5 (map) yields roughly 38% / 62%, comfortable for
		// the initial state and only a small scroll for the active state.
		val topInner =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				addView(statusPanel)
				addView(controlsPanel)
			}
		val topScroll =
			ScrollView(this).apply {
				isFillViewport = true
				addView(topInner)
			}
		root.addView(
			topScroll,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 3f),
		)

		buildMapAndScrubber(root)

		loadMapData()
		showInitialControls()
		hideScrubber()
	}

	override fun onDestroy() {
		stopPositionPlayback()
		super.onDestroy()
	}

	// ============================================================
	// UI construction
	// ============================================================

	private fun buildStatusPanel() {
		statusPanel =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(12), dp(8), dp(12), dp(4))
				setBackgroundColor("#11000000".toColorInt())
				// The activity uses a NoActionBar theme, so the system status bar draws
				// over our content. Push the status panel below it via the safe-area inset.
				ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
					val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
					v.setPadding(dp(12), bars.top + dp(8), dp(12), dp(4))
					insets
				}
			}

		val titleView =
			TextView(this).apply {
				text = "Navigation Tracking State"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
				setTextColor("#6B7280".toColorInt())
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		statusPanel.addView(titleView)

		statusIsTracking = makeStatusRow("isTracking")
		statusIsShowingOutside = makeStatusRow("isShowingOutsideThresholdPath")
		statusCurrentMode = makeStatusRow("currentTrackingMode")
		statusTravelledFraction = makeStatusRow("travelledFraction")
	}

	private fun makeStatusRow(label: String): TextView {
		val row =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(0, dp(1), 0, dp(1))
			}
		val labelView =
			TextView(this).apply {
				text = label
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
				typeface = Typeface.MONOSPACE
			}
		val valueView =
			TextView(this).apply {
				text = "—"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
				typeface = Typeface.MONOSPACE
				setPadding(dp(8), 0, 0, 0)
				gravity = Gravity.END
			}
		row.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		row.addView(valueView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		statusPanel.addView(row)
		return valueView
	}

	private fun buildControlsPanel() {
		controlsPanel =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(12), dp(4), dp(12), dp(8))
			}
		buildInitialControls()
		buildActiveControls()
		controlsPanel.addView(initialControls)
		controlsPanel.addView(activeControls)
	}

	private fun buildInitialControls() {
		initialControls =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
			}

		val modeLabel = makeSectionLabel("Tracking Mode")
		initialControls.addView(modeLabel)

		val tetheredRadio =
			RadioButton(this).apply {
				id = View.generateViewId()
				text = "Tethered"
				isChecked = pathTrackingMode == PathTrackingMode.TETHERED
			}
		val travelledRadio =
			RadioButton(this).apply {
				id = View.generateViewId()
				text = "Travelled"
				isChecked = pathTrackingMode == PathTrackingMode.TRAVELLED
			}
		trackingModeGroup =
			RadioGroup(this).apply {
				orientation = RadioGroup.HORIZONTAL
				addView(tetheredRadio, RadioGroup.LayoutParams(0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f))
				addView(travelledRadio, RadioGroup.LayoutParams(0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f))
				setOnCheckedChangeListener { _, checkedId ->
					pathTrackingMode =
						if (checkedId == tetheredRadio.id) {
							PathTrackingMode.TETHERED
						} else {
							PathTrackingMode.TRAVELLED
						}
				}
			}
		initialControls.addView(trackingModeGroup)

		val buttonRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(0, dp(8), 0, 0)
			}
		val singleBtn =
			Button(this).apply {
				text = "Single Path"
				setOnClickListener { drawNavigation(NavigationDemoMode.SINGLE) }
			}
		val multiBtn =
			Button(this).apply {
				text = "Multi-Destination"
				setOnClickListener { drawNavigation(NavigationDemoMode.MULTI_DESTINATION) }
			}
		buttonRow.addView(singleBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(4), 0) })
		buttonRow.addView(multiBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, 0, 0) })
		initialControls.addView(buttonRow)
	}

	private fun buildActiveControls() {
		activeControls =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				visibility = View.GONE
			}

		trackingEnabledSwitch =
			Switch(this).apply {
				text = "Tracking Enabled"
				isChecked = trackingEnabled
				setOnCheckedChangeListener { _, checked ->
					trackingEnabled = checked
					if (!trackingEnabled) {
						mapView.navigation.stopTracking { _ -> refreshTrackingStatus() }
					} else {
						trackCurrentPosition()
					}
				}
			}
		activeControls.addView(trackingEnabledSwitch)

		buildTetheredSettings()
		buildTravelledSettings()
		buildDestinationSelector()
		activeControls.addView(tetheredSettings)
		activeControls.addView(travelledSettings)
		activeControls.addView(destinationContainer)

		val clearBtn =
			Button(this).apply {
				text = "Clear Navigation"
				setOnClickListener { clearNavigation() }
			}
		activeControls.addView(
			clearBtn,
			LinearLayout
				.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
				).apply { setMargins(0, dp(8), 0, 0) },
		)
	}

	private fun buildTetheredSettings() {
		tetheredSettings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

		tetherThresholdLabel =
			TextView(this).apply {
				text = "Tether Threshold: ${tetherThreshold.toInt()} m"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
				setPadding(0, dp(8), 0, 0)
			}
		tetherThresholdSeek =
			SeekBar(this).apply {
				max = 20
				progress = tetherThreshold.toInt()
				setOnSeekBarChangeListener(
					object : SeekBar.OnSeekBarChangeListener {
						override fun onProgressChanged(
							seekBar: SeekBar?,
							progress: Int,
							fromUser: Boolean,
						) {
							val v = progress.coerceAtLeast(1)
							tetherThreshold = v.toDouble()
							tetherThresholdLabel.text = "Tether Threshold: $v m"
						}

						override fun onStartTrackingTouch(seekBar: SeekBar?) {}

						override fun onStopTrackingTouch(seekBar: SeekBar?) {
							trackCurrentPosition()
						}
					},
				)
			}

		hideMarkersSwitch =
			Switch(this).apply {
				text = "Hide Markers Outside Threshold"
				isChecked = hideMarkersOutsideThreshold
				setOnCheckedChangeListener { _, checked ->
					hideMarkersOutsideThreshold = checked
					trackCurrentPosition()
				}
			}

		val outsideModeLabel = makeSectionLabel("Outside Threshold Mode")
		val outsideModeOptions =
			listOf(
				"Tether + Dash" to CoordinateOutsideThresholdMode.TETHER_AND_DASH,
				"Tether Only" to CoordinateOutsideThresholdMode.TETHER_ONLY,
				"Untether" to CoordinateOutsideThresholdMode.UNTETHER,
			)
		outsideModeSpinner =
			Spinner(this).apply {
				adapter =
					ArrayAdapter(
						this@BlueDotPathTetheringDemoActivity,
						android.R.layout.simple_spinner_item,
						outsideModeOptions.map { it.first },
					).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
				onItemSelectedListener =
					object : AdapterView.OnItemSelectedListener {
						override fun onItemSelected(
							parent: AdapterView<*>?,
							view: View?,
							position: Int,
							id: Long,
						) {
							coordinateOutsideThresholdMode = outsideModeOptions[position].second
							trackCurrentPosition()
						}

						override fun onNothingSelected(parent: AdapterView<*>?) {}
					}
			}

		val outsideStyleLabel = makeSectionLabel("Outside Threshold Style")
		val outsideStyleOptions =
			listOf(
				"Dashed Boxes" to OutsideThresholdPathStyle.DASHED_BOXES,
				"Dashed Stripes" to OutsideThresholdPathStyle.DASHED_STRIPES,
				"Dashed Sparse" to OutsideThresholdPathStyle.DASHED_SPARSE,
				"Solid" to OutsideThresholdPathStyle.SOLID,
				"Bordered" to OutsideThresholdPathStyle.BORDERED,
			)
		outsideStyleSpinner =
			Spinner(this).apply {
				adapter =
					ArrayAdapter(
						this@BlueDotPathTetheringDemoActivity,
						android.R.layout.simple_spinner_item,
						outsideStyleOptions.map { it.first },
					).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
				onItemSelectedListener =
					object : AdapterView.OnItemSelectedListener {
						override fun onItemSelected(
							parent: AdapterView<*>?,
							view: View?,
							position: Int,
							id: Long,
						) {
							outsideThresholdPathStyle = outsideStyleOptions[position].second
							trackCurrentPosition()
						}

						override fun onNothingSelected(parent: AdapterView<*>?) {}
					}
			}

		tetheredSettings.addView(tetherThresholdLabel)
		tetheredSettings.addView(tetherThresholdSeek)
		tetheredSettings.addView(hideMarkersSwitch)
		tetheredSettings.addView(outsideModeLabel)
		tetheredSettings.addView(outsideModeSpinner)
		tetheredSettings.addView(outsideStyleLabel)
		tetheredSettings.addView(outsideStyleSpinner)
	}

	private fun buildTravelledSettings() {
		travelledSettings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

		val colorLabel = makeSectionLabel("Travelled Color (hex)")
		travelledColorInput =
			EditText(this).apply {
				setText(travelledColor)
				setOnFocusChangeListener { _, hasFocus ->
					if (!hasFocus) {
						val candidate = text.toString().trim()
						if (candidate.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
							travelledColor = candidate
							trackCurrentPosition()
						} else {
							setText(travelledColor)
						}
					}
				}
			}
		travelledSettings.addView(colorLabel)
		travelledSettings.addView(travelledColorInput)
	}

	private fun buildDestinationSelector() {
		destinationContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				visibility = View.GONE
			}
		val label = makeSectionLabel("Active Destination")
		val destinationOptions = listOf("Mucho Burrito", "Reebok", "Aritzia")
		destinationSpinner =
			Spinner(this).apply {
				adapter =
					ArrayAdapter(
						this@BlueDotPathTetheringDemoActivity,
						android.R.layout.simple_spinner_item,
						destinationOptions,
					).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
				onItemSelectedListener =
					object : AdapterView.OnItemSelectedListener {
						override fun onItemSelected(
							parent: AdapterView<*>?,
							view: View?,
							position: Int,
							id: Long,
						) {
							selectedDestination = position
							mapView.navigation.setActivePathByIndex(position) { _ -> refreshTrackingStatus() }
						}

						override fun onNothingSelected(parent: AdapterView<*>?) {}
					}
			}
		destinationContainer.addView(label)
		destinationContainer.addView(destinationSpinner)
	}

	private fun makeSectionLabel(text: String): TextView =
		TextView(this).apply {
			this.text = text
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			setPadding(0, dp(8), 0, 0)
			setTextColor("#6B7280".toColorInt())
		}

	private fun buildMapAndScrubber(root: LinearLayout) {
		val mapContainer = FrameLayout(this)
		mapView = MapView(this)
		mapContainer.addView(
			mapView.view,
			FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
		)

		loadingIndicator = ProgressBar(this)
		val loadingParams =
			FrameLayout
				.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
				).apply { gravity = Gravity.CENTER }
		mapContainer.addView(loadingIndicator, loadingParams)

		root.addView(
			mapContainer,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 5f),
		)
		// Top scroll : map weights of 3:5 (~38% / 62%) keep the initial state visible
		// without crowding the map, while letting the user scroll the active controls.

		buildScrubber()
		root.addView(scrubberContainer)
	}

	private fun buildScrubber() {
		scrubberContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(dp(12), dp(8), dp(12), dp(8))
				setBackgroundColor("#D9000000".toColorInt())
				gravity = Gravity.CENTER_VERTICAL
			}

		playPauseButton =
			Button(this).apply {
				text = "❚❚"
				setOnClickListener {
					isPaused = !isPaused
					playPauseButton.text = if (isPaused) "▶" else "❚❚"
				}
			}
		speedButton =
			Button(this).apply {
				text = "1x"
				setOnClickListener {
					playbackSpeed = if (playbackSpeed == 1) 2 else 1
					speedButton.text = "${playbackSpeed}x"
					restartPlaybackInterval()
				}
			}
		currentPositionLabel =
			TextView(this).apply {
				text = "0"
				setTextColor(Color.WHITE)
				setPadding(dp(8), 0, dp(8), 0)
			}
		positionSlider =
			SeekBar(this).apply {
				max = 0
			}
		totalPositionsLabel =
			TextView(this).apply {
				text = "/ 0"
				setTextColor(Color.WHITE)
				setPadding(dp(8), 0, 0, 0)
			}

		positionSlider.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar?,
					progress: Int,
					fromUser: Boolean,
				) {
					if (!fromUser) return
					currentPositionLabel.text = progress.toString()
					currentPositionIndex = progress
					currentPositions.getOrNull(progress)?.let { updatePosition(it) }
				}

				override fun onStartTrackingTouch(seekBar: SeekBar?) {}

				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
			},
		)

		scrubberContainer.addView(playPauseButton)
		scrubberContainer.addView(speedButton)
		scrubberContainer.addView(currentPositionLabel)
		scrubberContainer.addView(positionSlider, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		scrubberContainer.addView(totalPositionsLabel)
	}

	// ============================================================
	// Map setup
	// ============================================================

	private fun loadMapData() {
		// See Demo API Key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-mall",
			)

		mapView.getMapData(options) { mapResult ->
			mapResult.onFailure { err ->
				runOnUiThread { loadingIndicator.visibility = View.GONE }
				Log.e("BlueDotPathTethering", "getMapData error: $err")
			}
			mapResult.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { showResult ->
					runOnUiThread { loadingIndicator.visibility = View.GONE }
					showResult.onSuccess { onMapReady() }
					showResult.onFailure { err ->
						Log.e("BlueDotPathTethering", "show3dMap error: $err")
					}
				}
			}
		}
	}

	private fun onMapReady() {
		mapView.mapData.getByType<com.mappedin.models.Floor>(MapDataType.FLOOR) { res ->
			res.onSuccess { floors ->
				for (floor in floors) {
					floorIdToLevel[floor.id] = floor.elevation.toInt()
				}
				val level2 = floors.find { it.name == LEVEL_2_FLOOR_NAME }
				if (level2 != null) {
					mapView.setFloor(level2.id) { _ -> }
				}
			}
			preloadDirections()
			registerClickHandler()
			registerBlueDotPositionHandler()
		}
	}

	private fun preloadDirections() {
		mapView.mapData.getById<Space>(MapDataType.SPACE, START_ENTRANCE_SPACE_ID) { startRes ->
			val start =
				startRes.getOrNull() ?: run {
					Log.w("BlueDotPathTethering", "start entrance space not found: $START_ENTRANCE_SPACE_ID")
					return@getById
				}
			val startTarget = NavigationTarget.SpaceTarget(start)

			mapView.mapData.getById<Space>(MapDataType.SPACE, ARITZIA_SPACE_ID) { aritziaRes ->
				val aritzia = aritziaRes.getOrNull() ?: return@getById
				mapView.mapData.getDirections(startTarget, NavigationTarget.SpaceTarget(aritzia)) { dRes ->
					singleDirections = dRes.getOrNull()
					Log.d("BlueDotPathTethering", "Single directions loaded: ${singleDirections != null}")
				}
			}

			loadMultiDestinationDirections(startTarget)
		}
	}

	private fun loadMultiDestinationDirections(start: NavigationTarget) {
		mapView.mapData.getById<Space>(MapDataType.SPACE, MUCHO_BURRITO_SPACE_ID) { muchoRes ->
			val mucho = muchoRes.getOrNull() ?: return@getById
			mapView.mapData.getById<Space>(MapDataType.SPACE, REEBOK_SPACE_ID) { reebokRes ->
				val reebok = reebokRes.getOrNull() ?: return@getById
				mapView.mapData.getById<Space>(MapDataType.SPACE, ARITZIA_SPACE_ID) { aritziaRes ->
					val aritzia = aritziaRes.getOrNull() ?: return@getById
					val destinations =
						listOf(
							MultiDestinationTarget.Single(NavigationTarget.SpaceTarget(mucho)),
							MultiDestinationTarget.Single(NavigationTarget.SpaceTarget(reebok)),
							MultiDestinationTarget.Single(NavigationTarget.SpaceTarget(aritzia)),
						)
					mapView.mapData.getDirectionsMultiDestination(start, destinations) { dRes ->
						multiDirections = dRes.getOrNull()
						Log.d("BlueDotPathTethering", "Multi-dest directions loaded: ${multiDirections?.size}")
					}
				}
			}
		}
	}

	private fun registerClickHandler() {
		mapView.on(Events.Click) { payload: ClickPayload? ->
			val click = payload ?: return@on
			val floors = click.floors
			if (floors.isNullOrEmpty()) return@on
			val coord = click.coordinate
			updatePosition(
				ManualPositionOptions(
					latitude = coord.latitude,
					longitude = coord.longitude,
					accuracy = 5.0,
					heading = 0.0,
					floorLevel = floors.firstOrNull()?.elevation?.toInt(),
					confidence = 1.0,
				),
			)
		}
	}

	private fun registerBlueDotPositionHandler() {
		mapView.blueDot.on(BlueDotEvents.DotPositionUpdate) { payload ->
			val position = payload?.position ?: return@on
			lastBlueDotPosition = position
			trackCurrentPosition()
		}
	}

	// ============================================================
	// Navigation lifecycle
	// ============================================================

	private fun drawNavigation(mode: NavigationDemoMode) {
		val pathOptions =
			AddPathOptions(
				color = "#4b90e2",
				accentColor = "white",
				displayArrowsOnPath = false,
				animateArrowsOnPath = false,
			)
		val navOptions = NavigationOptions(pathOptions = pathOptions)
		val onDrawn: (Result<Any?>) -> Unit = { _ ->
			mapView.blueDot.enable(BlueDotOptions(radius = 5.0)) { _ ->
				mapView.blueDot.follow(
					mode = FollowMode.POSITION_AND_PATH_DIRECTION,
					cameraOptions = FollowCameraOptions(zoomLevel = 19.058859291133),
				) { _ -> }

				runOnUiThread {
					showActiveControls()
					refreshTrackingStatus()
					startPositionPlayback(loadPositions(mode))
				}
			}
		}

		when (mode) {
			NavigationDemoMode.SINGLE -> {
				val directions = singleDirections
				if (directions == null) {
					notifyDirectionsNotReady(mode)
					return
				}
				navigationMode = mode
				mapView.navigation.draw(directions, navOptions, onDrawn)
			}

			NavigationDemoMode.MULTI_DESTINATION -> {
				val directionsList = multiDirections
				if (directionsList.isNullOrEmpty()) {
					notifyDirectionsNotReady(mode)
					return
				}
				navigationMode = mode
				mapView.navigation.draw(directionsList, navOptions, onDrawn)
			}

			NavigationDemoMode.NONE -> {
				Unit
			}
		}
	}

	private fun notifyDirectionsNotReady(mode: NavigationDemoMode) {
		Log.w("BlueDotPathTethering", "No directions available yet for mode=$mode")
		runOnUiThread {
			android.widget.Toast
				.makeText(
					this,
					"Directions still loading — try again in a moment",
					android.widget.Toast.LENGTH_SHORT,
				).show()
		}
	}

	private fun clearNavigation() {
		mapView.navigation.clear { _ -> }
		mapView.blueDot.disable { _ -> }
		stopPositionPlayback()
		runOnUiThread {
			hideScrubber()
			hideTrackingStatusValues()
			navigationMode = NavigationDemoMode.NONE
			showInitialControls()
		}
	}

	private fun trackCurrentPosition() {
		val pos = lastBlueDotPosition ?: return
		if (navigationMode == NavigationDemoMode.NONE || !trackingEnabled) return

		val options: TrackCoordinateOptions =
			when (pathTrackingMode) {
				PathTrackingMode.TETHERED -> {
					TrackCoordinateOptions.Tethered(
						TetheredOptions(
							tetherThresholdDistance = tetherThreshold,
							hideMarkersOutsideThreshold = hideMarkersOutsideThreshold,
							coordinateOutsideThresholdMode = coordinateOutsideThresholdMode,
							outsideThresholdPathStyle = outsideThresholdPathStyle,
						),
					)
				}

				PathTrackingMode.TRAVELLED -> {
					TrackCoordinateOptions.Travelled(
						TravelledOptions(color = travelledColor),
					)
				}
			}

		mapView.navigation.trackCoordinate(pos, options) { _ -> refreshTrackingStatus() }
	}

	private fun refreshTrackingStatus() {
		mapView.navigation.isTracking { r ->
			runOnUiThread { statusIsTracking.text = r.getOrDefault(false).toString() }
		}
		mapView.navigation.isShowingOutsideThresholdPath { r ->
			runOnUiThread { statusIsShowingOutside.text = r.getOrDefault(false).toString() }
		}
		mapView.navigation.currentTrackingMode { r ->
			val mode: TrackingMode? = r.getOrNull()
			runOnUiThread {
				statusCurrentMode.text = mode?.value ?: "null"
			}
		}
		mapView.navigation.travelledFraction { r ->
			val frac = r.getOrNull()
			runOnUiThread {
				statusTravelledFraction.text = if (frac == null) "null" else "%.1f%%".format(frac * 100)
			}
		}
	}

	private fun hideTrackingStatusValues() {
		statusIsTracking.text = "—"
		statusIsShowingOutside.text = "—"
		statusCurrentMode.text = "—"
		statusTravelledFraction.text = "—"
	}

	private fun updatePosition(position: ManualPositionOptions) {
		mapView.blueDot.reportPosition(position) { _ -> }
	}

	// ============================================================
	// Position playback
	// ============================================================

	private fun loadPositions(mode: NavigationDemoMode): List<ManualPositionOptions> {
		val asset =
			when (mode) {
				NavigationDemoMode.SINGLE -> SINGLE_POSITIONS_ASSET
				NavigationDemoMode.MULTI_DESTINATION -> MULTI_POSITIONS_ASSET
				NavigationDemoMode.NONE -> return emptyList()
			}
		return parsePositionAsset(asset)
	}

	private fun parsePositionAsset(filename: String): List<ManualPositionOptions> =
		try {
			val raw = assets.open(filename).bufferedReader().use { it.readText() }
			val array = JSONArray(raw)
			val result = ArrayList<ManualPositionOptions>(array.length())
			for (i in 0 until array.length()) {
				val obj = array.optJSONObject(i) ?: continue
				val floorId = if (obj.has("floorOrFloorId")) obj.optString("floorOrFloorId", null) else null
				result.add(
					ManualPositionOptions(
						latitude = obj.getDouble("latitude"),
						longitude = obj.getDouble("longitude"),
						accuracy = if (obj.has("accuracy")) obj.getDouble("accuracy") else null,
						heading = if (obj.has("heading")) obj.getDouble("heading") else null,
						floorLevel = if (floorId != null) floorIdToLevel[floorId] else null,
						confidence = 1.0,
					),
				)
			}
			result
		} catch (e: Exception) {
			Log.e("BlueDotPathTethering", "Failed to read $filename: $e")
			emptyList()
		}

	private fun startPositionPlayback(positions: List<ManualPositionOptions>) {
		currentPositions = positions
		currentPositionIndex = 0
		isPaused = false
		playPauseButton.text = "❚❚"
		positionSlider.max = (positions.size - 1).coerceAtLeast(0)
		positionSlider.progress = 0
		currentPositionLabel.text = "0"
		totalPositionsLabel.text = "/ ${(positions.size - 1).coerceAtLeast(0)}"
		showScrubber()
		restartPlaybackInterval()
	}

	private fun restartPlaybackInterval() {
		playbackRunnable?.let { playbackHandler.removeCallbacks(it) }
		if (currentPositions.isEmpty()) return
		val intervalMs = PLAYBACK_INTERVAL_MS / playbackSpeed
		playbackRunnable =
			object : Runnable {
				override fun run() {
					if (!isPaused) {
						if (currentPositionIndex < currentPositions.size) {
							updatePosition(currentPositions[currentPositionIndex])
							positionSlider.progress = currentPositionIndex
							currentPositionLabel.text = currentPositionIndex.toString()
							currentPositionIndex++
						} else {
							currentPositionIndex = 0
						}
					}
					playbackHandler.postDelayed(this, intervalMs)
				}
			}
		playbackHandler.post(playbackRunnable!!)
	}

	private fun stopPositionPlayback() {
		playbackRunnable?.let { playbackHandler.removeCallbacks(it) }
		playbackRunnable = null
	}

	private fun showScrubber() {
		scrubberContainer.visibility = View.VISIBLE
	}

	private fun hideScrubber() {
		scrubberContainer.visibility = View.GONE
	}

	// ============================================================
	// Panel switching
	// ============================================================

	private fun showInitialControls() {
		initialControls.visibility = View.VISIBLE
		activeControls.visibility = View.GONE
	}

	private fun showActiveControls() {
		initialControls.visibility = View.GONE
		activeControls.visibility = View.VISIBLE
		tetheredSettings.visibility = if (pathTrackingMode == PathTrackingMode.TETHERED) View.VISIBLE else View.GONE
		travelledSettings.visibility = if (pathTrackingMode == PathTrackingMode.TRAVELLED) View.VISIBLE else View.GONE
		destinationContainer.visibility = if (navigationMode == NavigationDemoMode.MULTI_DESTINATION) View.VISIBLE else View.GONE
		trackingEnabledSwitch.isChecked = trackingEnabled
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
