package sk.matejsvrcek.znackar.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;
import sk.matejsvrcek.znackar.utils.FreemapTileSource;
import sk.matejsvrcek.znackar.viewmodel.LibraryViewModel;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class TrackTaskFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_NAME = "task_name";

    private int taskId;
    private String taskName;

    private TextView textTaskName;
    private MapView mapView;
    private Button buttonExport;
    private Button buttonDelete;

    private LibraryViewModel viewModel;

    // Method to create a new instance of this fragment

    public static TrackTaskFragment newInstance(String taskName, int taskId) {
        TrackTaskFragment fragment = new TrackTaskFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_NAME, taskName);
        fragment.setArguments(args);
        return fragment;
    }

    public TrackTaskFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            taskId = getArguments().getInt(ARG_TASK_ID);
            taskName = getArguments().getString(ARG_TASK_NAME);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        viewModel.setCurrentTaskId(taskId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_task, container, false);
        textTaskName = view.findViewById(R.id.text_task_name);
        mapView = view.findViewById(R.id.map_view);
        buttonExport = view.findViewById(R.id.button_export_track);
        buttonDelete = view.findViewById(R.id.button_delete);

        textTaskName.setText(taskName);

        // Setup for the map appearance
        mapView.setTileSource(new FreemapTileSource());
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.getController().setZoom(17.0);

        // Delete button logic
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



        // Export button logic
        buttonExport.setOnClickListener(v -> {
            String[] options = {"Export do zariadenia", "Export vyzdieľaním"};
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Export GPS záznamu")
                    .setItems(options, (dialog, which) -> {
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
                            Toast.makeText(getContext(), "Úloha nenájdená", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (which == 0) {
                            viewModel.exportTrackToDevice(currentTask);
                            Toast.makeText(getContext(), "Záznam vyexportovaný do zariadenia", Toast.LENGTH_SHORT).show();
                        } else if (which == 1) {
                            viewModel.exportTrackViaShare(currentTask);
                        }
                    })
                    .show();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mapView.getOverlays().clear();

        viewModel.getSavedTrackpoints().observe(getViewLifecycleOwner(), trackpoints -> {
            if (trackpoints != null && !trackpoints.isEmpty()) {
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (Trackpoint tp : trackpoints) {
                    geoPoints.add(new GeoPoint(tp.latitude, tp.longitude));
                }
                //Polyline setup
                Polyline polyline = new Polyline();
                polyline.setPoints(geoPoints);
                polyline.getOutlinePaint().setStrokeWidth(8f);
                polyline.getOutlinePaint().setColor(Color.BLUE);
                mapView.getOverlays().add(polyline);

                // Start marker for the first trackpoint
                Marker startMarker = new Marker(mapView);
                startMarker.setPosition(geoPoints.get(0));
                startMarker.setTitle("Štart");
                startMarker.setIcon(getResources().getDrawable(R.drawable.ic_flag_green, null));
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                startMarker.setInfoWindow(new WaypointInfoWindow(mapView));
                mapView.getOverlays().add(startMarker);

                // End marker for the last trackpoint
                Marker endMarker = new Marker(mapView);
                endMarker.setPosition(geoPoints.get(geoPoints.size() - 1));
                endMarker.setTitle("Koniec");
                endMarker.setIcon(getResources().getDrawable(R.drawable.ic_flag_black, null));
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                endMarker.setInfoWindow(new WaypointInfoWindow(mapView));
                mapView.getOverlays().add(endMarker);

                mapView.getController().setCenter(geoPoints.get(0));
            }
            mapView.invalidate();
        });
        //Returns the saved waypoints from the DB, then proceeds to draw them
        viewModel.getSavedWaypoints().observe(getViewLifecycleOwner(), waypoints -> {
            if (waypoints != null) {
                for (Waypoint wp : waypoints) {
                    Marker marker = new Marker(mapView);
                    GeoPoint position = new GeoPoint(wp.latitude, wp.longitude);
                    marker.setPosition(position);
                    marker.setTitle(wp.description);

                    String snippet = "Lat: " + wp.latitude + "\n" +
                            "Lon: " + wp.longitude + "\n" +
                            "Alt: " + wp.altitude + " m";

                    marker.setSnippet(snippet);
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_red, null));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setInfoWindow(new WaypointInfoWindow(mapView));
                    marker.setOnMarkerClickListener((m, mapView) -> {
                        m.showInfoWindow();
                        return true;
                    });

                    mapView.getOverlays().add(marker);
                }
                mapView.invalidate();
            }
        });

        viewModel.getShareIntentEvent().observe(getViewLifecycleOwner(), intent -> {
            if (intent != null) {
                startActivity(Intent.createChooser(intent, "Zdieľanie súboru"));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
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
        mapView.onPause();
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setVisibility(View.VISIBLE);
            }
        }
    }
}
