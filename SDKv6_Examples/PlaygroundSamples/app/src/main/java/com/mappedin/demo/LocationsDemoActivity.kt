package com.mappedin.demo

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.LocationCategory
import com.mappedin.models.LocationProfile
import com.mappedin.models.MapDataType
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Demo activity showing a category directory with location profiles.
 *
 * This demo displays all location categories organized in a hierarchical structure,
 * showing parent → child category relationships and their associated locations
 * with images, names, and descriptions.
 */
class LocationsDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var contentContainer: LinearLayout
	private val imageExecutor = Executors.newFixedThreadPool(4)
	private val imageCache = mutableMapOf<String, Bitmap?>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Locations"

		// Root layout with insets handling
		val root = FrameLayout(this).apply {
			setBackgroundColor(Color.parseColor("#F5F5F5"))
			ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
				val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
				view.setPadding(0, systemBars.top, 0, systemBars.bottom)
				insets
			}
		}

		// Scrollable content container
		val scrollView = ScrollView(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
		}

		contentContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			setPadding(dp(16), dp(16), dp(16), dp(16))
		}

		scrollView.addView(contentContainer)
		root.addView(scrollView)

		// Loading indicator
		loadingIndicator = ProgressBar(this).apply {
			layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				gravity = Gravity.CENTER
			}
		}
		root.addView(loadingIndicator)

		// Hidden MapView - needed to load data but not displayed
		mapView = MapView(this)
		val hiddenMapContainer = FrameLayout(this).apply {
			layoutParams = FrameLayout.LayoutParams(1, 1)
			visibility = View.INVISIBLE
		}
		hiddenMapContainer.addView(mapView.view)
		root.addView(hiddenMapContainer)

		setContentView(root)

		// See Trial API key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options = GetMapDataWithCredentialsOptions(
			key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
			secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
			mapId = "660c0c6e7c0c4fe5b4cc484c"
		)

		// Load the map data
		mapView.getMapData(options) { result ->
			result.onSuccess {
				Log.d("LocationsDemo", "getMapData success")
				loadCategoryDirectory()
			}.onFailure { error ->
				Log.e("LocationsDemo", "getMapData error: $error")
				runOnUiThread {
					loadingIndicator.visibility = View.GONE
					showError("Failed to load map data")
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		imageExecutor.shutdown()
	}

	/**
	 * Loads all location categories and profiles, then builds the directory UI.
	 */
	private fun loadCategoryDirectory() {
		// First, get all location categories
		mapView.mapData.getByType<LocationCategory>(MapDataType.LOCATION_CATEGORY) { categoriesResult ->
			categoriesResult.onSuccess { categories ->
				// Then get all location profiles
				mapView.mapData.getByType<LocationProfile>(MapDataType.LOCATION_PROFILE) { profilesResult ->
					profilesResult.onSuccess { profiles ->
						// Build lookup map for profiles
						val profileMap = profiles.associateBy { it.id }
						// Build lookup map for categories
						val categoryMap = categories.associateBy { it.id }

						runOnUiThread {
							loadingIndicator.visibility = View.GONE
							buildCategoryUI(categories, categoryMap, profileMap)
						}
					}.onFailure { error ->
						Log.e("LocationsDemo", "Failed to get profiles: $error")
						runOnUiThread {
							loadingIndicator.visibility = View.GONE
							showError("Failed to load location profiles")
						}
					}
				}
			}.onFailure { error ->
				Log.e("LocationsDemo", "Failed to get categories: $error")
				runOnUiThread {
					loadingIndicator.visibility = View.GONE
					showError("Failed to load categories")
				}
			}
		}
	}

	/**
	 * Builds the category directory UI from the loaded data.
	 */
	@SuppressLint("SetTextI18n")
	private fun buildCategoryUI(
		categories: List<LocationCategory>,
		categoryMap: Map<String, LocationCategory>,
		profileMap: Map<String, LocationProfile>
	) {
		// Find parent categories (categories with children)
		val parentCategories = categories.filter { it.children.isNotEmpty() }

		if (parentCategories.isEmpty()) {
			showError("No categories with children found")
			return
		}

		parentCategories.forEach { parentCategory ->
			// Loop through each child category
			parentCategory.children.forEach { childId ->
				val childCategory = categoryMap[childId] ?: return@forEach

				// Create category section
				val categorySection = LinearLayout(this).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						bottomMargin = dp(24)
					}
				}

				// Create title container with icons
				val titleContainer = LinearLayout(this).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						bottomMargin = dp(16)
					}
				}

				// Add parent category icon if exists
				if (parentCategory.icon.isNotEmpty()) {
					val parentIcon = ImageView(this).apply {
						layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
							rightMargin = dp(8)
						}
						scaleType = ImageView.ScaleType.FIT_CENTER
					}
					loadImage(parentCategory.icon, parentIcon)
					titleContainer.addView(parentIcon)
				}

				// Add parent category name
				val parentName = TextView(this).apply {
					text = parentCategory.name
					textSize = 16f
					setTextColor(Color.parseColor("#333333"))
				}
				titleContainer.addView(parentName)

				// Add separator arrow
				val separator = TextView(this).apply {
					text = " → "
					textSize = 16f
					setTextColor(Color.parseColor("#666666"))
				}
				titleContainer.addView(separator)

				// Add child category icon if exists
				if (childCategory.icon.isNotEmpty()) {
					val childIcon = ImageView(this).apply {
						layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
							rightMargin = dp(8)
						}
						scaleType = ImageView.ScaleType.FIT_CENTER
					}
					loadImage(childCategory.icon, childIcon)
					titleContainer.addView(childIcon)
				}

				// Add child category name
				val childName = TextView(this).apply {
					text = childCategory.name
					textSize = 16f
					setTextColor(Color.parseColor("#333333"))
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					).apply {
						weight = 1f
					}
				}
				titleContainer.addView(childName)

				categorySection.addView(titleContainer)

				// Add divider below title
				val divider = View(this).apply {
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						dp(2)
					).apply {
						bottomMargin = dp(16)
					}
					setBackgroundColor(Color.parseColor("#EEEEEE"))
				}
				categorySection.addView(divider)

				// Create locations grid container
				val locationsContainer = LinearLayout(this).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams = LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT
					)
				}

				// Add location cards for each profile in this category
				childCategory.locationProfiles.forEach { profileId ->
					val profile = profileMap[profileId]
					if (profile != null && profile.name.isNotEmpty()) {
						val card = createLocationCard(profile)
						locationsContainer.addView(card)
					}
				}

				categorySection.addView(locationsContainer)
				contentContainer.addView(categorySection)
			}
		}
	}

	/**
	 * Creates a card view for a location profile.
	 */
	private fun createLocationCard(profile: LocationProfile): FrameLayout {
		val card = FrameLayout(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = dp(16)
			}
			// Create rounded drawable for background
			val backgroundDrawable = GradientDrawable().apply {
				setColor(Color.WHITE)
				cornerRadius = dp(8).toFloat()
			}
			background = backgroundDrawable
			elevation = dp(4).toFloat()
			clipToOutline = true
			outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
		}

		val cardContent = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
		}

		// Add image if available
		val imageUrl = profile.images.firstOrNull()?.url
		if (!imageUrl.isNullOrEmpty()) {
			val imageView = ImageView(this).apply {
				layoutParams = LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					dp(200)
				)
				scaleType = ImageView.ScaleType.CENTER_CROP
				setBackgroundColor(Color.parseColor("#E0E0E0"))
			}
			loadImage(imageUrl, imageView)
			cardContent.addView(imageView)
		}

		// Location info container
		val infoContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			setPadding(dp(16), dp(16), dp(16), dp(16))
		}

		// Location name
		val nameView = TextView(this).apply {
			text = profile.name
			textSize = 18f
			setTextColor(Color.parseColor("#222222"))
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = dp(8)
			}
		}
		infoContainer.addView(nameView)

		// Location description
		if (!profile.description.isNullOrEmpty()) {
			val descView = TextView(this).apply {
				text = profile.description
				textSize = 14f
				setTextColor(Color.parseColor("#666666"))
				maxLines = 4
			}
			infoContainer.addView(descView)
		}

		cardContent.addView(infoContainer)
		card.addView(cardContent)

		return card
	}

	/**
	 * Loads an image from a URL into an ImageView, scaling it down if necessary.
	 */
	private fun loadImage(url: String, imageView: ImageView) {
		// Check cache first
		imageCache[url]?.let {
			imageView.setImageBitmap(it)
			return
		}

		imageExecutor.execute {
			try {
				val connection = URL(url).openConnection() as HttpURLConnection
				connection.doInput = true
				connection.connect()

				// Read the entire stream into a byte array so we can decode it twice
				val inputStream = connection.inputStream
				val bytes = inputStream.readBytes()
				inputStream.close()

				// First, decode just the bounds to check the image size
				val options = BitmapFactory.Options().apply {
					inJustDecodeBounds = true
				}
				BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

				// Calculate sample size to scale down large images
				// Target max dimension of 1024 pixels
				val maxDimension = 1024
				var sampleSize = 1
				if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
					val widthRatio = options.outWidth / maxDimension
					val heightRatio = options.outHeight / maxDimension
					sampleSize = maxOf(widthRatio, heightRatio)
				}

				// Decode the actual bitmap with the calculated sample size
				val decodeOptions = BitmapFactory.Options().apply {
					inSampleSize = sampleSize
				}
				val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)

				if (bitmap != null) {
					// Cache the bitmap
					imageCache[url] = bitmap

					runOnUiThread {
						imageView.setImageBitmap(bitmap)
					}
				}
			} catch (e: Exception) {
				Log.e("LocationsDemo", "Failed to load image: $url", e)
			}
		}
	}

	/**
	 * Shows an error message in the content container.
	 */
	private fun showError(message: String) {
		contentContainer.removeAllViews()
		val errorView = TextView(this).apply {
			text = message
			textSize = 16f
			setTextColor(Color.RED)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
		}
		contentContainer.addView(errorView)
	}

	/**
	 * Converts dp to pixels.
	 */
	private fun dp(value: Int): Int {
		val density = Resources.getSystem().displayMetrics.density
		return (value * density).toInt()
	}
}
