package pl.edu.agh.car_driver_advisor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TimeUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    SurfaceView cameraView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    FaceDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);

        detector = new FaceDetector.Builder(this)
                .setProminentFaceOnly(true) // optimize for single, relatively large face
                .setTrackingEnabled(true) // enable face tracking
                .setClassificationType(/* eyes open and smile */ FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
                .build();


        if (!detector.isOperational()) {
            Log.w("MainActivity", "Detector Dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), detector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(2.0f)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {

                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, RequestCameraPermissionID);
                            }
                        } else {
                            // Permission has already been granted
                        }
                        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new GraphicFaceTracker()));
                        cameraSource.start(cameraView.getHolder());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private static class GraphicFaceTracker extends Tracker<Face> {

        private final float OPEN_THRESHOLD_PROBABILITY = 0.85f;
        private final float CLOSE_THRESHOLD_PROBABILITY = 0.4f;
        private final long CLOSE_TIME_DIFF_DANGEROUS = 3000; // 3s
        private final long CLOSE_TIME_DIFF_DANGEROUS_2 = 7000; // 7s sleep

        private int state = 0; // 0 - beginning , 1 - open, 2 - close
        private long closeTime = -1;


        void detectEyesState(float value) {
            switch (state) {
                case 0:
                    if (value > OPEN_THRESHOLD_PROBABILITY) {
                        //Both eyes open
                        state = 1;
                        closeTime = -1;
                    }
                    break;

                case 1:
                    if (value < CLOSE_THRESHOLD_PROBABILITY) {
                        // Both eyes become closed
                        state = 2;
                        closeTime = new Date().getTime();
                    }
                    break;

                case 2:
                    if (value > OPEN_THRESHOLD_PROBABILITY)  {
                        // Both eyes are open again
                        Log.i("BlinkTracker", "blink occurred!");
                        state = 0;
                        closeTime = -1;
                    } else {
                        // eyes staies closed
                        long closeTimeDif = new Date().getTime() - closeTime;
                        if(closeTimeDif >= CLOSE_TIME_DIFF_DANGEROUS)
                            Log.i("BlinkTracker", "Eyes closed to long!");
                    }
                    break;
            }


        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {

            float left = face.getIsLeftEyeOpenProbability();
            float right = face.getIsRightEyeOpenProbability();
            if ((left == Face.UNCOMPUTED_PROBABILITY) ||
                    (right == Face.UNCOMPUTED_PROBABILITY)) {
                // One of the eyes was not detected.
                return;
            }

            float value = Math.min(left, right);
            detectEyesState(value);
        }
    }
}
