package com.isetb.lightsensorapp;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private boolean isAutoBrightnessEnabled = false;
    private boolean isReadingModeEnabled = false;
    private boolean isBatteryModeEnabled = false;
    private FrameLayout mainButton;
    private SpeechRecognizer speechRecognizer;
    private FrameLayout batteryOption;
    private FrameLayout readingOption;
    private boolean isVoiceActive = false;
    private View filterView;

    private FrameLayout voiceOption;
    private TextToSpeech textToSpeech;
    private SeekBar brightnessSeekBar;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Button readingModeButton;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_WRITE_SETTINGS_PERMISSION = 1002;
    private WindowManager windowManager;
    private WindowManager.LayoutParams overlayParams;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        brightnessSeekBar = findViewById(R.id.brightness_seekbar);
        mainButton = findViewById(R.id.on_off_button);
        batteryOption = findViewById(R.id.battery_option);
        readingOption = findViewById(R.id.reading_option);
        voiceOption = findViewById(R.id.voice_option);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        //requestWriteSettingsPermission();
        initializeOverlay();
        if (lightSensor == null) {
            Toast.makeText(this, "Capteur de lumière non disponible", Toast.LENGTH_SHORT).show();
        }
        configureSeekBar();
        initializeSeekBar();
        updateButtonState(mainButton, false);
        voiceOption.setOnClickListener(v -> openVoiceControl());
    }
    // Méthode pour ouvrir l'interface vocale
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }
    private void setupSpeechRecognizer() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "permission not allowed", Toast.LENGTH_SHORT).show();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }
            @Override
            public void onBeginningOfSpeech() {
            }
            @Override
            public void onEndOfSpeech() {
                startListening();
            }
            @Override
            public void onError(int error) {
                startListening();
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
            public void onBufferReceived(byte[] buffer) {}
        });
    }
    private void openVoiceControl() {
        // Toggle the state
        isVoiceActive = !isVoiceActive;
        // Change the background color based on the state
        GradientDrawable background = (GradientDrawable) voiceOption.getBackground();
        if (isVoiceActive) {
            background.setColor(getResources().getColor(android.R.color.holo_green_dark)); // Set to green
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.speak("Bonjour, comment je peux vous aider ?", TextToSpeech.QUEUE_FLUSH, null);
                }
            });
            setupSpeechRecognizer();
            startListening();
        } else {
            background.setColor(getResources().getColor(android.R.color.darker_gray)); // Reset to gray
            if (textToSpeech != null) {
                textToSpeech.speak("Au revoir.", TextToSpeech.QUEUE_FLUSH, null);
                // i want to destroy the listener
            }
        }
    }
    // Méthode pour activer/désactiver la luminosité automatique
    public void toggleAutoBrightness(View view) {
        isAutoBrightnessEnabled = !isAutoBrightnessEnabled;
        if (isAutoBrightnessEnabled) {
            Toast.makeText(this, "Mode automatique activé", Toast.LENGTH_SHORT).show();
            updateButtonState(mainButton, true);
            monitorLightSensor();
        } else {
            Toast.makeText(this, "Mode automatique désactivé", Toast.LENGTH_SHORT).show();
            updateButtonState(mainButton, false);
            sensorManager.unregisterListener(lightSensorListener);
        }
    }
    // Méthode pour activer/désactiver le mode batterie
    public void activateBatteryMode(View view) {
        isBatteryModeEnabled = !isBatteryModeEnabled;
        if (isBatteryModeEnabled) {
            Toast.makeText(this, "Mode économie d'énergie activé", Toast.LENGTH_SHORT).show();
            updateButtonState(batteryOption, true);
            adjustBrightness(0);
            updateSeekBarProgress(0);
        } else {
            Toast.makeText(this, "Mode économie d'énergie désactivé", Toast.LENGTH_SHORT).show();
            updateButtonState(batteryOption, false);
            adjustBrightness(500);
            updateSeekBarProgress(500);// Revenir à une luminosité normale
        }
    }
    // Méthode pour activer/désactiver le mode lecture
    public void toggleReadingMode(View view) {

        if (isReadingModeEnabled) {
            disableReadingMode();
        } else {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }
            enableReadingMode();
        }
    }
    // Méthode pour surveiller le capteur de lumière
    private void monitorLightSensor() {
        if (lightSensor != null) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    private final SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (isAutoBrightnessEnabled) {
                float lightValue = event.values[0];
                adjustBrightness(lightValue);
                updateSeekBarProgress(lightValue);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private void adjustAutoBrightnesss(float lightValue) {
        try {
            float brightness = Math.min(1, Math.max(0, lightValue / 1000));
            Toast.makeText(this, "gonna change the lightnes", Toast.LENGTH_SHORT).show();
            adjustBrightness((int) (brightness * 255));
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission requise pour modifier la luminosité.", Toast.LENGTH_SHORT).show();
        }
    }
    private void requestWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
    }
    private void adjustBrightness(float lightValue) {
        if (Settings.System.canWrite(this)) {
            float brightness = lightValue / 1000;
            brightness = Math.min(1, Math.max(0, brightness)); // Clamp entre 0 et 1
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    (int) (brightness * 255) // Conversion en échelle 0-255
            );
        } else {
            requestWriteSettingsPermission();
        }
    }
    private void configureSeekBar() {
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoBrightnessEnabled) {
                    adjustBrightness((int) (progress / 100.0 * 255));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void initializeSeekBar() {
        try {
            int currentBrightness = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
            );
            int brightnessPercentage = (int) (currentBrightness / 255.0 * 100);
            brightnessSeekBar.setProgress(brightnessPercentage);
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Impossible de récupérer la luminosité actuelle.", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateSeekBarProgress(float lightValue) {
        int progress = Math.min(100, Math.max(0, (int) (lightValue / 10)));
        brightnessSeekBar.setProgress(progress*2);
    }
    private void updateButtonState(View button, boolean isActive) {
        GradientDrawable background = (GradientDrawable) button.getBackground();
        if (isActive) {
            background.setColor(getResources().getColor(android.R.color.holo_green_dark)); // Vert
        } else {
            background.setColor(getResources().getColor(android.R.color.darker_gray)); // Gris
        }
    }
    //vocal
    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    private void processVoiceCommand(String command) {
        command = command.toLowerCase();
        Toast.makeText(this, command, Toast.LENGTH_SHORT).show();
        if (command.equalsIgnoreCase("augmente")) {
            // Réponse vocale en français pour augmenter la luminosité
            adjustBrightness(500);
            updateSeekBarProgress(500);
            textToSpeech.speak("J'augmente la luminosité.", TextToSpeech.QUEUE_FLUSH, null);
        } else if (command.equalsIgnoreCase("diminue")) {
            // Réponse vocale en français pour diminuer la luminosité
            adjustBrightness(0);
            updateSeekBarProgress(0);
            textToSpeech.speak("Je diminue la luminosité.", TextToSpeech.QUEUE_FLUSH, null);
        } else if(command.equalsIgnoreCase("au revoir"))
            // Réponse vocale pour une commande non reconnue
            openVoiceControl();
    }
    private void enableReadingMode() {
        if (filterView == null) { // Only add the overlay if it's not already added
            filterView = getLayoutInflater().inflate(R.layout.filter_overlay, null);
            windowManager.addView(filterView, overlayParams);

            // Reduce brightness
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        50 // Set brightness to a comfortable level
                );
            }

            isReadingModeEnabled = true;
            Toast.makeText(this, "Mode Lecture activé", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateReadingModeButton() {

    }
    private void disableReadingMode() {
        // Supprimer le filtre jaune
        try {
            windowManager.removeViewImmediate(filterView); // Remove the overlay
            filterView = null; // Clear the reference
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Restaurer la luminosité
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    150 // Rétablir une luminosité normale

            );

        }

        isReadingModeEnabled = false;
        updateReadingModeButton();
        Toast.makeText(this, "Mode Lecture désactivé", Toast.LENGTH_SHORT).show();
    }
    private void toggleReadingMode() {
        if (isReadingModeEnabled) {
            disableReadingMode();
        } else {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }
            enableReadingMode();
        }
    }
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    }
    private void initializeOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.CENTER;
    }
}