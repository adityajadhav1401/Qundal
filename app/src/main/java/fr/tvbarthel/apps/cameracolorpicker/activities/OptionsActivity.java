package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.ortiz.touchview.TouchImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import fr.tvbarthel.apps.cameracolorpicker.R;

public class OptionsActivity extends AppCompatActivity {

    static protected AppCompatButton nextButton, previousButton;
    protected TouchImageView imageSwitcher;
    protected TextView imageTextView;
    protected Integer currentImageIndex = 0;
    static protected ArrayList<Integer> imageIds;
    static protected ArrayList<BitmapDrawable> images;
    static protected Integer numImages;
    protected static int CAPTURE_INTERVAL = 15;

    static protected double minDist;
    static protected int minDistIndex;
    static protected double minTime;
    static protected ArrayList<Double> minDistArray;
    static protected ArrayList<Integer> minDistIndexArray;
    static protected ArrayList<Integer> minTimeArray;
    static AppDatabase db;
    static Activity oa;

    protected String folderName;
    protected String dataText;
    protected String output;
    protected GraphView graphView;
    String fileName = "data.txt";
    String graphFileName = "graph.jpeg";
    static protected int THRESHOLD = 10;

    SharedPreferences sharedpreferences;

    @SuppressLint("StaticFieldLeak")
    public void createGraphAndImages() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                final List<Frame> data = db.frameDao().getAll();
                final LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
                final LineGraphSeries<DataPoint> minTimeSeries = new LineGraphSeries<DataPoint>();
                minDistArray = new ArrayList<>();
                minDistIndexArray = new ArrayList<>();
                minTimeArray = new ArrayList<>();

                dataText = "";
                for (int i = 0; i < data.size(); i++) {
                    series.appendData(new DataPoint(data.get(i).fid, data.get(i).distance), true, 500);
                    dataText += String.valueOf(data.get(i).fid) + ",";
                    dataText += data.get(i).inColor + ",";
                    dataText += data.get(i).outColor + ",";
                    dataText += String.valueOf(data.get(i).inColorBw) + ",";
                    dataText += String.valueOf(data.get(i).outColorBw) + ",";
                    dataText += String.valueOf(data.get(i).distance) + "\n";

                    if (i%60 == 0) {
                        minDistIndex = i;
                        minDist = data.get(i).distance;
                    }

                    if (minDist > data.get(i).distance) {
                        minDist = data.get(i).distance;
                        minDistIndex = i;
                    }

                    if (i%60 == 59) {
                        minDistArray.add(minDist);
                        minDistIndexArray.add(minDistIndex);
                    }
                }

