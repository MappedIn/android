package ca.mappedin.playgroundsamples.examples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.web.MPIOptions

class RenderMap : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        this.title = "Render a map"

        val mapView = findViewById<MPIMapView>(R.id.mapView)

        mapView.loadVenue(
            MPIOptions.Init("5eab30aa91b055001a68e996", "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-mall"),
            MPIOptions.ShowVenue(labelAllLocationsOnInit = true, backgroundColor = "white")
        ) { Log.e(javaClass.simpleName, "Error loading map view")}

        // TODO: Looks like the error call back is not optional, it should be
    }
}