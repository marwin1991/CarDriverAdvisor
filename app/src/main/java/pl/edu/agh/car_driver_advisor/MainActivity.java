package pl.edu.agh.car_driver_advisor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import pl.edu.agh.car_driver_advisor.carvelocity.CarVelocityChecker;
import pl.edu.agh.car_driver_advisor.carvelocity.VoiceNotifier;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    final static int REQUEST_CAMERA_PERMISSION_ID = 1001;
    final static int REQUEST_ALL_REQUIRED_PERMISSIONS_ID = 8836;

    SurfaceView cameraView;
    CameraSource cameraSource;
    FaceDetector detector;

    private TextView carSpeedTextView;
    private TextView speedLimitTextView;
    private Handler speedLimitChangeHandler;
    private VoiceNotifier voiceNotifier;

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

        carSpeedTextView = findViewById(R.id.carSpeedTextView);
        speedLimitTextView = findViewById(R.id.speedLimitTextView);
        voiceNotifier = new VoiceNotifier(getApplicationContext());
        speedLimitChangeHandler = new SpeedLimitChangeHandler(this);

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
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_ID);
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
            case REQUEST_CAMERA_PERMISSION_ID: {
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
                break;
            }
            case REQUEST_ALL_REQUIRED_PERMISSIONS_ID: {
                if (grantResults.length > 0 && Arrays.stream(grantResults)
                        .allMatch(status -> status == PackageManager.PERMISSION_GRANTED)) {
                    accessLocation();
                }
                break;
            }
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {

        private final float OPEN_THRESHOLD_PROBABILITY = 0.85f;
        private final float CLOSE_THRESHOLD_PROBABILITY = 0.4f;
        private final long CLOSE_TIME_DIFF_DANGEROUS = 3000; // 3s

        private int state = 0; // 0 - beginning , 1 - open, 2 - close
        private long closeTime = -1;

        //Blining detection
        private int blinksInTimeDanger = 4;
        private int blinksTimeThreshold = 5000; // 5s
        private long blinkCounter = 0;
        private long firstBlinkTime = new Date().getTime();

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
                    if (value > OPEN_THRESHOLD_PROBABILITY) {
                        // Both eyes are open again
                        Log.i("BlinkTracker", "blink occurred!");
                        state = 0;
                        closeTime = -1;

                        long blinksTimeDif = new Date().getTime() - firstBlinkTime;
                        if (blinksTimeDif <= blinksTimeThreshold) {
                            if (blinkCounter >= blinksInTimeDanger) {
                                Log.i("BlinkTracker", "Too many blinks in short period of time!!!");
                                Alert.makeAlert(MainActivity.this.getLayoutInflater(), MainActivity.this, "Too many blinks in short period of time!!!");
                            } else {
                                blinkCounter++;
                            }
                        } else {
                            firstBlinkTime = new Date().getTime();
                            blinkCounter = 0;
                        }
                    } else {
                        // eyes staies closed
                        long closeTimeDif = new Date().getTime() - closeTime;
                        if (closeTimeDif >= CLOSE_TIME_DIFF_DANGEROUS) {
                            Log.i("BlinkTracker", "Eyes closed to long!");
                            Alert.makeAlert(MainActivity.this.getLayoutInflater(), MainActivity.this, "Waring!!! Eyes closed to long!");
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ignore) {
                            }
                        }

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

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            final double latitude = location.getLatitude();
            final double longitude = location.getLongitude();

            if (location.hasSpeed()) {
                double speed = location.getSpeed();
                carSpeedTextView.setText(String.format("%s km/h",
                        Math.round(speed * 36) / 10.0));
                new Thread(new CarVelocityChecker(voiceNotifier, latitude, longitude, speed,
                        speedLimitChangeHandler)).start();
            } else {
                // Speed could be calculated "manually" here. Distance to last location and time.
                String noSignalMsg = "GPS signal lost";
                carSpeedTextView.setText(noSignalMsg);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    };

    private void accessLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationProvider locationProvider = Objects.requireNonNull(locationManager)
                .getProvider(LocationManager.GPS_PROVIDER);

        if (locationProvider != null) {
            try {
                int locationMinRequestsTimeInterval = 1000; // 1s
                int minLocationUpdateDistance = 1;  // 1m
                locationManager.requestLocationUpdates(locationProvider.getName(),
                        locationMinRequestsTimeInterval, minLocationUpdateDistance, this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location Provider is not available at the moment!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        List<String> permissionsList = getRequiredPermissionsList();
        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[0]), REQUEST_ALL_REQUIRED_PERMISSIONS_ID);
        } else {
            accessLocation();
        }
    }

    private boolean shouldRequestForGivenPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED;
    }

    private List<String> getRequiredPermissionsList() {
        // add permissions here to request on start-up
        List<String> permissionsToGet = Arrays.asList(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        );

        return permissionsToGet.stream()
                .filter(this::shouldRequestForGivenPermission)
                .collect(Collectors.toList());
    }

    private static class SpeedLimitChangeHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        SpeedLimitChangeHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            String speedLimit = msg.getData().getString(CarVelocityChecker.SPEED_LIMIT_MSG_KEY);
            activity.speedLimitTextView.setText(String.format("%s km/h", speedLimit));
        }
    }

}
