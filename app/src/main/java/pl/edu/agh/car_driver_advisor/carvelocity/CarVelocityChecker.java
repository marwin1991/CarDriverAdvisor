package pl.edu.agh.car_driver_advisor.carvelocity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Optional;

public class CarVelocityChecker implements Runnable {
    private final static float MAX_OVERRUN_RATIO = 1.1f;
    private final static float CONVERT_MS_TO_KMH = 3.6f;
    private final static String SPEED_EXCEEDED_NOTIFICATION_MSG = "Speed limit alert";
    public final static String SPEED_LIMIT_MSG_KEY = "speed-limit";

    private final Handler speedLimitChangeHandler;
    private final RouteDataProvider routeDataProvider;
    private final VoiceNotifier voiceNotifier;

    private final double latitude;
    private final double longitude;
    private final double speed;

    public CarVelocityChecker(VoiceNotifier voiceNotifier, double latitude, double longitude,
                              double speed, Handler speedLimitChangeHandler) {
        this.routeDataProvider = new RouteDataProvider(10000);
        this.speedLimitChangeHandler = speedLimitChangeHandler;
        this.voiceNotifier = voiceNotifier;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
    }

    @Override
    public void run() {
        Optional<Integer> allowedSpeedOpt = routeDataProvider
                .getAllowedSpeedForRouteWithGivenCords(latitude, longitude);

        if(!allowedSpeedOpt.isPresent()) {
            return;
        }

        int allowedSpeed = allowedSpeedOpt.get();
        sendSpeedLimitUpdateMessage(allowedSpeed);
        if(speed * CONVERT_MS_TO_KMH > allowedSpeed * MAX_OVERRUN_RATIO) {
            voiceNotifier.sendVoiceNotification(SPEED_EXCEEDED_NOTIFICATION_MSG);
        }
    }

    private void sendSpeedLimitUpdateMessage(int allowedSpeed) {
        Message msg = speedLimitChangeHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(SPEED_LIMIT_MSG_KEY, String.valueOf(allowedSpeed));
        msg.setData(b);
        speedLimitChangeHandler.sendMessage(msg);
    }

}
