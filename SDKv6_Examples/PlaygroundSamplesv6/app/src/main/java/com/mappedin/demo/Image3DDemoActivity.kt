package com.mappedin.demo

import android.graphics.Color
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
import com.mappedin.models.AddImageOptions
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

class Image3DDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private var arenaFloor: Space? = null
	private val pixelsToMeters = 0.0617

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
						dp(12)
					)
					insets
				}
			}
		val titleView =
			TextView(this).apply {
				text = "Image3D"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Places billboard images on the Arena Floor and allows switching between them."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}

		// Image selector spinner
		val selectorLabel = TextView(this).apply {
			text = "Select Image:"
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
			setPadding(0, dp(8), 0, dp(4))
		}

		val imageSpinner = Spinner(this).apply {
			val items = listOf("Hockey", "Basketball", "Concert")
			adapter = ArrayAdapter(this@Image3DDemoActivity, android.R.layout.simple_spinner_item, items).apply {
				setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
			}
			onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
					onImageSelected(position)
				}
				override fun onNothingSelected(parent: AdapterView<*>?) {}
			}
		}

		header.addView(titleView)
		header.addView(descriptionView)
		header.addView(selectorLabel)
		header.addView(imageSpinner)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
		val loadingParams = FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
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
				mapId = "672a6f4f3a45ba000b893e1c",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
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
		// Add labels to all named spaces
		mapView.mapData.getByType<Space>(com.mappedin.models.MapDataType.SPACE) { r ->
			r.onSuccess { spaces ->
				spaces.filter { it.name.isNotEmpty() }.forEach { space ->
					mapView.labels.add(space, space.name, AddLabelOptions(interactive = true)) { }
				}

				// Find the Arena Floor space
				arenaFloor = spaces.find { it.name == "Arena Floor" }

				// Add the default hockey image to the arena floor
				arenaFloor?.let { floor ->
					val imageResource = getImageResource(0)
					val opts = AddImageOptions(
						width = 1014 * pixelsToMeters,
						height = 448 * pixelsToMeters,
						rotation = 239.0,
						verticalOffset = 1.0,
						flipImageToFaceCamera = false,
					)
					mapView.image3D.add(floor, imageResource, opts) { }
				}
			}
		}
	}

	private fun onImageSelected(position: Int) {
		arenaFloor?.let { floor ->
			mapView.image3D.removeAll()

			val imageResource = getImageResource(position)
			val opts = AddImageOptions(
				width = 1014 * pixelsToMeters,
				height = 448 * pixelsToMeters,
				rotation = 239.0,
				verticalOffset = 1.0,
				flipImageToFaceCamera = false,
			)
			mapView.image3D.add(floor, imageResource, opts) { }
		}
	}

	private fun getImageResource(position: Int): String {
		return when (position) {
			0 -> "https://appassets.androidplatform.net/assets/arena_hockey.png"
			1 -> "https://appassets.androidplatform.net/assets/arena_basketball.png"
			2 -> "https://appassets.androidplatform.net/assets/arena_concert.png"
			else -> "https://appassets.androidplatform.net/assets/arena_hockey.png"
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}

