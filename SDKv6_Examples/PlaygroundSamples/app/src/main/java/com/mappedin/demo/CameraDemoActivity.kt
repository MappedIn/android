package com.mappedin.demo

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
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
import com.mappedin.models.CameraAnimationOptions
import com.mappedin.models.CameraTarget
import com.mappedin.models.Coordinate
import com.mappedin.models.Events
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.Show3DMapOptions

class CameraDemoActivity : AppCompatActivity() {
	private lateinit var loadingIndicator: ProgressBar
	private var defaultPitch: Double? = null
	private var defaultZoomLevel: Double? = null
	private var defaultBearing: Double? = null
	private var defaultCenter: Coordinate? = null

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
				text = "Camera"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Interactive camera controls to adjust pitch, zoom, and animate the camera. Click on the map to focus on the clicked location."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Map view container with loading indicator
		val mapContainer = FrameLayout(this)
		val mapView = MapView(this)
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

		// Add camera control buttons
		val controlsLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(8), dp(8), dp(8), dp(8))
			}
		root.addView(controlsLayout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		setupButtons(controlsLayout, mapView)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "67881b4666a208000badecc4",
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
			r.onFailure {
				runOnUiThread {
					loadingIndicator.visibility = View.GONE
				}
				Log.e("CameraDemoActivity", "getMapData error: $it")
			}
		}
	}

	private fun setupButtons(
		controlsLayout: LinearLayout,
		mapView: MapView,
	) {
		// Pitch buttons
		val pitchLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_HORIZONTAL
			}
		controlsLayout.addView(pitchLayout)

		val increasePitchBtn =
			Button(this).apply {
				text = "Increase Pitch"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					mapView.camera.pitch { result ->
						result.onSuccess { currentPitch ->
							val newPitch = (currentPitch ?: 0.0) + 15.0
							val transform = CameraTarget(pitch = newPitch)
							mapView.camera.set(transform) {}
						}
					}
				}
			}
		pitchLayout.addView(increasePitchBtn)

		val decreasePitchBtn =
			Button(this).apply {
				text = "Decrease Pitch"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					mapView.camera.pitch { result ->
						result.onSuccess { currentPitch ->
							val newPitch = (currentPitch ?: 0.0) - 15.0
							val transform = CameraTarget(pitch = newPitch)
							mapView.camera.set(transform) {}
						}
					}
				}
			}
		pitchLayout.addView(decreasePitchBtn)

		// Zoom buttons
		val zoomLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_HORIZONTAL
			}
		controlsLayout.addView(zoomLayout)

		val zoomInBtn =
			Button(this).apply {
				text = "Zoom In"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					mapView.camera.zoomLevel { result ->
						result.onSuccess { currentZoom ->
							val newZoom = (currentZoom ?: 0.0) + 1.0
							val transform = CameraTarget(zoomLevel = newZoom)
							mapView.camera.set(transform) {}
						}
					}
				}
			}
		zoomLayout.addView(zoomInBtn)

		val zoomOutBtn =
			Button(this).apply {
				text = "Zoom Out"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					mapView.camera.zoomLevel { result ->
						result.onSuccess { currentZoom ->
							val newZoom = (currentZoom ?: 0.0) - 1.0
							val transform = CameraTarget(zoomLevel = newZoom)
							mapView.camera.set(transform) {}
						}
					}
				}
			}
		zoomLayout.addView(zoomOutBtn)

		// Animate and Reset buttons
		val animateResetLayout =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_HORIZONTAL
			}
		controlsLayout.addView(animateResetLayout)

		val animateBtn =
			Button(this).apply {
				text = "Animate"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					mapView.camera.center { result ->
						result.onSuccess { center ->
							center?.let {
								val transform =
									CameraTarget(
										center = it,
										zoomLevel = 21.0,
										pitch = 60.0,
										bearing = 180.0,
									)
								val options = CameraAnimationOptions(duration = 3000)
								mapView.camera.animateTo(transform, options) {}
							}
						}
					}
				}
			}
		animateResetLayout.addView(animateBtn)

		val resetBtn =
			Button(this).apply {
				text = "Reset"
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
				setOnClickListener {
					val transform =
						CameraTarget(
							zoomLevel = defaultZoomLevel,
							pitch = defaultPitch,
							bearing = defaultBearing,
							center = defaultCenter,
						)
					mapView.camera.set(transform) {}
				}
			}
		animateResetLayout.addView(resetBtn)
	}

	private fun onMapReady(mapView: MapView) {
		// Store default camera values
		mapView.camera.pitch { result ->
			result.onSuccess { defaultPitch = it }
		}
		mapView.camera.zoomLevel { result ->
			result.onSuccess { defaultZoomLevel = it }
		}
		mapView.camera.bearing { result ->
			result.onSuccess { defaultBearing = it }
		}
		mapView.camera.center { result ->
			result.onSuccess { defaultCenter = it }
		}

		// Focus the camera on the click location.
		mapView.on(Events.Click) { clickPayload ->
			clickPayload?.coordinate?.let { coordinate ->
				mapView.camera.focusOn(coordinate)
			}
		}

		// Log camera change events to the console.
		mapView.on(Events.CameraChange) { cameraTransform ->
			cameraTransform?.let {
				Log.d(
					"MappedinDemo",
					"Camera changed to bearing: ${it.bearing}, pitch: ${it.pitch}, zoomLevel: ${it.zoomLevel}, center: Lat: ${it.center.latitude}, Lon: ${it.center.longitude}",
				)
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
