package com.isetb.lightsensorapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceControlActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private TextView voiceStatusTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "permission not allowed", Toast.LENGTH_SHORT).show();
        }


        voiceStatusTextView = findViewById(R.id.voice_status_text);
        Button startListeningButton = findViewById(R.id.start_listening_button);

        // Configurer le SpeechRecognizer
        setupSpeechRecognizer();

        // Démarrer l'écoute vocale au clic
        startListeningButton.setOnClickListener(v -> startListening());
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                voiceStatusTextView.setText("Prêt à écouter...");
            }

            @Override
            public void onBeginningOfSpeech() {
                voiceStatusTextView.setText("Écoute...");
            }

            @Override
            public void onEndOfSpeech() {
                voiceStatusTextView.setText("Traitement...");
            }

            @Override
            public void onError(int error) {
                voiceStatusTextView.setText("Erreur d'écoute : " + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processVoiceCommand(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {

            }
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void processVoiceCommand(String command) {
        command = command.toLowerCase();
        voiceStatusTextView.setText("Commande reçue : " + command);
        if(command.equalsIgnoreCase("augmente")) {
            Toast.makeText(this, "augment", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("command", "increaseBrightness"); // Replace with your command
            startActivity(intent);

        }
        else if(command.equalsIgnoreCase("diminue"))
        {
            Toast.makeText(this, "augment", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("command", "decreaseBrightness"); // Replace with your command
            startActivity(intent);
        }





    }


    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
    private boolean matchesCommand(String command, List<String> keywords) {
        for (String keyword : keywords) {
            if (command.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
