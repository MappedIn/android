package com.mappedin.demo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mappedin.MapView

/**
 * Hosts the Icons examples (Gallery, Color Picker, Prefetch, Enterprise Category
 * Icons) in a tabbed interface, mirroring the web SDK examples.
 *
 * A single hidden [MapView] hosts the Mappedin bridge for the first three
 * (map-independent) examples, which share this MapView's `icons` API via
 * [mapView]. The Enterprise Category Icons example renders a real map and creates
 * its own [MapView].
 */
class IconsDemoActivity : AppCompatActivity() {
    /** Shared bridge host. The Icons extension does not require a loaded map. */
    lateinit var mapView: MapView
        private set

    private val tabTitles = listOf("Gallery", "Color Picker", "Prefetch", "Enterprise Category Icons")

    private companion object {
        const val ENTERPRISE_TAB_INDEX = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Icons"

        mapView = MapView(this)

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                    val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    view.setPadding(0, bars.top, 0, 0)
                    insets
                }
            }
        setContentView(root)

        val tabLayout = TabLayout(this)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        root.addView(
            tabLayout,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )

        val pager = ViewPager2(this)
        pager.id = View.generateViewId()
        pager.adapter = IconsPagerAdapter(this)
        root.addView(
            pager,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )

        // The Enterprise Category Icons tab renders a real map. ViewPager2's
        // horizontal swipe-to-switch gesture conflicts with the map's pan
        // gesture, so disable swiping while that tab is selected. Tabs can still
        // be switched via the TabLayout.
        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pager.isUserInputEnabled = position != ENTERPRISE_TAB_INDEX
                }
            },
        )

        // Attach the bridge WebView (1px tall, off-screen) so it loads and runs.
        root.addView(
            mapView.view,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1),
        )

        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private class IconsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> IconGalleryFragment()
                1 -> ColorPickerFragment()
                2 -> PrefetchDemoFragment()
                else -> EnterpriseCategoryIconsFragment()
            }
    }
}
