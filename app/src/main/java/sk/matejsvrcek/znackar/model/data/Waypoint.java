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
public class Waypoint {
    @PrimaryKey(autoGenerate = true)
    public int waypointId;
    public int taskId;
    public double latitude;
    public double longitude;
    public double altitude;
    public String description;
    public long timestamp;
}
