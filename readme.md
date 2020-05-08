# MappedIn Android SDK Examples

This repo contains a few examples to get you started with the MappedIn Android SDK:

 - **helloworld:** A basic app that shows minimal code to get a map loaded into a mapView with the SDK
 - **singlevenue:** An app that loads a single venue with a tabbed experience including search, and directory. It's a little rough around the edges. While it demos some interesting features and provides useful sample code, we'd recommend you not use it as a base for your own application.
 - **wayfindingSample:** The most fully featured sample app. It is integrated with google's location service. You can use a location spoofing app to see a "blue dot" experience and wayfind from the blue dot. We've provided keys with access to a fake mall called "Mappedin Demo Mall." If you want to try out the blue dot, start by spoofing your location to (43.52023014, -80.5352595)

 The JavaDocs are available [here](http://mappedin.github.io/android/).

## Setup
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

This will query the MappedIn servers and if successful will execute the `onCompleted` function on your `MappedInCallBack` object with an array of `Venue`s your key has access to. Each `Venue` will have basic details like the name and logo populated, but not the larger fields like `maps` and `locations`.

To get all of the data for a specific venue, call

```mappedIn.getVenue(Venue venue, boolean accessible, LocationGenerator locationGenerator, MappedInCallBack<Venue> callback)```

with the venue you are interested in. Similar to `getVenues`, this function will interface with the MappedIn API for you, sending the fully populated `Venue` to your callback's onComplete when it's done.

You can specify when you want accessible directions or not with the `accessible` parameter.

A `Venue` typically has a few different `Location` types, each with different properties. Passing in a custom `LocationGenerator` will let you build custom `Location` objects that have access to those properties. Your mappedin in representative will help you if this applies to you. If not, just pass in `null`.

### Venue
The `Venue` object will contain all information associated with a given venue (once it's been filled in with `getVenue`). You can find lists of all the Venue's `Map`, `Location`, `Category`, `Vortex` (connections between Maps like elevators and stairs) and `Polygon` objects.

### MapView
The `MapView` object is a `Fragment` that controls displaying the actual Map. Just call

```mapView.setMap(Map map);```

with the map you want to display (available from `venue.getMaps()`).

That's the basics. Directions, Markers and other features will be covered in a future tutorial, but you can see them in action right now in the Single Venue project.

# Mappedin Web Android Sample

There is also a sample app in this repo that demonstrates creating a Web View in an Android application, and displaying Mappedin Web. The example can be found in the webviewsample folder. This example does not use the Android SDK. You will require a Mappedin Web API Key for the sample to function. Please talk to your representative if you need one.
