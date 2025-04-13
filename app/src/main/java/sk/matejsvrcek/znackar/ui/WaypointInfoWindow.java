package sk.matejsvrcek.znackar.ui;

import android.widget.TextView;

import sk.matejsvrcek.znackar.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class WaypointInfoWindow extends InfoWindow {

    public WaypointInfoWindow(MapView mapView) {
        super(R.layout.waypoint_info_window, mapView);
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        TextView title = mView.findViewById(R.id.info_title);
        TextView description = mView.findViewById(R.id.info_description);

        title.setText(marker.getTitle());
        description.setText(marker.getSnippet());

        mView.setOnClickListener(v -> close());
    }

    @Override
    public void onClose() {

    }
}
//Simple class used to show the small window when a waypoint flag is tapped