                for (int j=0; j<minDistIndexArray.size(); j++) {
                    minDist = minDistArray.get(j);
                    minDistIndex = minDistIndexArray.get(j);

                    double sum = 0;
                    minTime = 0;
                    for (int i=minDistIndex; i<data.size() && Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1) < THRESHOLD; i++) {
                        sum += THRESHOLD-Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1);
                        minTime += data.get(i).fid * (THRESHOLD-Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1));
                    }

                    for (int i=minDistIndex; i>=0 && Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1) < THRESHOLD; i--) {
                        sum += THRESHOLD-Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1);
                        minTime += data.get(i).fid * (THRESHOLD-Math.abs(minDist-data.get(i).distance)*100/Math.max(minDist,1));
                    }

                    minTime /= Math.max(sum,1);
                    minTimeArray.add(Math.round((long)minTime));
                }

                Collections.sort(minTimeArray);
                imageIds.addAll(minTimeArray);
                numImages = imageIds.size();

                output = "";
                for (int i=0; i<minTimeArray.size(); i++) {
                    output += String.valueOf(minTimeArray.get(i));
                    if (i != minTimeArray.size()-1) output += ", ";
                }

                HashSet<Integer> set = new HashSet<>(minTimeArray);
                minTimeArray.clear();
                minTimeArray.addAll(set);

                Collections.sort(minTimeArray);

                Log.v("tag",output);

                for (int i=0; i<minTimeArray.size(); i++) {
                    double p1 = (double)minTimeArray.get(i)-0.2;
                    double p2 = (double)minTimeArray.get(i);
                    double p3 = (double)minTimeArray.get(i)+0.2;
                    Log.v("tag", String.valueOf(p1) + "  " + String.valueOf(p2) + "  " + String.valueOf(p3));
                    minTimeSeries.appendData(new DataPoint(p1,0), true, 500);
                    minTimeSeries.appendData(new DataPoint(p2,50), true, 500);
                    minTimeSeries.appendData(new DataPoint(p3,0), true, 500);
                }


                series.setColor(Color.rgb(0,255,0));
                minTimeSeries.setColor(Color.rgb(0,255,255));

                oa.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < numImages; i++) {
                            String imageName = String.valueOf(imageIds.get(i) - (imageIds.get(i) % CAPTURE_INTERVAL));
                            File imageFile = new File(getExternalFilesDir("matching_images"), imageName);
                            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), imageFile.getAbsolutePath());
                            Matrix matrix = new Matrix();
                            matrix.postRotate(90);
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), bitmapDrawable.getIntrinsicWidth(), bitmapDrawable.getIntrinsicHeight(), true);
                            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                            images.add(new BitmapDrawable(getResources(), rotatedBitmap));
                        }
                        try {
                            imageSwitcher.setImageDrawable(images.get(currentImageIndex));
                            imageTextView.setText(String.valueOf(currentImageIndex+1) + " : " + String.valueOf(imageIds.get(currentImageIndex)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        previousButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                currentImageIndex = Math.max(0, --currentImageIndex);
                                imageSwitcher.setImageDrawable(images.get(currentImageIndex));
                                imageTextView.setText(String.valueOf(currentImageIndex+1) + " : " + String.valueOf(imageIds.get(currentImageIndex)));
                            }
                        });

                        nextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                currentImageIndex = Math.min(numImages - 1, ++currentImageIndex);
                                imageSwitcher.setImageDrawable(images.get(currentImageIndex));
                                imageTextView.setText(String.valueOf(currentImageIndex+1) + " : " + String.valueOf(imageIds.get(currentImageIndex)));
                            }
                        });

                        series.setTitle("Color difference");
                        minTimeSeries.setTitle("Matching time");
                        graphView.setBackgroundColor(Color.WHITE);
                        graphView.addSeries(series);
                        graphView.addSeries(minTimeSeries);
                        graphView.getViewport().setXAxisBoundsManual(true);
                        graphView.getViewport().setMaxX(data.size());
                        graphView.setTitle(output);
                        graphView.setFitsSystemWindows(true);
                        graphView.setTitleTextSize(20);
                        graphView.getLegendRenderer().setVisible(true);
                        graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                        graphView.getLegendRenderer().setMargin(20);
                        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Time (seconds)");
                        graphView.getGridLabelRenderer().setVerticalAxisTitle("Eucledian distance (unit)");
                        graphView.getGridLabelRenderer().setPadding(20);
                        saveData();
                    }
                });

                return null;
            }
        }.execute();
    }

    public void saveData() {
        String filePath = folderName.toString();
        File dataFile = new File(getExternalFilesDir(filePath), fileName);
        File imageFile = new File(getExternalFilesDir(filePath), graphFileName);
        try {
            FileOutputStream fos = new FileOutputStream(dataFile);
            fos.write(dataText.getBytes());
            fos.close();

            Bitmap bitmap = graphView.takeSnapshot();
            fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            for (int i = 0; i < minTimeArray.size(); i++) {
                imageFile = new File(getExternalFilesDir("matching_images"),
                        String.valueOf(Math.round(minTimeArray.get(i)) - (Math.round(minTimeArray.get(i)) % OptionsActivity.CAPTURE_INTERVAL)));

                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), imageFile.getAbsolutePath());
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), bitmapDrawable.getIntrinsicWidth(), bitmapDrawable.getIntrinsicHeight(), true);
                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

                imageFile = new File(getExternalFilesDir(filePath), String.valueOf(Math.round(minTimeArray.get(i))));
                fos = new FileOutputStream(imageFile);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayGraphAndImages() {
        File defaultFolder = new File(getExternalFilesDir(folderName).toString());
        File[] files = defaultFolder.listFiles();
        for (File file : files) {
            if (!file.getName().equals(fileName) && !file.getName().equals(graphFileName)) {
                imageIds.add(Integer.valueOf(file.getName()));
            }
        }
        Collections.sort(imageIds);
        numImages = imageIds.size();

        for (int i = 0; i < numImages; i++) {
            File imageFile = new File(getExternalFilesDir(folderName),imageIds.get(i).toString());
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), imageFile.getAbsolutePath());
            images.add(bitmapDrawable);
        }

        try {
            imageSwitcher.setImageDrawable(images.get(currentImageIndex));
            imageTextView.setText(String.valueOf(imageIds.get(currentImageIndex)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentImageIndex = Math.max(0, --currentImageIndex);
                imageSwitcher.setImageDrawable(images.get(currentImageIndex));
                imageTextView.setText(String.valueOf(imageIds.get(currentImageIndex)));
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentImageIndex = Math.min(numImages - 1, ++currentImageIndex);
                imageSwitcher.setImageDrawable(images.get(currentImageIndex));
                imageTextView.setText(String.valueOf(imageIds.get(currentImageIndex)));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        boolean handled;
        switch (itemId) {
            case R.id.menu_options_activity_graph:
                Intent intent = new Intent(OptionsActivity.this,GraphActivity.class);
                Bundle b = new Bundle();
                b.putString("folderName",folderName);
                intent.putExtras(b);
                startActivity(intent);
                handled = true;
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        Locale locale = new Locale(sharedpreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.activity_options);
        db = Room.databaseBuilder(getApplicationContext(),AppDatabase.class, "frame").build();
        imageSwitcher = findViewById(R.id.image_switcher);
        nextButton = findViewById(R.id.next_button);
        previousButton = findViewById(R.id.previous_button);
        imageTextView = findViewById(R.id.image_id);
        imageIds = new ArrayList<>();
        images = new ArrayList<>();
        oa = OptionsActivity.this;
        graphView = findViewById(R.id.graph_view);


        folderName = getIntent().getStringExtra("folderName");
        Boolean toSave = getIntent().getBooleanExtra("toSave",false);

        if (toSave) {
            createGraphAndImages();
        }
        else {
            displayGraphAndImages();
        }
        setTitle(R.string.matching);
    }
}
