package sk.matejsvrcek.znackar.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.viewmodel.PhotoDocumentationViewModel;

import java.util.List;

public class PhotoDocumentationActivity extends AppCompatActivity {

    private static final String TASK_ID_KEY = "taskId";
    private static final String TAG_BEFORE_AFTER = "BeforeAfterFragment";
    private static final String TAG_MULTIPLE = "MultipleFragment";

    private RecyclerView recyclerViewPhotos;
    private PhotoAdapter photoAdapter;
    private Button btnBeforeAfter, btnMultiple, btnStop;
    private PhotoDocumentationViewModel viewModel;

    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_documentation);

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        }

        // Used in every Activity to make sure the notch used in newer devices is showing properly

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        viewModel = new ViewModelProvider(this).get(PhotoDocumentationViewModel.class);

        //Used to restore the state of the Activity in case of a configuration change,
        // if taskId is provided by an Intent it loads it into the ViewModel,
        // else it shows the task input dialog to start a new task because no taskId was provided

        if (savedInstanceState != null) {
            taskId = savedInstanceState.getInt(TASK_ID_KEY, -1);
        } else if (getIntent() != null && getIntent().hasExtra("taskId")) {
            taskId = getIntent().getIntExtra("taskId", -1);
        }

        if (taskId != -1) {
            viewModel.setCurrentTaskId(taskId);
            viewModel.initPhotos(taskId);
        } else {
            showTaskInputDialog();
        }

        //Setup for the recycler view

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        recyclerViewPhotos.setLayoutManager(new LinearLayoutManager(this));
        photoAdapter = new PhotoAdapter(null);
        recyclerViewPhotos.setAdapter(photoAdapter);

        btnBeforeAfter = findViewById(R.id.btnBeforeAfter);
        btnMultiple = findViewById(R.id.btnMultiple);
        btnStop = findViewById(R.id.btnStop);

        //Used to manage fragments within the activity

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                fragmentContainer.setVisibility(View.GONE);
            } else {
                fragmentContainer.setVisibility(View.VISIBLE);
            }
        });

        //Observes the photos LiveData and updates the adapter when it changes

        viewModel.getPhotos().observe(this, new Observer<List<Photo>>() {
            @Override
            public void onChanged(List<Photo> photos) {
                photoAdapter.setPhotos(photos);
            }
        });

        //Observes the navigationEvent and navigates to the
        // appropriate fragment when it changes

        viewModel.getNavigationEvent().observe(this, navEvent -> {
            if (navEvent != null) {
                switch (navEvent) {
                    case PHOTO_BEFORE_AFTER:
                        navigateToPhotoCaptureFragment("before_after");
                        break;
                    case PHOTO_MULTIPLE:
                        navigateToPhotoCaptureFragment("multiple");
                        break;
                }
                viewModel.clearNavigationEvent();
            }
        });

        btnBeforeAfter.setOnClickListener(v -> viewModel.onBeforeAfterClicked());
        btnMultiple.setOnClickListener(v -> viewModel.onMultipleClicked());
        btnStop.setOnClickListener(v -> confirmStop());

        //Blocks the user from leaving the Activity improperly, without ending the task

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(PhotoDocumentationActivity.this, "Ukončite momentálnu úlohu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Saves the state of the Activity in case of a configuration change

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(TASK_ID_KEY, taskId);
        super.onSaveInstanceState(outState);
    }

    //Restores the state of the Activity in case of a configuration change

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getInt("taskId", -1);
        }
    }

    //Shows the dialog to start a new task

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
                    String taskType = "track";
                    viewModel.startTask(taskName, taskDescription, newTaskId -> {
                        taskId = newTaskId;
                        viewModel.setCurrentTaskId(taskId);
                        viewModel.initPhotos(taskId);
                    });
                })
                .setNegativeButton("Späť", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Shows a confirmation dialog before ending the task

    private void confirmStop() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Ukončiť trasu")
                .setMessage("Naozaj chcete ukončiť danú úlohu?")
                .setPositiveButton("Áno", (dialog, which) -> {
                    viewModel.setTaskAsCompleted();
                    Log.d("SetTaskAsCompleted", "Task with ID " + taskId + " marked as completed.");

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Nie", null)
                .show();
    }

    //Navigates to the appropriate fragment based on the mode

    private void navigateToPhotoCaptureFragment(String mode) {
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        fragmentContainer.setVisibility(View.VISIBLE);
        fragmentContainer.bringToFront();

        if ("before_after".equals(mode)) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_BEFORE_AFTER) == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new BeforeAfterFragment(), TAG_BEFORE_AFTER)
                        .addToBackStack(null)
                        .commit();
            }
        } else if ("multiple".equals(mode)) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_MULTIPLE) == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MultipleFragment(), TAG_MULTIPLE)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.getPhotos().removeObservers(this);
    }
}
