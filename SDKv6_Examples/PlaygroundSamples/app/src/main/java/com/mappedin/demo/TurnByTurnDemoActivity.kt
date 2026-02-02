package com.mappedin.demo

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
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
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.AddMarkerOptions
import com.mappedin.models.AddPathOptions
import com.mappedin.models.CollisionRankingTier
import com.mappedin.models.Directions
import com.mappedin.models.FocusOnOptions
import com.mappedin.models.FocusTarget
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.InsetPadding
import com.mappedin.models.MapDataType
import com.mappedin.models.MapObject
import com.mappedin.models.NavigationTarget
import com.mappedin.models.Path
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import com.mappedin.models.Width
import kotlin.math.roundToInt

class TurnByTurnDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private var currentDirections: Directions? = null
	private var currentPath: Path? = null

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
				text = "Turn by Turn"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Displays turn-by-turn navigation instructions with markers at each step."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Add display mode selector
		val selectorContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(dp(16), dp(0), dp(16), dp(8))
				gravity = Gravity.CENTER_VERTICAL
			}

		val selectorLabel =
			TextView(this).apply {
				text = "Display Mode: "
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			}

		val spinner = Spinner(this)
		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Navigation", "Path"))
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		spinner.adapter = adapter

		spinner.onItemSelectedListener =
			object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long,
				) {
					onDisplayModeChanged(position)
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}

		selectorContainer.addView(selectorLabel)
		selectorContainer.addView(spinner)
		root.addView(selectorContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
				mapId = "67881b4666a208000badecc4",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { r2 ->
					r2.onSuccess {
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
						}
						onMapReady()
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
				android.util.Log.e("TurnByTurnDemoActivity", "getMapData error: $it")
			}
		}
	}

	private fun onMapReady() {
		// Add labels to all named spaces
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { spacesResult ->
			spacesResult.onSuccess { spaces ->
				spaces.forEach { space ->
					if (space.name.isNotEmpty()) {
						mapView.labels.add(
							target = space,
							text = space.name,
							options = AddLabelOptions(interactive = true),
						) { }
					}
				}

				// Find destination space
				val destination = spaces.find { it.name == "Family Med Lab EN-09" }

				// Get origin object
				mapView.mapData.getByType<MapObject>(MapDataType.MAP_OBJECT) { objResult ->
					objResult.onSuccess { objects ->
						val origin = objects.find { it.name == "Lobby" }

						if (origin != null && destination != null) {
							getAndDisplayDirections(origin, destination)
						}
					}
				}
			}
		}
	}

	private fun getAndDisplayDirections(
		origin: MapObject,
		destination: Space,
	) {
		mapView.mapData.getDirections(
			NavigationTarget.MapObjectTarget(origin),
			NavigationTarget.SpaceTarget(destination),
		) { result ->
			result.onSuccess { directions ->
				if (directions != null) {
					currentDirections = directions

					// Focus on the first 3 steps in the journey
					val focusCoordinates =
						directions.coordinates.take(3).map {
							FocusTarget.CoordinateTarget(it)
						}

					val focusOptions =
						FocusOnOptions(
							screenOffsets =
								InsetPadding(
									top = 50.0,
									left = 50.0,
									bottom = 50.0,
									right = 50.0,
								),
						)

					mapView.camera.focusOn(focusCoordinates, focusOptions)

					// Add markers for each direction instruction
					addInstructionMarkers(directions)

					// Draw navigation by default
					drawNavigation()
				}
			}
		}
	}

	private fun addInstructionMarkers(directions: Directions) {
		val instructions = directions.instructions

		for (i in instructions.indices) {
			val instruction = instructions[i]
			val nextInstruction = if (i < instructions.size - 1) instructions[i + 1] else null
			val isLastInstruction = i == instructions.size - 1

			val markerText =
				if (isLastInstruction) {
					"You Arrived!"
				} else {
					val actionType = instruction.action.type
					val bearing = instruction.action.bearing ?: ""
					val distance = nextInstruction?.distance?.roundToInt() ?: 0
					"$actionType $bearing and go $distance meters"
				}

			val markerTemplate =
				"""
				<div style="
					background: white;
					padding: 8px 12px;
					border-radius: 8px;
					box-shadow: 0 2px 8px rgba(0,0,0,0.15);
					font-family: -apple-system, BlinkMacSystemFont, sans-serif;
					font-size: 12px;
					white-space: nowrap;
				">
					<p style="margin: 0;">$markerText</p>
				</div>
				""".trimIndent()

			mapView.markers.add(
				instruction.coordinate,
				markerTemplate,
				AddMarkerOptions(
					rank = AddMarkerOptions.Rank.Tier(CollisionRankingTier.ALWAYS_VISIBLE),
				),
			) { }
		}
	}

	private fun onDisplayModeChanged(position: Int) {
		// Clear existing path and navigation
		currentPath?.let {
			mapView.paths.remove(it)
			currentPath = null
		}
		mapView.navigation.clear()

		if (position == 0) {
			// Navigation mode
			drawNavigation()
		} else {
			// Path mode
			drawPath()
		}
	}

	private fun drawPath() {
		val directions = currentDirections ?: return

		val pathOptions =
			AddPathOptions(
				width = Width.Value(0.5),
			)

		mapView.paths.add(directions.coordinates, pathOptions) { result ->
			result.onSuccess { path ->
				currentPath = path
			}
		}
	}

	private fun drawNavigation() {
		val directions = currentDirections ?: return
		mapView.navigation.draw(directions) { }
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
