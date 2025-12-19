package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.Events
import com.mappedin.models.Floor
import com.mappedin.models.FloorChangePayload
import com.mappedin.models.FloorStack
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions

class BuildingFloorSelectionDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var buildingSelector: Spinner
	private lateinit var floorSelector: Spinner
	private lateinit var loadingIndicator: ProgressBar
	private var floorStacks: List<FloorStack> = emptyList()
	private var allFloors: List<Floor> = emptyList()
	private var currentFloors: List<Floor> = emptyList()
	private var isUpdatingFromEvent = false
	private var isUpdatingSpinners = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Building & Floor Selection"

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				val padding = (16 * resources.displayMetrics.density).toInt()
				setPadding(padding, padding, padding, 0)
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
					val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
					val padding = (16 * resources.displayMetrics.density).toInt()
					view.setPadding(
						padding,
						systemBars.top + padding,
						padding,
						0,
					)
					insets
				}
			}
		setContentView(root)

		// Building selector
		buildingSelector = Spinner(this)
		val buildingParams =
			LinearLayout
				.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
				).apply {
					val margin = (8 * resources.displayMetrics.density).toInt()
					setMargins(0, 0, 0, margin)
				}
		root.addView(buildingSelector, buildingParams)

		// Floor selector
		floorSelector = Spinner(this)
		val floorParams =
			LinearLayout
				.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
				).apply {
					val margin = (16 * resources.displayMetrics.density).toInt()
					setMargins(0, 0, 0, margin)
				}
		root.addView(floorSelector, floorParams)

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
		loadingParams.gravity = android.view.Gravity.CENTER
		mapContainer.addView(loadingIndicator, loadingParams)

		root.addView(
			mapContainer,
			LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				0,
				1f,
			),
		)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "682e13a2703478000b567b66",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					mapView.show3dMap(Show3DMapOptions()) { r ->
						r
							.onSuccess {
								runOnUiThread {
									loadingIndicator.visibility = View.GONE
								}
								onMapReady()
							}.onFailure {
								runOnUiThread {
									loadingIndicator.visibility = View.GONE
								}
								Log.e("MappedinDemo", "show3dMap error: $it")
							}
					}
				}.onFailure {
					Log.e("MappedinDemo", "getMapData error: $it")
				}
		}
	}

	private fun onMapReady() {
		// Get all floor stacks
		mapView.mapData.getByType<FloorStack>(MapDataType.FLOOR_STACK) { result ->
			result.onSuccess { stacks ->
				floorStacks = stacks?.sortedBy { it.name } ?: emptyList()

				// Get all floors
				mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { floorsResult ->
					floorsResult.onSuccess { floors ->
						allFloors = floors ?: emptyList()
						runOnUiThread {
							populateFloorStacks()
							setupListeners()
						}
					}
				}
			}
		}
	}

	// Populate the building selector with the available floor stacks.
	private fun populateFloorStacks() {
		val buildingNames = floorStacks.map { it.name }
		val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, buildingNames)
		adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

		isUpdatingSpinners = true
		buildingSelector.adapter = adapter

		// Set the initial building to the current building.
		mapView.currentFloorStack { result ->
			result.onSuccess { currentFloorStack ->
				currentFloorStack?.let { stack ->
					val index = floorStacks.indexOfFirst { it.id == stack.id }
					if (index >= 0) {
						runOnUiThread {
							buildingSelector.setSelection(index)
							populateFloors(stack.id)
						}
					}
				}
			}
		}
	}

	// Populate the floor selector with the floors in the selected floor stack.
	private fun populateFloors(floorStackId: String) {
		val floorStack = floorStacks.find { it.id == floorStackId } ?: return

		// Get Floor objects for all floor IDs in this floor stack
		currentFloors =
			floorStack.floors
				.mapNotNull { floorId ->
					allFloors.find { it.id == floorId }
				}.sortedByDescending { it.elevation }

		val floorNames = currentFloors.map { it.name }

		val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, floorNames)
		adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

		isUpdatingSpinners = true
		floorSelector.adapter = adapter

		// Set the initial floor to the current floor.
		mapView.currentFloor { result ->
			result
				.onSuccess { currentFloor ->
					currentFloor?.let { floor ->
						val index = currentFloors.indexOfFirst { it.id == floor.id }
						if (index >= 0) {
							runOnUiThread {
								floorSelector.setSelection(index)
								// Only clear the flag after we've set the selection
								isUpdatingSpinners = false
							}
						} else {
							runOnUiThread {
								isUpdatingSpinners = false
							}
						}
					} ?: runOnUiThread {
						isUpdatingSpinners = false
					}
				}.onFailure {
					runOnUiThread {
						isUpdatingSpinners = false
					}
				}
		}
	}

	private fun setupListeners() {
		// Act on the floor-change event to update the floor selector.
		mapView.on(Events.FLOOR_CHANGE) { data ->
			val payload = data as? FloorChangePayload
			payload?.let {
				Log.d(
					"MappedinDemo",
					"Floor changed to: ${it.floor.name} in building: ${it.floor.floorStack?.name}",
				)

				runOnUiThread {
					isUpdatingFromEvent = true
					isUpdatingSpinners = true
					// Find the floor in our current floor list
					val index = currentFloors.indexOfFirst { floor -> floor.id == it.floor.id }
					if (index >= 0) {
						floorSelector.setSelection(index)
					}
					isUpdatingSpinners = false
					isUpdatingFromEvent = false
				}
			}
		}

		// Act on the floor-selector change event to change the Floor.
		floorSelector.onItemSelectedListener =
			object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long,
				) {
					if (isUpdatingFromEvent || isUpdatingSpinners) return

					if (position >= 0 && position < currentFloors.size) {
						val selectedFloor = currentFloors[position]
						mapView.setFloor(selectedFloor.id) {
							it.onSuccess {
								// Focus the camera on the selected floor.
								mapView.camera.focusOn(selectedFloor) {}
							}
						}
					}
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}

		// Act on the building-selector change event to change the FloorStack.
		buildingSelector.onItemSelectedListener =
			object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long,
				) {
					if (isUpdatingFromEvent || isUpdatingSpinners) return
					if (position >= 0 && position < floorStacks.size) {
						val selectedFloorStack = floorStacks[position]
						mapView.setFloorStack(selectedFloorStack.id) {
							it.onSuccess {
								runOnUiThread {
									populateFloors(selectedFloorStack.id)
								}
								// Focus the camera on the current floor.
								mapView.currentFloor { result ->
									result.onSuccess { currentFloor ->
										currentFloor?.let { floor ->
											mapView.camera.focusOn(floor) {}
										}
									}
								}
							}
						}
					}
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}
	}
}
