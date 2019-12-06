package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import fr.tvbarthel.apps.cameracolorpicker.R;
import fr.tvbarthel.apps.cameracolorpicker.utils.Cameras;
import fr.tvbarthel.apps.cameracolorpicker.views.CameraColorPickerPreview;


public class ColorPickerBaseActivity extends AppCompatActivity
        implements CameraColorPickerPreview.OnColorSelectedListener, View.OnClickListener {

    protected static final String TAG = ColorPickerBaseActivity.class.getSimpleName();


    private static Camera getCameraInstance() {
        Camera c = null;
        try { c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK); }
        catch (Exception e) { Log.e(TAG, e.getMessage()); }
        return c;
    }

    static protected Camera mCamera;
    static protected boolean mIsPortrait;
    static protected FrameLayout mPreviewContainer;
    static protected int mPreviewContainerWidth, mPreviewContainerHeight;
    static protected CameraColorPickerPreview mCameraPreview;

    protected CameraAsyncTask mCameraAsyncTask;
    static protected int mSelectedColorIn;
    static protected int mSelectedColorOut;
    static protected int[][][] mSelectedColorArray;
    static protected int outBwColor;
    static protected int inBwColor;
    protected View mPointerRing;
    protected View mPointerRingOut;

    protected View mPlayButton;
    protected View mStopButton;
    protected TextView mTimer;
    protected Handler mTimerHandler;
    protected boolean isTimerRunning;
    protected int mTimerMinutes, mTimerSeconds, mTimerSecondsPrevious, mTimerMilliseconds;
    protected long mMillisecondTime, mStartTime, mTimeBuff, mUpdateTime = 0L ;


    protected boolean mIsFlashOn;
    protected String action = null;

    protected Runnable mHideToast;
    protected View mToast;

    static protected int WINDOW_SIZE = 360;
    protected int[] outBwColorArray;
    protected int[] inBwColorArray;
    protected int[][] outColorArray;
    protected int[][] inColorArray;
    protected int windowIndex;

    protected MediaPlayer ringtone;
    protected TextView distanceText;

    static protected double distance;

    static protected int ARRAY_WIDTH = 31;
    static protected int ARRAY_HEIGHT = 241;

    protected String mode;
    protected Spinner modeSpinner;
    protected ArrayAdapter<CharSequence> modeAdapter;

    static protected AppDatabase db;
    static protected byte[] currentByteData;
    static protected int currentDataWidth;
    static protected int currentDataHeight;
    static protected Integer runtimeAutomatic = 5;

    protected View alignCheck;
    protected String folderName = "";
    protected EditText nameText;
    protected TextView nameErrorMessage, colorPickerStatus;
    protected Boolean runComplete, calibrated;
    protected Menu menu;
    static ColorPickerBaseActivity context;
    SharedPreferences sharedPreferences;

    protected View targetFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        Locale locale = new Locale(sharedPreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        isTimerRunning = false;
        runComplete = false;
        calibrated = false;
        outBwColorArray = new int[WINDOW_SIZE];
        inBwColorArray = new int[WINDOW_SIZE];
        outColorArray = new int[6][WINDOW_SIZE];
        inColorArray = new int[3][WINDOW_SIZE];
        context = ColorPickerBaseActivity.this;

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "frame").build();

        windowIndex = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            outBwColorArray[i] = -1;
            inBwColorArray[i] = -1;
            outColorArray[0][i] = -1; outColorArray[1][i] = -1; outColorArray[2][i] = -1;
            inColorArray[0][i] = -1; inColorArray[1][i] = -1; inColorArray[2][i] = -1;
        }


        setContentView(R.layout.activity_color_picker);
        initViews();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            setTitle(R.string.instangal);
        }

        Intent intent = getIntent();
        if (intent != null) action = intent.getAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraAsyncTask = new CameraAsyncTask();
        if (!folderName.equals("")) mCameraAsyncTask.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraAsyncTask.cancel(true);
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

        if (mCameraPreview != null) {
            mPreviewContainer.removeView(mCameraPreview);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_color_picker, menu);
        mIsFlashOn = false;
        final MenuItem flashItem = menu.findItem(R.id.menu_color_picker_action_flash);
        int flashIcon = mIsFlashOn ? R.drawable.ic_action_flash_on : R.drawable.ic_action_flash_off;
        flashItem.setIcon(flashIcon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        boolean handled;
        switch (itemId) {
            case R.id.menu_color_picker_action_flash:
                if (!isFlashSupported()) {
                    handled = true;
                    break;
                }
                int flashIcon = mIsFlashOn ? R.drawable.ic_action_flash_off : R.drawable.ic_action_flash_on;
                toggleFlash();
                item.setIcon(flashIcon);
                handled = true;
                break;
            case R.id.menu_color_picker_time:
                timerAlert();
                handled = true;
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    public int[] getOutColor() {
        int[] upColor = {0,0,0};
        int[] downColor = {0,0,0};

        int count = 0;
        for (int i = 0; i < ARRAY_HEIGHT; i++) {
            if ((i >=4*ARRAY_HEIGHT/48 && i < 9*ARRAY_HEIGHT/48) || (i >=39*ARRAY_HEIGHT/48 && i < 44*ARRAY_HEIGHT/48)) {
                if (i >=4*ARRAY_HEIGHT/48 && i < 9*ARRAY_HEIGHT/48) {
                    upColor[0] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][0];
                    upColor[1] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][1];
                    upColor[2] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][2];
                }
                else {
                    downColor[0] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][0];
                    downColor[1] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][1];
                    downColor[2] +=  mSelectedColorArray[i][ARRAY_WIDTH/2][2];
                }
                count++;
            }
        }

        upColor[0] /= (count/2); upColor[1] /= (count/2); upColor[2] /= (count/2);
        downColor[0] /= (count/2); downColor[1] /= (count/2); downColor[2] /= (count/2);
        upColor[0] = Math.max(upColor[0],1); upColor[1] = Math.max(upColor[1],1); upColor[2] = Math.max(upColor[2],1);

        int[] outColor = {upColor[0],upColor[1],upColor[2],downColor[0],downColor[1],downColor[2]};
        return outColor;

    }

    public int[] getInColor() {
        int[] inColor = {0,0,0};
        int[][] colColors = new int[ARRAY_WIDTH][3];
        int[] colBwColors = new int[ARRAY_WIDTH];

        int count  = 0;
        for (int j = 0; j < ARRAY_WIDTH; j++) {
            for (int i = 0; i < ARRAY_HEIGHT; i++) {
                if (i >= 23*ARRAY_HEIGHT/48 && i < 25*ARRAY_HEIGHT/48) {
                    colColors[j][0] += mSelectedColorArray[i][j][0];
                    colColors[j][1] += mSelectedColorArray[i][j][1];
                    colColors[j][2] += mSelectedColorArray[i][j][2];
                    count++;
                }
            }
            colColors[j][0] /= count; colColors[j][1] /= count; colColors[j][2] /= count;
            colBwColors[j] = rgbToBw(colColors[j]);
            count = 0;
        }

        int index = 0;
        int min = 255;
        for (int j = 0; j < ARRAY_WIDTH; j++) {
            if (colBwColors[j] < min) {
                min = colBwColors[j];
                index = j;
            }
        }

        if (index == 0 || (index != ARRAY_WIDTH-1 && colBwColors[index-1] > colBwColors[index+1])) {
            inColor[0] = (colColors[index][0] + colColors[index+1][0])/2;
            inColor[1] = (colColors[index][1] + colColors[index+1][1])/2;
            inColor[2] = (colColors[index][2] + colColors[index+1][2])/2;
        }
        else {
            inColor[0] = (colColors[index][0] + colColors[index-1][0])/2;
            inColor[1] = (colColors[index][1] + colColors[index-1][1])/2;
            inColor[2] = (colColors[index][2] + colColors[index-1][2])/2;
        }

        inColor[0] = mSelectedColorArray[ARRAY_HEIGHT/2][ARRAY_WIDTH/2][0];
        inColor[1] = mSelectedColorArray[ARRAY_HEIGHT/2][ARRAY_WIDTH/2][1];
        inColor[2] = mSelectedColorArray[ARRAY_HEIGHT/2][ARRAY_WIDTH/2][2];

        return inColor;
    }

    public int rgbToBw(int [] rgb) {
        return  (int) (rgb[0]*0.3 + rgb[1]*0.59 + rgb[2]*0.11);

    }

    public double norm(double val, double mean, double var) {
        return Math.exp(-Math.pow(val-mean,2)/var)/(2*Math.sqrt(var));
    }


    public int getWeightedMean(int[] colors) {
        double[] weights = new double[WINDOW_SIZE];
        double mean = 0;
        int count = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            if (colors[i] == -1) continue;
            mean += colors[i];
            count++;
        }
        mean /= count;

        double var = 0;
        count = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            if (colors[i] == -1) continue;
            var += Math.pow(colors[i] - mean, 2);
            count++;
        }
        var /= count;

        for (int i = 0; i < WINDOW_SIZE; i++) {
            weights[i] = norm(colors[i],mean,var);
        }

        double weightedMean = 0;
        double sumOfWeights = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            weightedMean += colors[i]*weights[i];
            sumOfWeights += weights[i];
        }
        weightedMean /= sumOfWeights;
        return (int) weightedMean;
    }


    @Override
    public void onColorSelected(int [][][] array, byte[] data, int width, int height) {
        mSelectedColorArray = array;
        currentByteData = data;
        currentDataWidth = width;
        currentDataHeight = height;

        int[] outColor = getOutColor();
        int[] inColor = getInColor();


        outColorArray[0][windowIndex] = outColor[0]; outColorArray[1][windowIndex] = outColor[1]; outColorArray[2][windowIndex] = outColor[2];
        outColorArray[3][windowIndex] = outColor[3]; outColorArray[4][windowIndex] = outColor[4]; outColorArray[5][windowIndex] = outColor[5];
        inColorArray[0][windowIndex] = inColor[0]; inColorArray[1][windowIndex] = inColor[1]; inColorArray[2][windowIndex] = inColor[2];
        windowIndex = (windowIndex + 1) % WINDOW_SIZE;


        mSelectedColorOut = Color.rgb((outColor[0]+outColor[3])/2,
                (outColor[1]+outColor[4])/2,(outColor[2]+outColor[5])/2);
        mSelectedColorIn = Color.rgb(inColor[0],inColor[1],inColor[2]);
        mPointerRing.getBackground().setColorFilter(mSelectedColorIn, PorterDuff.Mode.SRC_ATOP);
        mPointerRingOut.getBackground().setColorFilter(mSelectedColorOut, PorterDuff.Mode.SRC_ATOP);

        int THRESHOLD = 20;
        if (isTimerRunning) {
            colorPickerStatus.setText(getResources().getString(R.string.running));
            alignCheck.setBackgroundColor(getResources().getColor(R.color.color_accent_temp2));
            colorPickerStatus.setTextColor(getResources().getColor(R.color.color_accent_temp2));
        }
        else if (runComplete) {
            alignCheck.setBackgroundColor(getResources().getColor(R.color.color_primary));
            colorPickerStatus.setTextColor(getResources().getColor(R.color.color_primary));
            colorPickerStatus.setText(getResources().getString(R.string.complete));
        }
        else if (Math.abs((outColor[0]-outColor[3])*100.0/outColor[0])<=THRESHOLD
                && Math.abs((outColor[1]-outColor[4])*100.0/outColor[1])<=THRESHOLD
                && Math.abs((outColor[2]-outColor[5])*100.0/outColor[2])<=THRESHOLD) {
            colorPickerStatus.setText(getResources().getString(R.string.calibrated));
            colorPickerStatus.setTextColor(getResources().getColor(R.color.color_accent));
            alignCheck.setBackgroundColor(getResources().getColor(R.color.color_accent));
            calibrated = true;
            mPlayButton.setEnabled(true);
        }
        else {
            colorPickerStatus.setText(getResources().getString(R.string.calibrating));
            colorPickerStatus.setTextColor(getResources().getColor(R.color.color_accent_temp));
            alignCheck.setBackgroundColor(getResources().getColor(R.color.color_accent_temp));
            calibrated = false;
            mPlayButton.setEnabled(false);
        }

        outColor[0] = getWeightedMean(outColorArray[0]); outColor[1] = getWeightedMean(outColorArray[1]); outColor[2] = getWeightedMean(outColorArray[2]);
        outColor[3] = getWeightedMean(outColorArray[3]); outColor[4] = getWeightedMean(outColorArray[4]); outColor[5] = getWeightedMean(outColorArray[5]);
        inColor[0] = getWeightedMean(inColorArray[0]); inColor[1] = getWeightedMean(inColorArray[1]); inColor[2] = getWeightedMean(inColorArray[2]);

        float[] e1 = {inColor[0], inColor[1], inColor[2]};
        Color.RGBToHSV(inColor[0],inColor[1],inColor[2],e1);
        float[] e2 = {(outColor[0]+outColor[3])/2, (outColor[1]+outColor[4])/2, (outColor[2]+outColor[5])/2};
        Color.RGBToHSV((int)e2[0],(int)e2[1],(int)e2[2],e2);

        distance = Eucledian(e1, e2);
        distanceText.setText("Distance = " + String.format("%.2f", distance));
    }

    public double Eucledian(float[] e1, float[] e2) {
        return Math.sqrt(Math.pow(e1[1]-e2[1],2)*10000+Math.pow(e1[2]-e2[2],2)*10000);
    }

    public double ColourDistance(float[] e1, float[] e2) {
        long rmean = ( (long)e1[0] + (long)e2[0] ) / 2;
        long r = (long)e1[0] - (long)e2[0];
        long g = (long)e1[1] - (long)e2[1];
        long b = (long)e1[2] - (long)e2[2];
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }

    @Override
    public void onClick(View v) {
    }


    public static void insertFrame(final int fid) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                db.frameDao().insert(new Frame(fid, String.format("#%06X", (0xFFFFFF & mSelectedColorIn)),
                        String.format("#%06X", (0xFFFFFF & mSelectedColorOut)),
                                        inBwColor, outBwColor, distance));
                return null;
            }
        }.execute();
    }

    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;
            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;
            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);
            if (i!=0 && (i+2)%width==0) i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;
        r = y + (int)(1.402f*v);
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)(1.772f*u);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    public Runnable mTimerRunnable = new Runnable() {
        public void run() {
            mMillisecondTime = SystemClock.uptimeMillis() - mStartTime;
            mUpdateTime = mTimeBuff + mMillisecondTime;
            mTimerSeconds = (int) (mUpdateTime / 1000);
            Boolean check =  mode.equals("Automatic") && mTimerSeconds/60 == runtimeAutomatic && mTimerSeconds % OptionsActivity.CAPTURE_INTERVAL == 0;

            if (mTimerSeconds > mTimerSecondsPrevious) {
                insertFrame(mTimerSeconds);
                mTimerSecondsPrevious = mTimerSeconds;
            }

            if (mTimerSeconds % OptionsActivity.CAPTURE_INTERVAL == 0) {
                try{
                    String imageName = String.valueOf(mTimerSeconds);
                    File imageFile = new File(getExternalFilesDir("matching_images"),imageName);
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    Bitmap bitmap = Bitmap.createBitmap(convertYUV420_NV21toRGB8888(currentByteData,currentDataWidth,currentDataHeight),
                            currentDataWidth, currentDataHeight, Bitmap.Config.ARGB_8888);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();

                    if (check) mStopButton.callOnClick();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }

            mTimerMinutes = mTimerSeconds / 60;
            mTimerSeconds = mTimerSeconds % 60;
            mTimerMilliseconds = (int) (mUpdateTime % 1000);

            String text = "" + mTimerMinutes + ":"+ String.format(Locale.getDefault(),"%02d", mTimerSeconds) + ":" + String.format(Locale.getDefault(),"%03d", mTimerMilliseconds);
            mTimer.setText(text);

            if (!check) mTimerHandler.postDelayed(this, 0);
        }
    };


    protected void initViews() {
        mIsPortrait = getResources().getBoolean(R.bool.is_portrait);
        mPreviewContainer = findViewById(R.id.activity_color_picker_preview_container);
        mTimer = findViewById(R.id.activity_color_picker_timer);
        mPointerRing = findViewById(R.id.activity_color_picker_pointer_ring);
        mPointerRingOut = findViewById(R.id.activity_color_picker_pointer_ring_out);
        mPlayButton = findViewById(R.id.activity_color_picker_play_button);
        mStopButton = findViewById(R.id.activity_color_picker_stop_button);
        mStopButton.setEnabled(false);
        distanceText = findViewById(R.id.activity_color_picker_distance);
        alignCheck = findViewById(R.id.activity_color_picker_pointer_box);
        ringtone = MediaPlayer.create(this, R.raw.ringtone);
        modeSpinner = findViewById(R.id.mode_spinner);
        modeAdapter = ArrayAdapter.createFromResource(this,R.array.modes,android.R.layout.simple_spinner_item);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);
        targetFrame = findViewById(R.id.target_frame);
        colorPickerStatus = findViewById(R.id.activity_color_picker_status);



        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public  void onNothingSelected(AdapterView<?> adapterView) { }

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mode = adapterView.getItemAtPosition(i).toString();
                if (mode.equals("Automatic")) menu.findItem(R.id.menu_color_picker_time).setVisible(true);
                else menu.findItem(R.id.menu_color_picker_time).setVisible(false);
            }
        });


        mToast = findViewById(R.id.activity_color_picker_toast_temp);
        mToast.setTranslationY(0);
        mHideToast = new Runnable() {
            @Override
            public void run() {
                mToast.animate()
                        .setStartDelay(2000)
                        .translationY(-mToast.getMeasuredHeight())
                        .setDuration(1000)
                        .start();
            }
        };


        mTimerHandler = new Handler() ;
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButtonClick();
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopButtonClick();
            }
        });

        airplaneAlert();
    }

    protected boolean isFlashSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    protected void toggleFlash() {
        if (mCamera != null) {
            final Camera.Parameters parameters = mCamera.getParameters();
            final String flashParameter = mIsFlashOn ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH;
            parameters.setFlashMode(flashParameter);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mCameraPreview);
            mCamera.startPreview();

            mIsFlashOn = !mIsFlashOn;
        }
    }

    private static class CameraAsyncTask extends AsyncTask<Void, Void, Camera> {

        protected FrameLayout.LayoutParams mPreviewParams;

        @Override
        protected Camera doInBackground(Void... params) {
            Camera camera = getCameraInstance();
            if (camera == null) {
                context.finish();
            }
            else {
                Camera.Parameters cameraParameters = camera.getParameters();
                Camera.Size bestSize = Cameras.getBestPreviewSize(
                        cameraParameters.getSupportedPreviewSizes()
                        , mPreviewContainerWidth
                        , mPreviewContainerHeight
                        , mIsPortrait);

                cameraParameters.setPreviewSize(bestSize.width, bestSize.height);
                if (cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else if (cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                camera.setParameters(cameraParameters);
                Cameras.setCameraDisplayOrientation(context, camera);
                int[] adaptedDimension = Cameras.getProportionalDimension(
                        bestSize
                        , mPreviewContainerWidth
                        , mPreviewContainerHeight
                        , mIsPortrait);

                Log.v("tag", String.valueOf(mPreviewContainerWidth) + "xx"+String.valueOf(mPreviewContainerHeight));
                mPreviewParams = new FrameLayout.LayoutParams(adaptedDimension[0],adaptedDimension[1]);
                mPreviewParams.gravity = Gravity.CENTER;
            }
            return camera;
        }

        @Override
        protected void onPostExecute(Camera camera) {
            super.onPostExecute(camera);
            if (!isCancelled()) {
                mCamera = camera;
                if (mCamera == null) context.finish();
                else {
                    mCameraPreview = new CameraColorPickerPreview(context, mCamera);
                    mCameraPreview.setOnColorSelectedListener(context);
                    mCameraPreview.setOnClickListener(context);
                    mPreviewContainer.addView(mCameraPreview, 0, mPreviewParams);
                }
            }
        }

        @Override
        protected void onCancelled(Camera camera) {
            super.onCancelled(camera);
            if (camera != null) {
                camera.release();
            }
        }
    }

    public void timerAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.time_alert_title));
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.timer_alert,null);
        nameErrorMessage = dialogLayout.findViewById(R.id.name_error);
        nameText = dialogLayout.findViewById(R.id.run_time);
        nameText.setText(String.valueOf(runtimeAutomatic));
        builder.setPositiveButton(getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                nameErrorMessage.setVisibility(View.GONE);
                dialog.dismiss();
            }
        });
        builder.setView(dialogLayout);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nameText.getText().toString().equals("")) {
                    nameErrorMessage.setText(getResources().getString(R.string.time_alert_empty));
                    nameErrorMessage.setVisibility(View.VISIBLE);
                }
                else {
                    if (Integer.valueOf(nameText.getText().toString()) > 15) {
                        nameErrorMessage.setText(getResources().getString(R.string.time_alert_limit));
                        nameErrorMessage.setVisibility(View.VISIBLE);
                    }
                    else {
                        runtimeAutomatic = Integer.valueOf(nameText.getText().toString());
                        nameErrorMessage.setVisibility(View.GONE);
                        dialog.dismiss();
                    }
                }
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    public void playButtonClick() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                db.frameDao().deleteAll();
                return null;
            }
        }.execute();
        mTimerSecondsPrevious = -1;
        mStartTime = SystemClock.uptimeMillis();
        mPlayButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mTimerHandler.postDelayed(mTimerRunnable, 0);
        mStopButton.setEnabled(true);
        isTimerRunning = true;
        runComplete = false;
        modeSpinner.setEnabled(false);
        modeSpinner.setClickable(false);
        if (isFlashSupported() && !mIsFlashOn) {
            int flashIcon = mIsFlashOn ? R.drawable.ic_action_flash_off : R.drawable.ic_action_flash_on;
            toggleFlash();
            menu.getItem(1).setIcon(flashIcon);
        }
    }

    public void stopButtonClick() {
        mToast.setVisibility(View.INVISIBLE);
        mToast.setTranslationY(0);
        mTimeBuff += mMillisecondTime;
        mTimerHandler.removeCallbacks(mTimerRunnable);
        mStopButton.setEnabled(false);
        mToast.setVisibility(View.VISIBLE);
        mHideToast.run();
        isTimerRunning = false;
        runComplete = true;
        ringtone.start();
        modeSpinner.setEnabled(true);
        modeSpinner.setClickable(true);
        alignCheck.setBackgroundColor(getResources().getColor(R.color.color_primary));
        colorPickerStatus.setText("Run Complete!");
        Intent intent = new Intent(ColorPickerBaseActivity.this,OptionsActivity.class);
        Bundle b = new Bundle();
        b.putString("folderName",folderName);
        b.putBoolean("toSave",true);
        intent.putExtras(b);
        if (isFlashSupported() && mIsFlashOn) {
            int flashIcon = mIsFlashOn ? R.drawable.ic_action_flash_off : R.drawable.ic_action_flash_on;
            toggleFlash();
            menu.getItem(1).setIcon(flashIcon);
        }
        startActivity(intent);
        finish();
    }

    public void nameAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.name_alert_title));
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.name_alert,null);
        nameErrorMessage = dialogLayout.findViewById(R.id.name_error);
        nameText = dialogLayout.findViewById(R.id.person_name);
        builder.setPositiveButton(getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                nameErrorMessage.setVisibility(View.GONE);
                finish();
                dialog.dismiss();
            }
        });
        builder.setView(dialogLayout);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                folderName = nameText.getText().toString();
                if (folderName.equals("")) {
                    nameErrorMessage.setText(getResources().getString(R.string.name_alert_empty));
                    nameErrorMessage.setVisibility(View.VISIBLE);
                }
                else {
                    boolean found = false;
                    File defaultFolder = new File(getExternalFilesDir(null).toString());
                    File[] files = defaultFolder.listFiles();
                    for (File file : files) {
                        if (file.getName().equals(folderName)) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        nameErrorMessage.setText(getResources().getString(R.string.name_alert_exists));
                        nameErrorMessage.setVisibility(View.VISIBLE);
                    }
                    else {
                        mPreviewContainerWidth = mPreviewContainer.getWidth();
                        mPreviewContainerHeight = mPreviewContainer.getHeight();
                        mCameraAsyncTask.execute();
                        dialog.dismiss();
                    }
                }
            }
        });
    }

    public void airplaneAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.airplane_alert_title));
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.airplane_alert,null);
        builder.setPositiveButton(getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dialog.dismiss();
            }
        });
        builder.setView(dialogLayout);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameAlert();
                dialog.dismiss();
            }
        });
    }
}
