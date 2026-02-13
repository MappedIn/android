package com.mappedin.demo.composemapsample

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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

@Composable
fun MappedinComposable() {
    var mapLoaded by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val mapView by remember { mutableStateOf(MapView(ctx)) }

    AndroidView(
        factory = {
            mapView.view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            // See Trial API key Terms and Conditions
            // https://developer.mappedin.com/docs/demo-keys-and-maps
            val options = GetMapDataWithCredentialsOptions(
                key = "5eab30aa91b055001a68e996",
                secret = "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                mapId = "mappedin-demo-mall",
            )

            // Load map data.
            mapView.getMapData(options) { result ->
                result
                    .onSuccess {
                        Log.d("MappedinComposable", "getMapData success")
                        // Display the map.
                        mapView.show3dMap(Show3DMapOptions()) { r ->
                            r.onSuccess {
                                Log.d("MappedinComposable", "show3dMap success")
                                mapLoaded = true
                                onMapReady(mapView) { title, message ->
                                    dialogTitle = title
                                    dialogMessage = message
                                    showDialog = true
                                }
                            }
                            r.onFailure {
                                Log.e("MappedinComposable", "show3dMap error: $it")
                            }
                        }
                    }.onFailure {
                        Log.e("MappedinComposable", "getMapData error: $it")
                    }
            }

            mapView.view
        },
    )

    if (!mapLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}

private fun onMapReady(mapView: MapView, showMessage: (String, String) -> Unit) {
    // Set all spaces to be interactive so they can be clicked.
    mapView.mapData.getByType<Space>(MapDataType.SPACE) { result ->
        result.onSuccess { spaces ->
            spaces.forEach { space ->
                mapView.updateState(space, GeometryUpdateState(interactive = true)) { }
            }
        }
    }

    // Set up click listener.
    mapView.on(Events.Click) { clickPayload ->
        clickPayload ?: return@on
        handleClick(clickPayload, showMessage)
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

    // Draw an interactive navigation path from Microsoft to Apple.
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

    // Draw an interactive path from Uniqlo to Nespresso.
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

private fun handleClick(clickPayload: ClickPayload, showMessage: (String, String) -> Unit) {
    val title = clickPayload.floors?.firstOrNull()?.name ?: "Map Click"
    val message = StringBuilder()

    // If a label was clicked, add its text to the message.
    val labels = clickPayload.labels
    if (!labels.isNullOrEmpty()) {
        message.append("Label Clicked: ")
        message.append(labels.first().text)
        message.append("\n")
    }

    // If a space was clicked, add its name to the message.
    val spaces = clickPayload.spaces
    if (!spaces.isNullOrEmpty()) {
        val space = spaces.first()
        message.append("Space clicked: ")
        message.append(space.name)
        message.append("\n")
    }

    // If a path was clicked, add it to the message.
    if (!clickPayload.paths.isNullOrEmpty()) {
        message.append("You clicked a path.\n")
    }

    // Add the coordinates clicked to the message.
    message.append("Coordinate Clicked: \nLatitude: ")
    message.append(clickPayload.coordinate.latitude)
    message.append("\nLongitude: ")
    message.append(clickPayload.coordinate.longitude)

    showMessage(title, message.toString())
}
