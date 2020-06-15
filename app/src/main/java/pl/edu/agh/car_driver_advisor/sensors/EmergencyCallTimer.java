package pl.edu.agh.car_driver_advisor.sensors;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.widget.Button;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EmergencyCallTimer extends CountDownTimer {
    private AlertDialog alertDialog;
    private Button button;
    private CharSequence buttonText;
    private boolean isFinished = false;

    EmergencyCallTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    @Override
    public void onTick(long millisUntilFinished) {
        button.setText(String.format(Locale.getDefault(), "%s (%d)",
                buttonText,
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
        ));
    }

    @Override
    public void onFinish() {
        if(alertDialog.isShowing()) {
            alertDialog.dismiss();
            isFinished = true;
            resetButtonText();
        }
    }

    private void resetButtonText() {
        button.setText(String.format(Locale.getDefault(), "%s",buttonText));
    }

    void startTimer() {
        isFinished = false;
        super.start();
    }

    void cancelTimer() {
        super.cancel();
        resetButtonText();
    }

    void setAlertDialog(AlertDialog dialog) {
        this.alertDialog = dialog;
        this.button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        this.buttonText = this.button.getText();
    }

    boolean isFinished() {
        return isFinished;
    }
}
