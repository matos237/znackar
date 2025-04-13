package sk.matejsvrcek.znackar.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.utils.CameraUtils;
import sk.matejsvrcek.znackar.viewmodel.PhotoDocumentationViewModel;

import java.io.File;
import java.io.FileOutputStream;

public class BeforeAfterFragment extends Fragment {

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ImageView imageViewBefore, imageViewAfter;
    private TextView textViewBeforeName, textViewAfterName;
    private Button btnCancel, btnCapture;
    private PhotoDocumentationViewModel viewModel;

    private enum CaptureState { BEFORE, AFTER, DONE }
    private CaptureState currentState = CaptureState.BEFORE;

    private String beforeImagePath;
    private String afterImagePath;
    private String currentPhotoFilePath;
    private Uri currentPhotoUri;
    private int photoCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_before_after, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imageViewBefore = view.findViewById(R.id.imageViewBefore);
        imageViewAfter = view.findViewById(R.id.imageViewAfter);
        textViewBeforeName = view.findViewById(R.id.textViewBeforeName);
        textViewAfterName = view.findViewById(R.id.textViewAfterName);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnCapture = view.findViewById(R.id.btnCapture);

        viewModel = new ViewModelProvider(requireActivity()).get(PhotoDocumentationViewModel.class);

        //Restores the state of the Fragment in case of a configuration change

        if (savedInstanceState != null) {
            beforeImagePath = savedInstanceState.getString("beforeImagePath");
            afterImagePath = savedInstanceState.getString("afterImagePath");
            currentPhotoFilePath = savedInstanceState.getString("currentPhotoFilePath");
            String uriString = savedInstanceState.getString("currentPhotoUri");
            if (uriString != null) currentPhotoUri = Uri.parse(uriString);
            currentState = CaptureState.valueOf(savedInstanceState.getString("currentState", CaptureState.BEFORE.name()));
            photoCount = savedInstanceState.getInt("photoCount", viewModel.getNextPhotoCount());

            if (beforeImagePath != null) {
                imageViewBefore.setImageURI(Uri.fromFile(new File(beforeImagePath)));
                textViewBeforeName.setText("Stará: " + CameraUtils.getDisplayName(beforeImagePath));
            }
            if (afterImagePath != null) {
                imageViewAfter.setImageURI(Uri.fromFile(new File(afterImagePath)));
                textViewAfterName.setText("Nová: " + CameraUtils.getDisplayName(afterImagePath));
            }

            switch (currentState) {
                case BEFORE: btnCapture.setText("Stará"); break;
                case AFTER: btnCapture.setText("Nová"); break;
                case DONE: btnCapture.setText("OK"); break;
            }
        } else {
            if (photoCount == 0) {
                photoCount = viewModel.getNextPhotoCount();
            }
            btnCapture.setText("Stará");
        }

        //Calls the system camera to take a picture, if successfull
        //calls the compressCapturedImage method, updates the UI and currentState
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                compressCapturedImage(currentPhotoFilePath);

                if (currentState == CaptureState.BEFORE) {
                    imageViewBefore.setImageURI(currentPhotoUri);
                    beforeImagePath = currentPhotoFilePath;
                    textViewBeforeName.setText("Stará: " + CameraUtils.getDisplayName(beforeImagePath));
                    currentState = CaptureState.AFTER;
                    btnCapture.setText("Nová");
                } else if (currentState == CaptureState.AFTER) {
                    imageViewAfter.setImageURI(currentPhotoUri);
                    afterImagePath = currentPhotoFilePath;
                    textViewAfterName.setText("Nová: " + CameraUtils.getDisplayName(afterImagePath));
                    currentState = CaptureState.DONE;
                    btnCapture.setText("OK");
                }
            } else {
                Toast.makeText(getContext(), "Fotografia nevytvorená", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        //This button contains the logic that the Fragment uses to
        // take and save pictures according to the 3 states (BEFORE, AFTER, DONE)

        btnCapture.setOnClickListener(v -> {
            if (currentState == CaptureState.DONE) {
                viewModel.saveBeforeAfterPhotos(beforeImagePath, afterImagePath);
                Toast.makeText(getContext(), "Fotografie boli uložené", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                try {
                    String type = (currentState == CaptureState.BEFORE) ? "S" : "N";
                    String fileName = viewModel.generateFileName(type, photoCount);
                    File imageFile = CameraUtils.createImageFile(requireContext(), fileName);
                    currentPhotoFilePath = imageFile.getAbsolutePath();
                    currentPhotoUri = CameraUtils.getUriForFile(requireContext(), imageFile);
                    cameraLauncher.launch(currentPhotoUri);
                } catch (Exception e) {
                    Log.e("BeforeAfterFragment", "Error creating image file", e);
                    Toast.makeText(getContext(), "Chyba pri vytváraní fotografie", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //Used to store the state of the Fragment in case of a configuration change
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("beforeImagePath", beforeImagePath);
        outState.putString("afterImagePath", afterImagePath);
        outState.putString("currentPhotoFilePath", currentPhotoFilePath);
        outState.putString("currentPhotoUri", currentPhotoUri != null ? currentPhotoUri.toString() : null);
        outState.putString("currentState", currentState.name());
        outState.putInt("photoCount", photoCount);
    }

    //Image compression method that calls on CameraUtils class to retrieve
    // the user's desired quality of pictures

    private void compressCapturedImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(), Uri.fromFile(imageFile));

            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    CameraUtils.getImageCompressionQuality(requireContext()),
                    out
            );
            out.flush();
            out.close();
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
