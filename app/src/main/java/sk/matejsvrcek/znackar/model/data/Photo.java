package sk.matejsvrcek.znackar.model.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index({"taskId"})},
        foreignKeys = @ForeignKey(entity = Task.class,
        parentColumns = "taskId",
        childColumns = "taskId",
        onDelete = ForeignKey.CASCADE))
public class Photo {
    @PrimaryKey(autoGenerate = true)
    public int photoId;
    public int taskId;
    public String filePath;
}
