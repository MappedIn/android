package com.mappedin.demo

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mappedin.MapView
import com.mappedin.models.IconType
import com.mappedin.models.MappedinIcon

/**
 * Shows that icon SVGs use `fill="currentColor"` and recolor instantly without re-fetching.
 *
 * Tapping a swatch updates the `currentColor` applied to every rendered icon.
 */
class ColorPickerFragment : Fragment() {
	private companion object {
		const val SPAN_COUNT = 4
		const val SAMPLE_SIZE = 40
		val SWATCHES = listOf("#333333", "#ff5733", "#2266ff", "#1faa59", "#a855f7", "#e91e63")
	}

	private val mapView: MapView
		get() = (requireActivity() as IconsDemoActivity).mapView

	private val icons = mutableListOf<MappedinIcon>()
	private val svgCache = mutableMapOf<String, String>()
	private var currentColor = SWATCHES.first()
	private lateinit var adapter: ColorAdapter

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val density = resources.displayMetrics.density

		fun dp(value: Int) = (value * density).toInt()

		val root =
			LinearLayout(requireContext()).apply {
				orientation = LinearLayout.VERTICAL
			}

		val explanation =
			TextView(requireContext()).apply {
				text = "Icons use fill=\"currentColor\" and inherit the container color. Tap a swatch to recolor."
				textSize = 13f
				setTextColor(Color.DKGRAY)
				setPadding(dp(12), dp(12), dp(12), dp(8))
			}
		root.addView(explanation)

		val swatchRow =
			LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(dp(8), dp(4), dp(8), dp(8))
			}
		SWATCHES.forEach { hex ->
			val swatch =
				View(requireContext()).apply {
					setBackgroundColor(Color.parseColor(hex))
					layoutParams =
						LinearLayout.LayoutParams(dp(36), dp(36)).apply {
							marginEnd = dp(8)
						}
					setOnClickListener {
						currentColor = hex
						adapter.notifyDataSetChanged()
					}
				}
			swatchRow.addView(swatch)
		}
		root.addView(swatchRow)

		val recycler = RecyclerView(requireContext())
		adapter = ColorAdapter()
		recycler.layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
		recycler.adapter = adapter
		root.addView(recycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

		return root
	}

	override fun onViewCreated(
		view: View,
		savedInstanceState: Bundle?,
	) {
		super.onViewCreated(view, savedInstanceState)
		// The fragment instance is reused when its view is recreated, so reset
		// before populating to avoid duplicating samples.
		icons.clear()
		adapter.notifyDataSetChanged()
		mapView.icons.getByType(IconType.CATEGORIES) { result ->
			val all = result.getOrNull() ?: return@getByType
			requireActivity().runOnUiThread {
				icons.addAll(all.take(SAMPLE_SIZE))
				adapter.notifyDataSetChanged()
			}
		}
	}

	private inner class ColorAdapter : RecyclerView.Adapter<IconHolder>() {
		override fun getItemCount(): Int = icons.size

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int,
		): IconHolder {
			val density = parent.resources.displayMetrics.density

			fun dp(value: Int) = (value * density).toInt()

			val cell =
				LinearLayout(parent.context).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER_HORIZONTAL
					setPadding(dp(4), dp(8), dp(4), dp(8))
				}
			val webView =
				WebView(parent.context).apply {
					layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
					isVerticalScrollBarEnabled = false
					isHorizontalScrollBarEnabled = false
				}
			cell.addView(webView)
			return IconHolder(cell, webView)
		}

		override fun onBindViewHolder(
			holder: IconHolder,
			position: Int,
		) {
			val icon = icons[position]
			holder.currentName = icon.name

			val cached = svgCache[icon.name]
			if (cached != null) {
				holder.webView.renderIconSvg(cached, currentColor)
				return
			}
			mapView.icons.fetchSvg(icon.name) { result ->
				val svg = result.getOrNull() ?: return@fetchSvg
				svgCache[icon.name] = svg
				holder.webView.post {
					if (holder.currentName == icon.name) {
						holder.webView.renderIconSvg(svg, currentColor)
					}
				}
			}
		}
	}

	private class IconHolder(
		view: View,
		val webView: WebView,
		var currentName: String? = null,
	) : RecyclerView.ViewHolder(view)
}
