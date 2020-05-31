package pl.edu.agh.car_driver_advisor.carvelocity;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class VoiceNotifier {

    private TextToSpeech tts;

    public VoiceNotifier(Context applicationContext) {
        this.tts = new TextToSpeech(applicationContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if(tts.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
                    tts.setLanguage(Locale.US);
            } else if (status == TextToSpeech.ERROR) {
                Toast.makeText(applicationContext, "Text To Speech failed...",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void sendVoiceNotification(String messageToRead) {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        speakGivenSentences(messageToRead);
    }

    private void speakGivenSentences(String sentence) {
        tts.speak(sentence, TextToSpeech.LANG_COUNTRY_AVAILABLE, null, null);
    }

}
