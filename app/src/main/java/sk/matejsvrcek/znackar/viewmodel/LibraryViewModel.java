package sk.matejsvrcek.znackar.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import sk.matejsvrcek.znackar.model.Repository;
import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LibraryViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<List<Task>> tasks;
    private final MutableLiveData<Integer> currentPhotoTaskId = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentTrackTaskId = new MutableLiveData<>();
    private final MutableLiveData<Intent> shareIntentEvent = new MutableLiveData<>();

    // Constructor
    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        tasks = repository.getAllTasks();
    }

    // Getters
    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    public LiveData<Intent> getShareIntentEvent() {
        return shareIntentEvent;
    }

    public void initPhotos(int taskId) {
        currentPhotoTaskId.setValue(taskId);
    }

    //Sets the current task id
    public void setCurrentTaskId(int taskId) {
        currentTrackTaskId.setValue(taskId);
    }

    // Getters for LiveData of photos, trackpoints and waypoints, used by the fragments

    public LiveData<List<Photo>> getPhotos() {
        return Transformations.switchMap(currentPhotoTaskId, taskId -> {
            if (taskId != null && taskId != -1) return repository.getPhotosForTask(taskId);
            else return new MutableLiveData<>(new ArrayList<>());
        });
    }

    public LiveData<List<Trackpoint>> getSavedTrackpoints() {
        return Transformations.switchMap(currentTrackTaskId, taskId -> {
            if (taskId != null && taskId != -1) return repository.getTrackPointsForTask(taskId);
            else return new MutableLiveData<>(new ArrayList<>());
        });
    }

    public LiveData<List<Waypoint>> getSavedWaypoints() {
        return Transformations.switchMap(currentTrackTaskId, taskId -> {
            if (taskId != null && taskId != -1) return repository.getWaypointsForTask(taskId);
            else return new MutableLiveData<>(new ArrayList<>());
        });
    }

    //Deletes a task from the DB thru the repository

    public void deleteTask(Task task) {
        new Thread(() -> {
            int taskId = task.taskId;
            if ("photo".equals(task.type)) {
                List<Photo> photos = repository.getPhotosForDeletion(taskId);
                if (photos != null) {
                    for (Photo photo : photos) {
                        File file = new File(photo.filePath);
                        if (file.exists()) file.delete();
                    }
                }
                repository.deletePhotosForTask(taskId);
            } else if ("track".equals(task.type)) {
                repository.deleteTrackDataForTask(taskId);
            }
            repository.deleteTask(taskId);
        }).start();
    }

    //Decides whether to export to device or via share
    public void exportTrackToDevice(Task task) {
        exportTrack(task, false);
    }

    public void exportTrackViaShare(Task task) {
        exportTrack(task, true);
    }

    //Used to export photos via share, calls the repository to get the photos for export,
    // then proceeds to name the file accordingly and makes a .zip with all the pics inside,
    // proceeds to call the share intent
    public void exportPhotosViaShare(String taskName) {
        Integer taskId = currentPhotoTaskId.getValue();
        if (taskId == null || taskId == -1) return;

        repository.getPhotosForExport(taskId, photos -> {
            if (photos == null || photos.isEmpty()) return;
            new Thread(() -> {
                try {
                    String timeStamp = generateTimeStamp(taskId);
                    String zipFileName = removeDiacritics(taskName) + "_" + timeStamp + ".zip";
                    File zipFile = new File(getApplication().getCacheDir(), zipFileName);
                    if (zipFile.exists()) zipFile.delete();
                    zipPhotos(photos, zipFile);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/zip");
                    Uri contentUri = FileProvider.getUriForFile(getApplication(), "sk.matejsvrcek.znackar.fileprovider", zipFile);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntentEvent.postValue(shareIntent);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> shareIntentEvent.postValue(null), 500);
                    repository.setTaskAsExported(taskId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }


    //Exports the photos to the device, similiarly gets the photos from the DB,
    // but instead of zipping them simply moves them to the shared storage of the device,
    // there are 2 methods present, one of them for newer Android versions using MediaStore API and
    // older devices simply copy the pictures to the shared storage
    public void exportPhotosToDevice(String taskName) {
        Integer taskId = currentPhotoTaskId.getValue();
        if (taskId == null || taskId == -1) return;

        repository.getPhotosForExport(taskId, photos -> {
            if (photos == null || photos.isEmpty()) return;

            new Thread(() -> {
                try {
                    String timeStamp = generateTimeStamp(taskId);
                    String folderName = "Znackar/" + removeDiacritics(taskName) + "_" + timeStamp;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        ContentResolver resolver = getApplication().getContentResolver();
                        for (Photo photo : photos) {
                            File srcFile = new File(photo.filePath);
                            if (!srcFile.exists()) continue;
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME, srcFile.getName());
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName);
                            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                try (InputStream in = new FileInputStream(srcFile);
                                     OutputStream out = resolver.openOutputStream(uri)) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                                }
                            }
                        }
                    } else {
                        File destDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName);
                        if (!destDir.exists() && !destDir.mkdirs()) return;
                        for (Photo photo : photos) {
                            File srcFile = new File(photo.filePath);
                            if (!srcFile.exists()) continue;
                            copyFile(srcFile, new File(destDir, srcFile.getName()));
                        }
                    }
                    repository.setTaskAsExported(taskId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    //Exports the track data by calling the method for generating a GPX file first and then
    // based on bool viaShare decides whether to export via share or to device, no need for
    // MediaStore API since it's just a text file
    private void exportTrack(Task task, boolean viaShare) {
        int taskId = task.taskId;
        repository.getTrackpointsForExport(taskId, trackpoints -> {
            repository.getWaypointsForExport(taskId, waypoints -> {
                if ((trackpoints == null || trackpoints.isEmpty()) && (waypoints == null || waypoints.isEmpty())) return;

                String gpxContent = generateGpxContent(task, trackpoints, waypoints);
                String timeStamp = generateTimeStamp(taskId);
                String baseFileName = removeDiacritics(task.name) + "_" + timeStamp + ".gpx";

                try {
                    File gpxFile;
                    if (viaShare) {
                        gpxFile = new File(getApplication().getCacheDir(), baseFileName);
                    } else {
                        File destDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Znackar");
                        if (!destDir.exists() && !destDir.mkdirs()) return;
                        gpxFile = new File(destDir, baseFileName);
                    }

                    try (FileOutputStream fos = new FileOutputStream(gpxFile);
                         OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                        writer.write(gpxContent);
                    }

                    if (viaShare) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/xml");
                        Uri uri = FileProvider.getUriForFile(getApplication(), "sk.matejsvrcek.znackar.fileprovider", gpxFile);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntentEvent.postValue(shareIntent);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> shareIntentEvent.postValue(null), 500);
                    }
                    repository.setTaskAsExported(taskId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    //Generates the GPX file according to GPX 1.1 spec, adds all the necessary
    // tags + optional date and time
    private String generateGpxContent(Task task, List<Trackpoint> trackpoints, List<Waypoint> waypoints) {
        SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" creator=\"Znackar\">\n");
        sb.append("  <metadata><name>").append(task.name).append("</name><time>")
                .append(iso8601.format(new Date(task.dateTime))).append("</time></metadata>\n");
        sb.append("  <trk><name>").append(task.name).append("</name><trkseg>\n");
        if (trackpoints != null) {
            for (Trackpoint tp : trackpoints) {
                sb.append("    <trkpt lat=\"").append(tp.latitude).append("\" lon=\"").append(tp.longitude).append("\">\n")
                        .append("      <ele>").append(tp.altitude).append("</ele>\n");
                if (tp.timestamp > 0) sb.append("      <time>").append(iso8601.format(new Date(tp.timestamp))).append("</time>\n");
                sb.append("    </trkpt>\n");
            }
        }
        sb.append("  </trkseg></trk>\n");
        if (waypoints != null) {
            for (Waypoint wp : waypoints) {
                sb.append("  <wpt lat=\"").append(wp.latitude).append("\" lon=\"").append(wp.longitude).append("\">\n")
                        .append("    <ele>").append(wp.altitude).append("</ele>\n");
                if (wp.timestamp > 0) sb.append("    <time>").append(iso8601.format(new Date(wp.timestamp))).append("</time>\n");
                if (wp.description != null && !wp.description.isEmpty()) sb.append("    <desc>").append(wp.description).append("</desc>\n");
                sb.append("  </wpt>\n");
            }
        }
        sb.append("</gpx>");
        return sb.toString();
    }

    //Method used for zipping the photos when exporting via share

    private void zipPhotos(List<Photo> photos, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            byte[] buffer = new byte[1024];
            for (Photo photo : photos) {
                File file = new File(photo.filePath);
                if (!file.exists()) continue;
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    zos.closeEntry();
                }
            }
        }
    }

    //Used to copy files to the shared storage

    private void copyFile(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) fos.write(buffer, 0, len);
        }
    }

    //Removes diacritics from the task name to avoid encoding issues when creating folders/files

    private String removeDiacritics(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    //Generates a timestamp for the file names in a user-friendly format
    private String generateTimeStamp(int taskId) {
        List<Task> currentTasks = tasks.getValue();
        if (currentTasks != null) {
            for (Task t : currentTasks) {
                if (t.taskId == taskId && t.dateTime > 0) {
                    return new SimpleDateFormat("dd_MM_yyyy_HH_mm", Locale.getDefault()).format(t.dateTime);
                }
            }
        }
        return new SimpleDateFormat("dd_MM_yyyy_HH_mm", Locale.getDefault()).format(new Date());
    }
}