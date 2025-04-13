package sk.matejsvrcek.znackar.ui;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.viewmodel.LibraryViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LibraryActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, TaskAdapter.MultiSelectListener {

    private RecyclerView recyclerViewTasks;
    private TaskAdapter taskAdapter;
    private LibraryViewModel viewModel;
    private View fragmentContainer;
    private Toolbar toolbar;
    private List<Task> allTasks = new ArrayList<>();
    private String filterType = "all";
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Used in every Activity to make sure the notch used in newer devices is showing properly

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        //Toolbar on top of the activity

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Knižnica");

        //Setup for the recycler view

        recyclerViewTasks = findViewById(R.id.recycler_view_tasks);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, this);
        recyclerViewTasks.setAdapter(taskAdapter);

        fragmentContainer = findViewById(R.id.fragment_container);

        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        viewModel.getTasks().observe(this, tasks -> {
            allTasks = tasks;
            applyFilter();
        });

        //Used to manage fragments within the activity

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                fragmentContainer.setVisibility(View.GONE);
                recyclerViewTasks.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);
        return true;
    }

    // Used to filter the tasks shown in the recycler view
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_filter_all) {
            filterType = "all";
            applyFilter();
            return true;
        } else if (id == R.id.menu_filter_photo) {
            filterType = "photo";
            applyFilter();
            return true;
        } else if (id == R.id.menu_filter_track) {
            filterType = "track";
            applyFilter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Also used to filter the tasks shown in the recycler view
    private void applyFilter() {
        List<Task> filteredTasks = new ArrayList<>();
        String filterTitle;

        switch (filterType) {
            case "photo":
                filterTitle = "Fotografie";
                break;
            case "track":
                filterTitle = "Záznam trasy";
                break;
            case "all":
            default:
                filterTitle = "Všetky";
                break;
        }

        if ("all".equals(filterType)) {
            filteredTasks.addAll(allTasks);
        } else {
            for (Task task : allTasks) {
                if (filterType.equals(task.type)) {
                    filteredTasks.add(task);
                }
            }
        }

        // Updates the adapter and toolbar title
        taskAdapter.setTasks(filteredTasks);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Knižnica – " + filterTitle);
        }
    }


    //If the user is not in multi-select mode for deletion
    // this method will handle which fragment is supposed to be opened based on the task type
    @Override
    public void onTaskClick(Task task) {
        if (actionMode == null) {
            Fragment fragment;
            if ("photo".equals(task.type)) {
                fragment = PhotoTaskFragment.newInstance(task.name, task.taskId);
            } else if ("track".equals(task.type)) {
                fragment = TrackTaskFragment.newInstance(task.name, task.taskId);
            } else {
                return;
            }
            recyclerViewTasks.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    // If for when the user is in multi-select mode for deletion
    @Override
    public void onMultiSelectModeStarted() {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
        }
    }

    // Defines the ActionMode callback.
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_menu, menu);
            return true;
        }

        //Updates the number on top of the activity to show how many tasks are selected for deletion
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(taskAdapter.getSelectedTaskIds().size() + " selected");
            return false;
        }

        //Proceeds to delete all selected tasks
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete) {
                Set<Integer> selectedIds = taskAdapter.getSelectedTaskIds();
                for (Task task : allTasks) {
                    if (selectedIds.contains(task.taskId)) {
                        viewModel.deleteTask(task);
                    }
                }
                mode.finish();  // exit action mode
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            taskAdapter.setMultiSelectMode(false);
            actionMode = null;
        }
    };
}