package com.mappedin.demo

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mappedin.MapView
import com.mappedin.models.EnterpriseLocation
import com.mappedin.models.GeometryUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.SearchResultEnterpriseLocations
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

// Simple wrapper to hold either a suggestion or a location
sealed class SearchListItem {
	data class Suggestion(
		val suggestion: com.mappedin.models.Suggestion,
	) : SearchListItem()

	data class Location(
		val result: SearchResultEnterpriseLocations,
	) : SearchListItem()
}

class SearchDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var searchView: SearchView
	private lateinit var recyclerView: RecyclerView
	private lateinit var progressBar: ProgressBar
	private lateinit var searchAdapter: SearchAdapter
	private val searchResults = mutableListOf<SearchListItem>()
	private var searchReady = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Search"

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
					val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
					view.setPadding(0, systemBars.top, 0, 0)
					insets
				}
			}
		setContentView(root)

		// Search view
		searchView =
			SearchView(this).apply {
				queryHint = "Search..."
				setIconifiedByDefault(false)
				isSubmitButtonEnabled = false
			}
		root.addView(searchView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// Set up search listener immediately
		searchView.setOnQueryTextListener(
			object : SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String?): Boolean {
					performSearch(query)
					return false
				}

				override fun onQueryTextChange(newText: String?): Boolean {
					performSearch(newText)
					return true
				}
			},
		)

		// RecyclerView for search results
		recyclerView =
			RecyclerView(this).apply {
				layoutManager = LinearLayoutManager(this@SearchDemoActivity)
			}
		searchAdapter = SearchAdapter(searchResults) { item -> onItemSelected(item) }
		recyclerView.adapter = searchAdapter
		root.addView(
			recyclerView,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
		)

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
		progressBar = ProgressBar(this)
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		loadingParams.gravity = Gravity.CENTER
		mapContainer.addView(progressBar, loadingParams)

		root.addView(
			mapContainer,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 2f),
		)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-mall",
			)

		mapView.getMapData(options) { result ->
			result.onSuccess {
				mapView.show3dMap(Show3DMapOptions()) { r ->
					r.onSuccess {
						runOnUiThread {
							progressBar.visibility = View.GONE
						}
						onMapReady()
					}
					r.onFailure {
						runOnUiThread {
							progressBar.visibility = View.GONE
						}
					}
				}
			}
		}
	}

	private fun onMapReady() {
		// First, enable search
		mapView.mapData.search.enable { result ->
			result.onSuccess {
				runOnUiThread {
					setupSearch()
				}
			}
		}
	}

	private fun setupSearch() {
		searchReady = true
		// Show all locations initially
		loadAllLocations()
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun loadAllLocations() {
		if (!searchReady) return

		mapView.mapData.getByType<EnterpriseLocation>(MapDataType.ENTERPRISE_LOCATION) { result ->
			result.onSuccess { locations ->
				runOnUiThread {
					searchResults.clear()
					locations?.forEach { location ->
						searchResults.add(
							SearchListItem.Location(
								SearchResultEnterpriseLocations(
									item = location,
									score = 0.0,
									match = emptyMap(),
									type = "enterprise-location",
								),
							),
						)
					}
					searchAdapter.notifyDataSetChanged()
				}
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun performSearch(query: String?) {
		if (!searchReady) return

		if (query.isNullOrBlank()) {
			// Show all locations when search is cleared
			loadAllLocations()
		} else {
			// Get autocomplete suggestions
			mapView.mapData.search.suggest(query) { result ->
				result.onSuccess { suggestions ->
					runOnUiThread {
						searchResults.clear()
						suggestions?.forEach { suggestion ->
							searchResults.add(SearchListItem.Suggestion(suggestion))
						}
						searchAdapter.notifyDataSetChanged()
					}
				}
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun onItemSelected(item: SearchListItem) {
		when (item) {
			is SearchListItem.Suggestion -> {
				// User selected a suggestion - search for locations matching this suggestion
				searchView.setQuery(item.suggestion.suggestion, false)
				mapView.mapData.search.query(item.suggestion.suggestion) { result ->
					result.onSuccess { searchResult ->
						runOnUiThread {
							searchResults.clear()
							searchResult
								?.enterpriseLocations
								?.sortedByDescending { it.score }
								?.forEach { searchResults.add(SearchListItem.Location(it)) }
							searchAdapter.notifyDataSetChanged()
						}
					}
				}
			}
			is SearchListItem.Location -> {
				// User selected a location - focus on it
				val location = item.result.item
				mapView.camera.focusOn(location) { focusResult ->
					focusResult.onSuccess {
						// Highlight all spaces for this location
						location.spaces.forEach { spaceId ->
							mapView.mapData.getById<Space>(
								MapDataType.SPACE,
								spaceId,
							) { result ->
								result.onSuccess { space ->
									space?.let {
										mapView.updateState(it, GeometryUpdateState(color = "#BF4320")) { }
									}
								}
							}
						}
					}
				}
			}
		}
	}

	// RecyclerView Adapter for search results (suggestions and locations)
	private class SearchAdapter(
		private val items: List<SearchListItem>,
		private val onItemClick: (SearchListItem) -> Unit,
	) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
		class ViewHolder(
			view: View,
			val nameTextView: TextView,
			val descriptionTextView: TextView,
		) : RecyclerView.ViewHolder(view)

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int,
		): ViewHolder {
			val layout =
				LinearLayout(parent.context).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams =
						ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
						)
					setPadding(dp(16), dp(12), dp(16), dp(12))
				}
			val nameView =
				TextView(parent.context).apply {
					textSize = 16f
					setTextColor(Color.BLACK)
				}
			val descView =
				TextView(parent.context).apply {
					textSize = 14f
					setTextColor(Color.GRAY)
				}
			layout.addView(nameView)
			layout.addView(descView)

			return ViewHolder(layout, nameView, descView)
		}

		override fun onBindViewHolder(
			holder: ViewHolder,
			position: Int,
		) {
			val item = items[position]
			when (item) {
				is SearchListItem.Suggestion -> {
					holder.nameTextView.text = item.suggestion.suggestion
					holder.descriptionTextView.text = ""
					holder.descriptionTextView.visibility = View.GONE
				}
				is SearchListItem.Location -> {
					holder.nameTextView.text = item.result.item.name
					holder.descriptionTextView.text = item.result.item.description ?: ""
					holder.descriptionTextView.visibility = View.VISIBLE
				}
			}
			holder.itemView.setOnClickListener { onItemClick(item) }
		}

		override fun getItemCount() = items.size

		private fun dp(value: Int): Int {
			val density =
				Resources
					.getSystem()
					.displayMetrics.density
			return (value * density).toInt()
		}
	}
}
