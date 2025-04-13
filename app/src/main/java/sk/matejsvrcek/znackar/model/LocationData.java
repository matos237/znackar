package sk.matejsvrcek.znackar.model;

import org.osmdroid.util.GeoPoint;

public class LocationData {
    private final GeoPoint geoPoint;
    private final float bearing;

    public LocationData(GeoPoint geoPoint, float bearing) {
        this.geoPoint = geoPoint;
        this.bearing = bearing;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public double getLatitude() {
        return geoPoint.getLatitude();
    }

    public double getLongitude() {
        return geoPoint.getLongitude();
    }

    public double getAltitude() {
        return geoPoint.getAltitude();
    }

    public float getBearing() {
        return bearing;
    }
}

//Custom DataClass that allows the bearing to be sent with each location update
// along with the usual GeoPoint data