package com.mappedin.sample.mappedinwithindooratlas;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.mappedin.sdk.MPIMapView;
import com.mappedin.sdk.listeners.MPIMapViewListener;
import com.mappedin.sdk.models.*;
import com.mappedin.sdk.web.MPIError;
import com.mappedin.sdk.web.MPIOptions;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity implements MPIMapViewListener, IALocationListener {

    private static final String TAG = "MainActivity";  //TAG for log statements.

    private final String[] PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.BLUETOOTH_SCAN}; //Permissions required for this sample.

    private MPIMapView mMapView;
    private IALocationManager mIALocationManager;

    //Allows looking up the maps by floor. This sample assumes a single building or map group
    //is being used.
    private HashMap<Double, String> mMapsByFloor;

    private boolean mIsBlueDotEnabled = false;  //Monitor the state of Blue Dot visibility.
    private boolean mIsMapLoaded = false; //Monitor whether the map has loaded.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setContentView(R.layout.activity_main);

        mMapView = findViewById(R.id.mapView);

        //TODO: Enter your ClientId, ClientSecret and Venue here.
        MPIOptions.Init options = new MPIOptions.Init("5eab30aa91b055001a68e996",
                "RJyRXKcryCMy4erZqqCbuB1NbR66QTGNXVE0x3Pg6oCIlUR1",
                "mappedin-demo-office");

        MPIOptions.ShowVenue showVenueOptions = new MPIOptions.ShowVenue(null,
                true, "#CDCDCD", null);

        mMapView.setListener(this);
        mMapView.loadVenue(options, showVenueOptions, null);

        permissionsCheck();
    }

    //Resume location updates when the app is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        //Enable location updates.
        if (mIALocationManager != null) {
            mIALocationManager.requestLocationUpdates(IALocationRequest.create(), this);
        }
    }

    //Pause location updates when the app is paused.
    @Override
    protected void onPause() {
        super.onPause();
        //Disable location updates.
        if (mIALocationManager != null) {
            mIALocationManager.removeLocationUpdates(this);
        }
    }

    //Tear down the location manager.
    @Override
    protected void onDestroy() {
        mIALocationManager.destroy();
        super.onDestroy();
    }

    //Coordinates requesting permissions if needed or starting location updates if they've been granted.
    private void permissionsCheck() {
        if (arePermissionsGranted()) {
            //Enable Blue Dot
            startShowingLocation();
        } else if (isPermissionRationaleNeeded()) {
            //Show dialog about required permissions.
            showRationaleDialog();
        } else {
            //Ask for permissions.
            requestPermissionLauncher.launch(PERMISSIONS);
        }
    }

    //Checks if the location, Bluetooth scanning and internet permissions have been granted.
    private boolean arePermissionsGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                //A permission has not been granted.
                Log.i(TAG, "Permission is not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions already granted.");
        return true;
    }

    //Checks if the app should explain why it is asking for the permissions it needs.
    private boolean isPermissionRationaleNeeded() {
        for (String permission : PERMISSIONS) {
            if (shouldShowRequestPermissionRationale(permission)) {
                //A permission has not been granted.
                Log.i(TAG, "Show rationale for permission: " + permission);
                return false;
            }
        }
        Log.i(TAG, "Permission rationale not needed.");
        return true;
    }

    //Displays a dialog to the user explaining why the requested permissions are needed.
    private void showRationaleDialog() {
        //Explain to the user why the permissions are required.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Additional Permissions Needed");
        builder.setMessage("Location permission is required to show your location on the map. Bluetooth scanning is used to discover Bluetooth location beacons.");
        builder.setPositiveButton("Enable", (dialog, id) -> {
            //Request permissions.
            requestPermissionLauncher.launch(PERMISSIONS);
            dialog.dismiss();
        });
        builder.setNegativeButton("Deny", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog.
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> pair : isGranted.entrySet()) {
                    if (!pair.getValue()) {
                        allGranted = false;
                        Log.i(TAG, "Permission was not granted: " + pair.getKey());
                    }
                }

                if (allGranted) {
                    //Enable Blue Dot.
                    startShowingLocation();
                } else {
                    //Explain that Blue Dot won't be available.
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Location Disabled");
                    builder.setMessage("Location based permissions are not enabled. Your location will not be shown on the map.");
                    builder.create().show();
                }
            });

    //Enables location updates and display of the user's location on the map as a blue dot.
    private void startShowingLocation() {
        Log.i(TAG, "Enabling Blue Dot.");
        mIsBlueDotEnabled = true;
        mMapView.getBlueDotManager().enable(new MPIOptions.BlueDot());
        mIALocationManager = IALocationManager.create(this);
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), this);
    }

    @Override
    public void onBlueDotPositionUpdate(@NonNull MPIBlueDotPositionUpdate mpiBlueDotPositionUpdate) {
        Log.d(TAG, "Blue dot position update.");
    }

    @Override
    public void onBlueDotStateChange(@NonNull MPIBlueDotStateChange mpiBlueDotStateChange) {
        Log.d(TAG, "Blue dot state change.");
    }

    @Override
    public void onDataLoaded(@NonNull MPIData mpiData) {
        Log.d(TAG, "Data loaded.");

        mMapsByFloor = new HashMap<>();
        mpiData.getMaps().forEach(mpiMap -> mMapsByFloor.put(mpiMap.getElevation(), mpiMap.getId()));
    }

    @Override
    public void onFirstMapLoaded() {
        Log.d(TAG, "First map loaded.");
        mIsMapLoaded = true;

        //The Mappedin Blue Dot must be enabled after a map is loaded. Location updates may start
        //before the map has been loaded. Enabling it here ensures the Blue Dot is shown.
        if (mIsBlueDotEnabled) {
            mMapView.getBlueDotManager().enable(new MPIOptions.BlueDot());
        }
    }

    @Override
    public void onMapChanged(@NonNull MPIMap mpiMap) {
        Log.d(TAG, "Map changed.");
    }

    @Override
    public void onNothingClicked() {
        Log.d(TAG, "Nothing clicked.");
    }

    @Override
    public void onPolygonClicked(@NonNull MPINavigatable.MPIPolygon mpiPolygon) {
        Log.d(TAG, "Polygon clicked.");
    }

    @Override
    public void onStateChanged(@NonNull MPIState mpiState) {
        Log.d(TAG, "State changed.");
    }

    //Update the user's location on the map as their location changes.
    @Override
    public void onLocationChanged(IALocation iaLocation) {
        MPIPosition.MPICoordinates coordinates = new MPIPosition.MPICoordinates(
                iaLocation.getLatitude(), iaLocation.getLongitude(), iaLocation.getAccuracy(),
                iaLocation.getFloorLevel());
        MPIPosition position = new MPIPosition((double)System.currentTimeMillis(), coordinates,
                "", null);

        double floorLevel = iaLocation.getFloorLevel();
        if (mIsMapLoaded && mMapView.getCurrentMap().getElevation() != floorLevel) {
            Log.i(TAG, "Changing Floor from " + mMapView.getCurrentMap().getElevation() +
                    " to " + floorLevel);
            mMapView.setMap(mMapsByFloor.get(floorLevel), (Function1<? super MPIError, Unit>) null);
        }

        mMapView.getBlueDotManager().updatePosition(position);
        Log.i(TAG, "Location Update Lat: " + iaLocation.getLatitude() +
                " Long: " + iaLocation.getLongitude() +
                " Floor: " + iaLocation.getFloorLevel() +
                " Accuracy: " + iaLocation.getAccuracy());
    }

    //Show location status changes to the user as a toast.
    @Override
    public void onStatusChanged(String s, int status, Bundle bundle) {
        String msg;

        switch (status) {
            case IALocationManager.STATUS_AVAILABLE:
                msg = "Location Status: Available";
                break;
            case IALocationManager.STATUS_LIMITED:
                msg = "Location Status: Limited";
                break;
            case IALocationManager.STATUS_OUT_OF_SERVICE:
                msg = "Location Status: Out of service";
                break;
            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                msg = "Location Status: Temporarily unavailable";
                break;
            default:
                msg = "Location Status: Unknown";
                break;
        }

        Log.i(TAG, msg);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}