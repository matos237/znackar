package sk.matejsvrcek.znackar.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.ui.TrackRecordingActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "TrackingServiceChannel";

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean useFusedProvider;
    private Location lastKnownLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("LocationTrackingService", "Service onCreate");
        createNotificationChannel();

        // Check if the device has a fused location provider
        useFusedProvider = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

        //If it does, it uses the FusedLocationProviderClient,
        // otherwise it uses the legacy LocationManager
        if (useFusedProvider) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } else {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (isValidLocation(location) && shouldSendUpdate(location, currentMinDistance)) {
                        sendLocationBroadcast(location);
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {
                    Log.d("LocationTrackingService", "Provider enabled: " + provider);
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.d("LocationTrackingService", "Provider disabled: " + provider);
                }
            };
        }
    }

    private float currentMinDistance = 5f; // default value for min distance

    //Starts a foreground service with a notification when the service itself starts, this is needed
    // otherwise the app wouldn't receive any location data in the background
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocationTrackingService", "Service onStartCommand");

        int taskId = intent != null ? intent.getIntExtra("taskId", -1) : -1;

        startForeground(1, getNotification(taskId));
        startLocationUpdates();
        return START_STICKY;
    }

    //Starts the location updates, checks for permissions once again and sets the update
    // frequency based on the user's settings via SharedPreferences
    private void startLocationUpdates() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                String frequencySetting = SharedPreferencesManager.getGpsFrequency(this);
                long interval = 10000;
                long fastest = 5000;
                float minDistance = 5f;

                switch (frequencySetting) {
                    case "Cycling":
                        interval = 5000;
                        fastest = 3000;
                        minDistance = 15f;
                        break;
                    case "Driving":
                        interval = 3000;
                        fastest = 2000;
                        minDistance = 30f;
                        break;
                    case "Walking":
                        interval = 10000;
                        fastest = 5000;
                        minDistance = 5f;
                        break;
                    default:
                        interval = 10000;
                        fastest = 5000;
                        minDistance = 5f;
                        break;
                }


                currentMinDistance = minDistance;

                if (useFusedProvider) {
                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setInterval(interval);
                    locationRequest.setFastestInterval(fastest);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                    locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult == null) return;

                            for (Location location : locationResult.getLocations()) {
                                if (isValidLocation(location) && shouldSendUpdate(location, currentMinDistance)) {
                                    sendLocationBroadcast(location);
                                }
                            }
                        }
                    };

                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    Log.d("LocationTrackingService", "Fused location updates started");
                } else {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            interval,
                            minDistance,
                            locationListener
                    );
                    Log.d("LocationTrackingService", "Legacy location updates started");
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //Determines if an update should be sent based on the current location and the
    // minimum distance from the last updated location
    private boolean shouldSendUpdate(Location newLocation, float minDistanceMeters) {
        if (lastKnownLocation == null) {
            lastKnownLocation = newLocation;
            return true;
        }

        float distance = newLocation.distanceTo(lastKnownLocation);
        if (distance >= minDistanceMeters) {
            lastKnownLocation = newLocation;
            return true;
        }
        return false;
    }

    //Checks if the current location the service received is usable, meaning accuracy must be
    // kind of decent, must contain altitude and mustn't be older than 10s
    private boolean isValidLocation(Location location) {
        return location != null
                && location.hasAccuracy()
                && location.getAccuracy() <= 20
                && location.hasAltitude()
                && (System.currentTimeMillis() - location.getTime()) < 10000;
    }

    //Sends the location data to the ViewModel via a broadcast
    private void sendLocationBroadcast(Location location) {
        Log.d("LocationTracking", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude() +
                ", Alt: " + location.getAltitude() + ", Bearing: " + location.getBearing());

        Intent intent = new Intent("sk.matejsvrcek.znackar.LOCATION_UPDATE");
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        intent.putExtra("alt", location.getAltitude());
        intent.putExtra("bearing", location.getBearing());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //Creates the notification that is shown when the service is running
    private Notification getNotification(int taskId) {
        Intent intent = new Intent(this, TrackRecordingActivity.class);
        intent.putExtra("taskId", taskId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Značkár")
                .setContentText("Aplikácia zaznamenáva vašu polohu")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    //Creates the notification channel
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking Service",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
        Log.d("LocationTrackingService", "Notification channel created");
    }

    //Removes the location updates when the service is destroyed

    @Override
    public void onDestroy() {
        Log.d("LocationTrackingService", "Service onDestroy");
        super.onDestroy();
        if (useFusedProvider && fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        } else if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
