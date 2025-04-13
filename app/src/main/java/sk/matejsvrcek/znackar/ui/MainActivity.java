package sk.matejsvrcek.znackar.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.utils.CacheUtils;
import sk.matejsvrcek.znackar.viewmodel.MainViewModel;
import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    private LinearLayout btnPhotoDocumentation, btnTrackRecording, btnLibrary, btnSettings, btnGuide;
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CacheUtils.cleanCacheFiles(this);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);


        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Observe the ongoing task, if it's not null, show the dialog
        mainViewModel.getOngoingTask().observe(this, task -> {
            if (task != null) {
                showOngoingTaskDialog();
            }
        });

        //Calls the method to checking and requesting permissions
        checkAndRequestPermissions();

        btnPhotoDocumentation = findViewById(R.id.btnPhotoDocumentation);
        btnTrackRecording = findViewById(R.id.btnTrackRecording);
        btnLibrary = findViewById(R.id.btnLibrary);
        btnSettings = findViewById(R.id.btnSettings);
        btnGuide = findViewById(R.id.btnGuide);

        btnPhotoDocumentation.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PhotoDocumentationActivity.class)));

        btnTrackRecording.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TrackRecordingActivity.class)));

        btnLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LibraryActivity.class));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GuideActivity.class));
            }
        });
    }

    //Shows the dialog if there is an ongoing task
    private void showOngoingTaskDialog() {
        if (isFinishing() || isDestroyed()) {
            return; // Prevent WindowLeaked error
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nedokončená úloha")
                .setCancelable(false);
        builder.setMessage("Máte nedokončenú úlohu. Chcete pokračovať?");

        builder.setPositiveButton("Pokračovať", (dialog, which) -> {
            Intent intent = mainViewModel.getNavigationIntent();
            if (intent != null) {
                startActivity(intent);
                finish(); // Prevent MainActivity from staying open
            }
        });

        builder.setNegativeButton("Ukončiť", (dialog, which) -> {
            mainViewModel.setTaskAsCompleted();
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    //Checks each permission and collects them, later tells the user which ones are needed
    private void checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        }
    }


    // Handles the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            List<String> deniedPermissions = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (!deniedPermissions.isEmpty()) {
                showPermissionsSettingsDialog(deniedPermissions); // Make sure you're passing this list!
            }
        }
    }

    //Shows the dialog if there are some permissions that are needed based on the
    // list of denied permissions provided by checkAndRequestPermissions()
    private void showPermissionsSettingsDialog(List<String> deniedPermissions) {
        List<String> readableNames = new ArrayList<>();

        boolean needsCamera = deniedPermissions.contains(Manifest.permission.CAMERA);
        boolean needsFineLocation = deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION);
        boolean needsCoarseLocation = deniedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean needsBackgroundLocation = deniedPermissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        boolean needsStorage = deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean needsNotifications = deniedPermissions.contains(Manifest.permission.POST_NOTIFICATIONS);

        if (needsCamera) readableNames.add("Kamera");
        if (needsFineLocation || needsCoarseLocation) readableNames.add("Poloha");
        if (needsBackgroundLocation) readableNames.add("Poloha na pozadí");
        if (needsStorage) readableNames.add("Ukladanie do úložiska");
        if (needsNotifications) readableNames.add("Notifikácie");

        StringBuilder message = new StringBuilder("Pre správnu funkciu aplikácie sú potrebné nasledovné povolenia:\n\n");
        for (String feature : readableNames) {
            message.append("• ").append(feature).append("\n");
        }
        message.append("\nZáznam trasy si pre správnu funkciu vyžaduje mať vždy povolenú polohu.\n");
        message.append("\nPovolenia môžete udeliť v nastaveniach aplikácie.");

        new AlertDialog.Builder(this)
                .setTitle("Chýbajúce povolenia")
                .setMessage(message.toString())
                .setCancelable(false)
                .setPositiveButton("Otvoriť nastavenia", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Zrušiť", null)
                .show();
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainViewModel.getOngoingTask().removeObservers(this);
    }
}