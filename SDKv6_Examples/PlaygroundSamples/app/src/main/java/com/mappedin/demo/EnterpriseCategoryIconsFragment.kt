package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.mappedin.MapView
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.EnterpriseCategory
import com.mappedin.models.EnterpriseLocation
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.LabelAppearance
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

/**
 * Enterprise Category Icons example.
 *
 * Loads an Enterprise map, then adds a label to every space of every enterprise location.
 * Each label uses the small icon associated with the location's first category (falling back to
 * an information icon) tinted to match the web example.
 *
 * Unlike the other Icons examples (which only use the bridge), this example
 * renders a real map, so it owns its own visible [MapView].
 */
class EnterpriseCategoryIconsFragment : Fragment() {
	private var mapView: MapView? = null
	private var isDestroyed = false
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		// The fragment instance is reused when its view is recreated, so clear
		// the destroyed flag set in onDestroyView; otherwise callbacks bail out.
		isDestroyed = false
		val root = FrameLayout(requireContext())
		val map = MapView(requireContext())
		mapView = map
		root.addView(
			map.view,
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)

		loadingIndicator = ProgressBar(requireContext())
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		loadingParams.gravity = Gravity.CENTER
		root.addView(loadingIndicator, loadingParams)

		loadMap(map)
		return root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		// Stop any in-flight bridge work from touching a destroyed MapView, then
		// tear it down so the WebView is released when the user leaves the tab.
		isDestroyed = true
		mapView?.destroy()
		mapView = null
	}

	private fun loadMap(mapView: MapView) {
		// See Demo API Key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-enterprise",
			)

		mapView.getMapData(options) { result ->
			if (isDestroyed) return@getMapData
			result
				.onSuccess {
					mapView.show3dMap(Show3DMapOptions()) { showResult ->
						if (isDestroyed) return@show3dMap
						runOnUiThread { loadingIndicator.visibility = View.GONE }
						showResult
							.onSuccess { addCategoryIconLabels(mapView) }
							.onFailure { Log.e("MappedinDemo", "show3dMap error: $it") }
					}
				}.onFailure {
					runOnUiThread { loadingIndicator.visibility = View.GONE }
					Log.e("MappedinDemo", "getMapData error: $it")
				}
		}
	}

	/**
	 * Builds a label for every space of every enterprise location, using the
	 * small icon of the location's first category.
	 */
	private fun addCategoryIconLabels(mapView: MapView) {
		if (isDestroyed) return
		mapView.icons.initialize {}

		loadCategoriesById(mapView) { categoriesById ->
			if (isDestroyed) return@loadCategoriesById
			loadSpacesById(mapView) { spacesById ->
				if (isDestroyed) return@loadSpacesById
				mapView.mapData.getByType<EnterpriseLocation>(MapDataType.ENTERPRISE_LOCATION) { result ->
					if (isDestroyed) return@getByType
					result
						.onSuccess { locations ->
							for (location in locations) {
								if (location.name.isNotEmpty()) {
									addLabels(mapView, location, categoriesById, spacesById)
								}
							}
						}.onFailure { Log.e("MappedinDemo", "getByType(enterprise-location) error: $it") }
				}
			}
		}
	}

	private fun addLabels(
		mapView: MapView,
		location: EnterpriseLocation,
		categoriesById: Map<String, EnterpriseCategory>,
		spacesById: Map<String, Space>,
	) {
		val category = location.categories.firstOrNull()?.let { categoriesById[it] }
		val iconName = category?.iconFromDefaultList ?: "information"
		val color = category?.color ?: "black"

		mapView.icons.getByName(iconName) { iconResult ->
			if (isDestroyed) return@getByName
			val smallIconName = iconResult.getOrNull()?.smallIcon ?: "small-information-desk"
			mapView.icons.fetchSvg(smallIconName) { svgResult ->
				if (isDestroyed) return@fetchSvg
				val svg = svgResult.getOrNull() ?: return@fetchSvg
				val coloredSvg = svg.replace("currentColor", "#fafafa")
				val appearance = LabelAppearance(color = color, icon = coloredSvg, iconPadding = 10)
				val labelOptions = AddLabelOptions(labelAppearance = appearance)

				for (spaceId in location.spaces) {
					val space = spacesById[spaceId] ?: continue
					mapView.labels.add(target = space, text = location.name, options = labelOptions)
				}
			}
		}
	}

	private fun loadCategoriesById(
		mapView: MapView,
		completion: (Map<String, EnterpriseCategory>) -> Unit,
	) {
		mapView.mapData.getByType<EnterpriseCategory>(MapDataType.ENTERPRISE_CATEGORY) { result ->
			if (isDestroyed) return@getByType
			result
				.onSuccess { categories ->
					val byId = HashMap<String, EnterpriseCategory>()
					for (category in categories) byId[category.id] = category
					completion(byId)
				}.onFailure {
					Log.e("MappedinDemo", "getByType(enterprise-category) error: $it")
					completion(emptyMap())
				}
		}
	}

	private fun loadSpacesById(
		mapView: MapView,
		completion: (Map<String, Space>) -> Unit,
	) {
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			if (isDestroyed) return@getByType
			result
				.onSuccess { spaces ->
					val byId = HashMap<String, Space>()
					for (space in spaces) byId[space.id] = space
					completion(byId)
				}.onFailure {
					Log.e("MappedinDemo", "getByType(space) error: $it")
					completion(emptyMap())
				}
		}
	}

	private fun runOnUiThread(action: () -> Unit) {
		activity?.runOnUiThread {
			if (!isDestroyed) action()
		}
	}
}
