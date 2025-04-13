package sk.matejsvrcek.znackar.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.viewmodel.LibraryViewModel;
import java.util.ArrayList;
import java.util.List;

public class PhotoTaskFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_NAME = "task_name";

    private int taskId;
    private String taskName;

    private TextView textTaskName;
    private RecyclerView recyclerViewPhotos;
    private Button buttonExport;
    private Button buttonDelete;
    private PhotoAdapter photoAdapter;

    private LibraryViewModel viewModel;

    // Creates a new instance of the fragment with the provided task ID and name.

    public static PhotoTaskFragment newInstance(String taskName, int taskId) {
        PhotoTaskFragment fragment = new PhotoTaskFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_NAME, taskName);
        fragment.setArguments(args);
        return fragment;
    }

    public PhotoTaskFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            taskId = getArguments().getInt(ARG_TASK_ID);
            taskName = getArguments().getString(ARG_TASK_NAME);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        viewModel.initPhotos(taskId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_task, container, false);
        textTaskName = view.findViewById(R.id.text_task_name);
        recyclerViewPhotos = view.findViewById(R.id.recycler_view_photos);
        buttonExport = view.findViewById(R.id.button_export_photo);
        buttonDelete = view.findViewById(R.id.button_delete);

        textTaskName.setText(taskName);

        recyclerViewPhotos.setLayoutManager(new LinearLayoutManager(getContext()));
        photoAdapter = new PhotoAdapter(new ArrayList<Photo>());
        recyclerViewPhotos.setAdapter(photoAdapter);

        // Setup for the Delete button to remove the task and its associated photos
        buttonDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Odstrániť úlohu")
                    .setMessage("Naozaj chcete odstrániť túto úlohu?")
                    .setPositiveButton("Áno", (dialog, which) -> {
                        List<Task> currentTasks = viewModel.getTasks().getValue();
                        Task currentTask = null;
                        if (currentTasks != null) {
                            for (Task t : currentTasks) {
                                if (t.taskId == taskId) {
                                    currentTask = t;
                                    break;
                                }
                            }
                        }
                        if (currentTask == null) {
                            Toast.makeText(getContext(), "Úloha nebola nájdená", Toast.LENGTH_SHORT).show();
                        } else {
                            viewModel.deleteTask(currentTask);
                            Toast.makeText(getContext(), "Úloha odstránená", Toast.LENGTH_SHORT).show();
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    })
                    .setNegativeButton("Nie", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        // When the Export button is pressed, show a dialog to choose export method.
        buttonExport.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Export fotografií")
                    .setItems(new String[]{"Export do zariadenia", "Export zdieľaním"}, (dialog, which) -> {
                        if (which == 0) {
                            viewModel.exportPhotosToDevice(taskName);
                            Toast.makeText(getContext(), "Exportujem fotky do zariadenia", Toast.LENGTH_SHORT).show();
                        } else if (which == 1) {
                            viewModel.exportPhotosViaShare(taskName);
                        }
                    })
                    .show();
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            if (photos == null || photos.isEmpty()) {
                Log.d("PhotoTaskFragment", "No photos returned for task " + taskId);
            } else {
                Log.d("PhotoTaskFragment", "Found " + photos.size() + " photos for task " + taskId);
            }
            photoAdapter.setPhotos(photos);
        });

        viewModel.getShareIntentEvent().observe(getViewLifecycleOwner(), intent -> {
            if (intent != null) {
                startActivity(Intent.createChooser(intent, "Zdieľať súbor"));
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setVisibility(View.VISIBLE);
            }
        }
    }
}
