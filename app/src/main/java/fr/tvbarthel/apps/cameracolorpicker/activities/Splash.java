package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Locale;

import fr.tvbarthel.apps.cameracolorpicker.R;

public class Splash extends Activity {

    protected String[] PERMISSIONS;
    protected Thread background;
    protected ProgressBar progressBar;
    protected SharedPreferences sharedpreferences;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    background.start();
                    Log.v("tag","permission given");
                }
                else {
                    Toast.makeText(getApplicationContext(),"Please grant permissions to the app", Toast.LENGTH_LONG).show();
                    Log.v("tag","permission not given");
                    finish();
                }
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        Locale locale = new Locale(sharedpreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.activity_splash);
        PERMISSIONS = new String[]{
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
        };
        progressBar = findViewById(R.id.progress);
        background = new Thread() {
            public void run() {
                try {
                    for (int i=0; i<100; i++) {
                        progressBar.setProgress(i);
                        sleep(30);
                    }
                    Intent i=new Intent(getBaseContext(),Home.class);
                    startActivity(i);
                    finish();
                } catch (Exception e) { }
            }
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS,1);
        }
        else {
            Log.v("tag", "already permitted");
            background.start();
        }
    }
}
