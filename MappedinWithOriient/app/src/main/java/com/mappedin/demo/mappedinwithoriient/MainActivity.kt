package com.mappedin.demo.mappedinwithoriient

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mappedin.sdk.MPIMapView
import com.mappedin.sdk.listeners.MPIMapViewListener
import com.mappedin.sdk.models.MPIBlueDotPositionUpdate
import com.mappedin.sdk.models.MPIBlueDotStateChange
import com.mappedin.sdk.models.MPIData
import com.mappedin.sdk.models.MPIHeader
import com.mappedin.sdk.models.MPIMap
import com.mappedin.sdk.models.MPINavigatable
import com.mappedin.sdk.models.MPIPosition
import com.mappedin.sdk.models.MPIState
import com.mappedin.sdk.web.MPIOptions
import me.oriient.ipssdk.api.listeners.IPSCompletionListener
import me.oriient.ipssdk.api.listeners.IPSLoginListener
import me.oriient.ipssdk.api.listeners.IPSPositioningListener
import me.oriient.ipssdk.api.models.IPSError
import me.oriient.ipssdk.api.models.IPSFloor
import me.oriient.ipssdk.api.models.IPSPosition
import me.oriient.ipssdk.api.models.IPSSpace
import me.oriient.ipssdk.ips.IPSCalibrationDialog
import me.oriient.ipssdk.ips.IPSCore
import me.oriient.ipssdk.ips.IPSPositioning
import java.util.function.Consumer

class MainActivity : AppCompatActivity(), MPIMapViewListener, IPSPositioningListener {

