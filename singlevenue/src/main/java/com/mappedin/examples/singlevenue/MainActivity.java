package com.mappedin.examples.singlevenue;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import com.mappedin.jpct.Logger;
import com.mappedin.sdk.Analytics;
import com.mappedin.sdk.Category;
import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Cylinder;
import com.mappedin.sdk.Directions;
import com.mappedin.sdk.Focusable;
import com.mappedin.sdk.Instruction;
import com.mappedin.sdk.Location;
import com.mappedin.sdk.LocationGenerator;
import com.mappedin.sdk.Map;
import com.mappedin.sdk.MapView;
import com.mappedin.sdk.MapViewDelegate;
import com.mappedin.sdk.MappedIn;
import com.mappedin.sdk.MappedInException;
import com.mappedin.sdk.MappedinCallback;
import com.mappedin.sdk.Overlay;
import com.mappedin.sdk.Overlay2DLabel;
import com.mappedin.sdk.Path;
import com.mappedin.sdk.Polygon;
import com.mappedin.sdk.SmartSearch;
import com.mappedin.sdk.Vector3;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collections;

public class MainActivity extends FragmentActivity implements MapViewDelegate, SensorEventListener {

    private TabHost tabhost;
    private TabHost detailTabhost;
    private Button goButton = null;
    private Button walkingButton = null;
    private Button categoryBackButton = null;
    private Button levelChangeUpButton = null;
    private Button levelChangeDownButton = null;
    private TextView instructionTextView = null;
    private TextView titleLabel = null;
    private TextView selectOriginTextView = null;
    private TextView categoryTitleTextView = null;
    private TextView loading = null;
    private TextView levelNavTextView = null;
    private EditText search;
    private ListView categoryListView = null;
    private ListView categoryLocationListView = null;
    private ListView locationListView = null;
    private ListView suggestListView = null;
    private GridView searchGridView = null;
    private ImageView instructionImageView;
    private ImageView compass = null;

    private SensorManager mSensorManager;

    private Activity self;
    private MappedIn mappedIn;
    private Venue activeVenue = null;
    private Map[] maps;
    private MapView mapView;
    private MapViewDelegate delegate = this;
    private IAmHere iAmHere = null;
    private ArrayList<Polygon> hightLightPolygon = new ArrayList<>();
    private Polygon destinationPolygon = null;
    private SetMapCallback setMapCallback = new SetMapCallback();
    private SmartSearch smartSearch;

    private int currentLevelIndex = 0;
    private boolean navigationMode = false;
    private boolean accessibleDirections = false;
    private boolean walking = false;
    private boolean autoRotation = false;
    private float initialDegree = 0;

    static final int PICK_CONTACT_REQUEST = 1;  // The request code

    @Override
    protected void onResume(){
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }
    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mappedIn = new MappedIn(getApplication());
        setContentView(R.layout.activity_main);
        self = this;

        tabhost = (TabHost)findViewById(android.R.id.tabhost);
        tabhost.setup();
        tabhost.addTab(
                tabhost.newTabSpec("venue").setIndicator("Venue").setContent(R.id.venue_layout));
        tabhost.addTab(
                tabhost.newTabSpec("directory").setIndicator("Directory")
                        .setContent(R.id.directory_layout));
        tabhost.addTab(
                tabhost.newTabSpec("search").setIndicator("Search")
                        .setContent(R.id.search_layout));
        tabhost.setCurrentTab(0);

        // Directory
        detailTabhost = (TabHost)findViewById(R.id.detail_tab_host);
        detailTabhost.setup();
        detailTabhost.addTab(tabhost.newTabSpec("categories").setIndicator("Categories").setContent(R.id.categories));
        detailTabhost.addTab(tabhost.newTabSpec("stores").setIndicator("Stores A-Z").setContent(R.id.locations));
        detailTabhost.setCurrentTab(0);

