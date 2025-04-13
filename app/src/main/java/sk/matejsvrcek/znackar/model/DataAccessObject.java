package sk.matejsvrcek.znackar.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;

@Dao
public interface DataAccessObject {
    @Insert
    long insertTask(Task task);

    @Insert
    void insertPhoto(Photo photo);

    @Insert
    void insertTrackPoint(Trackpoint trackPoint);

    @Insert
    void insertWaypoint(Waypoint waypoint);

    //Read methods
    @Query("SELECT * FROM Task")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM Photo WHERE taskId = :taskId")
    LiveData<List<Photo>> getPhotosForTask(int taskId);

    @Query("SELECT * FROM Photo WHERE taskId = :taskId")
    List<Photo> getPhotosForExport(int taskId);

    @Query("SELECT * FROM Trackpoint WHERE taskId = :taskId")
    LiveData<List<Trackpoint>> getTrackPointsForTask(int taskId);

    @Query("SELECT * FROM Trackpoint WHERE taskId = :taskId")
    List<Trackpoint> getTrackpointsForExport(int taskId);

    @Query("SELECT * FROM Waypoint WHERE taskId = :taskId")
    List<Waypoint> getWaypointsForExport(int taskId);

    @Query("SELECT * FROM Waypoint WHERE taskId = :taskId")
    LiveData<List<Waypoint>> getWaypointsForTask(int taskId);

    @Query("SELECT * FROM Task WHERE isCompleted = 0 ORDER BY taskId DESC LIMIT 1")
    LiveData<Task> getOngoingTask();

    //Update methods
    @Query("UPDATE Task SET isCompleted = 1 WHERE taskId = :taskId")
    int setTaskAsCompleted(int taskId);

    @Query("UPDATE Task SET isExported = 1 WHERE taskId = :taskId")
    int setTaskAsExported(int taskId);

    //Methods for deletion
    @Query("DELETE FROM Photo WHERE taskId = :taskId")
    void deletePhotosForTask(int taskId);

    @Query("DELETE FROM Trackpoint WHERE taskId = :taskId")
    void deleteTrackpointsForTask(int taskId);

    @Query("DELETE FROM Waypoint WHERE taskId = :taskId")
    void deleteWaypointsForTask(int taskId);

    @Query("DELETE FROM Task WHERE taskId = :taskId")
    void deleteTask(int taskId);
}
