package pl.edu.agh.car_driver_advisor.sensors;

import android.app.Activity;
import android.app.AlertDialog;

import pl.edu.agh.car_driver_advisor.MainActivity;
import pl.edu.agh.car_driver_advisor.R;

public class DialogService {

    private static final String CRASH_TITLE = "Wykryto kolizję!";
    private static final String CRASH_MSG = "Czy wezwać pomoc?";
    private static final String EM_CALLED_MSG = "Pomoc wezwana";

    private static final int AUTO_ACCEPT_TIME = 16000;

    public static AlertDialog createCrashDialog(MainActivity activity) {
        EmergencyCallTimer timer = new EmergencyCallTimer(AUTO_ACCEPT_TIME, 100);
        AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.CrashDialogTheme)
                .setTitle(CRASH_TITLE)
                .setMessage(CRASH_MSG)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    activity.callEmergency();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.no, null)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            timer.setAlertDialog((AlertDialog) dialog);
            timer.startTimer();
        });

        alertDialog.setOnDismissListener(dialog -> {
            timer.cancelTimer();

            if(timer.isFinished()) {
                activity.callEmergency();
            }
        });

        return alertDialog;
    }

    public static AlertDialog createEmergencyCalledAlert(Activity activity) {
        return new AlertDialog.Builder(activity, R.style.CrashDialogTheme)
                .setTitle(CRASH_TITLE)
                .setMessage(EM_CALLED_MSG)
                .setPositiveButton(android.R.string.yes, null)
                .create();
    }
}
