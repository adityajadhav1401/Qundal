package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface FrameDao {
    @Query("SELECT * FROM frame")
    List<Frame> getAll();

//    @Query("INSERT INTO frame VALUES (:inColor, :outColor, :inColorBw, :outColorBw, :distance)")
//    void insert(int inColor, int outColor, int inColorBw, int outColorBw, int distance);

    @Insert
    void insert(Frame frame);

    @Query("DELETE FROM frame")
    void deleteAll();

}