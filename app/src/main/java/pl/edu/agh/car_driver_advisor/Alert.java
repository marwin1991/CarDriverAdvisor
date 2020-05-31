package pl.edu.agh.car_driver_advisor;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Alert {

    public static void makeAlert(LayoutInflater inflater, Activity activity, String s) {
        try {
            Thread thread = new Thread() {
                public void run() {
                    activity.runOnUiThread(() -> {
                        View layout = inflater.inflate(R.layout.custom_toast,
                                activity.findViewById(R.id.custom_toast_container));

                        TextView text = layout.findViewById(R.id.text);
                        text.setText(s);

                        Toast toast = new Toast(activity.getApplicationContext());
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.setView(layout);
                        toast.show();

                        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                        toneGen1.startTone(ToneGenerator.TONE_SUP_ERROR, 250);
                    });
                }
            };
            thread.start();
        } catch (Exception ignore){

        }
    }
}
