package com.mappedin.demo

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
import com.mappedin.models.EventMetaData
import com.mappedin.models.GetMapDataWithCredentialsOptions
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Demonstrates the Events extension for loading and displaying CMS events.
 *
 * This demo loads events from the mappedin-demo-mall venue and displays
 * them as a scrollable list of cards with images, names, dates, and descriptions.
 */
class EventsDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var contentContainer: LinearLayout
	private val imageExecutor = Executors.newFixedThreadPool(4)
	private val imageCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap?>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Events"

		val root =
			FrameLayout(this).apply {
				setBackgroundColor(Color.parseColor("#F5F5F5"))
				ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
					val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
					view.setPadding(0, systemBars.top, 0, systemBars.bottom)
					insets
				}
			}

		val scrollView =
			ScrollView(this).apply {
				layoutParams =
					FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT,
					)
			}

		contentContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams =
					ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT,
					)
				setPadding(dp(16), dp(16), dp(16), dp(16))
			}

		scrollView.addView(contentContainer)
		root.addView(scrollView)

		loadingIndicator =
			ProgressBar(this).apply {
				layoutParams =
					FrameLayout
						.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
						).apply {
							gravity = Gravity.CENTER
						}
			}
		root.addView(loadingIndicator)

		mapView = MapView(this)
		val hiddenMapContainer =
			FrameLayout(this).apply {
				layoutParams = FrameLayout.LayoutParams(1, 1)
				visibility = View.INVISIBLE
			}
		hiddenMapContainer.addView(mapView.view)
		root.addView(hiddenMapContainer)

		setContentView(root)

		// See Demo API Key Terms and Conditions
		// https://developer.mappedin.com/docs/demo-keys-and-maps
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-mall",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("EventsDemo", "getMapData success")
					loadEvents()
				}.onFailure { error ->
					Log.e("EventsDemo", "getMapData error: $error")
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

	private fun loadEvents() {
		mapView.mapData.eventsManager.load { result ->
			result.fold(
				onSuccess = {
					mapView.mapData.eventsManager.getEvents { eventsResult ->
						eventsResult.fold(
							onSuccess = { events ->
								runOnUiThread {
									loadingIndicator.visibility = View.GONE
									buildEventsUI(events)
								}
							},
							onFailure = { error ->
								Log.e("EventsDemo", "getEvents error: $error")
								runOnUiThread {
									loadingIndicator.visibility = View.GONE
									showError("Failed to load events")
								}
							},
						)
					}
				},
				onFailure = { error ->
					Log.e("EventsDemo", "load events error: $error")
					runOnUiThread {
						loadingIndicator.visibility = View.GONE
						showError("Failed to load events")
					}
				},
			)
		}
	}

	private fun buildEventsUI(events: List<EventMetaData>) {
		if (events.isEmpty()) {
			showError("No events found for this venue.")
			return
		}

		// Header
		val header =
			TextView(this).apply {
				text = "${events.size} Event${if (events.size != 1) "s" else ""}"
				textSize = 22f
				setTextColor(Color.parseColor("#222222"))
				layoutParams =
					LinearLayout
						.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
						).apply {
							bottomMargin = dp(16)
						}
			}
		contentContainer.addView(header)

		for (event in events) {
			val card = createEventCard(event)
			contentContainer.addView(card)
		}
	}

	private fun createEventCard(event: EventMetaData): FrameLayout {
		val card =
			FrameLayout(this).apply {
				layoutParams =
					LinearLayout
						.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
						).apply {
							bottomMargin = dp(16)
						}
				val backgroundDrawable =
					GradientDrawable().apply {
						setColor(Color.WHITE)
						cornerRadius = dp(8).toFloat()
					}
				background = backgroundDrawable
				elevation = dp(4).toFloat()
				clipToOutline = true
				outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
			}

		val cardContent =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams =
					ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT,
					)
			}

		val imageUrl = event.image?.url
		if (!imageUrl.isNullOrEmpty()) {
			val imageView =
				ImageView(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							dp(200),
						)
					scaleType = ImageView.ScaleType.CENTER_CROP
					setBackgroundColor(Color.parseColor("#E0E0E0"))
				}
			loadImage(imageUrl, imageView)
			cardContent.addView(imageView)
		}

		val infoContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams =
					LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT,
					)
				setPadding(dp(16), dp(16), dp(16), dp(16))
			}

		val nameView =
			TextView(this).apply {
				text = event.name
				textSize = 18f
				setTextColor(Color.parseColor("#222222"))
				layoutParams =
					LinearLayout
						.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
						).apply {
							bottomMargin = dp(4)
						}
			}
		infoContainer.addView(nameView)

		val dateText = formatDateRange(event.startDate, event.endDate)
		if (dateText.isNotEmpty()) {
			val dateView =
				TextView(this).apply {
					text = dateText
					textSize = 13f
					setTextColor(Color.parseColor("#888888"))
					layoutParams =
						LinearLayout
							.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.WRAP_CONTENT,
							).apply {
								bottomMargin = dp(8)
							}
				}
			infoContainer.addView(dateView)
		}

		if (!event.description.isNullOrEmpty()) {
			val descView =
				TextView(this).apply {
					text = event.description
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
	 * Formats an ISO 8601 date range into a human-readable string.
	 */
	private fun formatDateRange(
		startDate: String,
		endDate: String,
	): String {
		val start = startDate.substringBefore("T").ifEmpty { null }
		val end = endDate.substringBefore("T").ifEmpty { null }
		if (start == null && end == null) return ""
		if (start == null) return end!!
		if (end == null) return start
		if (start == end) return start
		return "$start — $end"
	}

	private fun loadImage(
		url: String,
		imageView: ImageView,
	) {
		imageCache[url]?.let {
			imageView.setImageBitmap(it)
			return
		}

		imageExecutor.execute {
			var connection: HttpURLConnection? = null
			try {
				connection = URL(url).openConnection() as HttpURLConnection
				connection.doInput = true
				connection.connect()

				val bytes = connection.inputStream.use { it.readBytes() }

				val options =
					BitmapFactory.Options().apply {
						inJustDecodeBounds = true
					}
				BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

				val maxDimension = 1024
				var sampleSize = 1
				if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
					val widthRatio = options.outWidth / maxDimension
					val heightRatio = options.outHeight / maxDimension
					sampleSize = maxOf(widthRatio, heightRatio)
				}

				val decodeOptions =
					BitmapFactory.Options().apply {
						inSampleSize = sampleSize
					}
				val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)

				if (bitmap != null) {
					imageCache[url] = bitmap
					runOnUiThread {
						imageView.setImageBitmap(bitmap)
					}
				}
			} catch (e: Exception) {
				Log.e("EventsDemo", "Failed to load image: $url", e)
			} finally {
				connection?.disconnect()
			}
		}
	}

	private fun showError(message: String) {
		contentContainer.removeAllViews()
		val errorView =
			TextView(this).apply {
				text = message
				textSize = 16f
				setTextColor(Color.RED)
				gravity = Gravity.CENTER
				layoutParams =
					LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT,
					)
			}
		contentContainer.addView(errorView)
	}

	private fun dp(value: Int): Int {
		val density = Resources.getSystem().displayMetrics.density
		return (value * density).toInt()
	}
}
