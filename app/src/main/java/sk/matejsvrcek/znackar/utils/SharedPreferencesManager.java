package sk.matejsvrcek.znackar.utils;

import android.content.Context;
import android.content.SharedPreferences;

import sk.matejsvrcek.znackar.model.LocationData;

import org.osmdroid.util.GeoPoint;

public class SharedPreferencesManager {

    private static final String PREF_NAME = "AppSettings";

    // Keys used for photo documentation preferences
    private static final String KEY_TASK_NAME = "current_task_name";
    private static final String KEY_PHOTO_COUNT = "photo_capture_count";

    // Keys for settings preferences
    private static final String KEY_IMAGE_QUALITY = "image_quality";
    private static final String KEY_GPS_FREQUENCY = "gps_frequency";

    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LON = "last_lon";
    private static final String KEY_LAST_ALT = "last_alt";
    private static final String KEY_LAST_BEARING = "last_bearing";

    private SharedPreferencesManager() {
        // Private constructor to prevent instantiation
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Photo documentation related methods

    public static void setCurrentTaskName(Context context, String taskName) {
        getSharedPreferences(context).edit().putString(KEY_TASK_NAME, taskName).apply();
    }

    public static String getCurrentTaskName(Context context) {
        return getSharedPreferences(context).getString(KEY_TASK_NAME, "");
    }

    public static int getNextPhotoCount(Context context) {
        return getSharedPreferences(context).getInt(KEY_PHOTO_COUNT, 1);
    }

    public static void updatePhotoCount(Context context) {
        int currentCount = getNextPhotoCount(context);
        getSharedPreferences(context).edit().putInt(KEY_PHOTO_COUNT, currentCount + 1).apply();
    }

    public static void resetPhotoCount(Context context) {
        getSharedPreferences(context).edit().putInt(KEY_PHOTO_COUNT, 1).apply();
    }


    public static void setImageQuality(Context context, String imageQuality) {
        getSharedPreferences(context).edit().putString(KEY_IMAGE_QUALITY, imageQuality).apply();
    }

    public static String getImageQuality(Context context) {
        return getSharedPreferences(context).getString(KEY_IMAGE_QUALITY, "Low"); // default value
    }

    // Track recording related methods

    public static void setGpsFrequency(Context context, String gpsFrequency) {
        getSharedPreferences(context).edit().putString(KEY_GPS_FREQUENCY, gpsFrequency).apply();
    }

    public static String getGpsFrequency(Context context) {
        return getSharedPreferences(context).getString(KEY_GPS_FREQUENCY, "Walking"); // default value
    }

    public static void setLastLocation(Context context, LocationData location) {
        getSharedPreferences(context).edit()
                .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(KEY_LAST_LON, Double.doubleToRawLongBits(location.getLongitude()))
                .putLong(KEY_LAST_ALT, Double.doubleToRawLongBits(location.getAltitude()))
                .putFloat(KEY_LAST_BEARING, location.getBearing())
                .apply();
    }

    public static GeoPoint getLastGeoPoint(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (!prefs.contains(KEY_LAST_LAT) || !prefs.contains(KEY_LAST_LON)) return null;
        double lat = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LAT, 0));
        double lon = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LON, 0));
        double alt = Double.longBitsToDouble(prefs.getLong(KEY_LAST_ALT, 0));
        return new GeoPoint(lat, lon, alt);
    }

    public static float getLastBearing(Context context) {
        return getSharedPreferences(context).getFloat(KEY_LAST_BEARING, 0f);
    }
}
