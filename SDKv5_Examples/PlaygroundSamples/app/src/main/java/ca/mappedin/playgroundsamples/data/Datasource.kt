package ca.mappedin.playgroundsamples.data

import ca.mappedin.playgroundsamples.examples.*
import ca.mappedin.playgroundsamples.model.Example

class Datasource {
    fun loadExamples(): List<Example> {
        return listOf<Example>(
            Example("Display a Map", "Basic venue loading and map rendering", RenderMap::class.java),
            Example("Add Interactivity", "React to location tapped", AddInteractivity::class.java),
            Example("Markers", "Adding HTML markers to the map view", Markers::class.java),
            Example("A-B navigation", "Get directions from A to B displayed on the map", ABWayfinding::class.java),
            Example("Blue Dot", "Display the Blue Dot on the map", BlueDot::class.java),
            Example("Camera controls", "Set, animate or focus the camera on a set of map objects", CameraControls::class.java),
            Example("List locations", "List locations of a venue without rendering the map", ListLocations::class.java),
            Example("Level selector", "Add a level selector", LevelSelector::class.java)
//            Example("Turn-by-turn directions", "Display text-based turn-by-turn directions"),
//            Example("Search", "Search locations within a venue")
        )
    }
}
