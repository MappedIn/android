package mappedin.com.wayfindingsample;

import android.Manifest;
import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.mappedin.jpct.Logger;
import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Cylinder;
import com.mappedin.sdk.Directions;
import com.mappedin.sdk.Element;
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
import com.mappedin.sdk.Navigatable;
import com.mappedin.sdk.Overlay;
import com.mappedin.sdk.Overlay2DImage;
import com.mappedin.sdk.Path;
import com.mappedin.sdk.Polygon;
import com.mappedin.sdk.SearchDelegate;
import com.mappedin.sdk.SearchResult;
import com.mappedin.sdk.SmartSearch;
import com.mappedin.sdk.Vector2;
import com.mappedin.sdk.Venue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MapViewDelegate, SensorEventListener {
    Context context;
    Activity self;

    Typeface robotoRegular;
    Typeface robotoItalic;

    enum State {
        WELCOME,
        START,
        LOCATION_DETAILS,
        ROUTE_PREVIEW,
        NAVIGATION
    }

    enum CameraMode {
        FREE_CAMERA,
        FOCUSED_ON_USER,
        FOCUSED_ON_PATH
    }

    State activityState;

    // Venue data
    MappedIn mappedIn;
    SmartSearch smartSearch;
    Map[] maps;
    ArrayList<Polygon> highlightedPolygons = new ArrayList<>();

    // welcome layout
    private LinearLayout welcomeLayout;
    private TextView welcomeTextView;
    private TextView loadingTextView;

    // search layout
    private RelativeLayout searchBarLayout;
    private LinearLayout searchLayout;
    private LinearLayout searchResultLayout;
    private View searchPageView;
    private EditText searchEditText;
    private ListView searchResultListView;
    private SearchEditListener searchEditListener;

    // level picker layout
    private LinearLayout levelPickerLayout;
    private ListView levelPickerListView;
    private SparseIntArray levelFloorMap = new SparseIntArray(0);
    private MapListAdapter mapListAdapter = null;

    private FloatingActionButton recenterBtn;

    // location detail
    private RelativeLayout locationLayout;
    private TextView locationNameTextView;

    // Route Details
    private LinearLayout routeDetailsLayout;
    private TextView toLocationTextView;
    private TextView timeToDestinationTextView;
    private TextView startNavigationBtn;

    // Accessibility
    private boolean accessible = false;
    private Switch accessibleSwitch;
    private View accessibleSwitchShadow;
    private AlertDialog accessibilityDialog;

    // navigation
    private LinearLayout directionBtnLayout;
    private Navigatable from;
    private Polygon to;
    private Polygon tappedPolygon;
    private NavigationPath currentNavigationPath;
    private int currentInstructionIndex;
    private RelativeLayout instructionBarLayout;
    private TextView distanceToInstructionTextView;
    private TextView instructionTextView;
    private ImageView directionIcon;
    private ImageButton previousStepBtn;
    private ImageButton nextStepBtn;
    private Float metersAMinute = 1.38889f * 60;
    private AlertDialog switchLocationDialog;
    private AlertDialog arriveAtDestinationDialog;

    // Location
    private LocationManager mLocationManager;
    private android.location.Location currentGPSLocation;
    private SensorManager mSensorManager;
    private float currentDegree = 0;
    private CameraMode cameraMode = CameraMode.FOCUSED_ON_USER;

    private IAmHere iAmHere;
    private Coordinate iAmHereCoord;
    private Integer iAmHereFloorIndex;
    boolean iAmAtVenue = false;
    private AlertDialog notAtVenueDialog;
    private AlertDialog directionsFailureDialog;

    // Map View
    private MapView mapView;
    private float framePadding = 4;

    // Map View Status
    Map currentMap;
    Integer currentMapPosition;
    SearchResultAdapter searchResultAdapter;
    Location activatedLocation;

    // Venue selector drawer
    private VenueListAdapter venueListAdapter;
    private ListView venueList;
    private Venue activeVenue = null;
    private ProgressBar venueProgressBar;
    private Comparator<Map> mapComparator;

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
        if (mapView != null) {
            mapView.onPause();
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mappedIn = new MappedIn(getApplication());
        setContentView(R.layout.activity_main);

        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final DrawerLayout drawer =  findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        context = this;
        self = this;

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Map View
        mapView = (MapView) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapView.setDelegate(this);
        mapComparator = new Comparator<Map>() {
            @Override
            public int compare(Map a, Map b) {
                return (a.getFloor() - b.getFloor());
            }
        };

        venueList = findViewById(R.id.venue_list_view);

        // venue loading progress bar
        venueProgressBar = findViewById(R.id.venue_progressBar);
        venueProgressBar.bringToFront();
        venueProgressBar.setVisibility(View.INVISIBLE);

        final MappedinCallback<Venue> getVenueCallback = new MappedinCallback<Venue>() {
            @Override
            public void onCompleted(Venue venue) {
                if (venue != null) {
                    addStoreLabel(venue);
                    enableSearch(venue);

                    maps = venue.getMaps();
                    Arrays.sort(maps, mapComparator);

                    iAmHereFloorIndex = 0;
                    mapView.setMap(maps[iAmHereFloorIndex]);
                    setMap(maps.length - 1);

                    if (maps.length == 1) {
                        levelPickerListView.setVisibility(View.INVISIBLE);
                        levelPickerLayout.setVisibility(View.INVISIBLE);
                    } else {
                        levelPickerListView.setVisibility(View.VISIBLE);
                        levelPickerLayout.setVisibility(View.VISIBLE);

                        for (int i = 0; i < maps.length; i++) {
                            levelFloorMap.put(maps[i].getFloor(), i);
                        }
                    }

                    mapListAdapter =
                            new MapListAdapter(MainActivity.this, context, maps);
                    levelPickerListView.setAdapter(mapListAdapter);

                    if (activeVenue != null) {
                        mapView.frame(currentMap, currentMap.getHeading(), (float) Math.PI / 4, 1f);
                    }
                }
            }

            @Override
            public void onError(Exception e) {

            }
        };
        final MappedinCallback<List<Venue>> getVenuesCallback = new VenuesCallback(mappedIn, getVenueCallback);

        venueListAdapter = new VenueListAdapter(context, null);
        mappedIn.getVenues(new MappedinCallback<List<Venue>>() {
            @Override
            public void onCompleted(final List<Venue> venues) {
                venueListAdapter.setVenues(venues);
                venueList.setAdapter(venueListAdapter);
                venueList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                        venueProgressBar.setVisibility(View.VISIBLE);
                        mapView.getView().setVisibility(View.INVISIBLE);
                        levelPickerLayout.setVisibility(View.INVISIBLE);
                        activeVenue = venues.get(position);
                        ((VenuesCallback) getVenuesCallback).setActiveVenue(activeVenue);
                        mappedIn.getVenues(getVenuesCallback);
                        drawer.closeDrawer(GravityCompat.START);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                Logger.log("get venues for mappedin failed");
            }
        });

        // Creating roboto typeface
        robotoRegular = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        robotoItalic = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Italic.ttf");

        // Loading page
        welcomeLayout = findViewById(R.id.welcome_layout);
        welcomeTextView = findViewById(R.id.welcome_text_view);
        welcomeTextView.setTypeface(robotoRegular);
        loadingTextView = findViewById(R.id.loading_text_view);
        loadingTextView.setTypeface(robotoItalic);

        // Search Bar
        searchBarLayout = findViewById(R.id.search_bar_layout);
        searchLayout = findViewById(R.id.search_layout);
        searchEditText = findViewById(R.id.search_edit_text);
        searchPageView = findViewById(R.id.search_page_bg_view);

        searchEditText.setTypeface(robotoRegular);
        searchEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                searchPageView.setVisibility(View.VISIBLE);
                return false;
            }
        });

        searchPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                searchPageView.setVisibility(View.GONE);
                Utils.hideSoftKeyboard(self);
                searchEditText.clearFocus();
                searchResultAdapter.updateResults(new SearchResult[0]);
                searchResultLayout.setVisibility(View.GONE);
                return true;
            }
        });

        searchResultLayout = findViewById(R.id.search_result_layout);
        searchResultLayout.setVisibility(View.GONE);
        searchResultListView = findViewById(R.id.search_result_list_view);
        searchResultAdapter
                = new SearchResultAdapter(
                context,
                R.layout.list_item_search,
                new ArrayList<SearchResult>(),
                robotoRegular);
        searchResultListView.setAdapter(searchResultAdapter);
        searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchPageView.setVisibility(View.GONE);
                Utils.hideSoftKeyboard(self);
                if (searchEditListener != null) {
                    searchEditListener.selected = true;
                }
                ArrayList<SearchResult> searchResults = searchResultAdapter.getResults();
                activatedLocation = searchResults.get(position).getResultObject(Location.class);
                if (activatedLocation != null) {
                    String locationName = activatedLocation.getName();
                    searchEditText.setText(locationName);
                    searchEditText.setSelection(locationName.length());
                    searchEditText.clearFocus();
                    searchResultAdapter.updateResults(new SearchResult[0]);
                    searchResultLayout.setVisibility(View.GONE);
                    if (activatedLocation.getPolygons().length > 0) {
                        resetAllPolygons();
                        // Pick closest polygon to the user's location
                        Polygon closestPolygon = activatedLocation.getPolygons()[0];
                        float distanceToPolygon = Float.MAX_VALUE;
                        if (iAmAtVenue) {
                            for (Polygon polygon : activatedLocation.getPolygons()) {
                                Directions directions = iAmHereCoord.directionsTo(maps[0].getVenue(), polygon, null, polygon.getLocations()[0], accessible);
                                if (directions != null) {
                                    float distance = directions.getDistance();
                                    if (distance < distanceToPolygon) {
                                        distanceToPolygon = distance;
                                        closestPolygon = polygon;
                                    }
                                }
                            }
                        }
                        setMap(levelFloorMap.get(closestPolygon.getMap().getFloor()));
                        focusOnLocation(closestPolygon);
                        highlightPolygon(closestPolygon);
                        to = closestPolygon;
                    }
                }
            }
        });

        // Level Picker
        levelPickerLayout = findViewById(R.id.level_picker_layout);
        levelPickerListView = findViewById(R.id.level_picker_list_view);
        levelPickerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mapListAdapter.setSelectedIndex(position);
                if (position != currentMapPosition) {
                    setMap(position);
                    iAmHereFloorIndex = position;
                    mapView.frame(currentMap, currentMap.getHeading(), (float) Math.PI / 4, 0.5f);
                }
            }
        });

        recenterBtn = findViewById(R.id.recenter_btn);

        // Location detail
        locationLayout = findViewById(R.id.location_layout);
        locationNameTextView = findViewById(R.id.location_name_text_view);
        locationNameTextView.setTypeface(robotoRegular);

        // Accessibility
        accessibleSwitch = findViewById(R.id.accessible_switch);
        accessibleSwitchShadow = findViewById(R.id.accessible_switch_shadow);
        accessible = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("ACCESSIBLE_ROUTE", false);
        accessibleSwitch.setChecked(accessible);
        accessibleSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (accessible != b) {
                            setAccessible(b);
                            if (activityState == State.ROUTE_PREVIEW || activityState == State.NAVIGATION) {
                                currentNavigationPath = createPath(1f);
                                if (currentNavigationPath != null) {
                                    drawMapElements(currentNavigationPath.pathElements);
                                    currentInstructionIndex = 0;
                                    showInstruction(currentNavigationPath.directions.getInstructions().get(currentInstructionIndex), iAmHereCoord);
                                    recenterCameraFocus(new View(context));
                                }
                            }
                        }
                    }
                }
        );
        accessibilityDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme).create();
        accessibilityDialog.setTitle(getResources().getString(R.string.accessibility_dialog_title));
        accessibilityDialog.setMessage(getResources().getString(R.string.accessibility_dialog_message));
        accessibilityDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.accessibility_dialog_disable),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setAccessible(false);
                        dialog.dismiss();
                        showRouteDetails(new View(context));
                    }
                });
        accessibilityDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.accessibility_dialog_enable),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setAccessible(true);
                        dialog.dismiss();
                        showRouteDetails(new View(context));
                    }
                });

        // Directions
        directionBtnLayout = findViewById(R.id.directions_button);
        TextView directionBtnTextView = findViewById(R.id.directions_button_text_view);
        directionBtnTextView.setTypeface(robotoRegular);
        directionBtnLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                boolean gotAccessiblePreference = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                        .getBoolean("GOT_ACCESSIBLE_PREFERENCE", false);
                if (gotAccessiblePreference) {
                    showRouteDetails(v);
                } else {
                    accessibilityDialog.show();
                    getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .edit()
                            .putBoolean("GOT_ACCESSIBLE_PREFERENCE", true)
                            .apply();
                }
            }
        });

        // Route Details
        routeDetailsLayout = findViewById(R.id.route_details_layout);
        toLocationTextView = findViewById(R.id.to_location_text_view);
        toLocationTextView.setTypeface(robotoRegular);
        timeToDestinationTextView = findViewById(R.id.time_to_destination_text_view);
        timeToDestinationTextView.setTypeface(robotoRegular);
        startNavigationBtn = findViewById(R.id.start_navigation_btn);
        startNavigationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigation(v);
            }
        });

        // Navigation
        instructionBarLayout = findViewById(R.id.navigation_instructions_layout);
        distanceToInstructionTextView = findViewById(R.id.distance_to_instruction);
        distanceToInstructionTextView.setTypeface(robotoRegular);
        instructionTextView = findViewById(R.id.instruction_text);
        instructionTextView.setTypeface(robotoRegular);
        directionIcon = findViewById(R.id.direction_icon);
        previousStepBtn = findViewById(R.id.previous_step_btn);
        nextStepBtn = findViewById(R.id.next_step_btn);

        notAtVenueDialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme).create();
        notAtVenueDialog.setTitle(getResources().getString(R.string.directions_failure_dialog_title));
        notAtVenueDialog.setMessage(getResources().getString(R.string.not_at_venue_dialog_message));
        notAtVenueDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.ok_btn),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        directionsFailureDialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme).create();
        directionsFailureDialog.setTitle(getResources().getString(R.string.directions_failure_dialog_title));
        directionsFailureDialog.setMessage(getResources().getString(R.string.directions_failure_dialog_message));
        directionsFailureDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.ok_btn),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        switchLocationDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme).create();
        switchLocationDialog.setTitle(getResources().getString(R.string.switch_location_dialog_title));
        switchLocationDialog.setMessage(getResources().getString(R.string.switch_location_dialog_message));
        switchLocationDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.dismiss_dialog),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        tappedPolygon = null;
                        dialog.dismiss();
                    }
                });
        switchLocationDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.confirm_dialog),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (tappedPolygon != null) {
                            closeRouteDetails(new View(context));

                            if (!highlightedPolygons.contains(tappedPolygon)) {
                                resetAllPolygons();
                                highlightPolygon(tappedPolygon);
                            }
                            to = tappedPolygon;
                            focusOnLocation(tappedPolygon);
                            dialog.dismiss();
                        }
                    }
                });

        arriveAtDestinationDialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme).create();
        arriveAtDestinationDialog.setMessage(getResources().getString(R.string.arrive_at_destination_dialog_message));
        arriveAtDestinationDialog.setCanceledOnTouchOutside(false);
        arriveAtDestinationDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.ok_btn),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        closeRouteDetails(new View(context));
                        dialog.dismiss();
                    }
                });

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        showWelcomePage();

        mappedIn.getVenues(getVenuesCallback);
    }

    private void setAccessible(boolean state){
        accessible = state;
        accessibleSwitch.setChecked(state);
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean("ACCESSIBLE_ROUTE", state)
                .apply();
    }

    private void hideWelcomePage(){
        welcomeLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
        levelPickerLayout.setVisibility(View.VISIBLE);
    }

    private void showWelcomePage(){
        activityState = State.WELCOME;
        welcomeLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
        levelPickerLayout.setVisibility(View.GONE);
    }

    public void showSearchBar(View v){
        searchBarLayout.setVisibility(View.VISIBLE);
    }

    private void hideSearchBar(View v){
        searchBarLayout.setVisibility(View.GONE);
    }

    public void clearSearch(View v) {
        searchEditText.getText().clear();
    }

    private void prepareLocationPage(Location location){
        clearLocationPage();
        if (location != null) {
            locationNameTextView.setText(location.getName());
        }
    }

    public void showLocationPage(View v){
        activityState = State.LOCATION_DETAILS;
        locationLayout.setVisibility(View.VISIBLE);
    }

    private void hideLocationPage(){
        locationLayout.setVisibility(View.GONE);
        clearLocationPage();
    }

    public void closeLocationPage(View v){
        hideLocationPage();
        searchEditText.getText().clear();
        activityState = State.START;
        resetAllPolygons();
        to = null;
    }

    private void clearLocationPage(){
        locationNameTextView.setText("");
    }
    private void setMap(final int newMapPosition){
        if (currentMapPosition == null || newMapPosition != currentMapPosition ||
                maps[currentMapPosition] != currentMap) {
            MappedinCallback<Map> setMapCallback = new MappedinCallback<Map>() {
                @Override
                public void onCompleted(Map map) {
                    currentMapPosition = newMapPosition;
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (activityState.equals(State.WELCOME)) {
                                        hideWelcomePage();
                                        activityState = State.START;
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                    }
                                    mapListAdapter.setSelectedIndex(currentMapPosition);
                                    venueProgressBar.setVisibility(View.INVISIBLE);
                                    mapView.getView().setVisibility(View.VISIBLE);
                                    if (maps.length > 1) {
                                        levelPickerLayout.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                }

                @Override
                public void onError(Exception exception) {

                }
            };
            if (newMapPosition < maps.length) {
                mapView.setMap(maps[newMapPosition], setMapCallback);
                currentMap = maps[newMapPosition];
                freeCameraFocus();
            }
        }
    }

    private void prepareRouteDetails(Location location) {
        clearLocationPage();
        if (location != null && currentNavigationPath !=  null) {
            int time = Math.round(currentNavigationPath.directions.getDistance() / metersAMinute);
            String timeString = String.format(getResources().getString(R.string.minutes_to_destination), time);
            timeToDestinationTextView.setText(timeString);

            Spannable toString = new SpannableString("To ");
            toString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, toString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toLocationTextView.setText(toString);

            Spannable toLocationName = new SpannableString(location.getName());
            toLocationName.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.azure)), 0, toLocationName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toLocationTextView.append(toLocationName);

            Spannable fromString = new SpannableString(" from ");
            fromString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, fromString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toLocationTextView.append(fromString);

            Spannable fromLocationName = new SpannableString(getResources().getString(R.string.your_location_name));
            fromLocationName.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.azure)), 0, fromLocationName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toLocationTextView.append(fromLocationName);
        }
    }

    public void showRouteDetails(View v){
        if (to != null) {
            currentNavigationPath = createPath(2f);
            if (iAmHere != null && currentNavigationPath != null) {
                freeCameraFocus();
                setMap(iAmHereFloorIndex);
                Focusable[] focusablePath = new Focusable[] {currentNavigationPath.routePath, to, iAmHereCoord};
                //TODO smooth out frame
                mapView.orbit(focusablePath, mapView.getCameraHeading(), 0.1f, 1f, framePadding);
                drawMapElements(currentNavigationPath.pathElements);
                hideLocationPage();
                activityState = State.ROUTE_PREVIEW;
                prepareRouteDetails(to.getLocations()[0]);
                routeDetailsLayout.setVisibility(View.VISIBLE);
                startNavigationBtn.setVisibility(View.VISIBLE);
            } else {
                if (!iAmAtVenue) {
                    notAtVenueDialog.show();
                } else {
                    directionsFailureDialog.show();
                }
            }
        }
    }

    private void hideRouteDetails(){
        routeDetailsLayout.setVisibility(View.GONE);
    }

    public void closeRouteDetails(View v){
        if (currentNavigationPath != null) {
            clearMapElements(currentNavigationPath.pathElements);
            currentNavigationPath = null;
        }
        if (activityState.equals(State.NAVIGATION)) {
            closeNavigation(v);
        } else {
            activityState = State.START;
            hideRouteDetails();
            resetAllPolygons();
            to = null;
        }
    }

    public void showInstructionBar(View v){
        instructionBarLayout.setVisibility(View.VISIBLE);
    }

    private void hideInstructionBar(){
        instructionBarLayout.setVisibility(View.GONE);
        accessibleSwitch.setVisibility(View.GONE);
        accessibleSwitchShadow.setVisibility(View.GONE);
    }

    public void startNavigation(View v){
        activityState = State.NAVIGATION;
        showInstructionBar(v);
        hideSearchBar(v);
        startNavigationBtn.setVisibility(View.GONE);
        setupNavigation();
        recenterCameraFocus(new View(context));
    }

    public void closeNavigation(View v){
        activityState = State.LOCATION_DETAILS;
        currentInstructionIndex = 0;

        hideRouteDetails();
        hideInstructionBar();
        showSearchBar(v);
        if (to != null) {
            prepareLocationPage(to.getLocations()[0]);
            showLocationPage(v);
        }
    }

    public void showNavigationSettings(View v) {
        if (accessibleSwitch.getVisibility() == View.GONE) {
            accessibleSwitch.setVisibility(View.VISIBLE);
            accessibleSwitchShadow.setVisibility(View.VISIBLE);
        } else {
            accessibleSwitch.setVisibility(View.GONE);
            accessibleSwitchShadow.setVisibility(View.GONE);
        }
    }

    private void focusOnLocation(Polygon polygon){
        activatedLocation = polygon.getLocations()[0];
        prepareLocationPage(activatedLocation);
        showLocationPage(new View(context));
        freeCameraFocus();
        mapView.frame(polygon, mapView.getCameraHeading(), 0.5f, 0.5f, framePadding);
    }

    private void highlightPolygon(Polygon polygon){
        mapView.setColor(polygon, getResources().getColor(R.color.azure),0.5f);
        highlightedPolygons.add(polygon);
    }

    private void resetAllPolygons(){
        for (Polygon polygon : highlightedPolygons){
            mapView.resetPolygon(polygon);
        }
        highlightedPolygons.clear();
    }

    private void addStoreLabel(Venue venue){
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50);
        textPaint.setTypeface(robotoRegular);
        mapView.addAllStoreLabels(venue, textPaint);
    }

    private void enableSearch(Venue venue){
        smartSearch = mappedIn.initiateSearch(venue);
        searchEditListener = new SearchEditListener(smartSearch);
        searchEditText.addTextChangedListener(searchEditListener);
        searchEditText.setOnEditorActionListener(searchEditListener);
        smartSearch.setDelegate(new SearchDelegate() {
            /**
             * Called when smart search returns suggestion results.
             *
             * @param query    can be used to identify where is the callback comes from
             * @param suggests String Array contains all suggestions
             */
            @Override
            public void suggestion(String query, String[] suggests) {

            }

            /**
             * Called when smart search returns search results.
             *
             * @param query    can be used to identify where is the callback comes from
             * @param total
             * @param pageSize
             * @param pageNum
             * @param results  Location array contains all search result in order
             */
            @Override
            public void search(String query, int total, int pageSize, int pageNum, SearchResult[] results) {
                SearchResultAdapter adapter
                        = (SearchResultAdapter) searchResultListView.getAdapter();
                adapter.updateResults(results);
                if (results.length == 0){
                    searchResultLayout.setVisibility(View.GONE);
                } else {
                    searchResultLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Called when a user taps a specific polygon. You probably want to highlight the polygon,
     * and unhighlight the others most of the time
     *
     * @param polygon the Polygon user tapped on
     * @return "true" to stop going down to next polygon, "false" for keep going to next polygon
     */
    @Override
    public boolean didTapPolygon(Polygon polygon) {
        if (polygon.getLocations().length > 0) {
            if (!(activityState.equals(State.WELCOME))) {
                if (!(activityState.equals(State.NAVIGATION))) {
                    if (activityState.equals(State.ROUTE_PREVIEW)) {
                        closeRouteDetails(new View(context));
                    }
                    if (!highlightedPolygons.contains(polygon)) {
                        resetAllPolygons();
                        highlightPolygon(polygon);
                    }
                    searchEditText.getText().clear();
                    to = polygon;
                    focusOnLocation(polygon);
                    return true;
                } else {
                    tappedPolygon = polygon;
                    switchLocationDialog.show();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when a user taps a specific overlay.
     *
     * @param overlay the overlay user tapped on
     * @return "true" to stop going down to next overlay or polygon, "false" for keep going to next overlay or polygon
     */
    @Override
    public boolean didTapOverlay(Overlay overlay) {
        if (currentNavigationPath != null && (overlay instanceof Overlay2DImage)) {
            Overlay2DImage vortexLabel = (Overlay2DImage) overlay;
            // Currently overlays appear on navigation path at vortexes such as stairs or elevators
            // Tapping will take you to the floor they are associated with on the path
            if (currentNavigationPath.vortexes.containsKey(vortexLabel)) {
                setMap(levelFloorMap.get(currentNavigationPath.vortexes.get(vortexLabel)));
                mapView.frame(currentMap, currentMap.getHeading(), (float) Math.PI / 4, 0.5f);
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a user taps nothing. If the user taps overtop of one or more
     * overlays and polygons and you return false every time, this event will
     * be fired at the end indicating there is nothing further below their finger.
     */
    @Override
    public void didTapNothing() {

    }

    /**
     * Called when the user rotates the camera
     *
     * @param bearing The angle measured in radian in a clockwise direction from the north line
     */
    @Override
    public void onCameraBearingChange(double bearing) {

    }

    /**
     * Called when user touches the screen to the move map
     */
    @Override
    public void manipulatedCamera() {
        freeCameraFocus();
    }

    @Override
    public void onBackPressed() {
        switch (activityState) {
            case WELCOME:
                break;
            case START:
                break;
            case LOCATION_DETAILS:
                closeLocationPage(new View(context));
                return;
            case ROUTE_PREVIEW:
                closeRouteDetails(new View(context));
                return;
            case NAVIGATION:
                closeRouteDetails(new View(context));
                return;
            default:
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    long LOCATION_REFRESH_TIME = 0;
                    float LOCATION_REFRESH_DISTANCE = 0;

                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            LOCATION_REFRESH_TIME,
                            LOCATION_REFRESH_DISTANCE,
                            mLocationListener);
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            LOCATION_REFRESH_TIME,
                            LOCATION_REFRESH_DISTANCE,
                            mLocationListener);
                    iAmHere = new IAmHere();
                    currentGPSLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (currentGPSLocation != null) {
                        iAmHereCoord = new Coordinate(currentGPSLocation, maps[iAmHereFloorIndex]);
                        iAmAtVenue = maps[iAmHereFloorIndex].insideMapBoundary(iAmHereCoord);
                        if (iAmAtVenue) {
                            iAmHere.setPosition(iAmHereCoord, 0);
                            from = iAmHereCoord;
                            iAmHere.addIAmHere(mapView);
                            recenterBtn.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            if (iAmHere != null) {
                // Updates the current position of the user marker only when change is over a meter
                if (currentGPSLocation == null || currentGPSLocation.distanceTo(location) > 1) {
                    currentGPSLocation = location;
                    if (currentMap != null) {
                        iAmHereCoord = new Coordinate(location, maps[iAmHereFloorIndex]);

                        // Hides marker if too far from venue
                        boolean newPositionInVenue = maps[iAmHereFloorIndex].insideMapBoundary(iAmHereCoord);
                        if (newPositionInVenue != iAmAtVenue) {
                            iAmAtVenue = newPositionInVenue;
                            if (newPositionInVenue) {
                                iAmHere.addIAmHere(mapView);
                            } else {
                                iAmHere.removeIAmHere(mapView);
                            }
                        }
                        if (iAmAtVenue) {
                            iAmHere.setPosition(iAmHereCoord, 1f);
                            from = iAmHereCoord;
                            recenterBtn.setVisibility(View.VISIBLE);
                        } else {
                            recenterBtn.setVisibility(View.GONE);
                            from = null;
                        }

                        // Update path during Navigation
                        if ((activityState == State.NAVIGATION || activityState == State.ROUTE_PREVIEW)
                                && currentNavigationPath != null) {
                            //////////////////////////////
                            // For faking floor changes //
                            //////////////////////////////
                            List<Instruction> instructions =  currentNavigationPath.directions.getInstructions();
                            if (instructions.get(0).action.getClass() == Instruction.TakeVortex.class) {
                                int distanceToFloorChange = Integer.MAX_VALUE;
                                try {
                                    distanceToFloorChange = (int) instructions.get(0)
                                            .coordinate.metersFrom(iAmHereCoord);
                                } catch (MappedInException e) {

                                }
                                if (distanceToFloorChange < 10) {
                                    int floor = to.getMap().getFloor();
                                    if (instructions.size() >= 1) {
                                        floor  = instructions.get(1).coordinate.getMap().getFloor();
                                    }
                                    setMap(levelFloorMap.get(floor));
                                    iAmHereFloorIndex = levelFloorMap.get(floor);
                                    iAmHereCoord = new Coordinate(location, maps[iAmHereFloorIndex]);
                                    iAmHere.setPosition(iAmHereCoord, 1);
                                    from = iAmHereCoord;
                                    mapView.frame(currentMap, currentMap.getHeading(), (float) Math.PI / 4, 1f);
                                    currentNavigationPath = createPath(0f);
                                    if (currentNavigationPath != null) {
                                        drawMapElements(currentNavigationPath.pathElements);
                                        currentInstructionIndex = 0;
                                        showInstruction(instructions.get(currentInstructionIndex), iAmHereCoord);
                                    } else {
                                        if (!iAmAtVenue) {
                                            notAtVenueDialog.show();
                                        } else {
                                            directionsFailureDialog.show();
                                        }
                                        closeRouteDetails(new View(context));
                                    }
                                }
                            }

                            currentNavigationPath = createPath(0f);
                            if (currentNavigationPath != null) {
                                int distanceToDestination = Integer.MAX_VALUE;
                                try {
                                    distanceToDestination = (int) iAmHereCoord.metersFrom(to);
                                } catch (MappedInException e) { }
                                // User has arrived at location
                                if (activityState == State.NAVIGATION && (distanceToDestination < 5 || iAmHereCoord.isInside(to))) {
                                    clearMapElements(currentNavigationPath.pathElements);
                                    arriveAtDestinationDialog.setTitle(
                                            String.format(
                                                    getResources().getString(R.string.arrive_at_destination_dialog_title),
                                                    to.getLocations()[0].getName()));
                                    arriveAtDestinationDialog.show();
                                } else {
                                    drawMapElements(currentNavigationPath.pathElements);
                                    if (!cameraMode.equals(CameraMode.FOCUSED_ON_PATH)) {
                                        currentInstructionIndex = 0;
                                        showInstruction(currentNavigationPath.directions.getInstructions().get(currentInstructionIndex), iAmHereCoord);
                                    }
                                }
                            } else {
                                if (!iAmAtVenue) {
                                    notAtVenueDialog.show();
                                } else {
                                    directionsFailureDialog.show();
                                }
                                closeRouteDetails(new View(context));
                            }
                        }
                        if (cameraMode.equals(CameraMode.FOCUSED_ON_USER)) {
                            recenterCameraFocus(new View(context));
                        }
                    }
                }
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float degree = event.values[0];
        // Updates heading of user marker when changes are greater than 10 degrees
        // Instant changes can cause jitters if user is also moving
        if (Math.abs(currentDegree - degree) > 10) {
            currentDegree = degree;
            if (iAmHere != null) {
                iAmHere.setRotation((float) Math.toRadians(degree), 0);
                if (cameraMode.equals(CameraMode.FOCUSED_ON_USER)) {
                    mapView.orbit(iAmHereCoord, (float) Math.toRadians(degree), (float) Math.PI / 5, 0.3f);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    // Focuses camera on user location marker
    public void recenterCameraFocus(View v){
        if (iAmHere != null && iAmAtVenue) {
            setMap(iAmHereFloorIndex);
            mapView.orbit(
                    iAmHereCoord, (float) Math.toRadians(currentDegree), (float) Math.PI / 5, 1);
            cameraMode = CameraMode.FOCUSED_ON_USER;
            recenterBtn.clearColorFilter();
        }
    }

    // Removes flag to focus on user location marker, and highlights recenter button
    public void freeCameraFocus(){
        cameraMode = CameraMode.FREE_CAMERA;
        recenterBtn.setColorFilter(getResources().getColor(R.color.azure));
    }

    // Clears given array of elements from the map
    private void clearMapElements(ArrayList<Element> elements) {
        for (Element element : elements) {
            mapView.removeElement(element);
        }
    }

    // Draws given array of elements on the map
    private void drawMapElements(ArrayList<Element> elements) {
        for (Element element : elements) {
            mapView.addElement(element);
        }
    }

   // Generates a path connecting the "from" and "to" locations
   private NavigationPath createPath(Float over) {
       if (currentNavigationPath != null) {
           clearMapElements(currentNavigationPath.pathElements);
       }
       ArrayList<Element> newPathElements = new ArrayList<>();
       Directions directions;
       if (to != null && from != null) {
           directions =
                   from.directionsTo(maps[0].getVenue(), to, null, to.getLocations()[0], accessible);
           if (directions != null) {
               final Coordinate[] pathCoor = directions.getPath();
               Path routePath = new Path(pathCoor, 1f, 1f, getResources().getColor(R.color.azure), over);
               newPathElements.add(routePath);

               // Marks turn points with cylinders
               final List<DirectionInstruction> directionInstructions = new ArrayList<>();
               List<Instruction> instructions = directions.getInstructions();
               Iterator<Instruction> ii = instructions.iterator();
               Instruction instruction = ii.next();
               List<Coordinate> coords = new ArrayList<>();
               HashMap<Overlay2DImage, Integer> vortexes = new HashMap<>();
               for (Coordinate coord : pathCoor) {
                   coords.add(coord);
                   if (coord.equals(instruction.coordinate)) {
                       DirectionInstruction directionInstruction =
                               new DirectionInstruction(instruction, coords);
                       directionInstructions.add(directionInstruction);
                       Cylinder turnPoint = new Cylinder(1.5f, 1.2f, Color.WHITE);
                       Coordinate coor = instruction.coordinate;
                       turnPoint.setPosition(coor, 0);
                       newPathElements.add(turnPoint);
                       if (instruction.action instanceof Instruction.TakeVortex) {
                           // Add overlay with vortex icon such as elevators and stairs
                           ShapeDrawable border = new ShapeDrawable(new OvalShape());
                           border.getPaint ().setColor (getResources().getColor(android.R.color.white));
                           ShapeDrawable oval = new ShapeDrawable(new OvalShape());
                           oval.getPaint ().setColor (getResources().getColor(R.color.azure));
                           Drawable image = getDirectionImage(instruction);

                           LayerDrawable combinedOverlay = new LayerDrawable(new Drawable[]{border, oval, image});
                           combinedOverlay.setLayerInset(1,5,5,5, 5);
                           combinedOverlay.setLayerInset(2,20,20,20, 20);

                           Overlay2DImage vortexLabel = new Overlay2DImage(50,50, combinedOverlay, 25, 25 );
                           vortexLabel.setPosition(coor);
                           Instruction.TakeVortex action = (Instruction.TakeVortex) instruction.action;
                           vortexes.put(vortexLabel, action.toMap.getFloor());
                           newPathElements.add(vortexLabel);

                           if (ii.hasNext()) {
                               // At end of vortex add overlay with reverse icon
                               Drawable reverseVortexImage = getReverseDirectionImage(instruction);
                               LayerDrawable reverseCombinedOverlay = new LayerDrawable(new Drawable[]{border, oval, reverseVortexImage});
                               reverseCombinedOverlay.setLayerInset(1,5,5,5, 5);
                               reverseCombinedOverlay.setLayerInset(2,20,20,20, 20);
                               Overlay2DImage reverseVortexLabel = new Overlay2DImage(50,50, reverseCombinedOverlay, 25, 25 );
                               reverseVortexLabel.setPosition(ii.next().coordinate);
                               vortexes.put(reverseVortexLabel, action.fromMap.getFloor());
                               newPathElements.add(reverseVortexLabel);
                           }
                       }
                       if (ii.hasNext()) {
                           instruction = ii.next();
                           coords = new ArrayList<>();
                           coords.add(coord);
                       }
                   }
               }
               return new NavigationPath(directions, routePath, newPathElements, vortexes);
           }
       }
       return null;
   }

   // Displays first navigation instruction
   private void setupNavigation() {
       if (currentNavigationPath != null) {
           currentInstructionIndex = 0;
           Instruction instruction = currentNavigationPath.directions.getInstructions().get(currentInstructionIndex);
           showInstruction(
                   instruction,
                   iAmHereCoord);
           focusOnInstruction(
                   instruction.coordinate,
                   iAmHereCoord);
       }
   }

   // Focuses on the instruction point on the navigation path
   private void focusOnInstruction(Coordinate instructionCoord, Coordinate previousCoord){
       cameraMode = CameraMode.FOCUSED_ON_PATH;
       recenterBtn.setColorFilter(getResources().getColor(R.color.azure));

       // Focus on instruction with angle based on previous instruction
       Vector2 instructionVector = new Vector2(instructionCoord.getVector());
       Vector2 previousStepVector = new Vector2(previousCoord.getVector());
       mapView.frame(
               instructionCoord, -previousStepVector.angle(instructionVector), (float) Math.PI / 5, 1, framePadding);
   }

   // Sets up the navigation bar with the directions text and turn icons
   // for given instruction and distance from previous instruction
   private void showInstruction(Instruction instruction, Coordinate previousCoord) {
       if (currentInstructionIndex == 0) {
           previousStepBtn.getBackground().clearColorFilter();
       } else {
           previousStepBtn.getBackground().setColorFilter(
                   getResources().getColor(R.color.azure),
                   PorterDuff.Mode.SRC_ATOP);
       }
       if (currentNavigationPath != null && currentInstructionIndex == currentNavigationPath.directions.getInstructions().size() - 1) {
           nextStepBtn.getBackground().clearColorFilter();
       } else {
           nextStepBtn.getBackground().setColorFilter(
                   getResources().getColor(R.color.azure),
                   PorterDuff.Mode.SRC_ATOP);
       }
       try {
           int distance = (int) instruction.coordinate.metersFrom(previousCoord);
           String distanceString = String.format(getResources().getString(R.string.distance_to_step), distance);
           distanceToInstructionTextView.setText(distanceString);
       } catch (MappedInException e) {

       }
       instructionTextView.setText(instruction.instruction);
       Drawable drawable = getDirectionImage(instruction);
       if (drawable != null) {
           directionIcon.setImageDrawable(drawable);
       }
   }

   // Displays next or previous instruction and focuses camera on step in path
   public void previewInstruction(View v){
       if (currentNavigationPath != null)  {
           int newIndex = currentInstructionIndex;
           String tag = v.getTag().toString();
           List<Instruction> instructions = currentNavigationPath.directions.getInstructions();
           if (tag.equals("next") && currentInstructionIndex < instructions.size() - 1) {
               newIndex  = currentInstructionIndex + 1;
           } else if (tag.equals("previous") && currentInstructionIndex > 0) {
               newIndex = currentInstructionIndex - 1;
           }
           if (newIndex != currentInstructionIndex) {
               currentInstructionIndex = newIndex;
               Instruction instruction = instructions.get(currentInstructionIndex);
               int instructionFloor = levelFloorMap.get(instruction.coordinate.getMap().getFloor());
               setMap(instructionFloor);
               // If there is a previous instruction use that to calculate distance and camera angle
               if (currentInstructionIndex > 0) {
                   showInstruction(instruction,
                           instructions.get(currentInstructionIndex - 1).coordinate);
                   focusOnInstruction(instruction.coordinate,
                           instructions.get(currentInstructionIndex - 1).coordinate);
               } else {
                   // else use the user coordinates to calculate distance and camera angle
                   showInstruction(instruction,
                           iAmHereCoord);
                   focusOnInstruction(instruction.coordinate,
                           iAmHereCoord);
               }
           }
       }
   }

    // Gets image associated with direction instruction
    Drawable getDirectionImage(Instruction instruction) {
        if (instruction.action instanceof Instruction.Turn) {
            Instruction.Turn turn = ((Instruction.Turn) instruction.action);
            switch (turn.relativeBearing) {
                case Left:
                    return getResources().getDrawable(R.drawable.ic_direction_left);
                case SlightLeft:
                    return getResources().getDrawable(R.drawable.ic_direction_slight_left);
                case Right:
                    return getResources().getDrawable(R.drawable.ic_direction_right);
                case SlightRight:
                    return getResources().getDrawable(R.drawable.ic_direction_slight_right);
                case Straight:
                    return getResources().getDrawable(R.drawable.ic_direction_straight);
                default:
                    return null;
            }
        } else if (instruction.action instanceof Instruction.Arrival
                || instruction.action instanceof Instruction.Departure) {
            return getResources().getDrawable(R.drawable.ic_direction_straight);
        } else if (instruction.action instanceof Instruction.TakeVortex) {
            Instruction.TakeVortex action = (Instruction.TakeVortex)instruction.action;
            switch (instruction.atLocation.getType()) {
                case ("elevator"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_elevator_down);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_elevator_up);
                    }
                case ("stairs"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_stairs_down);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_stairs_up);
                    }
                case ("escalator"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_escalator_down);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_escalator_up);
                    }
                default:
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_ramp_down);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_ramp_up);
                    }
            }
        } else {
            return getResources().getDrawable(R.drawable.ic_direction_straight);
        }
    }

    // Gets the image associated with the reverse of the direction instruction
    Drawable getReverseDirectionImage(Instruction instruction) {
        if (instruction.action instanceof Instruction.Turn) {
            Instruction.Turn turn = ((Instruction.Turn) instruction.action);
            switch (turn.relativeBearing) {
                case Left:
                    return getResources().getDrawable(R.drawable.ic_direction_right);
                case SlightLeft:
                    return getResources().getDrawable(R.drawable.ic_direction_slight_right);
                case Right:
                    return getResources().getDrawable(R.drawable.ic_direction_left);
                case SlightRight:
                    return getResources().getDrawable(R.drawable.ic_direction_slight_left);
                case Straight:
                    //TODO: u-turn
                    return getResources().getDrawable(R.drawable.ic_direction_u_turn);
                default:
                    return null;
            }
        } else if (instruction.action instanceof Instruction.Arrival
                || instruction.action instanceof  Instruction.Departure) {
            return getResources().getDrawable(R.drawable.ic_direction_straight);
        } else if (instruction.action instanceof Instruction.TakeVortex) {
            Instruction.TakeVortex action = (Instruction.TakeVortex)instruction.action;
            switch (instruction.atLocation.getType()) {
                case ("elevator"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_elevator_up);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_elevator_down);
                    }
                case ("stairs"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_stairs_up);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_stairs_down);
                    }
                case ("escalator"):
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_escalator_up);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_escalator_down);
                    }
                default:
                    if (action.fromMap.getFloor() > action.toMap.getFloor()) {
                        return getResources().getDrawable(R.drawable.ic_ramp_up);
                    } else {
                        return getResources().getDrawable(R.drawable.ic_ramp_down);
                    }
            }
        } else {
            return getResources().getDrawable(R.drawable.ic_direction_u_turn);
        }
    }
}