package ca.mappedin.playgroundsamples.data

import ca.mappedin.playgroundsamples.examples.AddInteractivity
import ca.mappedin.playgroundsamples.examples.RenderMap
import ca.mappedin.playgroundsamples.model.Example

class Datasource {
    fun loadExamples(): List<Example> {
        return listOf<Example>(
            Example("Display a map", "Basic venue loading and map rendering", RenderMap::class.java),
            Example("Add interactivity", "Make locations clickable with onPolygonClicked -callback", AddInteractivity::class.java),
//            Example("Markers", "Adding HTML markers to the map view"),
//            Example("Tooltips", "Adding clickable HTML tooltips to the map view"),
//            Example("A-B navigation", "Get directions from A to B displayed on the map"),
//            Example("Blue Dot onClick", "Display the Blue Dot and move it by tapping on the map"),
//            Example("Camera controls", "Set, animate or focus the camera on a set of map objects"),
//            Example("List locations", "List locations of a venue without rendering the map"),
//            Example("List categories", "List locations in sectioned by category"),
//            Example("Level selector", "Add a level selector"),
//            Example("Turn-by-turn directions", "Display text-based turn-by-turn directions"),
//            Example("Search", "Search locations within a venue")
        )
    }
}