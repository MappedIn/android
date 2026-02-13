# Jetpack Compose with Mappedin

This sample Android application demonstrates how to use the [Mappedin SDK for Android v6](https://docs.mappedin.com/android-sdk-api/v6/latest/) with [Jetpack Compose](https://developer.android.com/develop/ui/compose).

It wraps the Mappedin `MapView` inside an `AndroidView` composable to display an interactive indoor map.

## Prerequisites

- Android Studio Ladybug or later
- Android SDK 36
- JDK 21

## Getting Started

1. Open this project in Android Studio.
2. Sync the Gradle project.
3. Run the app on an emulator or device with API 24+.

The app uses [Mappedin demo keys](https://developer.mappedin.com/docs/demo-keys-and-maps) to load the Mappedin Demo Mall.

## Key Files

- **`MappedinComposable.kt`** - Composable function that wraps `MapView` in an `AndroidView` and loads the map using the v6 two-step pattern (`getMapData` then `show3dMap`).
- **`MainActivity.kt`** - The entry point Activity that hosts the Compose UI.

## Resources

- [Mappedin SDK for Android v6 API Reference](https://docs.mappedin.com/android-sdk-api/v6/latest/)
- [Getting Started Guide](https://developer.mappedin.com/android-sdk/getting-started)
- [Mappedin Developer Portal](https://developer.mappedin.com)
