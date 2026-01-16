package com.mappedin.demo

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddText3DPointOptions
import com.mappedin.models.Events
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.InitializeText3DState
import com.mappedin.models.Show3DMapOptions
import kotlin.random.Random

class Text3DDemoActivity : AppCompatActivity() {
    private lateinit var loadingIndicator: ProgressBar

    private val colors =
        listOf(
            "#ff0000",
            "#0000ff",
            "#008000",
            "#ffff00",
            "#800080",
            "#ffa500",
            "#ffc0cb",
            "#00ffff",
            "#ffffff",
            "#000000",
        )

    private fun getRandomColor(): String = colors.random()

    private fun getRandomFontSize(): Double {
        val min = 5.0
        val max = 8.0
        return Random.nextDouble(min, max)
    }

    private fun getRandomRotation(): Double = Random.nextDouble(0.0, 360.0)

    private fun getRandomWidth(): Double = Random.nextDouble(0.0, 1.0)

    private fun getRandomStrokeWidth(): Double = Random.nextDouble(0.0, 0.1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root)

        // Header UI (title + description) above the map
        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(
                        dp(16),
                        systemBars.top + dp(12),
                        dp(16),
                        dp(12),
                    )
                    insets
                }
            }
        val titleView =
            TextView(this).apply {
                text = "Text3D"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.create(typeface, Typeface.BOLD)
            }
        val descriptionView =
            TextView(this).apply {
                text = "Click anywhere on the map to place text with random appearance settings."
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor("#6B7280".toColorInt())
            }
        header.addView(titleView)
        header.addView(descriptionView)
        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Map view container with loading indicator
        val mapContainer = FrameLayout(this)
        val mapView = MapView(this)
        mapContainer.addView(
            mapView.view,
            FrameLayout.LayoutParams(
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
        mapContainer.addView(loadingIndicator, loadingParams)

        mapContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(mapContainer)

        // See Trial API key Terms and Conditions
        // https://developer.mappedin.com/web/v6/trial-keys-and-maps/
        val options =
            GetMapDataWithCredentialsOptions(
                key = "mik_yeBk0Vf0nNJtpesfu560e07e5",
                secret = "mis_2g9ST8ZcSFb5R9fPnsvYhrX3RyRwPtDGbMGweCYKEq385431022",
                mapId = "6679882a8298d5000b85ee89",
            )

        mapView.getMapData(options) { result ->
            result
                .onSuccess {
                    Log.d("MappedinDemo", "getMapData success")
                    val showOptions = Show3DMapOptions()
                    mapView.show3dMap(showOptions) { r2 ->
                        r2.onSuccess {
                            runOnUiThread {
                                loadingIndicator.visibility = View.GONE
                            }
                            Log.d("MappedinDemo", "show3dMap success")

                            onMapReady(mapView)
                        }
                        r2.onFailure {
                            runOnUiThread {
                                loadingIndicator.visibility = View.GONE
                            }
                            Log.e("MappedinDemo", "show3dMap error: $it")
                        }
                    }
                }.onFailure {
                    Log.e("MappedinDemo", "getMapData error: $it")
                }
        }
    }

    private fun onMapReady(mapView: MapView) {
        mapView.on(Events.Click) { clickPayload ->
            val coordinate = clickPayload?.coordinate ?: return@on

            // Generate random appearance settings
            val fontSize = getRandomFontSize()
            val color = getRandomColor()
            val outlineWidth = getRandomWidth()
            val outlineColor = getRandomColor()
            val strokeWidth = getRandomStrokeWidth()
            val strokeColor = getRandomColor()
            val rotation = getRandomRotation()

            val appearance =
                InitializeText3DState(
                    color = color,
                    fontSize = fontSize,
                    outlineWidth = outlineWidth,
                    outlineColor = outlineColor,
                    strokeWidth = strokeWidth,
                    strokeColor = strokeColor,
                )

            val options =
                AddText3DPointOptions(
                    appearance = appearance,
                    rotation = rotation,
                )

            // Log the appearance settings
            Log.d(
                "MappedinDemo",
                "Adding Text3D with: " +
                    "fontSize=${"%.2f".format(fontSize)}, " +
                    "color=$color, " +
                    "outlineWidth=${"%.2f".format(outlineWidth)}, " +
                    "outlineColor=$outlineColor, " +
                    "strokeWidth=${"%.3f".format(strokeWidth)}, " +
                    "strokeColor=$strokeColor, " +
                    "rotation=${"%.0f".format(rotation)}°",
            )

            mapView.text3D.add(
                target = coordinate,
                content = "Hello, world!",
                options = options,
            ) { result ->
                result.onSuccess { text3DView ->
                    if (text3DView != null) {
                        Log.d("MappedinDemo", "Text3D added with id: ${text3DView.id}")
                    }
                }
                result.onFailure { error ->
                    Log.e("MappedinDemo", "Failed to add Text3D: $error")
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }
}
