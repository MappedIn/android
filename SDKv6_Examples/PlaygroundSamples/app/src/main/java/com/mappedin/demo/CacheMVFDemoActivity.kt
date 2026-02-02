package com.mappedin.demo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Demonstrates downloading and caching MVF data using Mappedin's REST API.
 *
 * This demo shows how to:
 * 1. Check if MVF data is cached locally
 * 2. Download MVF bundle via REST API if not cached
 * 3. Load from cache using hydrateMapData
 * 4. Save the downloaded MVF to cache for offline use
 *
 * The MVF is downloaded from:
 * - Token endpoint: https://app.mappedin.com/api/v1/api-key/token
 * - MVF endpoint: https://app.mappedin.com/api/venue/{mapId}/mvf?version=3.0.0
 */
class CacheMVFDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar
	private lateinit var statusText: TextView
	private lateinit var clearCacheButton: Button
	private var currentMapId: String = ""

	companion object {
		private const val TAG = "CacheMVFDemo"
		private const val CACHE_FILE_PREFIX = "cached-mvf-"

		// API credentials (Demo API key)
		// See Trial API key Terms and Conditions: https://developer.mappedin.com/docs/demo-keys-and-maps
		private const val MAPPEDIN_KEY = "mik_yeBk0Vf0nNJtpesfu560e07e5"
		private const val MAPPEDIN_SECRET = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022"

		// Map ID from the Mapkit JS example
		private const val MAP_ID = "67a6641530e940000bac3c1a"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Cache MVF Data"

		// Create a FrameLayout to hold both the map view and loading indicator
		val container = FrameLayout(this)

		mapView = MapView(this)
		container.addView(
			mapView.view,
			ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)

		// Add loading indicator
		loadingIndicator = ProgressBar(this)
		val loadingParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		loadingParams.gravity = Gravity.CENTER
		container.addView(loadingIndicator, loadingParams)

		// Add top bar with status text and clear cache button
		val topBar =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				setBackgroundColor(0xCCFFFFFF.toInt())
				setPadding(16, 8, 16, 8)
				gravity = Gravity.CENTER_VERTICAL
			}

		// Apply window insets to account for status bar
		ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, windowInsets ->
			val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
			view.setPadding(16, statusBarInsets.top + 8, 16, 8)
			windowInsets
		}

		statusText =
			TextView(this).apply {
				setTextColor(0xFF000000.toInt())
				layoutParams =
					LinearLayout.LayoutParams(
						0,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						1f,
					)
			}
		topBar.addView(statusText)

		clearCacheButton =
			Button(this).apply {
				text = "Clear Cache"
				setOnClickListener { onClearCacheClicked() }
			}
		topBar.addView(clearCacheButton)

		val topBarParams =
			FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			)
		topBarParams.gravity = Gravity.TOP
		container.addView(topBar, topBarParams)

		setContentView(container)

		loadMapData()
	}

	private fun onClearCacheClicked() {
		if (currentMapId.isNotEmpty()) {
			deleteFromCache(currentMapId)
			updateStatus("Cache cleared! Restart activity to reload.")
		}
	}

	private var loadStartTime: Long = 0
	private var dataLoadDuration: Long = 0
	private var isCachedLoad: Boolean = false

	private fun loadMapData() {
		currentMapId = MAP_ID
		loadStartTime = System.currentTimeMillis()

		// Check if there is cached MVF data for this map
		val cachedData = loadFromCache(MAP_ID)

		if (cachedData != null) {
			Log.d(TAG, "Using cached MVF data for $MAP_ID")
			updateStatus("Loading from cache...")
			isCachedLoad = true
			loadFromCachedData(cachedData)
		} else {
			Log.d(TAG, "Downloading MVF data from REST API for $MAP_ID")
			updateStatus("Downloading MVF via REST API...")
			isCachedLoad = false
			downloadMvfFromApi()
		}
	}

	private fun loadFromCachedData(cachedData: ByteArray) {
		val hydrateStartTime = System.currentTimeMillis()

		// Get the cache URL using the helper - this uses the WebViewAssetLoader to serve files
		val cacheFileName = "$CACHE_FILE_PREFIX$MAP_ID.zip"
		val mvfUrl = MapView.getCacheUrl(cacheFileName)
		Log.d(TAG, "Loading from URL: $mvfUrl")

		// Pass credentials to enable outdoor view (tileset tokens require authentication)
		val options =
			GetMapDataWithCredentialsOptions(
				key = MAPPEDIN_KEY,
				secret = MAPPEDIN_SECRET,
				mapId = MAP_ID,
			)

		// Use hydrateMapDataFromURL for faster loading compared to the CacheMapDataDemoActivity
		// - avoids passing large data over bridge
		mapView.hydrateMapDataFromURL(mvfUrl, options) { result ->
			val hydrateEndTime = System.currentTimeMillis()
			dataLoadDuration = hydrateEndTime - hydrateStartTime

			result
				.onSuccess {
					Log.d(TAG, "hydrateMapDataFromURL success - loaded from cache in ${dataLoadDuration}ms")
					updateStatus("Loaded from cache!")
					showMap()
				}.onFailure { error ->
					Log.e(TAG, "hydrateMapDataFromURL error: $error (after ${dataLoadDuration}ms)")
					// If cache is corrupted, delete it and download fresh data
					deleteFromCache(MAP_ID)
					updateStatus("Cache invalid, downloading from API...")

					// Create a new MapView since hydrateMapData can only be called once
					recreateMapView()
					downloadMvfFromApi()
				}
		}
	}

	private fun downloadMvfFromApi() {
		val downloadStartTime = System.currentTimeMillis()

		CoroutineScope(Dispatchers.IO).launch {
			try {
				// Step 1: Get access token
				val tokenStartTime = System.currentTimeMillis()
				val accessToken = getAccessToken()
				val tokenDuration = System.currentTimeMillis() - tokenStartTime
				Log.d(TAG, "Got access token in ${tokenDuration}ms")

				withContext(Dispatchers.Main) {
					updateStatus("Got token, fetching MVF URL...")
				}

				// Step 2: Get MVF download URL
				val mvfUrlStartTime = System.currentTimeMillis()
				val mvfUrl = getMvfUrl(accessToken, MAP_ID)
				val mvfUrlDuration = System.currentTimeMillis() - mvfUrlStartTime
				Log.d(TAG, "Got MVF URL in ${mvfUrlDuration}ms")

				withContext(Dispatchers.Main) {
					updateStatus("Downloading MVF bundle...")
				}

				// Step 3: Download the MVF zip file
				val downloadZipStartTime = System.currentTimeMillis()
				val mvfData = downloadMvfZip(mvfUrl)
				val downloadZipDuration = System.currentTimeMillis() - downloadZipStartTime
				Log.d(TAG, "Downloaded MVF zip (${mvfData.size} bytes) in ${downloadZipDuration}ms")

				val totalDownloadDuration = System.currentTimeMillis() - downloadStartTime
				Log.d(
					TAG,
					"=== TOTAL MVF DOWNLOAD TIME: ${totalDownloadDuration}ms (token: ${tokenDuration}ms, mvfUrl: ${mvfUrlDuration}ms, download: ${downloadZipDuration}ms) ===",
				)

				withContext(Dispatchers.Main) {
					dataLoadDuration = totalDownloadDuration
					updateStatus("Downloaded MVF, hydrating...")
					hydrateMvfData(mvfData)
				}
			} catch (e: Exception) {
				Log.e(TAG, "Error downloading MVF: ${e.message}")
				withContext(Dispatchers.Main) {
					hideLoading()
					updateStatus("Error: ${e.message}")
				}
			}
		}
	}

	private fun getAccessToken(): String {
		val url = URL("https://app.mappedin.com/api/v1/api-key/token")
		val connection = url.openConnection() as HttpURLConnection
		connection.requestMethod = "POST"
		connection.setRequestProperty("Content-Type", "application/json")
		connection.doOutput = true

		val requestBody =
			JSONObject().apply {
				put("key", MAPPEDIN_KEY)
				put("secret", MAPPEDIN_SECRET)
			}

		connection.outputStream.use { os ->
			os.write(requestBody.toString().toByteArray())
		}

		val response = connection.inputStream.bufferedReader().use { it.readText() }
		val jsonResponse = JSONObject(response)
		return jsonResponse.getString("access_token")
	}

	private fun getMvfUrl(
		accessToken: String,
		mapId: String,
	): String {
		val url = URL("https://app.mappedin.com/api/venue/$mapId/mvf?version=3.0.0")
		val connection = url.openConnection() as HttpURLConnection
		connection.requestMethod = "GET"
		connection.setRequestProperty("Authorization", "Bearer $accessToken")

		val response = connection.inputStream.bufferedReader().use { it.readText() }
		val jsonResponse = JSONObject(response)
		return jsonResponse.getString("url")
	}

	private fun downloadMvfZip(mvfUrl: String): ByteArray {
		val url = URL(mvfUrl)
		val connection = url.openConnection() as HttpURLConnection
		connection.requestMethod = "GET"

		return connection.inputStream.use { it.readBytes() }
	}

	private fun hydrateMvfData(mvfData: ByteArray) {
		val hydrateStartTime = System.currentTimeMillis()

		// First save to cache, then load via URL for optimal performance
		saveToCache(MAP_ID, mvfData)

		// Get the cache URL using the helper - this uses the WebViewAssetLoader to serve files
		val cacheFileName = "$CACHE_FILE_PREFIX$MAP_ID.zip"
		val mvfUrl = MapView.getCacheUrl(cacheFileName)
		Log.d(TAG, "Hydrating from URL: $mvfUrl")

		// Pass credentials to enable outdoor view (tileset tokens require authentication)
		val options =
			GetMapDataWithCredentialsOptions(
				key = MAPPEDIN_KEY,
				secret = MAPPEDIN_SECRET,
				mapId = MAP_ID,
			)

		// Use hydrateMapDataFromURL for faster loading
		mapView.hydrateMapDataFromURL(mvfUrl, options) { result ->
			val hydrateEndTime = System.currentTimeMillis()
			val hydrateDuration = hydrateEndTime - hydrateStartTime
			Log.d(TAG, "hydrateMapDataFromURL took ${hydrateDuration}ms")

			result
				.onSuccess {
					Log.d(TAG, "hydrateMapDataFromURL success")
					updateStatus("Hydrated, displaying map...")
					showMap()
				}.onFailure { error ->
					Log.e(TAG, "hydrateMapDataFromURL error: $error")
					hideLoading()
					updateStatus("Error: ${error.message}")
				}
		}
	}

	private fun showMap() {
		val show3dMapStartTime = System.currentTimeMillis()

		mapView.show3dMap(Show3DMapOptions()) { result ->
			val show3dMapEndTime = System.currentTimeMillis()
			val show3dMapDuration = show3dMapEndTime - show3dMapStartTime
			val totalDuration = show3dMapEndTime - loadStartTime
			val source = if (isCachedLoad) "CACHE" else "REST_API"

			result
				.onSuccess {
					hideLoading()
					Log.d(TAG, "show3dMap success - took ${show3dMapDuration}ms")
					Log.d(
						TAG,
						"=== TOTAL MAP LOAD TIME ($source): ${totalDuration}ms (data: ${dataLoadDuration}ms, show3dMap: ${show3dMapDuration}ms) ===",
					)
					updateStatus("Map loaded from $source!")
					onMapReady()
				}.onFailure { error ->
					hideLoading()
					Log.e(TAG, "show3dMap error: $error (after ${show3dMapDuration}ms)")
					updateStatus("Error displaying map: ${error.message}")
				}
		}
	}

	private fun onMapReady() {
		Log.d(TAG, "Map displayed successfully")

		// Add labels to all named spaces
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				spaces.filter { it.name.isNotEmpty() }.forEach { space ->
					mapView.labels.add(space, space.name, AddLabelOptions(interactive = true)) { }
				}
			}
		}
	}

	private fun saveToCache(
		mapId: String,
		data: ByteArray,
	) {
		try {
			val cacheFile = getCacheFile(mapId)
			cacheFile.writeBytes(data)
			Log.d(TAG, "MVF data cached successfully to ${cacheFile.absolutePath}")
			Log.d(TAG, "Cache size: ${data.size} bytes")
		} catch (e: Exception) {
			Log.e(TAG, "Failed to save cache: ${e.message}")
		}
	}

	private fun loadFromCache(mapId: String): ByteArray? =
		try {
			val cacheFile = getCacheFile(mapId)
			if (cacheFile.exists()) {
				Log.d(TAG, "Found cached MVF data at ${cacheFile.absolutePath}")
				Log.d(TAG, "Cache size: ${cacheFile.length()} bytes")
				cacheFile.readBytes()
			} else {
				Log.d(TAG, "No cached MVF data found for $mapId")
				null
			}
		} catch (e: Exception) {
			Log.e(TAG, "Failed to load cache: ${e.message}")
			null
		}

	private fun deleteFromCache(mapId: String) {
		try {
			val cacheFile = getCacheFile(mapId)
			if (cacheFile.exists()) {
				cacheFile.delete()
				Log.d(TAG, "Deleted cached MVF data for $mapId")
			}
		} catch (e: Exception) {
			Log.e(TAG, "Failed to delete cache: ${e.message}")
		}
	}

	private fun getCacheFile(mapId: String): File = File(filesDir, "$CACHE_FILE_PREFIX$mapId.zip")

	private fun recreateMapView() {
		val parent = mapView.view.parent as? FrameLayout ?: return
		parent.removeView(mapView.view)

		// Destroy the old MapView to release WebView resources and prevent memory leaks
		mapView.destroy()

		mapView = MapView(this)
		parent.addView(
			mapView.view,
			0,
			ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
			),
		)
	}

	private fun updateStatus(message: String) {
		runOnUiThread {
			statusText.text = message
			statusText.visibility = View.VISIBLE
		}
	}

	private fun hideLoading() {
		runOnUiThread {
			loadingIndicator.visibility = View.GONE
		}
	}
}
