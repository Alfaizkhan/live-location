package com.alfaizkhan.live_location;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.view.View;

import androidx.annotation.NonNull;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * LiveLocationPlugin
 */
public class LiveLocationPlugin extends FlutterActivity implements MethodCallHandler {
    private LocationAssistantListener locationAssistantListener;
    private final Context context;
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "live_location");
        channel.setMethodCallHandler(new LiveLocationPlugin(registrar.context()));
    }

    private LiveLocationPlugin(Context context) {
        this.context = context;
        locationAssistantListener = new LocationAssistantListener(context);
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        switch (call.method) {

            case "getLatitude":
                if (locationAssistantListener.getLatitude() != null) {
                    result.success(locationAssistantListener.getLatitude());
                } else {
                    locationAssistantListener = new LocationAssistantListener(context);
                    result.success(null);
                }
                break;
            case "getLongitude":
                if (locationAssistantListener.getLongitude() != null) {
                    result.success(locationAssistantListener.getLongitude());
                } else {
                    locationAssistantListener = new LocationAssistantListener(context);
                    result.success(null);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationAssistantListener.getAssistant().start();
    }

    @Override
    protected void onPause() {
        locationAssistantListener.getAssistant().stop();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationAssistantListener.getAssistant().onPermissionsUpdated(requestCode, grantResults);//io.flutter.Log.i("i", "requestCode: " + requestCode);
    }
}

class LocationAssistantListener implements LocationAssistant.Listener {
    private final LocationAssistant assistant;
    private String latitude;
    private String longitude;

    public LocationAssistantListener(Context context) {
        assistant = new LocationAssistant(context, this, LocationAssistant.Accuracy.HIGH, 5000, false);
        assistant.start();
    }

    @Override
    public void onNeedLocationPermission() {
        assistant.requestLocationPermission();
        assistant.requestAndPossiblyExplainLocationPermission();
    }

    @Override
    public void onExplainLocationPermission() {
        io.flutter.Log.i("i", "onExplainLocationPermission: ");
    }

    @Override
    public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        io.flutter.Log.i("i", "onLocationPermissionPermanentlyDeclined: ");
    }

    @Override
    public void onNeedLocationSettingsChange() {
        io.flutter.Log.i("i", "LocationSettingsStatusCodes.RESOLUTION_REQUIRED: Please Turn on GPS location service.");
    }

    @Override
    public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        io.flutter.Log.i("i", "onFallBackToSystemSettings: ");
    }

    @Override
    public void onNewLocationAvailable(Location location) {
        if (location == null) return;
        latitude = location.getLatitude() + "";
        longitude = location.getLongitude() + "";
    }

    @Override
    public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
    }


    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public LocationAssistant getAssistant() {
        return assistant;
    }
}