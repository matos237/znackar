package sk.matejsvrcek.znackar.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import sk.matejsvrcek.znackar.R;
import sk.matejsvrcek.znackar.model.data.Photo;

import java.io.File;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private List<Photo> photos;
    private Context context;

    public PhotoAdapter(List<Photo> photos) {
        this.photos = photos;
    }

    //Creates the ViewHolder for the recycler view

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_list, parent, false);
        return new PhotoViewHolder(view);
    }

    //Binds the data to the ViewHolder

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        int reversedPosition = photos.size() - 1 - position;
        Photo photo = photos.get(reversedPosition);

        String fileName = new File(photo.filePath).getName();
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        holder.textViewName.setText(fileName);

        Glide.with(context)
                .load(new File(photo.filePath))
                .into(holder.imageViewPhoto);
    }


    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    //Defines the ViewHolder for the recycler view

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        ImageView imageViewPhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewPhotoName);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
        }
    }
}