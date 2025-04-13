package sk.matejsvrcek.znackar.viewmodel;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.Repository;
import sk.matejsvrcek.znackar.ui.PhotoDocumentationActivity;
import sk.matejsvrcek.znackar.ui.TrackRecordingActivity;

public class MainViewModel extends AndroidViewModel {
    private Repository repository;

    Task ongoingTask;
    private LiveData<Task> ongoingTaskLiveData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        ongoingTaskLiveData = repository.getOngoingTask();
    }

    //Returns the ongoing task, if one is unfinished
    public LiveData<Task> getOngoingTask() {
        return ongoingTaskLiveData;
    }

    //Sets the task as completed in the DB if the user chooses the option

    public void setTaskAsCompleted() {
        if (ongoingTaskLiveData == null || ongoingTaskLiveData.getValue() == null) {
            Log.w("MainViewModel", "setTaskAsCompleted: No ongoing task found.");
            return; // Prevent crash if no task is ongoing
        }
        ongoingTask = ongoingTaskLiveData.getValue();
        repository.setTaskAsCompleted(ongoingTask.taskId);
    }

    //Returns the navigation intent based on the type of the ongoing task

    public Intent getNavigationIntent() {
        if (getApplication() == null) {
            Log.w("MainViewModel", "getNavigationIntent: ViewModel is no longer active.");
            return null; // Prevent memory leak
        }

        ongoingTask = ongoingTaskLiveData.getValue();
        if (ongoingTask == null) {
            return null;
        }

        Log.d("MainViewModel", "getNavigationIntent: task type: " + ongoingTask.type);
        Log.d("TaskId", "getNavigationIntent: task id: " + ongoingTask.taskId);

        Class<?> destinationActivity;
        if ("photo".equals(ongoingTask.type)) {
            destinationActivity = PhotoDocumentationActivity.class;
        } else if ("track".equals(ongoingTask.type)) {
            destinationActivity = TrackRecordingActivity.class;
        } else {
            return null;
        }

        Intent intent = new Intent(getApplication(), destinationActivity);
        intent.putExtra("taskId", ongoingTask.taskId);
        return intent;
    }

}
