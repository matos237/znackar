package sk.matejsvrcek.znackar.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import sk.matejsvrcek.znackar.model.Repository;
import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.utils.SharedPreferencesManager;

import java.util.List;
import java.util.function.Consumer;
import java.text.Normalizer;

public class PhotoDocumentationViewModel extends AndroidViewModel {
    private Repository repository;
    private final MediatorLiveData<List<Photo>> photos = new MediatorLiveData<>();

    private int taskId;
    private String currentTaskName = "";

    // Navigation events to signal the Activity to navigate to a new fragment.
    public enum NavigationEvent {
        PHOTO_BEFORE_AFTER,
        PHOTO_MULTIPLE
    }

    private final MutableLiveData<NavigationEvent> navigationEvent = new MutableLiveData<>();

    public PhotoDocumentationViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
    }

    //Returns live data of photos

    public LiveData<List<Photo>> getPhotos() {
        return photos;
    }

    //Sets the current taskId and updates the photos LiveData

    public void setCurrentTaskId(int taskId) {
        this.taskId = taskId;
        LiveData<List<Photo>> repositoryPhotos = repository.getPhotosForTask(taskId);
        photos.addSource(repositoryPhotos, photos::setValue);
    }

    // Uses SharedPreferencesManager to set and get the current task name
    public void setCurrentTaskName(String taskName) {
        this.currentTaskName = taskName;
        SharedPreferencesManager.setCurrentTaskName(getApplication(), taskName);
    }

    public String getCurrentTaskName() {
        if (currentTaskName == null || currentTaskName.isEmpty()) {
            currentTaskName = SharedPreferencesManager.getCurrentTaskName(getApplication());
        }
        return currentTaskName;
    }

    // Uses SharedPreferencesManager to set and get the next photo count

    public int getNextPhotoCount() {
        return SharedPreferencesManager.getNextPhotoCount(getApplication());
    }

    public void updatePhotoCount(){
        SharedPreferencesManager.updatePhotoCount(getApplication());
    }

    public void resetPhotoCount() {
        SharedPreferencesManager.resetPhotoCount(getApplication());
    }

    //Removes diacritics from the photo name to avoid encoding issues
    private String removeDiacritics(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    //Generates the file name based on the task name, count and capture type
    public String generateFileName(String captureType, int count) {
        String taskNameNoDiacritics = removeDiacritics(getCurrentTaskName());
        String formattedCount = String.format("%02d", count);
        return formattedCount + "_" + taskNameNoDiacritics + captureType + ".jpg";
    }


    public void insertPhoto(Photo photo) {
        repository.insertPhoto(photo);
    }

    public void initPhotos(int taskId) {
        photos.removeSource(photos);
        LiveData<List<Photo>> repositoryPhotos = repository.getPhotosForTask(taskId);
        photos.addSource(repositoryPhotos, photos::setValue);
    }

    //Saves the before and after photos to the DB in one step
    public void saveBeforeAfterPhotos(String beforePath, String afterPath) {
        Photo beforePhoto = new Photo();
        beforePhoto.taskId = taskId;
        beforePhoto.filePath = beforePath;
        insertPhoto(beforePhoto);

        Photo afterPhoto = new Photo();
        afterPhoto.taskId = taskId;
        afterPhoto.filePath = afterPath;
        insertPhoto(afterPhoto);
        updatePhotoCount();
    }

    //Saves the multiple photos to the DB

    public void savePhoto(String filePath) {
        Photo photo = new Photo();
        photo.taskId = taskId;
        photo.filePath = filePath;
        insertPhoto(photo);
        updatePhotoCount();
    }

    //Inserts the new task into the DB and sets the taskId
    public void insertTask(Task task, Repository.InsertTaskCallback callback) {
        repository.insertTask(task, callback);
    }

    //Starts a new task with the given name, description and callback
    public void startTask(String taskName, String taskDescription, Consumer<Integer> onTaskCreated) {
        Task task = new Task();
        task.name = taskName;
        task.description = taskDescription;
        task.type = "photo";
        task.isCompleted = false;
        task.isExported = false;
        task.dateTime = System.currentTimeMillis();

        resetPhotoCount();
        setCurrentTaskName(taskName);

        insertTask(task, id -> {
            taskId = id;
            setCurrentTaskId(id);
            new Handler(Looper.getMainLooper()).post(() -> {
                LiveData<List<Photo>> repositoryPhotos = repository.getPhotosForTask(id);
                photos.addSource(repositoryPhotos, photos::setValue);
            });
            onTaskCreated.accept(id);
        });
    }

    //Sets the task as completed in the DB after completing the task
    public void setTaskAsCompleted() {
        if (taskId != -1) {
            repository.setTaskAsCompleted(taskId);
            Log.d("SetTaskAsCompleted", "Task with ID " + taskId + " marked as completed.");
        }
    }
    //Sets the navigation event based on the button clicked
    public void onBeforeAfterClicked() {
        navigationEvent.setValue(NavigationEvent.PHOTO_BEFORE_AFTER);
    }

    public void onMultipleClicked() {
        navigationEvent.setValue(NavigationEvent.PHOTO_MULTIPLE);
    }

    public LiveData<NavigationEvent> getNavigationEvent() {
        return navigationEvent;
    }

    public void clearNavigationEvent() {
        navigationEvent.setValue(null);
    }
}
