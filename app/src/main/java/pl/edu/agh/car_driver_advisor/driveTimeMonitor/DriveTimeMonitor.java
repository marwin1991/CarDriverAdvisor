package pl.edu.agh.car_driver_advisor.driveTimeMonitor;

import android.location.Location;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class DriveTimeMonitor {
    private final String SuggestABreak = "Prowadzisz już %d godzin, zaplanuj %d przerwę.";
    private final String BreakRecommended = "Prowadzisz już %d godzin, zrób conajmniej %d min. przerwę.";
    private final String BreakRequired = "Prowadzisz już %d godzin, konieczna %d min. przerwa.";

    private int BREAK_DIST_EPS_IN_METERS = 150;
    private int MIN_BREAK_DURATION_IN_MS = 15 * 60 * 1000 - 1;
    private int INTERVAL_BETWEEN_ALERTS_MS = 10 * 60 * 1000; // 10 min
    private List<BreakRecommendation> recommendations;
    private Consumer<String> notifyAction;

    /* for check if location after MIN_BREAK_DURATION was break of GPS signal lost */
    private Location lastLocation;
    private Location lastBreakLocation;

    private int driveTime;
    private int breakTime;
    private int requiredBreak = MIN_BREAK_DURATION_IN_MS;
    private long lastAlertTriggeredTime;

    public DriveTimeMonitor(Consumer<String> notifyAction) {
        this.notifyAction = notifyAction;
        driveTime = breakTime = 0;
        recommendations = Arrays.asList(
                new BreakRecommendation(4*3600*1000, 30, true, BreakRequired),
                new BreakRecommendation(2*3600*1000, 15, false, BreakRecommended),
                new BreakRecommendation((int)(1.5*3600*1000), 15, false, SuggestABreak)
        );
    }

    public void update(Location location){
        if (lastLocation == null) { lastBreakLocation = lastLocation = location; return; }
        long msElapsedSinceLastUpdate = location.getTime() - lastLocation.getTime();
        float movedDistance = lastLocation.distanceTo(location);
        lastLocation = location;

        if (msElapsedSinceLastUpdate > MIN_BREAK_DURATION_IN_MS && movedDistance < BREAK_DIST_EPS_IN_METERS) {
            // no update for a long time
            breakTime += msElapsedSinceLastUpdate;
            if (breakTime > requiredBreak) { resetDriveTime(); }
        }
        if (lastBreakLocation.distanceTo(location) < BREAK_DIST_EPS_IN_METERS) {
            // small movement around break location
            breakTime += msElapsedSinceLastUpdate; driveTime += msElapsedSinceLastUpdate;
            if (breakTime > requiredBreak) { resetDriveTime(); }
        } else {
            driveTime += msElapsedSinceLastUpdate;
            breakTime = 0; lastBreakLocation = location;
            BreakRecommendation r = selectBreakRecommendation();
            requiredBreak = r.getRequiredBreakTimeMs();
            if (r != null && location.getTime() - lastAlertTriggeredTime > INTERVAL_BETWEEN_ALERTS_MS) {
                notifyRecommendation(r);
                lastAlertTriggeredTime = location.getTime();
            }
        }
    }

    private void resetDriveTime() {
        driveTime = 0;
    }

    private BreakRecommendation selectBreakRecommendation() {
        for (BreakRecommendation b : recommendations) {
            if (b.match(driveTime)) return b;
        }
        return null;
    }

    private void notifyRecommendation(BreakRecommendation recommendation) {
        if (notifyAction == null || recommendation == null) return;
        notifyAction.accept(recommendation.getMsg());
    }
}

class BreakRecommendation {
    private final int requiredDriveTimeMs;
    private final int recommendedBreakDurationMin;
    private final boolean required;
    private final String msg;

    public BreakRecommendation(int requiredDriveTimeMs, int recommendedBreakDurationMin, boolean required, String msg) {
        this.requiredDriveTimeMs = requiredDriveTimeMs;
        this.recommendedBreakDurationMin = recommendedBreakDurationMin;
        this.required = required;
        this.msg = msg;
    }

    public boolean match(int driveTimeWithoutBreakMs) {
        return driveTimeWithoutBreakMs >= requiredDriveTimeMs;
    }

    public String getMsg() { return String.format(msg, msToHours(requiredDriveTimeMs), recommendedBreakDurationMin); }

    public int getRequiredBreakTimeMs() {return recommendedBreakDurationMin * 1000; }

    public boolean isRequired() { return required; }

    private static int msToHours(int mseconds) { return (int)(mseconds / (3600 * 1000)); }
}