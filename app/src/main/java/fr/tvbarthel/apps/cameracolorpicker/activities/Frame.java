package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Frame {

    Frame(int  fid, String inColor, String outColor, int inColorBw, int outColorBw, double distance) {
        this.fid = fid;
        this.inColor = inColor;
        this.outColor = outColor;
        this.inColorBw = inColorBw;
        this.outColorBw = outColorBw;
        this.distance = distance;
    }

    @PrimaryKey
    public int fid;

    @ColumnInfo(name = "in_color")
    public String inColor;

    @ColumnInfo(name = "out_color")
    public String outColor;

    @ColumnInfo(name = "in_color_bw")
    public int inColorBw;

    @ColumnInfo(name = "out_color_bw")
    public int outColorBw;

    @ColumnInfo(name = "distance")
    public double distance;

}