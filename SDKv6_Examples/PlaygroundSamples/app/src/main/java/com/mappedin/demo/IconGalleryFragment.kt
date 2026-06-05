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
 * Each icon is fetched with `mapView.icons.fetchSvg(...)` and rendered into a
 * small [WebView] cell. Every available icon is shown so developers can browse
 * the full set; the [RecyclerView] recycles cells to stay responsive.
 */
class IconGalleryFragment : Fragment() {
	private companion object {
		const val SPAN_COUNT = 4
	}

	private sealed interface GalleryItem {
		data class Header(
			val title: String,
		) : GalleryItem

		data class Icon(
			val icon: MappedinIcon,
		) : GalleryItem
	}

	private val mapView: MapView
		get() = (requireActivity() as IconsDemoActivity).mapView

	private val items = mutableListOf<GalleryItem>()
	private lateinit var adapter: GalleryAdapter

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val recycler = RecyclerView(requireContext())
		adapter = GalleryAdapter()
		val layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
		layoutManager.spanSizeLookup =
			object : GridLayoutManager.SpanSizeLookup() {
				override fun getSpanSize(position: Int): Int = if (items[position] is GalleryItem.Header) SPAN_COUNT else 1
			}
		recycler.layoutManager = layoutManager
		recycler.adapter = adapter
		return recycler
	}

	override fun onViewCreated(
		view: View,
		savedInstanceState: Bundle?,
	) {
		super.onViewCreated(view, savedInstanceState)
		// The fragment instance is reused when its view is recreated, so reset
		// before loading to avoid duplicating sections.
		items.clear()
		adapter.notifyDataSetChanged()
		// Load sequentially so sections always appear in this order, regardless
		// of how quickly each getByType call resolves.
		loadTypesInOrder(listOf(IconType.CATEGORIES, IconType.SMALL, IconType.INPUT, IconType.METADATA))
	}

	private fun loadTypesInOrder(types: List<IconType>) {
		val type = types.firstOrNull() ?: return
		val rest = types.drop(1)
		mapView.icons.getByType(type) { result ->
			val icons = result.getOrNull()
			val activity = activity ?: return@getByType
			activity.runOnUiThread {
				if (!isAdded) return@runOnUiThread
				if (icons != null) {
					items.add(GalleryItem.Header("${type.value} (${icons.size})"))
					icons.forEach { items.add(GalleryItem.Icon(it)) }
					adapter.notifyDataSetChanged()
				}
				loadTypesInOrder(rest)
			}
		}
	}

	private inner class GalleryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		private val typeHeader = 0
		private val typeIcon = 1

		override fun getItemViewType(position: Int): Int = if (items[position] is GalleryItem.Header) typeHeader else typeIcon

		override fun getItemCount(): Int = items.size

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int,
		): RecyclerView.ViewHolder {
			val density = parent.resources.displayMetrics.density

			fun dp(value: Int) = (value * density).toInt()

			return if (viewType == typeHeader) {
				val text =
					TextView(parent.context).apply {
						textSize = 16f
						setTextColor(Color.DKGRAY)
						setPadding(dp(12), dp(16), dp(12), dp(8))
					}
				HeaderHolder(text)
			} else {
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
				val label =
					TextView(parent.context).apply {
						textSize = 9f
						gravity = Gravity.CENTER
						setTextColor(Color.GRAY)
						maxLines = 2
						setPadding(0, dp(4), 0, 0)
					}
				cell.addView(webView)
				cell.addView(label)
				IconHolder(cell, webView, label)
			}
		}

		override fun onBindViewHolder(
			holder: RecyclerView.ViewHolder,
			position: Int,
		) {
			when (val item = items[position]) {
				is GalleryItem.Header -> (holder as HeaderHolder).text.text = item.title
				is GalleryItem.Icon -> bindIcon(holder as IconHolder, item.icon)
			}
		}

		private fun bindIcon(
			holder: IconHolder,
			icon: MappedinIcon,
		) {
			holder.label.text = icon.name
			holder.currentName = icon.name
			mapView.icons.fetchSvg(icon.name) { result ->
				val svg = result.getOrNull() ?: return@fetchSvg
				holder.webView.post {
					if (holder.currentName == icon.name) {
						holder.webView.renderIconSvg(svg, "#333333")
					}
				}
			}
		}
	}

	private class HeaderHolder(
		val text: TextView,
	) : RecyclerView.ViewHolder(text)

	private class IconHolder(
		view: View,
		val webView: WebView,
		val label: TextView,
		var currentName: String? = null,
	) : RecyclerView.ViewHolder(view)
}
