# Mappedin Android Integrations

In this repo you can find a number of sample applications demonstrating different ways to integrate with Mappedin to render your maps and begin building your own custom indoor mapping experiences on Android. To learn more about ways to integrate with Mappedin, check out [our developer portal](https://developer.mappedin.com/).

The Mappedin SDK for Android enables you to build powerful, highly flexible, unique, indoor mapping experiences natively inside your Android apps. This repo contains projects showcasing each version of our fully native SDK, with more projects coming soon to get you started on more advanced projects. 

This repo also contains a project demonstrating how to integrate with our our out of the box web product, [Mappedin Web](https://www.mappedin.com/wayfinding/web-app/), within a webview on a mobile app. 

## Mappedin Android SDK v4 Examples

Version 4 of the Android SDK is the current version. This is a minimal sample app that you can use to get started. [API documentation](https://developer.mappedin.com/docs/android/latest/mappedin/) and [a walkthrough guide](https://developer.mappedin.com/guides/android/) are available via [our developer portal](https://developer.mappedin.com/).

## Mappedin Android SDK v3 Examples

Version 4 of the Android SDK is now in LTS (long term support). This is a minimal sample app that you can use to get started. [API documentation](https://developer.mappedin.com/docs/android/v3/mappedin/) is available via [our developer portal](https://developer.mappedin.com/).

## Mappedin Android SDK v2 Examples

Version 2 of the Android SDK is now deprecated. It supports much of the sample functionality as v1 and more, but in v2, rendering is powered by the [Mapbox Maps SDK](https://docs.mapbox.com/android/maps/overview/). This means that you'll be able to use some features of the Mapbox Maps SDK as well as Mappedin features.

You can view all the [documentation](https://developer.mappedin.com/docs/android/mappedin/) for the SDK along with informative [guides](https://developer.mappedin.com/guides/android/) at [developer.mappedin.com](https://developer.mappedin.com/).

In this repo we've provided some sample code to get you started on the SDK, and we've also provided a key and secret for the SDK that has access to some demo venues. When you're ready to start using your own venues with the SDK you will need to contact a Mappedin representative to get your own unique key and secret.

### Quickstart

This is simple walkthrough to get you started with the Mappedin Android SDK as quickly as possible, with minimal code. What you'll see is a map rendered on the screen for you to manipulate with familiar guestures, but no other interactions are enabled. The Mappedin Android SDK provides many ways to interact with the MapView and map data, but as this sample demonstrates, there is no built-in UI or default map interactions. You have complete flexibility over the behaviour, look and feel of applications built using the SDK.

Use this sample to start off if you want to integrate with the Mappedin Android SDK from scratch. More samples will be coming soon to demonstrate how to interact with the map in differnet ways to build sophisticated experiences. 

## Mappedin Android SDK v1 Examples

Version 1 of the Android SDK is now deprecated.

You can view all the up to date documentation in this project's [GitHub Page](http://mappedin.github.io/android/).

We've provided a key and secret in this repo that has access to some demo venues.

When you're ready to start using your own venues with the SDK you will need to contact a Mappedin representative to get your own unique key and secret.

### helloworld

This is simple walkthrough to get you started with v1 of the Mappedin Android SDK, with minimal code. The Mappedin Android SDK provides many ways to interact with the MapView and map data, but as this sample demonstrates, there is no built-in UI or default map interactions. You have complete flexibility over the behaviour, look and feel of applications built using the SDK.

Use this sample to start off if you want to integrate with v1 of the Mappedin Android SDK from scratch.

### singlevenue

An app that loads a single venue with a tabbed experience including search, and directory. It's a little rough around the edges. While it demos some interesting features and provides useful sample code, we'd recommend you not use it as a base for your own application.

### wayfindingSample

The most fully featured sample app. It is integrated with google's location service. You can use a location spoofing app to see a "blue dot" experience and wayfind from the blue dot. We've provided keys with access to a fake mall called "Mappedin Demo Mall." If you want to try out the blue dot, start by spoofing your location to (43.52023014, -80.5352595)


## Mappedin Android SDK v1 Setup
Just include compile 'com.mappedin.sdk:mappedin:0.1.+' from Maven Central. You can also clone this repo and use the pre-configured samples. We recommend you ensure your credentials are working with the Hello World sample before anything else.

Not sure how to use Maven Central? Open your project's build.gradle file, and ensure you have `mavenCentral()` in your `repositories` list:

```
repositories {
    mavenCentral()
}
```

Then, add a dependency on the Mappedin SDK. Add `compile 'com.mappedin.sdk:mappedin:0.1.+'` to the `dependencies` list in your `build.gradle`. Your `dependencies` might look something like this:

```
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    testCompile 'junit:junit:4.12'
    compile 'com.android.support:appcompat-v7:23.4.0'
    compile 'com.mappedin.sdk:mappedin:0.1.+'
 }
 ```

## API Quick Start
You can view all the documentation in this project's [GitHub page](http://mappedin.github.io/android/), but here's a quick guide to the key classes you need to just display a Map.

### mappedin
This is the class to start with. It controls all communication with the MappedIn API. First, make sure you have your API key and Secret set in your `AndroidManifest.xml` like so:

```
<meta-data android:name="com.mappedin.sdk.Username" android:value="<your key here>"/>
<meta-data android:name="com.mappedin.sdk.Password" android:value="<your secret here>"/>
```

To get you started we've provided a key and secret in this repo that has access to some demo venues.

When you're ready to start using your own venues with the SDK you will need to contact a Mappedin representative to get your own unique key and secret.

Once you have your manifest set up correctly, you can begin writing actual code. Instantiate a `com.mappedin.sdk.MappedIn` object, passing in your application's Context. Then make a call to

```mappedin.getVenues( MappedInCallBack<Venue[]> )```

This will query the Mappedin servers and if successful will execute the `onCompleted` function on your `MappedInCallBack` object with an array of `Venue`s your key has access to. Each `Venue` will have basic details like the name and logo populated, but not the larger fields like `maps` and `locations`.

To get all of the data for a specific venue, call

```mappedIn.getVenue(Venue venue, boolean accessible, LocationGenerator locationGenerator, MappedInCallBack<Venue> callback)```

with the venue you are interested in. Similar to `getVenues`, this function will interface with the Mappedin API for you, sending the fully populated `Venue` to your callback's onComplete when it's done.

You can specify when you want accessible directions or not with the `accessible` parameter.

A `Venue` typically has a few different `Location` types, each with different properties. Passing in a custom `LocationGenerator` will let you build custom `Location` objects that have access to those properties. Your mappedin in representative will help you if this applies to you. If not, just pass in `null`.

### Venue
The `Venue` object will contain all information associated with a given venue (once it's been filled in with `getVenue`). You can find lists of all the Venue's `Map`, `Location`, `Category`, `Vortex` (connections between Maps like elevators and stairs) and `Polygon` objects.

### MapView
The `MapView` object is a `Fragment` that controls displaying the actual Map. Just call

```mapView.setMap(Map map);```

with the map you want to display (available from `venue.getMaps()`).

That's the basics. Directions, Markers and other features will be covered in a future tutorial, but you can see them in action right now in the Single Venue project.


## Web Examples

### Mappedin Web Example

This is a short demonstration of creating a Web View in an Android application, and displaying Mappedin Web. It also contains an example of setting up external links to open in a separate browser.

To get you started we've provided a Mappedin Web API key and secret that has access to some demo venues.

When you're ready to start using your own venues you will need to contact a Mappedin representative to get your own unique key and secret. Add your Mappedin API keys, search keys, and the venue's slug to this file.
