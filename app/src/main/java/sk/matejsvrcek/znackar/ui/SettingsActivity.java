package sk.matejsvrcek.znackar.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.utils.SharedPreferencesManager;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerImageQuality, spinnerGpsFrequency;
    private Button btnSave;

    private final Map<String, String> gpsFrequencyMap = new HashMap<>();
    private final Map<String, String> gpsFrequencyReverseMap = new HashMap<>();
    private final Map<String, String> imageQualityMap = new HashMap<>();
    private final Map<String, String> imageQualityReverseMap = new HashMap<>();

    {
        // GPS frequency mappings
        gpsFrequencyMap.put("Chôdza", "Walking");
        gpsFrequencyMap.put("Bicykel", "Cycling");
        gpsFrequencyMap.put("Auto", "Driving");

        gpsFrequencyReverseMap.put("Walking", "Chôdza");
        gpsFrequencyReverseMap.put("Cycling", "Bicykel");
        gpsFrequencyReverseMap.put("Driving", "Auto");

        // Image quality mappings
        imageQualityMap.put("Vysoká", "High");
        imageQualityMap.put("Stredná", "Medium");
        imageQualityMap.put("Nízka", "Low");

        imageQualityReverseMap.put("High", "Vysoká");
        imageQualityReverseMap.put("Medium", "Stredná");
        imageQualityReverseMap.put("Low", "Nízka");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Used in every Activity to make sure the notch used in newer devices is showing properly

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        spinnerImageQuality = findViewById(R.id.spinnerImageQuality);
        spinnerGpsFrequency = findViewById(R.id.spinnerGpsFrequency);
        btnSave = findViewById(R.id.btnSaveSettings);

        // Setup for the spinners

        ArrayAdapter<CharSequence> qualityAdapter = ArrayAdapter.createFromResource(
                this, R.array.image_quality_array, android.R.layout.simple_spinner_item);
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImageQuality.setAdapter(qualityAdapter);

        ArrayAdapter<CharSequence> gpsAdapter = ArrayAdapter.createFromResource(
                this, R.array.gps_frequency_array, android.R.layout.simple_spinner_item);
        gpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGpsFrequency.setAdapter(gpsAdapter);

        // Pre-select saved settings
        String currentQualityEng = SharedPreferencesManager.getImageQuality(this);
        String currentFrequencyEng = SharedPreferencesManager.getGpsFrequency(this);

        setSpinnerSelection(spinnerImageQuality, imageQualityReverseMap.get(currentQualityEng));
        setSpinnerSelection(spinnerGpsFrequency, gpsFrequencyReverseMap.get(currentFrequencyEng));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedQualitySk = spinnerImageQuality.getSelectedItem().toString();
                String selectedFrequencySk = spinnerGpsFrequency.getSelectedItem().toString();

                // Map Slovak to English before saving
                String qualityEng = imageQualityMap.get(selectedQualitySk);
                String frequencyEng = gpsFrequencyMap.get(selectedFrequencySk);

                SharedPreferencesManager.setImageQuality(SettingsActivity.this, qualityEng);
                SharedPreferencesManager.setGpsFrequency(SettingsActivity.this, frequencyEng);

                finish();
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }
}
