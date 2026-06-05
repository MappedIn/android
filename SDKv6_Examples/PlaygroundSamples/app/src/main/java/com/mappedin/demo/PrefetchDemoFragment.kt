package com.mappedin.demo

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mappedin.MapView
import com.mappedin.models.IconCategory
import com.mappedin.models.IconSubtype
import com.mappedin.models.IconType
import com.mappedin.models.MappedinIcon

/**
 * Mirrors the web "Prefetch Demo" example: exercises the prefetch APIs, cache
 * checks (`isCached`), cached-vs-uncached fetch timing, and `clearCache`.
 */
class PrefetchDemoFragment : Fragment() {
    private companion object {
        const val SPAN_COUNT = 4
        const val MAX_RESULTS = 20
        val NAMES = listOf("information", "elevator-up", "book")
    }

    private val mapView: MapView
        get() = (requireActivity() as IconsDemoActivity).mapView

    private val results = mutableListOf<MappedinIcon>()
    private lateinit var adapter: ResultAdapter
    private lateinit var statusLabel: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

        val buttons =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
        buttons.addView(button("Prefetch by names") { prefetchNames() })
        buttons.addView(button("Prefetch by type (Small)") { prefetchType() })
        buttons.addView(button("Prefetch by subtype (Amenities)") { prefetchSubtype() })
        buttons.addView(button("Prefetch by category (Food and drink)") { prefetchCategory() })
        buttons.addView(button("Cached vs uncached fetch") { compareFetch() })
        buttons.addView(button("Clear cache") { clearCache() })

        val scrollButtons = ScrollView(requireContext())
        scrollButtons.addView(buttons)
        root.addView(scrollButtons, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        statusLabel =
            TextView(requireContext()).apply {
                text = "Tap a button to begin."
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(Color.DKGRAY)
                setPadding(dp(12), dp(4), dp(12), dp(8))
            }
        root.addView(statusLabel)

        val recycler = RecyclerView(requireContext())
        adapter = ResultAdapter()
        recycler.layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
        recycler.adapter = adapter
        root.addView(recycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        return root
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun setStatus(text: String) {
        requireActivity().runOnUiThread { statusLabel.text = text }
    }

    private fun showIcons(icons: List<MappedinIcon>) {
        requireActivity().runOnUiThread {
            results.clear()
            results.addAll(icons.take(MAX_RESULTS))
            adapter.notifyDataSetChanged()
        }
    }

    private fun prefetchNames() {
        setStatus("Prefetching ${NAMES.joinToString()} ...")
        mapView.icons.prefetch(NAMES) {
            mapView.icons.isCached(NAMES.first()) { cachedResult ->
                val cached = cachedResult.getOrNull() ?: false
                setStatus("Prefetched ${NAMES.size} icons. isCached(\"${NAMES.first()}\") = $cached")
            }
            mapView.icons.getByName(NAMES.first()) { /* warm up lookup */ }
            collectByNames(NAMES)
        }
    }

    private fun collectByNames(names: List<String>) {
        if (names.isEmpty()) {
            showIcons(emptyList())
            return
        }
        // Count every result (success or failure) so the grid still updates when
        // some lookups fail, instead of waiting forever for a full success count.
        val collected = mutableListOf<MappedinIcon>()
        var pending = names.size
        val lock = Any()
        names.forEach { name ->
            mapView.icons.getByName(name) { result ->
                synchronized(lock) {
                    result.getOrNull()?.let { collected.add(it) }
                    pending--
                    if (pending == 0) {
                        showIcons(collected.toList())
                    }
                }
            }
        }
    }

    private fun prefetchType() {
        setStatus("Prefetching all Small icons ...")
        mapView.icons.prefetchByType(IconType.SMALL) {
            mapView.icons.getByType(IconType.SMALL) { result ->
                val icons = result.getOrNull() ?: emptyList()
                setStatus("Prefetched ${icons.size} Small icons (showing first $MAX_RESULTS).")
                showIcons(icons)
            }
        }
    }

    private fun prefetchSubtype() {
        setStatus("Prefetching Amenities icons ...")
        mapView.icons.prefetchBySubtype(IconSubtype.AMENITIES) {
            mapView.icons.getBySubtype(IconSubtype.AMENITIES) { result ->
                val icons = result.getOrNull() ?: emptyList()
                setStatus("Prefetched ${icons.size} Amenities icons (showing first $MAX_RESULTS).")
                showIcons(icons)
            }
        }
    }

    private fun prefetchCategory() {
        setStatus("Prefetching Food and drink icons ...")
        mapView.icons.prefetchByCategory(IconCategory.FOOD_AND_DRINK) {
            mapView.icons.getByCategory(IconCategory.FOOD_AND_DRINK) { result ->
                val icons = result.getOrNull() ?: emptyList()
                setStatus("Prefetched ${icons.size} Food and drink icons (showing first $MAX_RESULTS).")
                showIcons(icons)
            }
        }
    }

    private fun compareFetch() {
        val name = NAMES.first()
        setStatus("Clearing cache then timing fetches for \"$name\" ...")
        mapView.icons.clearCache {
            val uncachedStart = System.nanoTime()
            mapView.icons.fetchSvg(name) {
                val uncachedMs = (System.nanoTime() - uncachedStart) / 1_000_000.0
                val cachedStart = System.nanoTime()
                mapView.icons.fetchSvg(name) {
                    val cachedMs = (System.nanoTime() - cachedStart) / 1_000_000.0
                    setStatus(
                        "Uncached: %.1f ms, cached: %.1f ms".format(uncachedMs, cachedMs),
                    )
                    mapView.icons.getByName(name) { result ->
                        result.getOrNull()?.let { showIcons(listOf(it)) }
                    }
                }
            }
        }
    }

    private fun clearCache() {
        mapView.icons.clearCache {
            setStatus("Cache cleared.")
            showIcons(emptyList())
        }
    }

    private inner class ResultAdapter : RecyclerView.Adapter<IconHolder>() {
        override fun getItemCount(): Int = results.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconHolder {
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
            val label =
                TextView(parent.context).apply {
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)
                    maxLines = 2
                }
            cell.addView(webView)
            cell.addView(label)
            return IconHolder(cell, webView, label)
        }

        override fun onBindViewHolder(holder: IconHolder, position: Int) {
            val icon = results[position]
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

    private class IconHolder(
        view: View,
        val webView: WebView,
        val label: TextView,
        var currentName: String? = null,
    ) : RecyclerView.ViewHolder(view)
}
