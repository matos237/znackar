package sk.matejsvrcek.znackar.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;

public class Repository {
    private DataAccessObject dataAccessObject;
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    public Repository(Context context) {
        Database db = Database.getInstance(context);
        dataAccessObject = db.appDao();
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public void insertTask(final Task task, final InsertTaskCallback callback) {
        executorService.execute(() -> {
            long id = dataAccessObject.insertTask(task);
            mainThreadHandler.post(() -> callback.onTaskInserted((int) id));
        });
    }

    public void insertPhoto(final Photo photo) {
        executorService.execute(() -> dataAccessObject.insertPhoto(photo));
    }

    public void insertTrackPoint(final Trackpoint trackPoint) {
        executorService.execute(() -> dataAccessObject.insertTrackPoint(trackPoint));
    }

    public void insertWaypoint(final Waypoint waypoint) {
        executorService.execute(() -> dataAccessObject.insertWaypoint(waypoint));
    }

    public void getPhotosForExport(final int taskId, final GetPhotosCallback callback) {
        executorService.execute(() -> {
            List<Photo> photos = dataAccessObject.getPhotosForExport(taskId);
            mainThreadHandler.post(() -> callback.onPhotosLoaded(photos));
        });
    }

    //Export methods utilizing callbacks
    public void getWaypointsForExport(final int taskId, final GetWaypointsCallback callback) {
        executorService.execute(() -> {
            List<Waypoint> waypoints = dataAccessObject.getWaypointsForExport(taskId);
            mainThreadHandler.post(() -> callback.onWaypointsLoaded(waypoints));
        });
    }

    public void getTrackpointsForExport(final int taskId, final GetTrackpointsCallback callback) {
        executorService.execute(() -> {
            List<Trackpoint> trackpoints = dataAccessObject.getTrackpointsForExport(taskId);
            mainThreadHandler.post(() -> callback.onTrackpointsLoaded(trackpoints));
        });
    }

    // LiveData queries mainly for UI updates
    public LiveData<List<Task>> getAllTasks() {
        return dataAccessObject.getAllTasks();
    }

    public LiveData<List<Photo>> getPhotosForTask(int taskId) {
        return dataAccessObject.getPhotosForTask(taskId);
    }

    public LiveData<List<Trackpoint>> getTrackPointsForTask(int taskId) {
        return dataAccessObject.getTrackPointsForTask(taskId);
    }

    public LiveData<List<Waypoint>> getWaypointsForTask(int taskId) {
        return dataAccessObject.getWaypointsForTask(taskId);
    }

    public LiveData<Task> getOngoingTask() {
        return dataAccessObject.getOngoingTask();
    }
    public void setTaskAsCompleted(final int taskId) {
        executorService.execute(() -> dataAccessObject.setTaskAsCompleted(taskId));
    }

    public void setTaskAsExported(final int taskId) {
        executorService.execute(() -> dataAccessObject.setTaskAsExported(taskId));
    }

    // Callback interfaces for asynchronous operations
    public interface InsertTaskCallback {
        void onTaskInserted(int taskId);
    }

    public interface GetPhotosCallback {
        void onPhotosLoaded(List<Photo> photos);
    }

    public interface GetWaypointsCallback {
        void onWaypointsLoaded(List<Waypoint> waypoints);
    }

    public interface GetTrackpointsCallback {
        void onTrackpointsLoaded(List<Trackpoint> trackpoints);
    }

    //Deletion methods
    public void deletePhotosForTask(final int taskId) {
        executorService.execute(() -> dataAccessObject.deletePhotosForTask(taskId));
    }

    public void deleteTrackDataForTask(final int taskId) {
        executorService.execute(() -> {
            dataAccessObject.deleteTrackpointsForTask(taskId);
            dataAccessObject.deleteWaypointsForTask(taskId);
        });
    }

    public void deleteTask(final int taskId) {
        executorService.execute(() -> dataAccessObject.deleteTask(taskId));
    }

    public List<Photo> getPhotosForDeletion(int taskId) {
        return dataAccessObject.getPhotosForExport(taskId);
    }
}
