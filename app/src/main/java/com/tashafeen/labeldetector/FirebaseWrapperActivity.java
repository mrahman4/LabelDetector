package com.tashafeen.labeldetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudTextDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.tashafeen.labeldetector.POJOs.LabelInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FirebaseWrapperActivity extends AppCompatActivity  implements SurfaceHolder.Callback {

    private static final String TAG = "LabelDetectorApp";
    private static final int REQUEST_CAMERARESULT =201;

    Camera camera;

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    FirebaseApp secondaryFirebaseApp;


    LabelsAdapter labelsAdapter;
    ListView listView;
    ArrayList<LabelInfo> labelsArray;

    TextView textView;
    boolean firstTime = false ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_wrapper);

        secondaryFirebaseApp = FirebaseApp.getInstance("secondary");
        FirebaseDatabase database = FirebaseDatabase.getInstance(secondaryFirebaseApp);
        DatabaseReference objectDetectedRef = database.getReference("applications/walabot/timestamp").getRef();


        // Read from the database
        objectDetectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                //Integer timestamp = dataSnapshot.getValue(Integer.class);
                //Log.d(TAG, "Value is: " + timestamp);

                labelsArray.clear();
                labelsAdapter.notifyDataSetChanged();

                if(firstTime) {
                    //Take a picture automatically
                    takePictureForDetectedObject();
                }
                firstTime = true;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });


        labelsArray = new ArrayList<LabelInfo>();

        labelsAdapter = new LabelsAdapter(this, labelsArray);

        listView = findViewById(R.id.labels_list);
        listView.setAdapter(labelsAdapter);

        textView = findViewById(R.id.text_view);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( (checkSelfPermission(android.Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) &&
                    (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)){
                setCamera();
            }else{
                if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
                    Toast.makeText(this,"Your Permission is needed to get access the camera",Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[]{android.Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERARESULT);
            }
        }else{
            setCamera();
        }


        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();


        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    private void setCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        releaseCameraAndPreview();
        for (int cameraId = 0; cameraId < cameraCount; cameraId++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try{
                    camera = Camera.open(cameraId);
                } catch (Exception e) {
                    Log.e( getString(R.string.app_name), e.getMessage());
                    e.printStackTrace();
                }
                break;
            }
        }
    }


    private void releaseCameraAndPreview() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
    private void getLabelsFromPicture(FirebaseVisionImage image) {


        /* //This code if you want to use mobile API instead of Cloud API
        FirebaseVisionLabelDetectorOptions options =
                new FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.8f)
                        .build();


        FirebaseVisionLabelDetector detector = FirebaseVision.getInstance(secondaryFirebaseApp).getVisionLabelDetector(options);

            try {
                Task<List<FirebaseVisionLabel>> result =
                    detector.detectInImage(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                        @Override
                                        public void onSuccess(List<FirebaseVisionLabel> labels) {
                                            for (FirebaseVisionLabel label : labels) {
                                                String text = label.getLabel();
                                                String entityId = label.getEntityId();
                                                float confidence = label.getConfidence();
                                            }
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Failed to read value.", e);
                                        }
                                    });

            } catch (Exception e) {
                Log.e( getString(R.string.app_name), e.getMessage());
                e.printStackTrace();
            }
        */

        FirebaseVisionCloudDetectorOptions options1 =
                new FirebaseVisionCloudDetectorOptions.Builder()
                        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                        .setMaxResults(15)
                        .build();



        FirebaseVisionCloudLabelDetector labelDetector = FirebaseVision.getInstance(secondaryFirebaseApp).getVisionCloudLabelDetector(options1);
        Task<List<FirebaseVisionCloudLabel>> result1 =
                labelDetector.detectInImage(image)
                         .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
                                        // Task completed successfully
                                        labelsArray.clear();
                                        for (FirebaseVisionCloudLabel label: labels) {

                                            String text = label.getLabel();
                                            String entityId = label.getEntityId();
                                            float confidence = label.getConfidence();

                                            LabelInfo element = new LabelInfo(text,entityId,confidence);
                                            labelsArray.add(element);
                                        }
                                        labelsAdapter.notifyDataSetChanged();
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.w(TAG, "Failed to read label value.", e);
                                    }
                                });


            FirebaseVisionCloudTextDetector textDetector = FirebaseVision.getInstance(secondaryFirebaseApp).getVisionCloudTextDetector(options1);
            textDetector.detectInImage(image).addOnSuccessListener(
            new OnSuccessListener<FirebaseVisionCloudText>() {
                @Override
                public void onSuccess(FirebaseVisionCloudText texts) {
                    textView.setText(texts.getText());
                }
            })
            .addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            Log.w(TAG, "Failed to read text value.", e);
                        }
            });

    }


    private void takePictureForDetectedObject() {

        if (camera!= null) {

            Camera.Parameters params = camera.getParameters();

            // Check what resolutions are supported by your camera
            List<Camera.Size> sizes = params.getSupportedPictureSizes();

            Camera.Size bestSize = sizes.get(0);

            // Iterate through all available resolutions and choose one.
            for (Camera.Size size : sizes) {
                //select first existing resolution
                Log.i(TAG, "Available resolution: "+size.width+" "+size.height);
                if ( size.height > bestSize.height && size.width > bestSize.width) {
                    bestSize = size;
                }
            }

            //640*480
            String bestSizeString = "best height is: " + bestSize.height + " , and width: " + bestSize.width ;
            Toast.makeText(getApplicationContext(), bestSizeString, Toast.LENGTH_LONG).show();

            params.setPictureSize(bestSize.width,bestSize.height);
            camera.setParameters(params);

            try {
                camera.setPreviewDisplay( surfaceView.getHolder() );
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
            camera.takePicture(null,null,mPictureCallback);

        }

    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data,final Camera camera) {
            //1- Display picture
            //2- Call firebase ML


            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(1280)
                    .setHeight(720)
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation( FirebaseVisionImageMetadata.ROTATION_0 )
                    .build();


            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);


            Toast.makeText(getApplicationContext(), "Picture Saved", Toast.LENGTH_LONG).show();
            getLabelsFromPicture(image);

            refreshCamera();
        }

        public Bitmap getBitmapFromAsset(Context context, String filePath) {
            AssetManager assetManager = context.getAssets();

            InputStream is;
            Bitmap bitmap = null;
            try {
                is = assetManager.open(filePath);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }
    };


    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }



        Camera.Parameters parameters = camera.getParameters();
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (sizes == null || sizes.size() <= 0) {
            return;
        }

        Camera.Size bestSize = null;
        int diff = Integer.MAX_VALUE;

        for (Camera.Size tmpSize : sizes) {
            int newDiff = Math.abs(tmpSize.width - surfaceView.getRootView().getWidth()) + Math.abs(tmpSize.height - surfaceView.getRootView().getHeight());
            if (newDiff == 0) {
                bestSize = tmpSize;
                break;
            } else if (newDiff < diff) {
                bestSize = tmpSize;
                diff = newDiff;
            }
        }


        int height = bestSize.height, width = bestSize.width;
        if(display.getRotation() == Surface.ROTATION_0) {
            parameters.setPreviewSize(height, width);
            camera.setDisplayOrientation(90);
        }

        if(display.getRotation() == Surface.ROTATION_90) {
            parameters.setPreviewSize(width, height);
        }

        if(display.getRotation() == Surface.ROTATION_180) {
            parameters.setPreviewSize(height, width);
        }

        if(display.getRotation() == Surface.ROTATION_270) {
            parameters.setPreviewSize(width, height);
            camera.setDisplayOrientation(180);
        }

        camera.setParameters(parameters);

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
        param.setPreviewSize(352, 288);
        camera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }



    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) getBaseContext().getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERARESULT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    setCamera();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }



}
