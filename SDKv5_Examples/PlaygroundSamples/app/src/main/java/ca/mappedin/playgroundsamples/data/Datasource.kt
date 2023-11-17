package ca.mappedin.playgroundsamples.data

import ca.mappedin.playgroundsamples.examples.ABWayfinding
import ca.mappedin.playgroundsamples.examples.AddInteractivity
import ca.mappedin.playgroundsamples.examples.BlueDot
import ca.mappedin.playgroundsamples.examples.CameraControls
import ca.mappedin.playgroundsamples.examples.FlatLabels
import ca.mappedin.playgroundsamples.examples.FloatingLabels
import ca.mappedin.playgroundsamples.examples.LevelSelector
import ca.mappedin.playgroundsamples.examples.ListLocations
import ca.mappedin.playgroundsamples.examples.Markers
import ca.mappedin.playgroundsamples.examples.RenderMap
import ca.mappedin.playgroundsamples.examples.Search
import ca.mappedin.playgroundsamples.examples.Tooltips
import ca.mappedin.playgroundsamples.examples.TurnByTurnDirections
import ca.mappedin.playgroundsamples.model.Example

class Datasource {
    fun loadExamples(): List<Example> {
        return listOf<Example>(
            Example("Display a Map", "Basic venue loading and map rendering", RenderMap::class.java),
            Example("Add Interactivity", "React to touch events", AddInteractivity::class.java),
            Example("Floating Labels", "Display and modify floating labels", FloatingLabels::class.java),
            Example("Flat Labels", "Display and modify flat labels", FlatLabels::class.java),
            Example("Markers", "Adding HTML markers to the map view", Markers::class.java),
            Example("A-B Navigation", "Get directions from A to B displayed on the map", ABWayfinding::class.java),
            Example("Blue Dot", "Display the Blue Dot on the map", BlueDot::class.java),
            Example("Camera Controls", "Set, animate or focus the camera on a set of map objects", CameraControls::class.java),
            Example("List Locations", "List locations of a venue without rendering the map", ListLocations::class.java),
            Example("Tooltips", "Adding HTML tooltips to the map view", Tooltips::class.java),
            Example("Building & Level Selection", "Add a building & level selector", LevelSelector::class.java),
            Example("Turn-by-Turn Directions", "Display text-based turn-by-turn directions", TurnByTurnDirections::class.java),
            Example("Search", "Search locations within a venue", Search::class.java),
        )
    }
}
