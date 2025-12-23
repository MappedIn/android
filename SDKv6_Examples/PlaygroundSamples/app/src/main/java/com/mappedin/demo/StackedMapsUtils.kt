package com.mappedin.demo

import com.mappedin.MapView
import com.mappedin.data.MapData
import com.mappedin.models.Floor
import com.mappedin.models.FloorUpdateState
import com.mappedin.models.MapDataType

/**
 * Options for expanding floors in a stacked view.
 *
 * @param distanceBetweenFloors The vertical spacing between floors in meters. Default: 10
 * @param animate Whether to animate the floor expansion. Default: true
 * @param cameraPanMode The camera pan mode to use ("default" or "elevation"). Default: "elevation"
 */
data class ExpandOptions(
	val distanceBetweenFloors: Double = 10.0,
	val animate: Boolean = true,
	val cameraPanMode: String = "elevation",
)

/**
 * Options for collapsing floors back to their original positions.
 *
 * @param animate Whether to animate the floor collapse. Default: true
 */
data class CollapseOptions(
	val animate: Boolean = true,
)

/**
 * Utility object for managing stacked floor views.
 *
 * Provides functions to expand all floors vertically (stacked view) and collapse them back
 * to a single floor view. This creates a 3D exploded view effect where all floors are visible
 * at different altitudes.
 *
 * Example usage:
 * ```kotlin
 * // Expand floors with default options
 * StackedMapsUtils.expandFloors(mapView)
 *
 * // Expand floors with custom gap
 * StackedMapsUtils.expandFloors(mapView, ExpandOptions(distanceBetweenFloors = 20.0))
 *
 * // Collapse floors back
 * StackedMapsUtils.collapseFloors(mapView)
 * ```
 */
object StackedMapsUtils {

	/**
	 * Expands all floors vertically to create a stacked view.
	 *
	 * Each floor is positioned at an altitude based on its elevation multiplied by the
	 * distance between floors. This creates a 3D exploded view where all floors are visible.
	 *
	 * @param mapView The MapView instance
	 * @param options Options controlling the expansion behavior
	 */
	fun expandFloors(
		mapView: MapView,
		options: ExpandOptions = ExpandOptions(),
	) {
		// Set camera pan mode to elevation for better navigation in stacked view
		mapView.camera.setPanMode(options.cameraPanMode)

		// Get the current floor ID to identify the active floor
		mapView.currentFloor { currentFloorResult ->
			val currentFloorId = currentFloorResult.getOrNull()?.id

			// Get all floors
			mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { result ->
				result.onSuccess { floors ->
					floors.forEach { floor ->
						val newAltitude = floor.elevation * options.distanceBetweenFloors
						val isCurrentFloor = floor.id == currentFloorId

						// First, make sure the floor is visible
						mapView.getState(floor) { stateResult ->
							stateResult.onSuccess { currentState ->
								if (currentState != null &&
									(!currentState.visible || !currentState.geometry.visible)
								) {
									// Make the floor visible first with 0 opacity if not current
									mapView.updateState(
										floor,
										FloorUpdateState(
											visible = true,
											altitude = 0.0,
											geometry =
												FloorUpdateState.Geometry(
													visible = true,
													opacity = if (isCurrentFloor) 1.0 else 0.0,
												),
										),
									)
								}

								// Then animate or update to the new altitude
								if (options.animate) {
									mapView.animateState(
										floor,
										FloorUpdateState(
											altitude = newAltitude,
											geometry =
												FloorUpdateState.Geometry(
													opacity = 1.0,
												),
										),
									)
								} else {
									mapView.updateState(
										floor,
										FloorUpdateState(
											altitude = newAltitude,
											visible = true,
											geometry =
												FloorUpdateState.Geometry(
													visible = true,
													opacity = 1.0,
												),
										),
									)
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Collapses all floors back to their original positions.
	 *
	 * Floors are returned to altitude 0, and only the current floor remains fully visible.
	 * Other floors are hidden to restore the standard single-floor view.
	 *
	 * @param mapView The MapView instance
	 * @param options Options controlling the collapse behavior
	 */
	fun collapseFloors(
		mapView: MapView,
		options: CollapseOptions = CollapseOptions(),
	) {
		// Reset camera pan mode to default
		mapView.camera.setPanMode("default")

		// Get the current floor ID to identify the active floor
		mapView.currentFloor { currentFloorResult ->
			val currentFloorId = currentFloorResult.getOrNull()?.id

			// Get all floors
			mapView.mapData.getByType<Floor>(MapDataType.FLOOR) { result ->
				result.onSuccess { floors ->
					floors.forEach { floor ->
						val isCurrentFloor = floor.id == currentFloorId

						if (options.animate) {
							// Animate to altitude 0 and fade out non-current floors
							mapView.animateState(
								floor,
								FloorUpdateState(
									altitude = 0.0,
									geometry =
										FloorUpdateState.Geometry(
											opacity = if (isCurrentFloor) 1.0 else 0.0,
										),
								),
							)

							// After animation, hide non-current floors
							if (!isCurrentFloor) {
								// Use a handler to delay hiding the floor
								android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
									mapView.updateState(
										floor,
										FloorUpdateState(
											visible = false,
											altitude = 0.0,
											geometry =
												FloorUpdateState.Geometry(
													visible = false,
													opacity = 0.0,
												),
										),
									)
								}, 1000) // Default animation duration
							}
						} else {
							mapView.updateState(
								floor,
								FloorUpdateState(
									altitude = 0.0,
									visible = isCurrentFloor,
									geometry =
										FloorUpdateState.Geometry(
											visible = isCurrentFloor,
											opacity = if (isCurrentFloor) 1.0 else 0.0,
										),
								),
							)
						}
					}
				}
			}
		}
	}
}

