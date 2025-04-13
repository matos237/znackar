package sk.matejsvrcek.znackar.utils;

import android.content.Context;

import java.io.File;

public class CacheUtils {

    public static void cleanCacheFiles(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();

        if (files != null) {
            long sixHoursMillis = 6 * 60 * 60 * 1000;
            long now = System.currentTimeMillis();

            for (File file : files) {
                if ((file.getName().endsWith(".gpx") || file.getName().endsWith(".zip"))
                        && now - file.lastModified() > sixHoursMillis) {
                    file.delete();
                }
            }
        }
    }
}
//Simple cache clearing class used to clear everything in the cache that is older than 6 hours,
// thus making sure old photos and GPX files don't remain in the cache for long