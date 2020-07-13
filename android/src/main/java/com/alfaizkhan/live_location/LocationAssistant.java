package com.alfaizkhan.live_location;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alfaizkhan.live_location.Model.Accuracy;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class LocationAssistant
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "LocationAssistant" ;

    public interface Listener {

        void onNeedLocationPermission();

        void onExplainLocationPermission();

        void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);

        void onNeedLocationSettingsChange();

        void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);

        void onNewLocationAvailable(Location location);

        void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);

    }



    private final int REQUEST_CHECK_SETTINGS = 0;
    private final int REQUEST_LOCATION_PERMISSION = 1;

    // Parameters
    private final Context context;
    private AppCompatActivity activity;
    private Listener listener;
    private final int priority;
    private final long updateInterval;
    private final boolean allowMockLocations;

    // Internal state
    private boolean permissionGranted;
    private boolean locationRequested;
    private boolean locationStatusOk;
    private boolean changeSettings;
    private boolean updatesRequested;
    private Location bestLocation;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Status locationStatus;
    private boolean mockLocationsEnabled;
    private int numTimesPermissionDeclined;

    // Mock location rejected
    private Location lastMockLocation;
    private int numGoodReadings;

    public LocationAssistant(final Context context, Listener listener, Accuracy accuracy, long updateInterval,
                             boolean allowMockLocations) {
        this.context = context;
        if (context instanceof AppCompatActivity)
            this.activity = (AppCompatActivity) context;
        this.listener = listener;
        switch (accuracy) {
            case HIGH:
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case MEDIUM:
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case LOW:
                priority = LocationRequest.PRIORITY_LOW_POWER;
                break;
            case PASSIVE:
            default:
                priority = LocationRequest.PRIORITY_NO_POWER;
        }
        this.updateInterval = updateInterval;
        this.allowMockLocations = allowMockLocations;

        // Set up the Google API client
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void start() {
        checkMockLocations();
        googleApiClient.connect();
    }

    public void register(AppCompatActivity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        checkInitialLocation();
        acquireLocation();
    }

    public void stop() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
        permissionGranted = false;
        locationRequested = false;
        locationStatusOk = false;
        updatesRequested = false;
    }

    public void requestAndPossiblyExplainLocationPermission() {
        if (permissionGranted) return;
        if (activity == null) {

            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                && listener != null)
            listener.onExplainLocationPermission();
        else
            requestLocationPermission();
    }

    public void requestLocationPermission() {
        if (activity == null) {

            return;
        }
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    public boolean onPermissionsUpdated(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) return false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            acquireLocation();
            return true;
        } else {
            numTimesPermissionDeclined++;
            if (numTimesPermissionDeclined >= 2 && listener != null)
                listener.onLocationPermissionPermanentlyDeclined(onGoToAppSettingsFromView,
                        onGoToAppSettingsFromDialog);
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != REQUEST_CHECK_SETTINGS) return;
        if (resultCode == AppCompatActivity.RESULT_OK) {
            changeSettings = false;
            locationStatusOk = true;
        }
        acquireLocation();
    }

    public void changeLocationSettings() {
        if (locationStatus == null) return;
        if (activity == null) {

            return;
        }
        try {
            locationStatus.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            if (listener != null)
            changeSettings = false;
            acquireLocation();
        }
    }

    private void acquireLocation() {
        if (!permissionGranted) checkLocationPermission();
        if (!permissionGranted) {
            if (numTimesPermissionDeclined >= 2) return;
            if (listener != null)
                listener.onNeedLocationPermission();

            return;
        }
        if (!locationRequested) {
            requestLocation();
            return;
        }
        if (!locationStatusOk) {
            if (changeSettings) {
                if (listener != null)
                    listener.onNeedLocationSettingsChange();

            } else
                checkProviders();
            return;
        }
        if (!updatesRequested) {
            requestLocationUpdates();
            // Check back in a few
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    acquireLocation();
                }
            }, 10000);
            return;
        }

        if (!checkLocationAvailability()) {
            // Something is wrong - probably the providers are disabled.
            checkProviders();
        }
    }

    private void checkInitialLocation() {
        if (!googleApiClient.isConnected() || !permissionGranted || !locationRequested || !locationStatusOk)
            return;
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            onLocationChanged(location);
        } catch (SecurityException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void checkMockLocations() {
        mockLocationsEnabled = false;
    }

    private void checkLocationPermission() {
        permissionGranted = Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocation() {
        if (!googleApiClient.isConnected() || !permissionGranted) return;
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(priority);
        locationRequest.setInterval(updateInterval);
        locationRequest.setFastestInterval(updateInterval);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
                .setResultCallback(onLocationSettingsReceived);
    }

    private boolean checkLocationAvailability() {
        if (!googleApiClient.isConnected() || !permissionGranted) return false;
        try {
            LocationAvailability la = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
            return (la != null && la.isLocationAvailable());
        } catch (SecurityException e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    private void checkProviders() {
        // Do it the old fashioned way
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) return;
        if (listener != null)
            listener.onFallBackToSystemSettings(onGoToLocationSettingsFromView, onGoToLocationSettingsFromDialog);

    }

    private void requestLocationUpdates() {
        if (!googleApiClient.isConnected() || !permissionGranted || !locationRequested) return;
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            updatesRequested = true;
        } catch (SecurityException e) {
            Log.d(TAG, e.toString());
        }
    }

    private final DialogInterface.OnClickListener onGoToLocationSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            }
        }
    };

    private final View.OnClickListener onGoToLocationSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            }
        }
    };

    private final DialogInterface.OnClickListener onGoToDevSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            }
        }
    };

    private final View.OnClickListener onGoToDevSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            }
        }
    };

    private final DialogInterface.OnClickListener onGoToAppSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        }
    };

    private final View.OnClickListener onGoToAppSettingsFromView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        }
    };

    private boolean isLocationPlausible(Location location) {
        if (location == null) return false;

        boolean isMock = mockLocationsEnabled || (Build.VERSION.SDK_INT >= 18 && location.isFromMockProvider());
        if (isMock) {
            lastMockLocation = location;
            numGoodReadings = 0;
        } else
            numGoodReadings = Math.min(numGoodReadings + 1, 1000000); // Prevent overflow

        if (numGoodReadings >= 20) lastMockLocation = null;

        if (lastMockLocation == null) return true;

        double d = location.distanceTo(lastMockLocation);
        return (d > 1000);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        acquireLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        boolean plausible = isLocationPlausible(location);
        if (!allowMockLocations && !plausible) {
            if (listener != null) listener.onMockLocationsDetected(onGoToDevSettingsFromView,
                    onGoToDevSettingsFromDialog);
            return;
        }

        bestLocation = location;
        if (listener != null)
            listener.onNewLocationAvailable(location);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private final ResultCallback<LocationSettingsResult> onLocationSettingsReceived = new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(@NonNull LocationSettingsResult result) {
            locationRequested = true;
            locationStatus = result.getStatus();
            switch (locationStatus.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    locationStatusOk = true;
                    checkInitialLocation();
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    locationStatusOk = false;
                    changeSettings = true;
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    locationStatusOk = false;
                    break;
            }
            acquireLocation();
        }
    };
}
