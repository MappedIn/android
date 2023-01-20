package ca.mappedin.playgroundsamples.examples

import androidx.appcompat.app.AppCompatActivity
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.*

class ListLocations : AppCompatActivity(), MPIMapViewListener {
    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
        TODO("Not yet implemented")
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
        TODO("Not yet implemented")
    }

    override fun onDataLoaded(data: MPIData) {
        TODO("Not yet implemented")
    }

    override fun onFirstMapLoaded() {
        TODO("Not yet implemented")
    }

    override fun onMapChanged(map: MPIMap) {
        TODO("Not yet implemented")
    }

    override fun onNothingClicked() {
        TODO("Not yet implemented")
    }

    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
        TODO("Not yet implemented")
    }

    override fun onStateChanged(state: MPIState) {
        TODO("Not yet implemented")
    }
}