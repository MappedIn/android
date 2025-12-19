package com.mappedin.demo

import android.graphics.Color
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mappedin.MapView
import com.mappedin.models.AddLabelOptions
import com.mappedin.models.AddPathOptions
import com.mappedin.models.ClickPayload
import com.mappedin.models.EnterpriseLocation
import com.mappedin.models.Events
import com.mappedin.models.GeometryUpdateState
import com.mappedin.models.GetMapDataWithCredentialsOptions
import com.mappedin.models.MapDataType
import com.mappedin.models.NavigationOptions
import com.mappedin.models.NavigationTarget
import com.mappedin.models.Show3DMapOptions
import com.mappedin.models.Space

class InteractivityDemoActivity : AppCompatActivity() {
	private lateinit var mapView: MapView
	private lateinit var loadingIndicator: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = "Interactivity"

		val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		setContentView(root)

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
				text = "Interactivity"
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
				typeface = Typeface.create(typeface, Typeface.BOLD)
			}
		val descriptionView =
			TextView(this).apply {
				text = "Click on labels, spaces, and paths to see interactive features in action."
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
				setTextColor("#6B7280".toColorInt())
			}
		header.addView(titleView)
		header.addView(descriptionView)
		root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
		// https://developer.mappedin.com/api-keys/
		val options =
			GetMapDataWithCredentialsOptions(
				key = "5eab30aa91b055001a68e996",
				secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
				mapId = "mappedin-demo-mall",
			)

		mapView.getMapData(options) { result ->
			result
				.onSuccess {
					Log.d("MappedinDemo", "getMapData success")
					mapView.show3dMap(Show3DMapOptions()) { r ->
						r.onSuccess {
							runOnUiThread {
								loadingIndicator.visibility = View.GONE
							}
							Log.d("MappedinDemo", "show3dMap success - Map displayed")
							onMapReady()
						}
						r.onFailure {
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

	private fun onMapReady() {
		// Set all spaces to be interactive so they can be clicked
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
			result.onSuccess { spaces ->
				spaces.forEach { space ->
					mapView.updateState(space, GeometryUpdateState(interactive = true)) { }
				}
			}
		}

		// Set up click listener
		mapView.on(Events.CLICK) { event ->
			val clickPayload = event as? ClickPayload ?: return@on
			handleClick(clickPayload)
		}

		// Add interactive labels to all spaces with names.
		mapView.mapData.getByType<Space>(MapDataType.SPACE) { spacesResult ->
			spacesResult.onSuccess { spaces ->
				spaces.forEach { space ->
					if (space.name.isNotEmpty()) {
						mapView.labels.add(
							target = space,
							text = space.name,
							options = AddLabelOptions(interactive = true),
						)
					}
				}
			}
		}

		// Draw an interactive navigation path from Microsoft to Apple
		mapView.mapData.getByType<EnterpriseLocation>(MapDataType.ENTERPRISE_LOCATION) { result ->
			result.onSuccess { locations ->
				val microsoft = locations.find { it.name == "Microsoft" }
				val apple = locations.find { it.name == "Apple" }

				if (microsoft != null && apple != null) {
					mapView.mapData.getDirections(
						NavigationTarget.EnterpriseLocationTarget(microsoft),
						NavigationTarget.EnterpriseLocationTarget(apple),
					) { dirResult ->
						dirResult.onSuccess { directions ->
							if (directions != null) {
								val pathOptions = AddPathOptions(interactive = true)
								val navOptions = NavigationOptions(pathOptions = pathOptions)
								mapView.navigation.draw(directions, navOptions) { }
							}
						}
					}
				}
			}
		}

		// Draw an interactive path from Uniqlo to Nespresso
		mapView.mapData.getByType<EnterpriseLocation>(MapDataType.ENTERPRISE_LOCATION) { result ->
			result.onSuccess { locations ->
				val uniqlo = locations.find { it.name == "Uniqlo" }
				val nespresso = locations.find { it.name == "Nespresso" }

				if (uniqlo != null && nespresso != null) {
					mapView.mapData.getDirections(
						NavigationTarget.EnterpriseLocationTarget(uniqlo),
						NavigationTarget.EnterpriseLocationTarget(nespresso),
					) { dirResult ->
						dirResult.onSuccess { directions ->
							if (directions != null) {
								val pathOptions = AddPathOptions(interactive = true)
								mapView.paths.add(directions.coordinates, pathOptions) { }
							}
						}
					}
				}
			}
		}
	}

	private fun handleClick(clickPayload: ClickPayload) {
		val title: String
		val message = StringBuilder()

		// Use the map name as the title (from floors)
		title = clickPayload.floors?.firstOrNull()?.name ?: "Map Click"

		// If a label was clicked, add its text to the message
		val labels = clickPayload.labels
		if (!labels.isNullOrEmpty()) {
			message.append("Label Clicked: ")
			message.append(labels.first().text)
			message.append("\n")
		}

		// If a space was clicked, add its location name to the message
		val spaces = clickPayload.spaces
		if (!spaces.isNullOrEmpty()) {
			val space = spaces.first()
			message.append("Space clicked: ")
			message.append(space.name)
			message.append("\n")
		}

		// If a path was clicked, add it to the message
		if (!clickPayload.paths.isNullOrEmpty()) {
			message.append("You clicked a path.\n")
		}

		// Add the coordinates clicked to the message
		message.append("Coordinate Clicked: \nLatitude: ")
		message.append(clickPayload.coordinate.latitude)
		message.append("\nLongitude: ")
		message.append(clickPayload.coordinate.longitude)

		showMessage(title, message.toString())
	}

	private fun showMessage(
		title: String,
		message: String,
	) {
		runOnUiThread {
			val builder: AlertDialog.Builder = AlertDialog.Builder(this)
			builder
				.setMessage(message)
				.setTitle(title)

			val dialog: AlertDialog = builder.create()
			dialog.show()
		}
	}

	private fun dp(value: Int): Int {
		val density = resources.displayMetrics.density
		return (value * density).toInt()
	}
}
