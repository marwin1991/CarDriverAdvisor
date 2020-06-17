package pl.edu.agh.car_driver_advisor.carvelocity;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Optional;

public class CarVelocityChecker implements Runnable {
    private final static float MAX_OVERRUN_RATIO = 1.1f;
    private final static float CONVERT_MS_TO_KMH = 3.6f;
    private final static String SPEED_EXCEEDED_NOTIFICATION_MSG = "Przekroczono dopuszczalną prędkość";
    private final static long MIN_TIME_INTERVAL_BETWEEN_API_CALLS_IN_MS = 10000;    // 10s
    private final static long MIN_TIME_INTERVAL_BETWEEN_NOTIFICATIONS = 20000;    // 20s

    public final static String CAR_SPEED_MSG_KEY = "car-speed";
    public final static String SPEED_LIMIT_MSG_KEY = "speed-limit";
    public final static String SPEED_LIMIT_EXTENDED_MSG_KEY = "speed-limit-extended";

    private static long lastApiCallTime;
    private static long lastNotificationTime;
    private static Integer lastApiCallResult;
    private static Location lastLocation;

    private final Handler speedLimitChangeHandler;
    private final RouteDataProvider routeDataProvider;
    private final VoiceNotifier voiceNotifier;

    private Location location;
    private double speed;

    public CarVelocityChecker(VoiceNotifier voiceNotifier, Location location,
                              double speed, Handler speedLimitChangeHandler) {
        this.routeDataProvider = new RouteDataProvider();
        this.speedLimitChangeHandler = speedLimitChangeHandler;
        this.voiceNotifier = voiceNotifier;
        this.location = location;
        this.speed = speed;
    }

    @Override
    public void run() {
        synchronized (CarVelocityChecker.class) {
            if(areProvidedDataNotCorrect()) {
                return;
            }

            speed = speed == -1.0 ?
                    calculateCarSpeedInKMH() :
                    speed * CONVERT_MS_TO_KMH;

            sendCarSpeedUpdateMessageToHandler((int) speed);
            lastLocation = location;

            Optional<Integer> allowedSpeedOpt = getSpeedLimitFromAPIorCache();
            if (!allowedSpeedOpt.isPresent()) {
                return;
            }

            int allowedSpeed = allowedSpeedOpt.get();
            boolean speedLimitExtended = speed > allowedSpeed * MAX_OVERRUN_RATIO;
            sendSpeedLimitUpdateMessageToHandler(allowedSpeed, speedLimitExtended);

            if (speedLimitExtended && location.getTime() >
                    lastNotificationTime + MIN_TIME_INTERVAL_BETWEEN_NOTIFICATIONS) {
                voiceNotifier.sendVoiceNotification(SPEED_EXCEEDED_NOTIFICATION_MSG);
                lastNotificationTime = location.getTime();
            }
        }
    }
    
    public boolean areProvidedDataNotCorrect() {
        if(lastLocation == null && speed == -1.0) {
            lastLocation = location;
            return true;
        }
        else if(lastLocation == null) {
            return false;
        }

        return location.getTime() < lastLocation.getTime();
    }

    private double calculateCarSpeedInKMH() {
        double distance = location.distanceTo(lastLocation);
        double time = (location.getTime() - lastLocation.getTime()) / 1000.0;
        return (distance / time) * CONVERT_MS_TO_KMH;
    }

    private Optional<Integer> getSpeedLimitFromAPIorCache() {
        long currentTime = System.currentTimeMillis();
        if (lastApiCallTime + MIN_TIME_INTERVAL_BETWEEN_API_CALLS_IN_MS > currentTime) {
            return Optional.ofNullable(lastApiCallResult);
        }

        lastApiCallTime = currentTime;
        lastApiCallResult = routeDataProvider
                .getAllowedSpeedForRouteWithGivenCords(location.getLatitude(), location.getLongitude())
                .orElse(null);

        return Optional.ofNullable(lastApiCallResult);
    }

    private void sendCarSpeedUpdateMessageToHandler(int speed) {
        Message msg = speedLimitChangeHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(CAR_SPEED_MSG_KEY, String.valueOf(speed));
        msg.setData(b);
        speedLimitChangeHandler.sendMessage(msg);
    }

    private void sendSpeedLimitUpdateMessageToHandler(int allowedSpeed, boolean speedLimitExtended) {
        Message msg = speedLimitChangeHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(SPEED_LIMIT_MSG_KEY, String.valueOf(allowedSpeed));
        b.putBoolean(SPEED_LIMIT_EXTENDED_MSG_KEY, speedLimitExtended);
        msg.setData(b);
        speedLimitChangeHandler.sendMessage(msg);
    }

}
