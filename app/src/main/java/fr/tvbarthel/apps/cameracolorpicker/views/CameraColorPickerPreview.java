package fr.tvbarthel.apps.cameracolorpicker.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;

import java.util.Arrays;

public class CameraColorPickerPreview extends TextureView implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private static final String TAG = CameraColorPickerPreview.class.getCanonicalName();
    protected Camera mCamera;
    protected Camera.Size mPreviewSize;
    protected int[][][] mSelectedColorArray;
    protected OnColorSelectedListener mOnColorSelectedListener;

    protected int ARRAY_WIDTH = 31;
    protected int ARRAY_HEIGHT = 241;

    protected int RATIO;

    public CameraColorPickerPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mCamera.getParameters().getPreviewFormat();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        this.setSurfaceTextureListener(this);

        mPreviewSize = mCamera.getParameters().getPreviewSize();
        mSelectedColorArray = new int[ARRAY_HEIGHT][ARRAY_WIDTH][3];
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
//            Log.v("tag", String.valueOf(width) + "x"  + String.valueOf(height));
            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mOnColorSelectedListener != null) {
            int midX = mPreviewSize.width / 2;
            int midY = mPreviewSize.height / 2;

            int shift = mPreviewSize.width / 4;
            midX -= shift;

            // Compute the average selected color.
            for (int i = 0; i < ARRAY_HEIGHT; i++) {
                for (int j = 0; j < ARRAY_WIDTH; j++) {
                    mSelectedColorArray[i][j][0] = 0;
                    mSelectedColorArray[i][j][1] = 0;
                    mSelectedColorArray[i][j][2] = 0;
                    addColorFromYUV420(data, mSelectedColorArray[i][j], 1,
                            (midX - ARRAY_HEIGHT/2) + i, (midY - ARRAY_WIDTH/2) + j,
                            mPreviewSize.width, mPreviewSize.height);
                }
            }

            mOnColorSelectedListener.onColorSelected(mSelectedColorArray, data, mPreviewSize.width, mPreviewSize.height);
        }
    }

    protected void addColorFromYUV420(byte[] data, int[] averageColor, int count, int x, int y, int width, int height) {
        // The code converting YUV420 to rgb format is highly inspired from this post http://stackoverflow.com/a/10125048
        final int size = width * height;
        final int Y = data[y * width + x] & 0xff;
        final int xby2 = x / 2;
        final int yby2 = y / 2;

        final float V = (float) (data[size + 2 * xby2 + yby2 * width] & 0xff) - 128.0f;
        final float U = (float) (data[size + 2 * xby2 + 1 + yby2 * width] & 0xff) - 128.0f;

        // Do the YUV -> RGB conversion
        float Yf = 1.164f * ((float) Y) - 16.0f;
        int red = (int) (Yf + 1.596f * V);
        int green = (int) (Yf - 0.813f * V - 0.391f * U);
        int blue = (int) (Yf + 2.018f * U);

        // Clip rgb values to [0-255]
        red = red < 0 ? 0 : red > 255 ? 255 : red;
        green = green < 0 ? 0 : green > 255 ? 255 : green;
        blue = blue < 0 ? 0 : blue > 255 ? 255 : blue;

        averageColor[0] += (red - averageColor[0]) / count;
        averageColor[1] += (green - averageColor[1]) / count;
        averageColor[2] += (blue - averageColor[2]) / count;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener onColorSelectedListener) {
        mOnColorSelectedListener = onColorSelectedListener;
    }

    public interface OnColorSelectedListener {

        void onColorSelected(int [][][] array, byte[] data, int width, int height);
    }

}