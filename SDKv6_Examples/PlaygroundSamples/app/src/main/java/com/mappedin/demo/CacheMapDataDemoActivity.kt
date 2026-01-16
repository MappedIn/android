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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Demonstrates caching map data for offline use.
 *
 * This demo shows how to:
 * 1. Check if map data is cached locally
 * 2. Load from cache using hydrateMapData if available
 * 3. Fetch from server using getMapData if not cached
 * 4. Save the fetched data to cache using toBinaryBundle
 *
 * The cached data is stored in the app's internal files directory.
 */
class CacheMapDataDemoActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var clearCacheButton: Button
    private var currentMapId: String = ""

    companion object {
        private const val TAG = "CacheMapDataDemo"
        private const val CACHE_FILE_PREFIX = "cached-map-"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Cache Map Data"

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

    private fun loadMapData() {
        val options =
            GetMapDataWithCredentialsOptions(
                key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
                secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
                mapId = "67881b4666a208000badecc4",
            )

        val mapId = options.mapId
        currentMapId = mapId

        // Check if there is cached data for this map
        val cachedData = loadFromCache(mapId)

        if (cachedData != null) {
            Log.d(TAG, "Using cached map data for $mapId")
            updateStatus("Loading from cache...")
            loadFromCachedData(cachedData, options)
        } else {
            Log.d(TAG, "Fetching map data from server for $mapId")
            updateStatus("Fetching from server...")
            fetchFromServer(options)
        }
    }

    private var loadStartTime: Long = 0

    private fun loadFromCachedData(
        cachedData: ByteArray,
        options: GetMapDataWithCredentialsOptions,
    ) {
        // Create the backup object in the format expected by hydrateMapData
        val mainArray = JSONArray()
        for (byte in cachedData) {
            mainArray.put(byte.toInt() and 0xFF)
        }
        val backupObject =
            JSONObject().apply {
                put("type", "binary")
                put("main", mainArray)
            }

        loadStartTime = System.currentTimeMillis()
        val hydrateStartTime = System.currentTimeMillis()

        mapView.hydrateMapData(backupObject, options) { result ->
            val hydrateEndTime = System.currentTimeMillis()
            val hydrateDuration = hydrateEndTime - hydrateStartTime

            result
                .onSuccess {
                    Log.d(TAG, "hydrateMapData success - loaded from cache in ${hydrateDuration}ms")
                    updateStatus("Loaded from cache!")
                    showMap(isCached = true, dataLoadDuration = hydrateDuration)
                }.onFailure { error ->
                    Log.e(TAG, "hydrateMapData error: $error (after ${hydrateDuration}ms)")
                    // If cache is corrupted, delete it and fetch fresh data
                    deleteFromCache(options.mapId)
                    updateStatus("Cache invalid, fetching from server...")

                    // Create a new MapView since hydrateMapData can only be called once
                    recreateMapView()
                    fetchFromServer(options)
                }
        }
    }

    private fun fetchFromServer(options: GetMapDataWithCredentialsOptions) {
        loadStartTime = System.currentTimeMillis()
        val getMapDataStartTime = System.currentTimeMillis()

        mapView.getMapData(options) { result ->
            val getMapDataEndTime = System.currentTimeMillis()
            val getMapDataDuration = getMapDataEndTime - getMapDataStartTime

            result
                .onSuccess {
                    Log.d(TAG, "getMapData success - fetched from server in ${getMapDataDuration}ms")
                    updateStatus("Fetched from server...")

                    // Cache saving is deferred until after show3dMap to avoid blocking the render
                    showMap(
                        isCached = false,
                        dataLoadDuration = getMapDataDuration,
                        mapIdToCache = options.mapId,
                    )
                }.onFailure { error ->
                    Log.e(TAG, "getMapData error: $error (after ${getMapDataDuration}ms)")
                    hideLoading()
                    updateStatus("Error: ${error.message}")
                }
        }
    }

    private fun showMap(
        isCached: Boolean = false,
        dataLoadDuration: Long = 0,
        mapIdToCache: String? = null,
    ) {
        val show3dMapStartTime = System.currentTimeMillis()

        mapView.show3dMap(Show3DMapOptions()) { result ->
            val show3dMapEndTime = System.currentTimeMillis()
            val show3dMapDuration = show3dMapEndTime - show3dMapStartTime
            val totalDuration = show3dMapEndTime - loadStartTime
            val source = if (isCached) "CACHE" else "NETWORK"

            result
                .onSuccess {
                    hideLoading()
                    Log.d(TAG, "show3dMap success - took ${show3dMapDuration}ms")
                    Log.d(
                        TAG,
                        "=== TOTAL MAP LOAD TIME ($source): ${totalDuration}ms (data: ${dataLoadDuration}ms, show3dMap: ${show3dMapDuration}ms) ===",
                    )
                    onMapReady()

                    // Save to cache after map is displayed to avoid blocking the render
                    if (mapIdToCache != null) {
                        Log.d(TAG, "Starting background cache save...")
                        saveToCache(mapIdToCache)
                    }
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

    private fun saveToCache(mapId: String) {
        mapView.mapData.toBinaryBundle(downloadLanguagePacks = true) { result ->
            result
                .onSuccess { bundle ->
                    if (bundle != null) {
                        try {
                            val cacheFile = getCacheFile(mapId)
                            cacheFile.writeBytes(bundle.main)
                            Log.d(TAG, "Map data cached successfully to ${cacheFile.absolutePath}")
                            Log.d(TAG, "Cache size: ${bundle.main.size} bytes")
                            updateStatus("Cached for offline use!")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save cache: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "toBinaryBundle returned null")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "toBinaryBundle error: $error")
                }
        }
    }

    private fun loadFromCache(mapId: String): ByteArray? =
        try {
            val cacheFile = getCacheFile(mapId)
            if (cacheFile.exists()) {
                Log.d(TAG, "Found cached data at ${cacheFile.absolutePath}")
                Log.d(TAG, "Cache size: ${cacheFile.length()} bytes")
                cacheFile.readBytes()
            } else {
                Log.d(TAG, "No cached data found for $mapId")
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
                Log.d(TAG, "Deleted cached data for $mapId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cache: ${e.message}")
        }
    }

    private fun getCacheFile(mapId: String): File = File(filesDir, "$CACHE_FILE_PREFIX$mapId.bin")

    private fun recreateMapView() {
        val parent = mapView.view.parent as? FrameLayout ?: return
        parent.removeView(mapView.view)

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
