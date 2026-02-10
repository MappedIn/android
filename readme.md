# Mappedin Android Samples

This repo contains sample applications demonstrating different ways to integrate with the Mappedin SDK for Android to render maps and build a custom indoor mapping experience. To learn more about ways to integrate with Mappedin, refer to [developer.mappedin.com](https://developer.mappedin.com/).

The Mappedin SDK for Android enables you to build powerful and highly flexible indoor mapping experiences natively on Android.

---

## Mappedin SDK for Android v6 Samples

To read more about the Mappedin SDK for Android, refer to [Getting Started with Mappedin SDK for Android](https://developer.mappedin.com/android-sdk/getting-started) and additional guides in the Mappedin developer docs.

The sample projects in this repo provide a key and secret to access demo maps. Production apps will need their own key and secret. Refer to [Create a Key & Secret](https://developer.mappedin.com/android-sdk/getting-started#create-a-key--secret) for instructions on how to create your own.

The following table lists the sample activities that pertain to the latest version of the Mappedin SDK for Android.

| **Sample**                   | **Description**                                                                              | **Guide**                         |
| ---------------------------- | -------------------------------------------------------------------------------------------- | --------------------------------- |
| [DisplayMapDemo]             | The most basic example to show a map.                                                        | [Getting Started]                 |
| [AreaShapesDemo]             | Demonstrates using shapes to show areas and route directions around closed areas.            | [Areas & Shapes]                  |
| [BlueDotDemo]                | Demonstrates using Blue Dot to show the user's position on the map.                          | [Blue Dot]                        |
| [BuildingFloorSelectionDemo] | Demonstrates switching between maps for venues with multiple floors and or multiple buildings. | [Building & Floor Selection]    |
| [CacheMapDataDemo]           | Demonstrates how to use cached map data to modify data between reloads.                      | [Cache Map Data]                  |
| [CacheMVFDemo]               | Demonstrates how to use cached Mappedin Venue Format ([MVFv3]) files for quicker reloads.    | [Cache MVF File]                  |
| [CameraDemo]                 | Demonstrates how to move the camera.                                                         | [Camera]                          |
| [ColorsAndTexturesDemo]      | Demonstrates how to apply custom colors and textures to the map.                             | [Images, Textures & Colors]       |
| [DynamicFocusDemo]           | Demonstrates how to use Dynamic Focus.                                                       | [Dynamic Focus]                   |
| [DynamicFocusManualDemo]     | Demonstrates how to create a custom Dynamic Focus effect.                                    | [Custom Dynamic Focus]            |
| [Image3DDemo]                | Demonstrates how to add images on a map.                                                     | [Images, Textures & Colors]       |
| [InteractivityDemo]          | Demonstrates how to capture and act on touch events.                                         | [Interactivity]                   |
| [LabelsDemo]                 | Demonstrates adding rich labels to the map.                                                  | [Labels]                          |
| [LocationsDemo]              | Demonstrates using location profiles and categories.                                         | [Location Profiles & Categories]  |
| [MarkersDemo]                | Demonstrates adding HTML Markers to the map.                                                 | [Markers]                         |
| [ModelsDemo]                 | Demonstrates adding 3D models to the map.                                                    | [3D Models]                       |
| [MultiFloorViewDemo]         | Demonstrates using multi floor view.                                                         | [Multi Floor View & Stacked Maps] |
| [NavigationDemo]             | Demonstrates wayfinding and navigation across multiple floors.                               | [Wayfinding]                      |
| [OfflineModeDemo]            | Demonstrates loading a map from a local Mappedin Venue Format ([MVFv3]) file.                | [Offline Mode]                    |
| [PathsDemo]                  | Demonstrates how to draw a path between two rooms.                                           | [Wayfinding]                      |
| [QueryDemo]                  | Demonstrates how to find the nearest room based on a coordinate and click event.             |                                   |
| [SearchDemo]                 | Demonstrates how to use the suggest and search feature.                                      | [Search]                          |
| [StackedMapsDemo]            | Demonstrates how to use the stacked maps.                                                    | [Multi Floor View & Stacked Maps] |
| [Text3DDemo]                 | Demonstrates how to use Text3D labels.                                                       | [Flat Text]                       |
| [TurnByTurnDemo]             | Demonstrates how to use turn by turn directions.                                             | [Turn-by-Turn Directions]         |

[DisplayMapDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/DisplayMapDemoActivity.kt
[AreaShapesDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/AreaShapesDemoActivity.kt
[BlueDotDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/BlueDotDemoActivity.kt
[BuildingFloorSelectionDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/BuildingFloorSelectionDemoActivity.kt
[CacheMapDataDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/CacheMapDataDemoActivity.kt
[CacheMVFDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/CacheMVFDemoActivity.kt
[CameraDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/CameraDemoActivity.kt
[ColorsAndTexturesDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/ColorsAndTexturesDemoActivity.kt
[DynamicFocusDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/DynamicFocusDemoActivity.kt
[DynamicFocusManualDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/DynamicFocusManualDemoActivity.kt
[Image3DDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/Image3DDemoActivity.kt
[InteractivityDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/InteractivityDemoActivity.kt
[LabelsDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/LabelsDemoActivity.kt
[LocationsDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/LocationsDemoActivity.kt
[MarkersDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/MarkersDemoActivity.kt
[ModelsDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/ModelsDemoActivity.kt
[MultiFloorViewDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/MultiFloorViewDemoActivity.kt
[NavigationDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/NavigationDemoActivity.kt
[OfflineModeDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/OfflineModeDemoActivity.kt
[PathsDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/PathsDemoActivity.kt
[QueryDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/QueryDemoActivity.kt
[SearchDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/SearchDemoActivity.kt
[StackedMapsDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/StackedMapsDemoActivity.kt
[Text3DDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/Text3DDemoActivity.kt
[TurnByTurnDemo]: ./SDKv6_Examples/PlaygroundSamples/app/src/main/java/com/mappedin/demo/TurnByTurnDemoActivity.kt
[MVFv3]: https://developer.mappedin.com/docs/mvf/v3/getting-started
[Getting Started]: https://developer.mappedin.com/android-sdk/getting-started
[Areas & Shapes]: https://developer.mappedin.com/android-sdk/shapes
[Blue Dot]: https://developer.mappedin.com/android-sdk/blue-dot
[Building & Floor Selection]: https://developer.mappedin.com/android-sdk/level-selection
[Cache Map Data]: https://developer.mappedin.com/android-sdk/getting-started#caching-and-loading-map-data-as-json
[Cache MVF File]: https://developer.mappedin.com/android-sdk/getting-started#caching-and-loading-map-data-as-a-mvf-file
[Camera]: https://developer.mappedin.com/android-sdk/camera
[Dynamic Focus]: https://developer.mappedin.com/android-sdk/dynamic-focus
[Custom Dynamic Focus]: https://developer.mappedin.com/android-sdk/dynamic-focus#implementing-dynamic-focus-using-mapview
[Images, Textures & Colors]: https://developer.mappedin.com/android-sdk/images-textures
[Interactivity]: https://developer.mappedin.com/android-sdk/interactivity
[Labels]: https://developer.mappedin.com/android-sdk/labels
[Location Profiles & Categories]: https://developer.mappedin.com/android-sdk/location-profiles-categories
[Markers]: https://developer.mappedin.com/android-sdk/markers
[3D Models]: https://developer.mappedin.com/android-sdk/3d-models
[Multi Floor View & Stacked Maps]: https://developer.mappedin.com/android-sdk/stacked-maps
[Wayfinding]: https://developer.mappedin.com/android-sdk/wayfinding
[Offline Mode]: https://developer.mappedin.com/android-sdk/getting-started#offline-loading-mode
[Search]: https://developer.mappedin.com/android-sdk/enterprise-data#search
[Flat Text]: https://developer.mappedin.com/android-sdk/labels#flat-labels-with-text3d
[Turn-by-Turn Directions]: https://developer.mappedin.com/android-sdk/wayfinding#turn-by-turn-directions

---

## Additional Resources

- [Mappedin Developer Site](https://developer.mappedin.com/)
- [Mappedin SDK for Android Getting Started Guide](https://developer.mappedin.com/android-sdk/getting-started)
- [Mappedin SDK for Android API Docs](https://docs.mappedin.com/android-sdk-api/v6/latest/)
- [Release Notes](https://developer.mappedin.com/android-sdk/release-notes)

---
