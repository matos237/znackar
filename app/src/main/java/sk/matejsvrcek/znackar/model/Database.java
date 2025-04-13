package sk.matejsvrcek.znackar.model;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import sk.matejsvrcek.znackar.model.data.Photo;
import sk.matejsvrcek.znackar.model.data.Task;
import sk.matejsvrcek.znackar.model.data.Trackpoint;
import sk.matejsvrcek.znackar.model.data.Waypoint;

@androidx.room.Database(entities = {Task.class, Photo.class, Trackpoint.class, Waypoint.class}, version = 11, exportSchema = false)
public abstract class Database extends RoomDatabase {
    public abstract DataAccessObject appDao();

    private static volatile Database INSTANCE;

    public static Database getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized(Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    Database.class, "app_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