        // Category
        categoryListView = (ListView)findViewById(R.id.category_list_view);
        categoryLocationListView = (ListView)findViewById(R.id.category_locations_list_view);
        categoryTitleTextView = (TextView)findViewById(R.id.top_category_text_view);
        categoryBackButton = (Button)findViewById(R.id.category_back_btn);
        categoryBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryTitleTextView.setText("All Categories");
                categoryListView.setVisibility(View.VISIBLE);
                categoryLocationListView.setVisibility(View.INVISIBLE);
                categoryBackButton.setVisibility(View.INVISIBLE);
            }
        });

        // Stores
        locationListView = (ListView)findViewById(R.id.locations_list_view);

        //location detail
        titleLabel = (TextView) findViewById(R.id.titleLabel);
        selectOriginTextView = (TextView) findViewById(R.id.selectOriginTextView);
        goButton = (Button) findViewById(R.id.goButton);
        goButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearLocationDetails();
                prepareNavigation();
            }
        });

        compass = (ImageView) findViewById(R.id.compass_image);
        compass.bringToFront();
        compass.setImageDrawable(getResources().getDrawable(R.drawable.compass));
        compass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoRotation = !autoRotation;
            }
        });
        instructionTextView = (TextView)findViewById(R.id.instruction_text);
        instructionImageView = (ImageView)findViewById(R.id.instruction_image);
        walkingButton = (Button)findViewById(R.id.walk_btn);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //map
        mappedIn.getVenues(new getVenuesCallback());

        loading = (TextView) findViewById(R.id.venue_loading_textview);

        //search
        search = (EditText) findViewById(R.id.search_edit_text);
        suggestListView = (ListView) findViewById(R.id.suggest_list_view);
        searchGridView = (GridView) findViewById(R.id.search_grid_view);

        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH){
                    suggestListView.setVisibility(View.INVISIBLE);
                    smartSearch.search(search.getText().toString(), 1000);
                    return true;
                }
                return false;
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                final String input = charSequence.toString();
                // try get suggest result in 500ms(0.5s)
                smartSearch.suggest(input, 500);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // auto hides the keyboard when navigating away form the search tab
        tabhost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId)
            {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(search.getWindowToken(), 0);

            }
        });
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param sensorEvent the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (autoRotation && iAmHere != null) {
            float degree = sensorEvent.values[0];
            Coordinate[] frame = iAmHere.getFrame();
            if (frame != null) {
                mapView.orbit(iAmHere.frame,
                        -initialDegree + (float) Math.toRadians(degree),
                        (float)Math.PI/5, 0);
            }
            if(iAmHere != null) {
                iAmHere.setRotation(initialDegree - (float) Math.toRadians(degree), 0);
            }
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Get the basic info for all Venues we have access to
    private class getVenuesCallback implements MappedinCallback<List<Venue>> {
        @Override
        public void onCompleted(final List<Venue> venues) {
            if (venues.size() == 0 ) {
                Logger.log("No venues available! Are you using the right credentials? Talk to your mappedin representative.");
                return;
            }
            // Grab the first venue, which is likely all you have
            activeVenue = venues.get(0);
            setTitle(activeVenue.getName());
            mapView = (MapView) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
            mapView.setDelegate(delegate);
            LocationGenerator customerLocation = new LocationGenerator() {
                @Override
                public Location locationGenerator(ByteBuffer data, int _index, Venue venue){
                    return new CustomerLocation(data, _index, venue);
                }
            };
            final LocationGenerator[] locationGenerators = {customerLocation};
            mappedIn.getVenue(activeVenue, locationGenerators, new GetVenueCallback());
        }

        @Override
        public void onError(Exception e) {
            Logger.log("Error loading any venues. Did you set your credentials? Exception: " + e);
        }
    }

    // Get the full details on a single Venue
    private class GetVenueCallback implements MappedinCallback<Venue> {
        @Override
        public void onCompleted(final Venue venue) {
            activeVenue = venue;
            smartSearch = mappedIn.initiateSearch(activeVenue);
            SearchCallback callback = new SearchCallback(self, smartSearch, search, suggestListView, searchGridView);
            smartSearch.setDelegate(callback);
            maps = venue.getMaps();
            if (maps.length == 0) {
                Logger.log("No maps! Make sure your venue is set up correctly!");
                return;
            }

            levelNavTextView = (TextView)findViewById(R.id.level_nav_textLabel);
            levelNavTextView.setText(maps[currentLevelIndex].getName());

            levelChangeUpButton = (Button) findViewById(R.id.level_up_btn);
            levelChangeDownButton = (Button) findViewById(R.id.level_down_btn);

            if (currentLevelIndex == maps.length-1) {
                levelChangeUpButton.setEnabled(false);
            }
            levelChangeUpButton.setOnClickListener(new View.OnClickListener() {
                public void onClick (View v) {
                    if (currentLevelIndex < maps.length) {
                        currentLevelIndex += 1;
                        levelChangeDownButton.setEnabled(true);
                        mapView.setMap(maps[currentLevelIndex], setMapCallback);
                        if (currentLevelIndex == maps.length-1) {
                            levelChangeUpButton.setEnabled(false);
                        }
                    }
                    levelNavTextView.setText(maps[currentLevelIndex].getName());
                }
            });

            if (currentLevelIndex == 0) {
                levelChangeDownButton.setEnabled(false);
            }
            levelChangeDownButton.setOnClickListener(new View.OnClickListener() {
                public void onClick (View v) {
                    if (currentLevelIndex > 0) {
                        currentLevelIndex -= 1;
                        levelChangeUpButton.setEnabled(true);
                        mapView.setMap(maps[currentLevelIndex], setMapCallback);
                        if (currentLevelIndex == 0) {
                            levelChangeDownButton.setEnabled(false);
                        }
                    }
                    levelNavTextView.setText(maps[currentLevelIndex].getName());
                }
            });

            if (maps.length == 1) {
                levelChangeUpButton.setVisibility(View.INVISIBLE);
                levelChangeDownButton.setVisibility(View.INVISIBLE);
                levelNavTextView.setVisibility(View.INVISIBLE);
            }

            Arrays.sort(maps, new Comparator<Map>() {
                @Override
                public int compare(Map a, Map b) {
                    return a.getFloor() - b.getFloor();
                }
            });
            showLoadingLogo();
            mapView.setMap(maps[currentLevelIndex], setMapCallback);
            TextPaint textPaint = new TextPaint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(30);
            textPaint.setTypeface(Typeface.SANS_SERIF);
            mapView.addAllStoreLabels(venue, textPaint);

            Category[] unsortedCategories = activeVenue.getCategories();
            ArrayList<Category> catArrList = new ArrayList<>(Arrays.asList(unsortedCategories));
            Collections.sort(catArrList, new Comparator<Category>() {
                @Override
                public int compare(Category category1, Category category2) {
                    return category1.getName().toLowerCase().compareTo(category2.getName().toLowerCase());
                }
            });
            final Category[] categories = catArrList.toArray(new Category[0]);

            CategoryListAdapter categoryListAdapter =
                    new CategoryListAdapter(self, R.layout.list_item_category, categories);
            categoryListView.setAdapter(categoryListAdapter);
            categoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Category selectCategory = categories[i];

                    Location[] unsortedLocations = selectCategory.getLocations();
                    final Location[] locations = sortLocations(unsortedLocations);

                    LocationListAdapter locationListAdapter =
                            new LocationListAdapter(self, R.layout.list_item_location, locations);
                    categoryTitleTextView.setText(selectCategory.getName());
                    categoryLocationListView.setAdapter(locationListAdapter);
                    categoryLocationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            Location selectLocation = locations[i];
                            ((ApplicationSingleton) getApplication()).setActiveLocation(selectLocation);
                            locationDetail();
                        }
                    });
                    categoryListView.setVisibility(View.INVISIBLE);
                    categoryLocationListView.setVisibility(View.VISIBLE);
                    categoryBackButton.setVisibility(View.VISIBLE);
                }
            });

            Location[] unsortedLocations = activeVenue.getLocations();
            final Location[] locations = sortLocations(unsortedLocations);
            LocationListAdapter locationListAdapter =
                    new LocationListAdapter(self, R.layout.list_item_location, locations);
            locationListView.setAdapter(locationListAdapter);
            locationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Location selectLocation = locations[i];
                    ((ApplicationSingleton) getApplication()).setActiveLocation(selectLocation);
                    locationDetail();
                }
            });
        }

        @Override
        public void onError(Exception e) {
            Logger.log("Error loading Venue: " + e);
        }
    }

    protected Location[] sortLocations (Location[] unsortedLocations) {
        ArrayList<Location> locArrList = new ArrayList<>(Arrays.asList(unsortedLocations));
        Collections.sort(locArrList, new Comparator<Location>() {
            @Override
            public int compare(Location location1, Location location2) {
                return location1.getName().toLowerCase().compareTo(location2.getName().toLowerCase());
            }
        });
        return locArrList.toArray(new Location[0]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        clearLocationDetails();
        clearHighlightedColours();
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                mapView.removeAllElements();
                int polygonIndex = data.getIntExtra("polygon_index", -1);
                if (polygonIndex != -1){
                    tabhost.setCurrentTab(0);
                    Polygon[] polygons = activeVenue.getPolygons();
                    destinationPolygon = polygons[polygonIndex];
                    mapView.frame(destinationPolygon.getMap(), 0, (float)Math.PI/5, 1);
                    highlightPolygon(destinationPolygon, Color.GREEN, Color.WHITE);
                    prepareNavigation();
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    @Override
    public boolean didTapPolygon(Polygon polygon) {
        if (navigationMode) {
            if (unwalkedMapPath != null || walkedMapPath != null) {
                didTapNothing();
            } else {
                if (polygon.getLocations().length == 0) {
                    return false;
                }
                highlightPolygon(polygon, 0x4ca1fc, Color.WHITE);
                startNavigation(polygon, destinationPolygon);
                runOnUiThread(new Runnable() {
                    public void run() {
                        selectOriginTextView.setVisibility(View.INVISIBLE);
                    }
                });
                return true;
            }
        }
        clearHighlightedColours();
        if (polygon.getLocations().length == 0) {
            return false;
        }
        destinationPolygon = polygon;
        highlightPolygon(polygon, 0x4ca1fc, Color.WHITE);

        showLocationDetails(destinationPolygon.getLocations()[0]);
        return true;
    }

    @Override
    public boolean didTapOverlay(Overlay overlay) {
        return false;
    }

    @Override
    public void didTapNothing() {
        clearHighlightedColours();
        clearLocationDetails();
        stopNavigation();
    }

    @Override
    public void onCameraBearingChange(final double bearing) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                compass.setRotation((float)(bearing/Math.PI*180));
            }
        });
    }

    @Override
    public void manipulatedCamera() {
        autoRotation = false;
    }

    void showLoadingLogo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading != null){
                    loading.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    void hideLoadingLogo(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading != null){
                    loading.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void highlightPolygon(Polygon polygon, int color, int labelColor) {
        if (!hightLightPolygon.contains(polygon)) {
            hightLightPolygon.add(polygon);
        }
        mapView.setColor(polygon, color, labelColor, 1);
        mapView.setHeight(polygon, 0.3f, 1);
    }

    private void clearHighlightedColours() {
        for  (Polygon polygon: hightLightPolygon) {
            mapView.resetPolygon(polygon);
        }
        hightLightPolygon.clear();
    }

    private void showLocationDetails(Location location) {
        clearLocationDetails();
        titleLabel.setText(location.getName());

        // This sample is using the Ion framework for easy image loading/cacheing. You can use what you like
        // https://github.com/koush/ion
        if (location.getClass() == Tenant.class) {
            Tenant tenant = (Tenant)location;
        } else if (location.getClass() == Amenity.class) {
            Amenity amenity = (Amenity)location;
        }
        titleLabel.setVisibility(View.VISIBLE);
        goButton.setVisibility(View.VISIBLE);
    }

    private void clearLocationDetails() {
        titleLabel.setVisibility(View.INVISIBLE);
        goButton.setVisibility(View.INVISIBLE);
    }

    Overlay2DLabel directionLabel = null;
    private Path unwalkedMapPath;
    private Path walkedMapPath;
    private List<Cylinder> turnPoints = new ArrayList<>();
    List<DirectionInstruction> directionInstructions = new ArrayList<>();
    Timer timer = new Timer();
    private void prepareNavigation() {
        stopNavigation();
        navigationMode = true;
        selectOriginTextView.setVisibility(View.VISIBLE);
        iAmHere = new IAmHere();
    }

    private void startNavigation(Polygon from, Polygon to) {
        selectOriginTextView.setVisibility(View.INVISIBLE);
        instructionImageView.setVisibility(View.VISIBLE);
        instructionTextView.setVisibility(View.VISIBLE);
        walkingButton.setVisibility(View.VISIBLE);
        Directions directions =
                from.directionsTo(activeVenue, to, from.getLocations()[0], to.getLocations()[0], accessibleDirections);

        levelChangeDownButton.setEnabled(false);
        levelChangeUpButton.setEnabled(false);

        if (directions != null) {
            final Analytics.Wayfind wayfind = Analytics.getInstance().startedWayfind(to.getLocations()[0]);
            final Coordinate[] pathCoor = directions.getPath();
            unwalkedMapPath = new Path(pathCoor, 1f, 3f, Color.BLUE, 1);
            mapView.addElement(unwalkedMapPath);
            mapView.frame(
                    new Focusable[]{unwalkedMapPath, to, from}, 0, (float) Math.PI / 5, 1);

            directionInstructions = new ArrayList<>();
            final List<Instruction> instructions = directions.getInstructions();
            Iterator<Instruction> ii = instructions.iterator();
            Instruction instruction = ii.next();
            List<Coordinate> coords = new ArrayList<>();
            for (Coordinate coord : pathCoor) {
                coords.add(coord);
                if (coord.equals(instruction.coordinate)) {
                    DirectionInstruction directionInstruction =
                            new DirectionInstruction(instruction, coords);
                    directionInstructions.add(directionInstruction);
                    Cylinder turnPoint = new Cylinder(1f, 3.1f, Color.WHITE);
                    turnPoints.add(turnPoint);
                    Coordinate coor = instruction.coordinate;
                    turnPoint.setPosition(coor, 0);
                    mapView.addElement(turnPoint);
                    if (ii.hasNext()) {
                        instruction = ii.next();
                        coords = new ArrayList<>();
                        coords.add(coord);
                    }
                }
            }

            final int[] markIndex = {0};
            iAmHere.setPosition(pathCoor[0].getMap(), pathCoor[0], 0);
            iAmHere.addIAmHere(mapView);
            instructionTextView.setVisibility(View.VISIBLE);
            instructionTextView.setText(directions.getInstructions().get(0).instruction);
            instructionImageView.setVisibility(View.VISIBLE);
            Drawable drawable = Utils.setDirectionImage(self, directions.getInstructions().get(0));
            if (drawable != null) {
                instructionImageView.setImageDrawable(drawable);
            }
            directionLabel =
                    new Overlay2DLabel("Start Here", 24, Typeface.DEFAULT_BOLD);
            Coordinate position = new Coordinate(
                    new Vector3(pathCoor[0].getX(), pathCoor[0].getY(), pathCoor[0].getZ()+3), pathCoor[0].getMap());
            directionLabel.setPosition(position);
            directionLabel.setBackgroundColor(Color.WHITE);
            directionLabel.setTextColor(Color.BLACK);
            mapView.addElement(directionLabel);
            walkingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!walking) {
                        walking = true;
                        walkingButton.setText("Stop");
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                markIndex[0]++;
                                if (markIndex[0] >= pathCoor.length) {
                                    if (wayfind != null){
                                        wayfind.successful();
                                    }
                                    timer.cancel();
                                } else {
                                    Coordinate coordinate = pathCoor[markIndex[0]];
                                    updateIAmHereLocation(coordinate, pathCoor);
                                }
                            }
                        }, 0, 800);
                    } else {
                        timer.cancel();
                        walking = false;
                        walkingButton.setText("Start");
                    }
                }
            });
        }
    }

    private void updateIAmHereLocation(Coordinate coordinate, Coordinate[] pathCoor) {
        iAmHere.setPosition(coordinate.getMap(), coordinate, 0.8f);
        updateWalkedPercentage(coordinate, pathCoor);

        Map coordinateMap = coordinate.getMap();
        mapView.setMap(coordinateMap);
        if (coordinateMap != maps[currentLevelIndex]) {
            for (int i = 0; i < maps.length; i++) {
                if (coordinateMap == maps[i]) {
                    currentLevelIndex = i;
                    break;
                }
            }

            levelNavTextView.setText(maps[currentLevelIndex].getName());
        }
        final Instruction currInstruction = Utils.getNextInstruction(directionInstructions, coordinate);
        final Drawable drawable = Utils.setDirectionImage(self, currInstruction);
        if (drawable != null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    instructionImageView.setImageDrawable(drawable);
                }
            });
        }
        float angle = -mapView.getCameraHeading();
        if (currInstruction.coordinate != coordinate){
            float x1 = currInstruction.coordinate.getX();
            float y1 = currInstruction.coordinate.getY();
            float x2 = coordinate.getX();
            float y2 = coordinate.getY();
            if (x1 == x2){
                angle = y1 > y2 ? 0 : (float)Math.PI;
            }
            else if (y1 == y2){
                angle = x1 > x2 ? (float)Math.PI/2 : -(float)Math.PI/2;
            } else {
                angle = (float)Math.atan((x1 - x2) / (y1 - y2));
                if (y1 < y2 ){
                    angle = (float)Math.PI + angle;
                }
            }
            iAmHere.setRotation(angle, 0);
        }
        try {
            float distance = currInstruction.coordinate.metersFrom(coordinate);
            directionLabel.setText(String.format("%.2fm", distance));
            Coordinate position = new Coordinate(
                    new Vector3(currInstruction.coordinate.getX(),
                            currInstruction.coordinate.getY(),
                            currInstruction.coordinate.getZ()+3),
                    currInstruction.coordinate.getMap());
            directionLabel.setPosition(position);
            final String display =
                    String.format("%s in %.2fm", currInstruction.instruction, distance);

            runOnUiThread(new Runnable() {
                public void run() {
                    instructionTextView.setText(display);
                }
            });
        } catch (MappedInException e) {
            runOnUiThread(new Runnable() {
                public void run() {
                    instructionTextView.setText(currInstruction.instruction);
                }
            });
        }
        mapView.frame(
                new Focusable[]{iAmHere.arrow, iAmHere.cylinder, currInstruction.coordinate},
                angle, (float) Math.PI / 5, 0.8f);
    }

    private void stopNavigation() {
        if (unwalkedMapPath != null) {
            mapView.removeElement(unwalkedMapPath);
            unwalkedMapPath = null;
        }
        if (walkedMapPath != null) {
            mapView.removeElement(walkedMapPath);
            walkedMapPath = null;
        }
        if (directionLabel != null){
            mapView.removeElement(directionLabel);
        }
        if (!turnPoints.isEmpty()){
            for (int i = 0; i < turnPoints.size(); i++){
                mapView.removeElement(turnPoints.get(i));
            }
            turnPoints.clear();
        }
        if (iAmHere != null) {
            iAmHere.removeIAmHere(mapView);
        }
        navigationMode = false;
        timer.cancel();
        walkingButton.setVisibility(View.INVISIBLE);
        instructionImageView.setVisibility(View.INVISIBLE);
        instructionTextView.setVisibility(View.INVISIBLE);

        if (currentLevelIndex < maps.length-1) {
            levelChangeUpButton.setEnabled(true);
        }
        if (currentLevelIndex > 0) {
            levelChangeDownButton.setEnabled(true);
        }
    }

    private void locationDetail() {
        Intent showLocationDetail = new Intent(this, LocationActivity.class);
        startActivityForResult(showLocationDetail, PICK_CONTACT_REQUEST);
    }

    Coordinate oldClosestCoor = null;
    public void updateWalkedPercentage(Coordinate position, Coordinate[] path) {
        int closestCoordinateIndex = 0;
        int coordinateIndex = 0;
        float minDistance = Float.MAX_VALUE;
        for (Coordinate coordinate : path){
            if (coordinate.getMap() == position.getMap()){
                float distance = (coordinate.getVector().lengthSquared(position.getVector()));
                if (distance < minDistance) {
                    closestCoordinateIndex = coordinateIndex;
                    minDistance = distance;
                }
            }
            coordinateIndex++;
        }
        if (oldClosestCoor != path[closestCoordinateIndex]) {
            oldClosestCoor = path[closestCoordinateIndex];
            List<Coordinate> unwalkedPath = new ArrayList<>();
            List<Coordinate> walkedPath = new ArrayList<>();
            for (int i = 0; i < path.length; i++) {
                if (i < closestCoordinateIndex) {
                    walkedPath.add(path[i]);
                } else if (i == closestCoordinateIndex) {
                    walkedPath.add(path[i]);
                    unwalkedPath.add(path[i]);
                } else {
                    unwalkedPath.add(path[i]);
                }
            }
            Path newWalkedMapPath = new Path(walkedPath.toArray(new Coordinate[walkedPath.size()]), 1f, 3f, Color.LTGRAY, 0);
            Path newUnwalkedMapPath = new Path(unwalkedPath.toArray(new Coordinate[unwalkedPath.size()]), 1f, 3f, Color.BLUE, 0);

            if (newWalkedMapPath != null) {
                mapView.addElement(newWalkedMapPath);
                if (walkedMapPath != null) {
                    mapView.removeElement(walkedMapPath);
                }
                walkedMapPath = newWalkedMapPath;
            }
            if (newUnwalkedMapPath != null) {
                mapView.addElement(newUnwalkedMapPath);
                if (unwalkedMapPath != null) {
                    mapView.removeElement(unwalkedMapPath);
                }
                unwalkedMapPath = newUnwalkedMapPath;
            }
            Logger.log("update path end");
        }
    }



    class SetMapCallback implements MappedinCallback<Map> {

        /**
         * Function that will be called when the Mappedin API call has finished successfully
         *
         * @param map Data returned for the Mappedin API call
         */
        @Override
        public void onCompleted(Map map) {
            hideLoadingLogo();
        }

        /**
         * Function that will be called if the Mappedin API call failed
         *
         * @param exception The error that occurred
         */
        @Override
        public void onError(Exception exception) {

        }
    }
}
