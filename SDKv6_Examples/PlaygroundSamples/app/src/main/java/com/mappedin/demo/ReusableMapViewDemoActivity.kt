package com.mappedin.demo

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout
import com.mappedin.MapView
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

/**
 * Demonstrates loading a single [MapView] once and reusing it across several
 * screens. Each tab is a separate [ReusableMapScreenFragment], and the one
 * shared map's WebView is physically reparented into whichever screen is
 * currently visible.
 *
 * Because the underlying WebView boots, downloads the venue, and renders only
 * once, switching tabs is instant: the map is moved into the new screen and the
 * camera is re-focused, rather than creating and loading a brand new map.
 */
class ReusableMapViewDemoActivity : AppCompatActivity() {
	private lateinit var sharedMapView: MapView
	private lateinit var contentContainer: ViewGroup

	/** True once the map has loaded and rendered for the first time. */
	var isMapReady: Boolean = false
		private set

	/** True if loading the map data or rendering it failed. */
	var loadFailed: Boolean = false
		private set

	/** Spaces from the loaded venue, used to pick a focus target per screen. */
	private var spaces: List<Space> = emptyList()

	/** The screen currently displaying the shared map. */
	private var currentScreen: ReusableMapScreenFragment? = null

	/** Index of the currently selected tab, persisted across config changes. */
	private var selectedIndex = 0

	private val screenTitles = listOf("Screen 1", "Screen 2", "Screen 3")

	private companion object {
		// A stable, hierarchy-unique id for the fragment container so the
		// FragmentManager can restore fragments across configuration changes.
		// (A generated id would differ on every recreation and break restore.)
		const val CONTENT_VIEW_ID = 0x00FF0A01
		const val KEY_SELECTED_TAB = "selected_tab"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Reusable MapView"

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
					val bars =
						insets.getInsets(
							WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
						)
					view.setPadding(0, bars.top, 0, 0)
					insets
				}
			}
		setContentView(root)

		val tabLayout = TabLayout(this)
		screenTitles.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
		root.addView(
			tabLayout,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
		)

		val container =
			androidx.fragment.app.FragmentContainerView(this).apply {
				id = CONTENT_VIEW_ID
			}
		contentContainer = container
		root.addView(
			container,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
		)

		// Create the single shared map and load it exactly once.
		sharedMapView = MapView(this)
		loadMap()

		tabLayout.addOnTabSelectedListener(
			object : TabLayout.OnTabSelectedListener {
				override fun onTabSelected(tab: TabLayout.Tab) {
					selectedIndex = tab.position
					showScreen(tab.position)
				}

				override fun onTabUnselected(tab: TabLayout.Tab) = Unit

				override fun onTabReselected(tab: TabLayout.Tab) = Unit
			},
		)

		// Restore (or default to) the selected tab and always show its screen.
		// Showing it explicitly replaces any auto-restored fragment, so the
		// content area is never left empty after a configuration change.
		selectedIndex = savedInstanceState?.getInt(KEY_SELECTED_TAB, 0) ?: 0
		tabLayout.getTabAt(selectedIndex)?.select()
		if (selectedIndex == 0) {
			showScreen(0)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(KEY_SELECTED_TAB, selectedIndex)
	}

	private fun loadMap() {
		// See Demo API Key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
				secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
				mapId = "660c0c6e7c0c4fe5b4cc484c",
			)

		sharedMapView.getMapData(options) { dataResult ->
			dataResult
				.onSuccess {
					sharedMapView.show3dMap(Show3DMapOptions()) { showResult ->
						showResult
							.onSuccess {
								// Automatically add default labels and markers so
								// the map looks complete on its first and only load.
								sharedMapView.__EXPERIMENTAL__auto()
								// The map is rendered now, so mark it ready and hide
								// the loading indicator. Focusing depends on the
								// spaces query below, which is loaded separately so
								// a failure there never leaves the screen stuck on
								// the spinner.
								runOnUiThread {
									isMapReady = true
									currentScreen?.onMapStateChanged()
								}
								sharedMapView.mapData.getByType<Space>(MapDataType.SPACE) { spacesResult ->
									spacesResult.onSuccess { loaded ->
										runOnUiThread {
											spaces = loaded.filter { it.name.isNotBlank() }
											// Refresh the active screen so its header
											// and camera reflect the loaded spaces.
											currentScreen?.onMapStateChanged()
										}
									}
								}
							}.onFailure { onLoadFailed() }
					}
				}.onFailure { onLoadFailed() }
		}
	}

	private fun onLoadFailed() {
		runOnUiThread {
			loadFailed = true
			currentScreen?.onMapStateChanged()
		}
	}

	private fun showScreen(position: Int) {
		val fragment = ReusableMapScreenFragment.newInstance(position, screenTitles[position])
		supportFragmentManager
			.beginTransaction()
			.replace(contentContainer.id, fragment)
			.commit()
	}

	/**
	 * Reparents the shared map's WebView into [container]. The view is first
	 * detached from any previous parent so it can be safely re-attached.
	 */
	fun attachMapTo(container: ViewGroup) {
		val mapWebView = sharedMapView.view
		(mapWebView.parent as? ViewGroup)?.removeView(mapWebView)
		// Insert at the bottom of the z-order so any overlay (e.g. the loading
		// indicator) added to the container stays visible on top.
		container.addView(
			mapWebView,
			0,
			ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)
	}

	/** Removes the shared map's WebView from its current parent, if any. */
	fun detachMap() {
		val mapWebView = sharedMapView.view
		(mapWebView.parent as? ViewGroup)?.removeView(mapWebView)
	}

	/** Records which screen owns the map and focuses it if the map is ready. */
	fun onScreenResumed(screen: ReusableMapScreenFragment) {
		currentScreen = screen
		focusForScreen(screen.screenIndex)
	}

	/**
	 * Clears the active screen reference when [screen]'s view goes away, so any
	 * in-flight load callbacks do not act on a tab the user has left.
	 */
	fun onScreenPaused(screen: ReusableMapScreenFragment) {
		if (currentScreen === screen) {
			currentScreen = null
		}
	}

	/** Focuses the shared map on the space chosen for the given screen index. */
	fun focusForScreen(index: Int) {
		if (!isMapReady) {
			return
		}
		spaceForScreen(index)?.let { sharedMapView.camera.focusOn(it) }
	}

	/** The name of the space a screen focuses on, or null if not loaded yet. */
	fun focusTargetName(index: Int): String? = spaceForScreen(index)?.name

	/** The space a given screen index focuses on, or null if none are loaded. */
	private fun spaceForScreen(index: Int): Space? {
		if (spaces.isEmpty()) {
			return null
		}
		return when (index) {
			0 -> spaces.first()
			1 -> spaces[spaces.size / 2]
			else -> spaces.last()
		}
	}

	override fun onDestroy() {
		// Tear down the shared map only when the whole demo is finished.
		sharedMapView.destroy()
		super.onDestroy()
	}
}
