package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;
import java.util.Locale;

import fr.tvbarthel.apps.cameracolorpicker.R;

public class Home extends AppCompatActivity {
    protected AppCompatButton runButton, historyButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);

        Locale locale = new Locale(sharedPreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        runButton = findViewById(R.id.run);
        historyButton = findViewById(R.id.history);

        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productAlert();
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getBaseContext(),History.class);
                startActivity(i);
            }
        });
        setTitle(R.string.qundal);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        boolean handled;
        switch (itemId) {
            case R.id.menu_home_activity_settings:
                Intent intent = new Intent(Home.this,Settings.class);
                startActivity(intent);
                handled = true;
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sharedPreferences.getString("changed","false").equals("true")) {
            recreate();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("changed","false");
            editor.commit();
        }
    }

    public void productAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getResources().getString(R.string.product));
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.product_alert,null);

        LinearLayout instaNGL = dialogLayout.findViewById(R.id.instaNGAL);
        LinearLayout instaIMF = dialogLayout.findViewById(R.id.instaCRP);
        LinearLayout instaGAL = dialogLayout.findViewById(R.id.instaPSA);


        builder.setView(dialogLayout);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();
        dialog.show();


        instaNGL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getBaseContext(),ColorPickerBaseActivity.class);
                startActivity(i);
                dialog.dismiss();
            }
        });

        instaIMF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        instaGAL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }
}