package sk.matejsvrcek.znackar.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.utils.FreemapTileSource;
import sk.matejsvrcek.znackar.utils.SharedPreferencesManager;
import sk.matejsvrcek.znackar.viewmodel.TrackRecordingViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class TrackRecordingActivity extends AppCompatActivity {

    private static final String TASK_ID_KEY = "taskId";
    private static final String FOLLOW_MODE_KEY = "isFollowModeActive";

    private MapView map;
    private GeoPoint lastGeoPoint = null;
    private float lastBearing = 0f;
    private Button btnWaypoint, btnCenter, btnStop;
    private Polyline polyline;
    private int taskId = -1;
    private TrackRecordingViewModel viewModel;
    private MyLocationNewOverlay locationOverlay;
    private boolean isFollowModeActive = true;
    private boolean userIsTouchingMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_track_recording);

        // Used in every Activity to make sure the notch used in newer devices is showing properly

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        viewModel = new ViewModelProvider(this).get(TrackRecordingViewModel.class);

        if (savedInstanceState != null) {
            taskId = savedInstanceState.getInt(TASK_ID_KEY, -1);
            isFollowModeActive = savedInstanceState.getBoolean(FOLLOW_MODE_KEY, true);
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("taskId")) {
            taskId = intent.getIntExtra("taskId", -1);
        }

        if (taskId != -1) {
            viewModel.setCurrentTaskId(taskId);
        } else {
            showTaskInputDialog();
        }

        map = findViewById(R.id.map);
        btnWaypoint = findViewById(R.id.btnWaypoint);
        btnCenter = findViewById(R.id.btnCenter);
        btnStop = findViewById(R.id.btnStop);

        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMultiTouchControls(true);
        map.setTileSource(new FreemapTileSource());

        // Disables follow mode when the user is touching the map

        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (!userIsTouchingMap) {
                    disableFollowMode();
                    userIsTouchingMap = true;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                userIsTouchingMap = false;
            }
            return false;
        });

        //Shows the user's current location

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        if (isFollowModeActive) {
            locationOverlay.enableFollowLocation();
        }
        map.getOverlays().add(locationOverlay);

        lastGeoPoint = SharedPreferencesManager.getLastGeoPoint(this);
        lastBearing = SharedPreferencesManager.getLastBearing(this);

        //Setup for the polyline used to draw the route

        polyline = new Polyline();
        polyline.getOutlinePaint().setStrokeWidth(8f);
        polyline.getOutlinePaint().setColor(Color.BLUE);
        map.getOverlays().add(polyline);

        //Zoom for the map
        map.getController().setZoom(18.0);

        //Loads the trackpoints from the DB, then proceeds to draw them,
        // first trackpoint is marked as a green flag
        viewModel.getSavedTrackPoints().observe(this, trackpoints -> {
            List<GeoPoint> geoPoints = new ArrayList<>();
            if (trackpoints != null) {
                for (var tp : trackpoints) {
                    geoPoints.add(new GeoPoint(tp.latitude, tp.longitude, tp.altitude));
                }
            }
            polyline.setPoints(geoPoints);

            if (!geoPoints.isEmpty()) {
                GeoPoint startPoint = geoPoints.get(0);
                Marker startMarker = new Marker(map);
                startMarker.setPosition(startPoint);
                startMarker.setTitle("Štart");
                startMarker.setIcon(getResources().getDrawable(R.drawable.ic_flag_green, null));
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                startMarker.setInfoWindow(new WaypointInfoWindow(map));
                startMarker.setOnMarkerClickListener((m, mapView) -> {
                    m.showInfoWindow();
                    return true;
                });
                map.getOverlays().add(startMarker);
            }
            map.invalidate();
        });

        //Returns all the saved waypoints from the DB, then proceeds to draw them,
        // they're marked with a red flag

        viewModel.getSavedWaypoints().observe(this, waypoints -> {
            for (int i = map.getOverlays().size() - 1; i >= 0; i--) {
                if (map.getOverlays().get(i) instanceof Marker) {
                    Marker marker = (Marker) map.getOverlays().get(i);
                    if ("Waypoint".equals(marker.getTitle())) {
                        map.getOverlays().remove(i);
                    }
                }
            }
            if (waypoints != null) {
                for (var wp : waypoints) {
                    Marker marker = new Marker(map);
                    GeoPoint position = new GeoPoint(wp.latitude, wp.longitude, wp.altitude);
                    marker.setPosition(position);
                    marker.setTitle(wp.description);
                    String snippet = "Lat: " + wp.latitude + "\n" + "Lon: " + wp.longitude + "\n" + "Alt: " + wp.altitude + " m";
                    marker.setSnippet(snippet);
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_red, null));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setInfoWindow(new WaypointInfoWindow(map));
                    marker.setOnMarkerClickListener((m, mapView) -> {
                        m.showInfoWindow();
                        return true;
                    });
                    map.getOverlays().add(marker);
                }
            }
            map.invalidate();
        });

        // Checks if the user has granted location permission

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Starts the location tracking service if the user has granted location permission

        viewModel.getCurrentTaskId().observe(this, id -> {
            if (id != null && id != -1 && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startLocationTrackingService();
            }
        });

        // Updates the last known location

        viewModel.getLocationLiveData().observe(this, locationData -> {
            if (isFinishing() || locationData == null) return;
            lastGeoPoint = new GeoPoint(locationData.getLatitude(), locationData.getLongitude(), locationData.getAltitude());
            lastBearing = locationData.getBearing();
            SharedPreferencesManager.setLastLocation(this, locationData);
            if (isFollowModeActive && lastGeoPoint != null) {
                map.getController().animateTo(lastGeoPoint);
            }
            map.setMapOrientation(lastBearing);
        });

        btnWaypoint.setOnClickListener(v -> {
            if (lastGeoPoint != null) {
                showWaypointInputDialog(lastGeoPoint);
            }
        });

        btnCenter.setOnClickListener(v -> {
            if (lastGeoPoint != null) {
                isFollowModeActive = true;
                locationOverlay.enableFollowLocation();
                map.getController().animateTo(lastGeoPoint);
                map.setMapOrientation(lastBearing);
                map.getController().setZoom(20.0);
            }
        });

        btnStop.setOnClickListener(v -> confirmStop());

        // Makes sure the user stops the activity correctly, ending the task
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(TrackRecordingActivity.this, "Ukončite momentálnu úlohu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Handles the logic for disabling the follow mode when the user touches the map

    private void disableFollowMode() {
        if (isFollowModeActive) {
            isFollowModeActive = false;
            locationOverlay.disableFollowLocation();
            Toast.makeText(this, "Automatické sledovanie vypnuté", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (isFollowModeActive) {
            locationOverlay.enableFollowLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    // Saves the current task ID and follow mode state

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(TASK_ID_KEY, taskId);
        outState.putBoolean(FOLLOW_MODE_KEY, isFollowModeActive);
        super.onSaveInstanceState(outState);
    }

    //Handles the way the activity behaves when the user returns to it
    // from the notification created by the LocationTrackingService
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        int newTaskId = intent.getIntExtra("taskId", -1);
        if (newTaskId != -1) {
            taskId = newTaskId;
            viewModel.setCurrentTaskId(taskId);
        } else {
            finish();
        }
    }

    // Shows a dialog for the user to enter the task name and description

    private void showTaskInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.fragment_task_input, null);
        builder.setView(dialogView);

        EditText editTaskName = dialogView.findViewById(R.id.editTaskName);
        EditText editTaskDescription = dialogView.findViewById(R.id.editTaskDescription);

        builder.setTitle("Vložte názov úlohy")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String taskName = editTaskName.getText().toString();
                    String taskDescription = editTaskDescription.getText().toString();
                    insertTask(taskName, taskDescription, "track"); // this will set taskId and start tracking
                })
                .setNegativeButton("Späť", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });

        builder.create().show();
    }

    // Inserts a new task into the database and starts the location tracking service

    private void insertTask(String taskName, String taskDescription, String taskType) {
        Task task = new Task();
        task.name = taskName;
        task.description = taskDescription;
        task.type = taskType;
        task.isCompleted = false;
        task.isExported = false;
        task.dateTime = System.currentTimeMillis();

        viewModel.insertTask(task, id -> {
            taskId = id;
            viewModel.setCurrentTaskId(id);
            viewModel.startLocationTrackingService();
        });
    }

    // Shows a dialog for the user to enter the waypoint description

    private void showWaypointInputDialog(GeoPoint point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_waypoint_input, null);
        builder.setView(dialogView);

        EditText editWaypointDescription = dialogView.findViewById(R.id.editWaypointDescription);

        builder.setTitle("Vložte názov bodu")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String description = editWaypointDescription.getText().toString();
                    viewModel.saveWaypoint(point, description);
                    Marker marker = new Marker(map);
                    marker.setPosition(point);
                    marker.setTitle("Waypoint");

                    String snippet = "Popis: " + description + "\n" +
                            "Lat: " + point.getLatitude() + "\n" +
                            "Lon: " + point.getLongitude() + "\n" +
                            "Alt: " + point.getAltitude() + " m";

                    marker.setSnippet(snippet);
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_red, null));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setInfoWindow(new WaypointInfoWindow(map)); // <-- Custom InfoWindow
                    marker.setOnMarkerClickListener((m, mapView) -> {
                        m.showInfoWindow();
                        return true;
                    });

                    map.getOverlays().add(marker);
                    map.invalidate();

                })
                .setNegativeButton("Zrušiť", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    //Ends the activity correctly, flagging it as completed in the DB
    private void confirmStop() {
        new AlertDialog.Builder(this)
                .setTitle("Ukončiť trasu")
                .setMessage("Naozaj chcete ukončiť danú úlohu?")
                .setPositiveButton("Áno", (dialog, which) -> {
                    viewModel.getLocationLiveData().removeObservers(this);
                    viewModel.setTaskAsCompleted(taskId);
                    viewModel.stopLocationTrackingService();
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                    finish();
                })
                .setNegativeButton("Nie", null)
                .show();
    }
}