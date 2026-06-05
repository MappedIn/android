package com.mappedin.demo

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment

/**
 * A single screen in the Reusable MapView demo. Each instance reparents the
 * shared map's WebView into its own container and asks the host to focus the
 * camera on this screen's space. The map itself is never created or destroyed
 * here; it is owned by [ReusableMapViewDemoActivity] and shared across screens.
 */
class ReusableMapScreenFragment : Fragment() {
	val screenIndex: Int
		get() = requireArguments().getInt(ARG_INDEX)

	private val screenTitle: String
		get() = requireArguments().getString(ARG_TITLE).orEmpty()

	private lateinit var mapContainer: FrameLayout
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var descriptionView: TextView

	private val host: ReusableMapViewDemoActivity
		get() = requireActivity() as ReusableMapViewDemoActivity

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val root =
			LinearLayout(requireContext()).apply {
				orientation = LinearLayout.VERTICAL
			}

		val header =
			LinearLayout(requireContext()).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(16), dp(12), dp(16), dp(12))
			}
		val titleView =
			TextView(requireContext()).apply {
				text = screenTitle
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				setTypeface(typeface, android.graphics.Typeface.BOLD)
			}
		descriptionView =
			TextView(requireContext()).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(
			header,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
		)

		mapContainer = FrameLayout(requireContext())
		loadingIndicator = ProgressBar(requireContext())
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			).apply { gravity = Gravity.CENTER }
		mapContainer.addView(loadingIndicator, loadingParams)
		root.addView(
			mapContainer,
			LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
		)

		return root
	}

	override fun onResume() {
		super.onResume()
		host.attachMapTo(mapContainer)
		updateForState()
		host.onScreenResumed(this)
	}

	override fun onDestroyView() {
		// Detach the shared map so the next screen can reparent it. Do not
		// destroy it; the host owns the map's lifecycle.
		host.onScreenPaused(this)
		host.detachMap()
		super.onDestroyView()
	}

	/** Called by the host whenever the shared map's load state changes. */
	fun onMapStateChanged() {
		updateForState()
		host.focusForScreen(screenIndex)
	}

	private fun updateForState() {
		when {
			host.loadFailed -> {
				loadingIndicator.visibility = View.GONE
				descriptionView.text = "The shared map failed to load."
			}
			host.isMapReady -> {
				loadingIndicator.visibility = View.GONE
				val target = host.focusTargetName(screenIndex)
				descriptionView.text = "Reusing the shared map, focused on: ${target ?: "this venue"}"
			}
			else -> {
				loadingIndicator.visibility = View.VISIBLE
				descriptionView.text = "Loading the shared map once. It will be reused on every screen."
			}
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}

	companion object {
		private const val ARG_INDEX = "screen_index"
		private const val ARG_TITLE = "screen_title"

		fun newInstance(
			index: Int,
			title: String,
		): ReusableMapScreenFragment =
			ReusableMapScreenFragment().apply {
				arguments =
					Bundle().apply {
						putInt(ARG_INDEX, index)
						putString(ARG_TITLE, title)
					}
			}
	}
}
