package com.example.cameraapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A basic Camera preview class
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private LinearLayout linearLayout, addressLayout;
    ImageView retryButton, doneButton;
    private Camera mCamera;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean safeToTakePicture = false;
    Window window;



    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.stopPreview();
            linearLayout.setVisibility(VISIBLE);
            doneButton.setOnClickListener(new OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(View v) {

                    File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    Log.e("PATH", "onPictureTaken: " + pictureFile.getAbsolutePath());
                    if (pictureFile == null) {
                        Log.d("TAG", "Error creating media file, check storage permissions");
                        return;
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        Bitmap b = Bitmap.createBitmap(addressLayout.getWidth() , addressLayout.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(b);
                        addressLayout.draw(c);
//                        addressLayout.setDrawingCacheEnabled(true);
//                        Bitmap address = addressLayout.getDrawingCache();
                        Bitmap cameraImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();

                        matrix.postRotate(90);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(cameraImage, cameraImage.getWidth(), cameraImage.getHeight(), true);

                        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                        b = Bitmap.createScaledBitmap(b, rotatedBitmap.getWidth(), rotatedBitmap.getHeight()/3, true);

                        Bitmap toDisk = overlay(rotatedBitmap,b);


//
//                        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
//                        Bitmap workingBitmap = Bitmap.createBitmap(cameraImage);
//                        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
//                        Canvas result = new Canvas(mutableBitmap);
//                        result.drawBitmap(address, 0, 0, paint);
//                        result.setBitmap(toDisk);
//                        toDisk.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        toDisk.compress(Bitmap.CompressFormat.JPEG, 100,fos);

//                        FileOutputStream fos = new FileOutputStream(pictureFile);
//                        fos.write(data);
                        fos.flush();
                        fos.close();
                        camera.startPreview();
                        safeToTakePicture = true;
                        linearLayout.setVisibility(INVISIBLE);

                    } catch (FileNotFoundException e) {
                        Log.d("TAG", "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d("TAG", "Error accessing file: " + e.getMessage());
                    } finally {
                        Toast.makeText(getContext(), "Picture Saved", Toast.LENGTH_LONG).show();
                    }
                }
            });
            retryButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    camera.startPreview();
                    safeToTakePicture = true;
                    linearLayout.setVisibility(INVISIBLE);
                    Toast.makeText(getContext(), "Picture discarded", Toast.LENGTH_LONG).show();

                }
            });

        }
    };

    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        Log.e("mediaStorageDir", "getOutputMediaFile: " + mediaStorageDir.getAbsolutePath());
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    public CameraPreview(Context context, Camera camera, LinearLayout layout, ImageView retry, ImageView done, Window window, LinearLayout addressLayout) {
        super(context);
        mCamera = camera;
        this.linearLayout = layout;
        retryButton = retry;
        doneButton = done;
        this.window = window;
        this.addressLayout = addressLayout;


        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(0);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d("TAG", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setDisplayOrientation(90);


            mCamera.startPreview();
            safeToTakePicture = true;

        } catch (Exception e) {
            Log.d("TAG", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //System.gc(); tried this because it was suggested in a stackoverflow question but it didn't help.

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // A pressed gesture has started, the motion contains the initial starting location
                break;

            case MotionEvent.ACTION_UP:
                // A pressed gesture has finished, the motion contains the final release location
                // as well as any intermediate points since the last down or move event.
                if (safeToTakePicture) {
                    Toast.makeText(getContext(), "Picture taken", Toast.LENGTH_SHORT).show();

                    mCamera.takePicture(null, null, mPicture);
                    safeToTakePicture = false;

                }


                break;

            default:
                break;
        }

        return true; //processed
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void takeScreenshot() {


        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);


        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = window.getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            File imageFile = new File(mPath);

//            FileOutputStream outputStream = new FileOutputStream(imageFile);
//            int quality = 100;
//            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            Bitmap bmp = Bitmap.createBitmap(CameraPreview.this.getWidth(), CameraPreview.this.getHeight(), Bitmap.Config.ARGB_8888);
            PixelCopy.request(CameraPreview.this, bmp, i -> {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(imageFile);
                    int quality = 100;
                    bmp.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //"iv_Result" is the image view
            }, new Handler(Looper.getMainLooper()));
//            outputStream.flush();
//            outputStream.close();


            openScreenshot(imageFile);

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        getContext().startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void capturePicture() {

    }
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        bmp1.recycle();
        bmp2.recycle();
        return bmOverlay;
    }
}

