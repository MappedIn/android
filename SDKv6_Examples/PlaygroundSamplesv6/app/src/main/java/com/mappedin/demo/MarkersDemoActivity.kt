package com.mappedin.demo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.mappedin.models.AnimationOptions
import com.mappedin.models.Annotation
import com.mappedin.models.ClickPayload
import com.mappedin.models.CollisionRankingTier
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MarkerPlacement
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

class MarkersDemoActivity : AppCompatActivity() {
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
						dp(12)
					)
					insets
				}
			}
		val titleView =
			TextView(this).apply {
				text = "Markers"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Markers are added to show annotations on the map. Click a Marker to remove it."
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
				mapId = "67a6641530e940000bac3c1a",
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
		mapView.mapData.getByType<Annotation>(com.mappedin.models.MapDataType.ANNOTATION) { r ->
			r.onSuccess { annotations ->
				val opts =
					AddMarkerOptions(
						interactive = AddMarkerOptions.Interactive.True,
						placement = AddMarkerOptions.Placement.Single(MarkerPlacement.CENTER),
						rank = AddMarkerOptions.Rank.Tier(CollisionRankingTier.HIGH),
					)

				// Add markers for all annotations that have icons.
				annotations.forEach { annotation ->
					val iconUrl = annotation.icon?.url
					val markerHtml =
						"""
						<div class='mappedin-annotation-marker'>
							<div style='width: 30px; height: 30px'>
							<img src='$iconUrl' alt='${annotation.name}' width='30' height='30' />
							</div>
						</div>
						""".trimIndent()
					mapView.markers.add(annotation, markerHtml, opts) { }
				}

				// Remove markers that are clicked on.
				mapView.on("click") { payload ->
					val click = payload as? ClickPayload
					val clickedMarker = click?.markers?.firstOrNull() ?: return@on
					mapView.markers.remove(clickedMarker) { }
				}
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}