    private val TAG = "MainActivity" // TAG for log statements.

    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
    ) // Permissions required for this sample.

    private lateinit var mMapView: MPIMapView
    private var mIsBlueDotEnabled = false // Monitor the state of Blue Dot visibility.
    private var mIsMapLoaded = false // Monitor whether the map has loaded.

    private lateinit var mMapsByFloor: java.util.HashMap<Int, String>

    private var calibrationDialog: IPSCalibrationDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMapView = findViewById<MPIMapView>(R.id.mapView)

        // Use loadVenue to load venue
        // TODO: Enter your ClientId, ClientSecret and Venue here.
        mMapView.loadVenue(
            MPIOptions.Init(
                "YOUR_CLIENT_ID",
                "YOUR_CLIENT_SECRET",
                "YOUR_VENUE_SLUG",
                headers = listOf(MPIHeader("testName", "testValue")),
            ),
            MPIOptions.ShowVenue(labelAllLocationsOnInit = true, backgroundColor = "#CDCDCD"),
        ) {}

        mMapView.listener = this

        permissionsCheck()
    }

    // Coordinates requesting permissions if needed or starting location updates if they've been granted.
    private fun permissionsCheck() {
        if (arePermissionsGranted()) {
            // Enable Blue Dot
            loginToOriient()
        } else if (isPermissionRationaleNeeded()) {
            // Show dialog about required permissions.
            showRationaleDialog()
        } else {
            // Ask for permissions.
            requestPermissionLauncher.launch(PERMISSIONS)
        }
    }

    // Checks if the location, Bluetooth scanning and internet permissions have been granted.
    private fun arePermissionsGranted(): Boolean {
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // A permission has not been granted.
                Log.i(TAG, "Permission is not granted: $permission")
                return false
            }
        }
        Log.d(TAG, "All permissions already granted.")
        return true
    }

    // Checks if the app should explain why it is asking for the permissions it needs.
    private fun isPermissionRationaleNeeded(): Boolean {
        for (permission in PERMISSIONS) {
            if (shouldShowRequestPermissionRationale(permission)) {
                // A permission has not been granted.
                Log.i(TAG, "Show rationale for permission: $permission")
                return false
            }
        }
        Log.i(TAG, "Permission rationale not needed.")
        return true
    }

    // Displays a dialog to the user explaining why the requested permissions are needed.
    private fun showRationaleDialog() {
        // Explain to the user why the permissions are required.
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Additional Permissions Needed")
        builder.setMessage("Location permission is required to show your location on the map. Bluetooth scanning is used to discover Bluetooth location beacons.")
        builder.setPositiveButton("Enable") { dialog: DialogInterface, _: Int ->
            // Request permissions.
            requestPermissionLauncher.launch(PERMISSIONS)
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "Deny",
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.create().show()
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog.
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { isGranted: Map<String, Boolean> ->
            var allGranted = true
            for ((key, value) in isGranted) {
                if (!value) {
                    allGranted = false
                    Log.i(TAG, "Permission was not granted: $key")
                }
            }
            if (allGranted) {
                // Enable Blue Dot.
                loginToOriient()
            } else {
                // Explain that Blue Dot won't be available.
                val builder =
                    AlertDialog.Builder(this)
                builder.setTitle("Location Disabled")
                builder.setMessage("Location based permissions are not enabled. Your location will not be shown on the map.")
                builder.create().show()
            }
        }

    private fun loginToOriient() {
        Log.i(TAG, "Log in to Oriient.")

        // TODO: Enter your userID, apiKey and ipsDomain here.
        IPSCore.login(
            "YOUR_MAP_ID",
            "YOUR_API_KEY",
            "YOUR_IPS_DOMAIN
            object : IPSLoginListener {
                override fun onError(loginError: IPSError) {
                    Log.d(TAG, "onError() called with error: $loginError")
                }

                override fun onLogin(list: List<IPSSpace>) {
                    Log.d(TAG, "onLogin() called with: list = [$list]")
                    postOriientLogin()
                }
            },
        )
    }

    private fun postOriientLogin() {
        Log.d(TAG, "postOriientLogin")
        // The observer will notify the calibration progress and necessity.
        IPSPositioning.addPositioningListener(this)

        // Turn on automatic calibration.
        IPSPositioning.setAutomaticCalibrationEnabled(true)

        startPositioning()
    }

    private fun startPositioning() {
        // Start positioning.
        IPSPositioning.startPositioning(
            "YOUR_BUILDING_ID",
            null,
            null,
            object : IPSCompletionListener {
                override fun onCompleted() {
                    Log.d(TAG, "Positioning started successfully")
                    // Enable Blue Dot.
                    mMapView.blueDotManager.enable(MPIOptions.BlueDot())
                    mIsBlueDotEnabled = true
                }

                override fun onError(error: IPSError) {
                    Log.e(
                        TAG,
                        "Failed to start positioning: error = " + error.code + " [" + error.message + "]",
                    )
                }
            },
        )
    }

    private fun stopPositioning() {
        IPSPositioning.stopPositioning(object : IPSCompletionListener {
            override fun onCompleted() {
                Log.d(TAG, "Positioning stopped successfully")
                mIsBlueDotEnabled = false
            }

            override fun onError(error: IPSError) {
                Log.e(TAG, "Failed to stop positioning: error = [" + error.message + "]")
            }
        })
    }

    // Start of MPIMapViewListener Methods.
    override fun onBlueDotPositionUpdate(update: MPIBlueDotPositionUpdate) {
    }

    override fun onBlueDotStateChange(stateChange: MPIBlueDotStateChange) {
    }

    override fun onDataLoaded(data: MPIData) {
        mMapsByFloor = HashMap()
        data.maps.forEach(
            Consumer { (id, _, _, elevation): MPIMap ->
                if (elevation != null) {
                    mMapsByFloor[elevation.toInt()] = id
                }
            },
        )
    }

    override fun onFirstMapLoaded() {
        Log.d(TAG, "First map loaded.")
        mIsMapLoaded = true

        // The Mappedin Blue Dot must be enabled after a map is loaded. Location updates may start
        // before the map has been loaded. Enabling it here ensures the Blue Dot is shown.

        // The Mappedin Blue Dot must be enabled after a map is loaded. Location updates may start
        // before the map has been loaded. Enabling it here ensures the Blue Dot is shown.
        if (mIsBlueDotEnabled) {
            mMapView.blueDotManager.enable(MPIOptions.BlueDot())
        }
    }

    override fun onMapChanged(map: MPIMap) {
    }

    override fun onNothingClicked() {
    }

    override fun onPolygonClicked(polygon: MPINavigatable.MPIPolygon) {
    }

    override fun onStateChanged(state: MPIState) {
    }

    // Start IPSPositioningListener methods.
    override fun onError(posError: IPSError) {
        Log.e(TAG, "onLoginError: " + posError.message)
    }

    override fun onPositioningEngineStateChanged(state: Int) {
        when (state) {
            IPSPositioning.PositioningEngineState.CALIBRATING -> {
                Log.i(TAG, "Positioning sate: CALIBRATING")
            }

            IPSPositioning.PositioningEngineState.IDLE -> {
                Log.i(TAG, "Positioning sate: IDLE")
            }

            IPSPositioning.PositioningEngineState.POSITIONING_AND_CALIBRATING -> {
                Log.i(TAG, "Positioning sate: POSITIONING AND CALIBRATING")
            }
        }
    }

    override fun onPositionUpdate(position: IPSPosition) {
        val floorLevel = 3

        Log.i(
            TAG,
            "Location Update Lat: " + position.latitude +
                " Long: " + position.longitude +
                " Floor: " + floorLevel +
                " Accuracy: " + position.accuracy,
        )

        val coordinates = MPIPosition.MPICoordinates(
            position.latitude,
            position.longitude,
            position.accuracy,
            floorLevel,
        )
        val mpiPosition = MPIPosition(
            System.currentTimeMillis().toDouble(),
            coordinates,
            "",
            null,
        )

        mMapView.blueDotManager.updatePosition(mpiPosition)
    }

    override fun onCalibrationProgress(progress: Double) {
        Log.i(TAG, "onCalibrationProgress: $progress")
    }

    @Deprecated("Deprecated in Java")
    override fun onCalibrationGestureNeeded(calibNeeded: Boolean) {
        Log.i(TAG, "onCalibrationGestureNeeded? $calibNeeded")
    }

    override fun onCalibrationGestureNeeded(calibNeeded: Boolean, reason: Int?) {
        Log.i(TAG, "onCalibrationGestureNeeded? $calibNeeded Reason: $reason")

        runOnUiThread {
            if (calibNeeded) {
                if (calibrationDialog == null) {
                    calibrationDialog = IPSCalibrationDialog.Builder(this)
                        .create()
                }
                calibrationDialog?.show()
            } else {
                calibrationDialog?.dismiss()
            }
        }
    }

    override fun onFloorChanged(floor: IPSFloor) {
        Log.i(TAG, "onFloorChanged: $floor")

        // Change maps if the floor has changed.
        if (mIsMapLoaded && mMapView.currentMap?.elevation?.toInt() != floor.order) {
            Log.i(
                TAG,
                "Changing Floor from " + mMapView.currentMap?.elevation +
                    " to " + floor.order,
            )
            mMapsByFloor[floor.order]?.let {
                mMapView.setMap(it) { err ->
                    err?.message?.let { message ->
                        Log.e("setMap ", message)
                    }
                }
            }
        }
    }
}
