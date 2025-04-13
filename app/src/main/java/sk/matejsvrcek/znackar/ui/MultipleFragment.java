package sk.matejsvrcek.znackar.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.utils.CameraUtils;
import sk.matejsvrcek.znackar.viewmodel.PhotoDocumentationViewModel;

import java.io.File;
import java.io.FileOutputStream;

public class MultipleFragment extends Fragment {

    private RecyclerView recyclerViewPhotos;
    private Button btnTakePicture, btnStop;
    private PhotoDocumentationViewModel viewModel;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri currentPhotoUri;
    private String currentPhotoFilePath;

    private PhotoAdapter photoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multiple, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerViewPhotos = view.findViewById(R.id.recyclerViewPhotos);
        btnTakePicture = view.findViewById(R.id.btnCapture2);
        btnStop = view.findViewById(R.id.btnStop2);

        viewModel = new ViewModelProvider(requireActivity()).get(PhotoDocumentationViewModel.class);

        //Used to restore the state of the Fragment in case of a configuration change

        if (savedInstanceState != null) {
            currentPhotoFilePath = savedInstanceState.getString("currentPhotoFilePath");
            String uriString = savedInstanceState.getString("currentPhotoUri");
            if (uriString != null) currentPhotoUri = Uri.parse(uriString);
        }

        //Setup for the recycler view

        photoAdapter = new PhotoAdapter(null);
        recyclerViewPhotos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPhotos.setAdapter(photoAdapter);

        viewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            photoAdapter.setPhotos(photos);
        });

        //Calls the system camera to take a picture, if successfull compresses it

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                compressCapturedImage(currentPhotoFilePath);
                viewModel.savePhoto(currentPhotoFilePath);
            } else {
                Toast.makeText(getContext(), "Fotografia nevytvorená", Toast.LENGTH_SHORT).show();
            }
        });

        //Starts the camera activity and asks the ViewModel about
        // the photoCount and to generate a file name
        btnTakePicture.setOnClickListener(v -> {
            int photoCount = viewModel.getNextPhotoCount();
            String fileName = viewModel.generateFileName("", photoCount);
            File imageFile = CameraUtils.createImageFile(requireContext(), fileName);
            currentPhotoFilePath = imageFile.getAbsolutePath();
            currentPhotoUri = CameraUtils.getUriForFile(requireContext(), imageFile);
            cameraLauncher.launch(currentPhotoUri);
        });

        //Back button

        btnStop.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    //Saves the state of the Fragment in case of a configuration change

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPhotoFilePath", currentPhotoFilePath);
        outState.putString("currentPhotoUri", currentPhotoUri != null ? currentPhotoUri.toString() : null);
    }

    //Compresses the captured image

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
