package com.mappedin.demo

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddMarkerOptions
import com.mappedin.models.CollisionRankingTier
import com.mappedin.models.Directions
import com.mappedin.models.FocusTarget
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.NavigationOptions
import com.mappedin.models.NavigationTarget
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

class NavigationDemoActivity : AppCompatActivity() {
	private var currentDirections: Directions? = null
	private lateinit var mapView: MapView
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
				text = "Navigation"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Demonstrates custom marker configurations for navigation"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Add heading
		val headingView =
			TextView(this).apply {
				text = "Choose Marker Option"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
				setPadding(dp(16), dp(8), dp(16), dp(4))
			}
		root.addView(headingView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Add button controls
		val buttonContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(dp(16), dp(0), dp(16), dp(8))
				gravity = Gravity.CENTER
			}

		val defaultButton =
			Button(this).apply {
				text = "Default"
				setOnClickListener { drawWithMode(1) }
			}
		val noMarkersButton =
			Button(this).apply {
				text = "No Start / End"
				setOnClickListener { drawWithMode(2) }
			}
		val pirateButton =
			Button(this).apply {
				text = "Custom"
				setOnClickListener { drawWithMode(3) }
			}

		buttonContainer.addView(
			defaultButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				setMargins(dp(4), 0, dp(4), 0)
			},
		)
		buttonContainer.addView(
			noMarkersButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				setMargins(dp(4), 0, dp(4), 0)
			},
		)
		buttonContainer.addView(
			pirateButton,
			LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
				setMargins(dp(4), 0, dp(4), 0)
			},
		)
		root.addView(buttonContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
				mapId = "64ef49e662fd90fe020bee61",
			)

		mapView.getMapData(options) { r ->
			r.onSuccess {
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
		}
	}

	private fun onMapReady(mapView: MapView) {
		mapView.mapData.getByType<Space>(com.mappedin.models.MapDataType.SPACE) { res ->
			res.onSuccess { spaces ->
				if (spaces.size < 2) return@onSuccess
				val origin = spaces.find { (it.name == "Oak Meeting Room") }
				val destination = spaces.find { it.name == "Office 211 \uD83D\uDCBC" }
				if (origin != null && destination != null) {
					mapView.mapData.getDirections(
						NavigationTarget.SpaceTarget(origin),
						NavigationTarget.SpaceTarget(destination),
					) { dRes ->
						dRes.onSuccess { directions ->
							if (directions != null) {
								currentDirections = directions
								mapView.camera.focusOn(
									directions.coordinates.map { FocusTarget.CoordinateTarget(it) },
								)
								// Draw with default mode initially
								drawWithMode(1)
							}
						}
					}
				}
			}
		}
	}

	private fun drawWithMode(mode: Int) {
		val directions = currentDirections ?: return

		// Clear existing navigation
		mapView.navigation.clear()

		val pathOptions =
			com.mappedin.models.AddPathOptions(
				color = "#4b90e2",
				displayArrowsOnPath = true,
				animateDrawing = true,
			)

		val navOptions =
			when (mode) {
				1 -> {
					// Mode 1: Default markers (don't specify createMarkers)
					NavigationOptions(pathOptions = pathOptions)
				}
				2 -> {
					// Mode 2: No markers for departure/destination, default for connection
					NavigationOptions(
						pathOptions = pathOptions,
						createMarkers =
							NavigationOptions.CreateMarkers.withDefaults(
								departure = false,
								destination = false,
								connection = true,
							),
					)
				}
				3 -> {
					// Mode 3: Custom markers
					NavigationOptions(
						pathOptions = pathOptions,
						createMarkers =
							NavigationOptions.CreateMarkers.withCustomMarkers(
								departure =
									NavigationOptions.CreateMarkers.CreateMarkerValue.CustomMarker(
										template = getPirateDepartureMarker(),
										options =
											AddMarkerOptions(
												rank = AddMarkerOptions.Rank.Tier(CollisionRankingTier.ALWAYS_VISIBLE),
												interactive = AddMarkerOptions.Interactive.True,
											),
									),
								destination =
									NavigationOptions.CreateMarkers.CreateMarkerValue.CustomMarker(
										template = getPirateDestinationMarker(),
										options =
											AddMarkerOptions(
												rank = AddMarkerOptions.Rank.Tier(CollisionRankingTier.ALWAYS_VISIBLE),
												interactive = AddMarkerOptions.Interactive.True,
											),
									),
								connection =
									NavigationOptions.CreateMarkers.CreateMarkerValue.CustomMarker(
										template = getPirateConnectionMarker(),
										options =
											AddMarkerOptions(
												rank = AddMarkerOptions.Rank.Tier(CollisionRankingTier.ALWAYS_VISIBLE),
												interactive = AddMarkerOptions.Interactive.True,
											),
									),
							),
					)
				}
				else -> NavigationOptions(pathOptions = pathOptions)
			}

		mapView.navigation.draw(directions, navOptions) { }
	}

	private fun getPirateDepartureMarker(): String =
		"""
		<svg width="48" height="48" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
			<!-- Pirate Ship -->
			<circle cx="24" cy="24" r="22" fill="#8B4513" opacity="0.2"/>
			<path d="M12 28 L12 22 L18 18 L30 18 L36 22 L36 28 L32 32 L16 32 Z" fill="#8B4513" stroke="#5D2E0F" stroke-width="2"/>
			<rect x="22" y="10" width="4" height="12" fill="#5D2E0F"/>
			<path d="M26 10 L36 14 L26 18" fill="#DC143C"/>
			<circle cx="24" cy="24" r="3" fill="#FFD700"/>
		</svg>
		""".trimIndent()

	private fun getPirateDestinationMarker(): String =
		"""
		<svg width="56" height="56" viewBox="0 0 56 56" xmlns="http://www.w3.org/2000/svg">
			<!-- Background glow -->
			<circle cx="28" cy="28" r="26" fill="#FFD700" opacity="0.2"/>

			<!-- Treasure chest body -->
			<rect x="14" y="24" width="28" height="18" rx="2" fill="#8B4513" stroke="#5D2E0F" stroke-width="2"/>

			<!-- Chest lid -->
			<path d="M 14 24 Q 14 18 20 16 L 36 16 Q 42 18 42 24" fill="#6D3913" stroke="#5D2E0F" stroke-width="2"/>

			<!-- Lid highlight -->
			<path d="M 16 24 Q 16 20 20 18 L 36 18 Q 40 20 40 24" fill="#8B4513"/>

			<!-- Front band -->
			<rect x="14" y="24" width="28" height="5" fill="#5D2E0F"/>

			<!-- Center lock plate -->
			<rect x="26" y="24" width="4" height="18" fill="#5D2E0F"/>

			<!-- Lock -->
			<circle cx="28" cy="33" r="3" fill="#DAA520" stroke="#8B6914" stroke-width="1"/>
			<circle cx="28" cy="33" r="1.5" fill="#5D2E0F"/>

			<!-- Gold coins spilling out -->
			<circle cx="20" cy="30" r="2.5" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
			<circle cx="36" cy="30" r="2.5" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
			<circle cx="18" cy="36" r="2" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
			<circle cx="38" cy="36" r="2" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
			<circle cx="24" cy="38" r="2" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
			<circle cx="32" cy="38" r="2" fill="#FFD700" stroke="#DAA520" stroke-width="1"/>
		</svg>
		""".trimIndent()

	private fun getPirateConnectionMarker(): String =
		"""
		<svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
			<!-- Compass -->
			<circle cx="20" cy="20" r="18" fill="#2C3E50" opacity="0.2"/>
			<circle cx="20" cy="20" r="14" fill="#34495E" stroke="#95A5A6" stroke-width="2"/>
			<circle cx="20" cy="20" r="10" fill="#2C3E50"/>
			<path d="M20 12 L23 20 L20 22 L17 20 Z" fill="#DC143C"/>
			<path d="M20 28 L23 20 L20 18 L17 20 Z" fill="#ECF0F1"/>
			<circle cx="20" cy="20" r="2" fill="#FFD700"/>
		</svg>
		""".trimIndent()

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
