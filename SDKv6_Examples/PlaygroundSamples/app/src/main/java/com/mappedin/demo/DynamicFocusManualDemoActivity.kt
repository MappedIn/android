package com.mappedin.demo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.Events
import com.mappedin.models.Facade
import com.mappedin.models.FacadeUpdateState
import com.mappedin.models.Floor
import com.mappedin.models.FloorStack
import com.mappedin.models.FloorUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Demonstrates manual Dynamic Focus-like behaviour using MapView methods.
 *
 * This demo implements similar effects to DynamicFocus but using direct MapView
 * state updates for more custom control over facade/floor visibility.
 */
class DynamicFocusManualDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var statusLabel: TextView
	private lateinit var eventLogLabel: TextView
	private lateinit var buildingSpinner: Spinner
	private lateinit var floorSpinner: Spinner

	private val floorToShowByBuilding = mutableMapOf<String, Floor>()
	private var currentElevation = 0.0

	// Cached data from async getByType calls
	private var allFloorStacks: List<FloorStack> = emptyList()
	private var allFloors: List<Floor> = emptyList()
	private var allFacades: List<Facade> = emptyList()
	private var currentFloorStackFloors: List<Floor> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Dynamic Focus (Manual)"

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

		// Event log label
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

		// Building selector row
		val buildingRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(8), dp(16), dp(4))
			}
		val buildingLabel =
			TextView(this).apply {
				text = "Building: "
				textSize = 14f
				setTextColor(Color.DKGRAY)
			}
		buildingSpinner =
			Spinner(this).apply {
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}
		buildingRow.addView(buildingLabel)
		buildingRow.addView(buildingSpinner)
		mainLayout.addView(buildingRow)

		// Floor selector row
		val floorRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(4), dp(16), dp(8))
			}
		val floorLabel =
			TextView(this).apply {
				text = "Floor: "
				textSize = 14f
				setTextColor(Color.DKGRAY)
			}
		floorSpinner =
			Spinner(this).apply {
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
			}
		floorRow.addView(floorLabel)
		floorRow.addView(floorSpinner)
		mainLayout.addView(floorRow)

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

	private fun loadMap() {
		// Trial API key - see https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "682e13a2703478000b567b66",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("DynamicFocusManual", "getMapData success")
					mapView.show3dMap(Show3DMapOptions()) { mapResult ->
						mapResult.onSuccess {
							runOnUiThread {
								loadingIndicator.visibility = View.GONE
							}
							onMapReady()
						}
						mapResult.onFailure { error ->
							runOnUiThread {
								loadingIndicator.visibility = View.GONE
								statusLabel.text = "Error: ${error.message}"
							}
							Log.e("DynamicFocusManual", "show3dMap error: $error")
						}
					}
				}.onFailure { error ->
					runOnUiThread {
						loadingIndicator.visibility = View.GONE
						statusLabel.text = "Error: ${error.message}"
					}
					Log.e("DynamicFocusManual", "getMapData error: $error")
				}
		}
	}

	private fun onMapReady() {
		runOnUiThread {
			statusLabel.text = "Manual floor visibility control active"
		}

		// Label all spaces with names
		mapView.__EXPERIMENTAL__auto()

		// Fetch all floor stacks, floors, and facades, then set up the UI
		mapView.mapData.getByType<FloorStack>(MapDataType.FLOOR_STACK) { stacksResult ->
			stacksResult.onSuccess { stacks ->
				allFloorStacks = stacks?.sortedBy { it.name } ?: emptyList()

				mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { floorsResult ->
					floorsResult.onSuccess { floors ->
						allFloors = floors ?: emptyList()

						mapView.mapData.getByType<Facade>(MapDataType.FACADE) { facadesResult ->
							facadesResult.onSuccess { facades ->
								allFacades = facades ?: emptyList()
								Log.d("DynamicFocusManual", "Loaded ${allFacades.size} facades")

								runOnUiThread {
									populateFloorStacks()
									updateFloorsToShow()
									setupEventListeners()
								}
							}
						}
					}
				}
			}
		}
	}

	// Track the currently selected building (null = outdoor/no building selected)
	private var currentSelectedFloorStackId: String? = null

	// Building spinner listener - stored as a property so we can detach/reattach it
	private val buildingSpinnerListener =
		object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parent: AdapterView<*>?,
				view: View?,
				position: Int,
				id: Long,
			) {
				if (allFloorStacks.isNotEmpty() && position < allFloorStacks.size) {
					val selectedFloorStack = allFloorStacks[position]
					switchToBuilding(selectedFloorStack)
				}
			}

			override fun onNothingSelected(parent: AdapterView<*>?) {}
		}

	/**
	 * Switch to viewing a building by manually managing facades and floors.
	 * This does NOT call setFloorStack or setFloor for buildings - instead it manages
	 * visibility of floors and facades directly while staying on the outdoor floor.
	 * This allows facades to remain rendered while showing building interiors.
	 *
	 * @param floorStack The floor stack to switch to
	 * @param focusCamera Whether to focus the camera on the target. Set to false when
	 *   switching due to panning (facades-in-view-change) to avoid snapping back.
	 */
	private fun switchToBuilding(
		floorStack: FloorStack,
		focusCamera: Boolean = true,
	) {
		currentSelectedFloorStackId = floorStack.id

		Log.d("DynamicFocusManual", "switchToBuilding: ${floorStack.name} (${floorStack.id}) focusCamera=$focusCamera")

		// Check if this is an Outdoor-type floor stack
		val isOutdoor = floorStack.type == FloorStack.FloorStackType.OUTDOOR

		if (isOutdoor) {
			// Switching to Outdoor - close all facades (show them) and hide all building floors
			Log.d("DynamicFocusManual", "  Switching to Outdoor view")

			// Close all facades (make them visible)
			allFacades.forEach { facade ->
				closeFacade(facade)
			}

			// Set floor to the outdoor floor
			floorStack.defaultFloor?.let { defaultFloorId ->
				val floor = allFloors.find { it.id == defaultFloorId }
				floor?.let {
					mapView.setFloor(it.id)
					// Only focus if explicitly requested (e.g., from picker selection)
					if (focusCamera) {
						mapView.camera.focusOn(it)
					}
				}
			}
		} else {
			// Switching to a Building - open its facade (hide it) and show its floors
			Log.d("DynamicFocusManual", "  Switching to Building: ${floorStack.name}")

			// Process all facades
			allFacades.forEach { facade ->
				if (facade.floorStack == floorStack.id) {
					// Open this building's facade (hide it to reveal interior)
					openFacade(facade)
				} else {
					// Close other building facades (show them)
					closeFacade(facade)
				}
			}

			// Populate floors for this building's floor selector
			populateFloors(floorStack.id)

			// Focus on the default floor WITHOUT calling setFloor
			// This keeps the outdoor floor as the "current floor" so facades remain rendered
			// Only focus if explicitly requested (e.g., from picker selection)
			if (focusCamera) {
				floorStack.defaultFloor?.let { defaultFloorId ->
					val floor = allFloors.find { it.id == defaultFloorId }
					floor?.let {
						// Just focus the camera on the building's floor, don't change current floor
						mapView.camera.focusOn(it)
					}
				}
			}
		}
	}

	private fun populateFloorStacks() {
		val floorStackNames = allFloorStacks.map { it.name }

		// Detach listener before making changes
		buildingSpinner.onItemSelectedListener = null

		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, floorStackNames)
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		buildingSpinner.adapter = adapter

		// Set initial building
		mapView.currentFloorStack { result ->
			result
				.onSuccess { currentFloorStack ->
					currentFloorStack?.let { stack ->
						val index = allFloorStacks.indexOfFirst { it.id == stack.id }
						runOnUiThread {
							if (index >= 0) {
								buildingSpinner.setSelection(index)
							}
						}
						populateFloors(stack.id)
					}
				}

			// Reattach listener unconditionally after async call completes
			// This ensures the spinner remains responsive even if result fails or currentFloorStack is null
			runOnUiThread {
				buildingSpinner.post {
					buildingSpinner.onItemSelectedListener = buildingSpinnerListener
				}
			}
		}
	}

	// Floor spinner listener - stored as a property so we can detach/reattach it
	private val floorSpinnerListener =
		object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parent: AdapterView<*>?,
				view: View?,
				position: Int,
				id: Long,
			) {
				if (currentFloorStackFloors.isNotEmpty() && position < currentFloorStackFloors.size) {
					val selectedFloor = currentFloorStackFloors[position]
					val floorStackId = currentSelectedFloorStackId ?: return

					// Update the floor to show for this building
					floorToShowByBuilding[floorStackId] = selectedFloor
					currentElevation = selectedFloor.elevation

					// Get the floor stack and show the selected floor
					val floorStack = allFloorStacks.find { it.id == floorStackId }
					if (floorStack != null) {
						showFloors(floorStack)
					}

					Log.d("DynamicFocusManual", "Floor selected: ${selectedFloor.name}")
				}
			}

			override fun onNothingSelected(parent: AdapterView<*>?) {}
		}

	private fun populateFloors(floorStackId: String) {
		val floorStack = allFloorStacks.find { it.id == floorStackId } ?: return

		// Get Floor objects for all floor IDs in this floor stack
		currentFloorStackFloors =
			floorStack.floors
				.mapNotNull { floorId -> allFloors.find { it.id == floorId } }
				.sortedByDescending { it.elevation }

		val floorNames = currentFloorStackFloors.map { it.name }

		runOnUiThread {
			// Detach listener before making changes to prevent unwanted triggers
			floorSpinner.onItemSelectedListener = null

			val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, floorNames)
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
			floorSpinner.adapter = adapter

			// Set initial floor selection based on default floor or previously selected floor
			val floorToSelect =
				floorToShowByBuilding[floorStackId]
					?: allFloors.find { it.id == floorStack.defaultFloor }

			val index =
				floorToSelect?.let { floor ->
					currentFloorStackFloors.indexOfFirst { it.id == floor.id }
				} ?: 0

			if (index >= 0 && index < currentFloorStackFloors.size) {
				floorSpinner.setSelection(index)
			}

			// Reattach listener after selection is set
			// Use post to ensure selection change is fully processed first
			floorSpinner.post {
				floorSpinner.onItemSelectedListener = floorSpinnerListener
			}
		}
	}

	private fun updateFloorsToShow() {
		floorToShowByBuilding.clear()
		allFloorStacks.forEach { floorStack ->
			// Get the Floor objects for this floor stack
			val floorsInStack =
				floorStack.floors.mapNotNull { floorId ->
					allFloors.find { it.id == floorId }
				}
			val floor = floorsInStack.find { it.elevation == currentElevation }
			if (floor != null) {
				floorToShowByBuilding[floorStack.id] = floor
			}
		}
	}

	private fun showFloors(building: FloorStack) {
		// Get the default floor object
		val defaultFloorObj = allFloors.find { it.id == building.defaultFloor }
		val floorToShow = floorToShowByBuilding[building.id] ?: defaultFloorObj ?: return
		val height = 10 * currentElevation

		// Get all Floor objects for this building
		val floorsInBuilding =
			building.floors.mapNotNull { floorId ->
				allFloors.find { it.id == floorId }
			}

		floorsInBuilding.forEach { floor ->
			if (floor.id == floorToShow.id) {
				mapView.updateState(
					floor,
					FloorUpdateState(
						visible = true,
						altitude = height,
						footprint =
							FloorUpdateState.Footprint(
								visible = currentElevation > 0,
								height = height,
								altitude = -height,
							),
					),
				)
			} else {
				mapView.updateState(
					floor,
					FloorUpdateState(visible = false),
				)
			}
		}
	}

	private fun openFacade(facade: Facade) {
		// Get the FloorStack object from the facade's floorStack ID
		val floorStack = allFloorStacks.find { it.id == facade.floorStack } ?: return

		Log.d("DynamicFocusManual", "openFacade: ${floorStack.name} (setting opacity to 0)")

		// First, show the floor we want to see
		showFloors(floorStack)

		// Animate the facade out (hide it to reveal interior)
		mapView.animateState(
			facade,
			FacadeUpdateState(opacity = 0.0),
		)
	}

	private fun closeFacade(facade: Facade) {
		val floorStack = allFloorStacks.find { it.id == facade.floorStack }
		Log.d("DynamicFocusManual", "closeFacade: ${floorStack?.name ?: "unknown"} (setting opacity to 1)")

		// Animate the facade in (show it to hide interior)
		mapView.animateState(
			facade,
			FacadeUpdateState(opacity = 1.0),
		) { _ ->
			// Hide all floors for this building after animation completes
			floorStack?.floors?.forEach { floorId ->
				val floor = allFloors.find { it.id == floorId }
				floor?.let {
					mapView.updateState(
						it,
						FloorUpdateState(visible = false),
					)
				}
			}
		}
	}

	private fun setupEventListeners() {
		// When facades come into view, switch to show that building's interior
		mapView.on(Events.FacadesInViewChange) { event ->
			event?.let { payload ->
				logEvent("facades-in-view-change: ${payload.facades.size} facade(s)")

				// If a facade is in view, switch to that building
				if (payload.facades.isNotEmpty()) {
					val facade = payload.facades.first()
					val floorStackId = facade.floorStack

					// Only switch if it's a different building
					if (floorStackId != null && floorStackId != currentSelectedFloorStackId) {
						val floorStack = allFloorStacks.find { it.id == floorStackId }
						floorStack?.let {
							Log.d("DynamicFocusManual", "Facade in view - switching to building: ${it.name}")
							// Don't focus camera - user is already panning to this location
							switchToBuilding(it, focusCamera = false)

							// Update the building spinner to reflect the change
							val buildingIndex = allFloorStacks.indexOf(it)
							runOnUiThread {
								buildingSpinner.onItemSelectedListener = null
								if (buildingIndex >= 0) {
									buildingSpinner.setSelection(buildingIndex)
								}
								buildingSpinner.post { buildingSpinner.onItemSelectedListener = buildingSpinnerListener }
							}
						}
					}
				} else {
					// No facades in view - switch to outdoor
					// Don't focus camera - user panned away, keep camera where it is
					val outdoorFloorStack = allFloorStacks.find { it.type == FloorStack.FloorStackType.OUTDOOR }
					if (outdoorFloorStack != null && outdoorFloorStack.id != currentSelectedFloorStackId) {
						Log.d("DynamicFocusManual", "No facades in view - switching to outdoor")
						switchToBuilding(outdoorFloorStack, focusCamera = false)

						// Update the building spinner to reflect the change
						val buildingIndex = allFloorStacks.indexOf(outdoorFloorStack)
						runOnUiThread {
							buildingSpinner.onItemSelectedListener = null
							if (buildingIndex >= 0) {
								buildingSpinner.setSelection(buildingIndex)
							}
							buildingSpinner.post { buildingSpinner.onItemSelectedListener = buildingSpinnerListener }
						}
					}
				}
			}
		}

		// Act on the floor-change event to update the level selector and floor visibility
		mapView.on(Events.FloorChange) { event ->
			event?.let { payload ->
				val newFloor = payload.floor
				currentElevation = newFloor.elevation
				updateFloorsToShow()

				val newFloorStackId = newFloor.floorStack?.id
				Log.d("DynamicFocusManual", "floor-change: ${newFloor.name} (floorStackId=$newFloorStackId)")

				// Update UI spinners - detach listeners before making changes
				val buildingIndex = allFloorStacks.indexOfFirst { it.id == newFloorStackId }

				runOnUiThread {
					// Detach listeners
					buildingSpinner.onItemSelectedListener = null
					floorSpinner.onItemSelectedListener = null

					if (buildingIndex >= 0) {
						buildingSpinner.setSelection(buildingIndex)
					}

					// Update floor spinner selection if the floor is in current floor stack
					val floorIndex = currentFloorStackFloors.indexOfFirst { it.id == newFloor.id }
					if (floorIndex >= 0) {
						floorSpinner.setSelection(floorIndex)
					}

					// Reattach listeners after a post
					buildingSpinner.post {
						buildingSpinner.onItemSelectedListener = buildingSpinnerListener
					}
					floorSpinner.post {
						floorSpinner.onItemSelectedListener = floorSpinnerListener
					}
				}

				// Get the floor stack name for logging
				val floorStackName = newFloor.floorStack?.name ?: "Unknown"
				logEvent("floor-change: ${newFloor.name} ($floorStackName)")
			}
		}
	}

	private fun logEvent(message: String) {
		val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
		val timestamp = timeFormat.format(Date())
		runOnUiThread {
			eventLogLabel.text = "Event [$timestamp]: $message"
		}
		Log.d("DynamicFocusManual", "[Event] $message")
	}
}
