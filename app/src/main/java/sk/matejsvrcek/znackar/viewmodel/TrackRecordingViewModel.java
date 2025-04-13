package sk.matejsvrcek.znackar.viewmodel;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import sk.matejsvrcek.znackar.model.Repository;
import sk.matejsvrcek.znackar.model.LocationData;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;
import sk.matejsvrcek.znackar.utils.LocationTrackingService;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class TrackRecordingViewModel extends AndroidViewModel {
    private final Repository repository;
    private final MutableLiveData<Integer> currentTaskIdLiveData = new MutableLiveData<>();
    private final LocationLiveData locationLiveData;

    private final androidx.lifecycle.Observer<LocationData> locationObserver = this::saveTrackpoint;

    public TrackRecordingViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        locationLiveData = new LocationLiveData(application);
        locationLiveData.observeForever(locationObserver);
    }

    // Clear the observer when the ViewModel is cleared
    @Override
    protected void onCleared() {
        super.onCleared();
        locationLiveData.removeObserver(locationObserver);
    }

    // Set the current task ID
    public void setCurrentTaskId(int taskId) {
        currentTaskIdLiveData.postValue(taskId);
    }

    public LiveData<Integer> getCurrentTaskId() {
        return currentTaskIdLiveData;
    }

    // Insert a task into the DB, sets in within the ViewModel and tells the Activity
    public void insertTask(Task task, Repository.InsertTaskCallback callback) {
        repository.insertTask(task, id -> {
            setCurrentTaskId(id);
            callback.onTaskInserted(id);
        });
    }
    //Sets the task as completed in the DB
    public void setTaskAsCompleted(int taskId) {
        if (taskId != -1) {
            repository.setTaskAsCompleted(taskId);
        }
    }
    //Gets the trackpoints and waypoints for the current task
    public LiveData<List<Trackpoint>> getSavedTrackPoints() {
        return Transformations.switchMap(currentTaskIdLiveData, taskId -> {
            if (taskId != null && taskId != -1) {
                return repository.getTrackPointsForTask(taskId);
            } else {
                return new MutableLiveData<>(new ArrayList<>());
            }
        });
    }

    public LiveData<List<Waypoint>> getSavedWaypoints() {
        return Transformations.switchMap(currentTaskIdLiveData, taskId -> {
            if (taskId != null && taskId != -1) {
                return repository.getWaypointsForTask(taskId);
            } else {
                return new MutableLiveData<>(new ArrayList<>());
            }
        });
    }

    //Saves the current trackpoint received from the LocationTrackingService
    public void saveTrackpoint(LocationData locationData) {
        Integer taskId = currentTaskIdLiveData.getValue();
        if (taskId == null || taskId == -1) return;

        GeoPoint point = locationData.getGeoPoint();
        Trackpoint tp = new Trackpoint();
        tp.taskId = taskId;
        tp.latitude = point.getLatitude();
        tp.longitude = point.getLongitude();
        tp.altitude = point.getAltitude();
        tp.timestamp = System.currentTimeMillis();

        repository.insertTrackPoint(tp);
    }

    //Saves the waypoint, if the user decides to create it
    public void saveWaypoint(GeoPoint point, String description) {
        Integer taskId = currentTaskIdLiveData.getValue();
        if (taskId == null || taskId == -1) return;

        Waypoint wp = new Waypoint();
        wp.taskId = taskId;
        wp.latitude = point.getLatitude();
        wp.longitude = point.getLongitude();
        wp.altitude = point.getAltitude();
        wp.description = description;
        wp.timestamp = System.currentTimeMillis();

        repository.insertWaypoint(wp);
    }

    public LiveData<LocationData> getLocationLiveData() {
        return locationLiveData;
    }

    //Starts the LocationTrackingService
    public void startLocationTrackingService() {
        Context context = getApplication();
        Integer taskId = currentTaskIdLiveData.getValue();
        Log.d("TaskIdNotification", "taskId: " + taskId);
        if (taskId == null || taskId == -1) return;

        Intent serviceIntent = new Intent(context, LocationTrackingService.class);
        serviceIntent.putExtra("taskId", taskId);
        context.startForegroundService(serviceIntent);
    }

    //Stops the LocationTrackingService
    public void stopLocationTrackingService() {
        Context context = getApplication();
        Intent serviceIntent = new Intent(context, LocationTrackingService.class);
        context.stopService(serviceIntent);
    }

    public static class LocationLiveData extends LiveData<LocationData> {
        private final Context context;

        // BroadcastReceiver to receive location updates
        private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("sk.matejsvrcek.znackar.LOCATION_UPDATE".equals(intent.getAction())) {
                    double lat = intent.getDoubleExtra("lat", 0);
                    double lng = intent.getDoubleExtra("lng", 0);
                    double alt = intent.getDoubleExtra("alt", 0);
                    float bearing = intent.getFloatExtra("bearing", 0f);
                    setValue(new LocationData(new GeoPoint(lat, lng, alt), bearing));
                }
            }
        };

        public LocationLiveData(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected void onActive() {
            super.onActive();
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    locationReceiver, new IntentFilter("sk.matejsvrcek.znackar.LOCATION_UPDATE"));
        }

        @Override
        protected void onInactive() {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(locationReceiver);
            super.onInactive();
        }
    }
}