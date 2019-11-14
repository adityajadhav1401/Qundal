package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Locale;

import fr.tvbarthel.apps.cameracolorpicker.R;

public class Settings extends AppCompatActivity {
    LinearLayout language, bugs, rating, about;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    TextView appLanguageText;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        final String mLanguageCode = sharedpreferences.getString("language","en");
        Locale locale = new Locale(sharedpreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        sharedpreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        language = findViewById(R.id.app_language);
        bugs = findViewById(R.id.bugs);
        rating = findViewById(R.id.rating);
        about = findViewById(R.id.about);
        appLanguageText = findViewById(R.id.app_language_text);
        context = this;
        if (mLanguageCode.equals("en")) appLanguageText.setText("English (EN)");
        else if (mLanguageCode.equals("hi")) appLanguageText.setText("हिंदी (HI)");


        language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(getResources().getString(R.string.select_language));
                LayoutInflater inflater = getLayoutInflater();
                final View dialogLayout = inflater.inflate(R.layout.language_alert,null);


                if (mLanguageCode.equals("en")) ((RadioButton) dialogLayout.findViewById(R.id.en)).setChecked(true);
                else if (mLanguageCode.equals("hi")) ((RadioButton) dialogLayout.findViewById(R.id.hi)).setChecked(true);

                builder.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String language = "en";
                            RadioGroup radioGroup = dialogLayout.findViewById(R.id.language_rg);
                            int selectedLanguage = radioGroup.getCheckedRadioButtonId();
                            editor = sharedpreferences.edit();
                            if (selectedLanguage == R.id.en) {
                                editor.putString("language", "en");
                                language = "en";
                            }
                            else if (selectedLanguage == R.id.hi) {
                                language = "hi";
                                editor.putString("language", "hi");
                            }
                            editor.putString("changed", "true");
                            editor.apply();
                            Locale locale = new Locale(language);
                            Locale.setDefault(locale);
                            Configuration config = new Configuration();
                            config.locale = locale;
                            getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
                            recreate();
                        }
                    });
                builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setView(dialogLayout);
                builder.setCancelable(false);
                builder.show();
            }
        });
        setTitle(R.string.settings);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home ) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
