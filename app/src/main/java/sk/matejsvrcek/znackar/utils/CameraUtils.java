package sk.matejsvrcek.znackar.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

public class CameraUtils {

    //Creates the image file for the camera
    public static File createImageFile(Context context, String fileName) {
        File storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return new File(storageDir, fileName);
    }

    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    //Returns the display name of the image, leaving out the .jpg extension
    public static String getDisplayName(String filePath) {
        if (filePath == null) return "";
        String fileName = new File(filePath).getName();
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return fileName;
    }


    //Returns the image compression quality based on the selected option in the settings
    public static int getImageCompressionQuality(Context context) {
        String quality = SharedPreferencesManager.getImageQuality(context);
        switch (quality) {
            case "High":
                return 100;
            case "Medium":
                return 75;
            case "Low":
                return 50;
            default:
                return 50;
        }
    }
}
