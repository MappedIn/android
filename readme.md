# MappedIn Android SDK Example

This repo contains a simple example to get you started with the MappedIn Android SDK. Before you can do anything, make sure you have an API key from MappedIn. Talk to your representative to get one. The JavaDocs are available [here](http://mappedin.github.io/android/).

## Setup
Just include the com.mappedin.sdk project from Maven Central. You can also clone this repo and use the pre-configured samples. We recommend you ensure your credentials are working with the Hello World sample before anything else.

## API Quick Start
You can view all the documentation in this project's GitHub page, but here's a quick guide to the key classes you need to just display a Map.

### mappedin
This is the class to start with. It controls all communication with the MappedIn API. First, make sure you have your API key and Secret set in your `AndroidManifest.xml` like so:

```
<meta-data android:name="com.mappedin.sdk.Username" android:value="<your key here>"/>
<meta-data android:name="com.mappedin.sdk.Password" android:value="<your secret here>"/>
```
If you don't know your key and secret, please talk to your mappedin representative to get access.

Once you have your manifest set up correctly, you can begin writing actual code. Instantiate a `com.mappedin.sdk.MappedIn` object, passing in your application's Context. Then make a call to 

```mappedin.getVenues( MappedInCallBack<List<Venue>> )```

This will query the MappedIn servers and if successful will execute the `onCompleted` function on your `MappedInCallBack` object with a list of `Venue`s your key has access to. Each `Venue` will have basic details like the name and logo populated, but not the larger fields like `maps` and `locations`.

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
