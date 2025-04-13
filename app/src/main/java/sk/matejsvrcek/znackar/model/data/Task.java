package sk.matejsvrcek.znackar.model.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int taskId;
    public String name;
    public String description;
    // Add the type field to distinguish between "photo" and "track" tasks
    public String type;
    public boolean isCompleted;
    public boolean isExported;
    public long dateTime;
}
