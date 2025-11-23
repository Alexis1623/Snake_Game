package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar seekBarMusic, seekBarSFX;
    private TextView tvMusicVolume, tvSFXVolume;
    private Button btnResetDefaults, btnBack;
    private SharedPreferences prefs;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);

        // Inicializar vistas
        seekBarMusic = findViewById(R.id.seekBarMusic);
        seekBarSFX = findViewById(R.id.seekBarSFX);
        tvMusicVolume = findViewById(R.id.tvMusicVolume);
        tvSFXVolume = findViewById(R.id.tvSFXVolume);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnBack = findViewById(R.id.btnBack);

        // Cargar configuración guardada
        loadSavedSettings();

        // Configurar listeners
        setupSeekBarListeners();
        setupButtonListeners();

        Log.d(TAG, "SettingsActivity creado");
    }

    private void loadSavedSettings() {
        int savedVolume = prefs.getInt("music_volume", 70);

        seekBarMusic.setProgress(savedVolume);
        tvMusicVolume.setText(savedVolume + "%");

        Log.d(TAG, "Volumen cargado: " + savedVolume + "%");
    }

    private void setupSeekBarListeners() {
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvMusicVolume.setText(progress + "%");
                    updateMusicVolume(progress);

                    // Mostrar mute cuando sea 0
                    if (progress == 0) {
                        Toast.makeText(SettingsActivity.this, "Mute", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int volume = seekBar.getProgress();
                saveVolumeSetting(volume);
                Log.d(TAG, "Volumen guardado: " + volume + "%");
            }
        });
   seekBarSFX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvSFXVolume.setText(progress + "%");
                 }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
              }
        });
    }

    private void setupButtonListeners() {
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Volviendo al menú principal");
            finish();
        });

        btnResetDefaults.setOnClickListener(v -> {
            Log.d(TAG, "Restableciendo valores por defecto");
            resetToDefaults();
            Toast.makeText(this, "Valores restablecidos", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateMusicVolume(int volume) {
        // Guardar inmediatamente
        saveVolumeSetting(volume);

        // Actualizar el servicio de música
        MusicService.updateVolume(this);

        Log.d(TAG, "Volumen actualizado a: " + volume + "%");
    }

    private void saveVolumeSetting(int volume) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("music_volume", volume);
        editor.apply();
    }

    private void resetToDefaults() {
        int defaultVolume = 70;

        seekBarMusic.setProgress(defaultVolume);
        tvMusicVolume.setText(defaultVolume + "%");
        saveVolumeSetting(defaultVolume);

        // Actualizar volumen inmediatamente
        MusicService.updateVolume(this);

        Log.d(TAG, "Valores restablecidos a volumen: " + defaultVolume + "%");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "SettingsActivity resumido");

        // Recargar configuración por si cambió en otro lugar
        int currentVolume = prefs.getInt("music_volume", 70);
        seekBarMusic.setProgress(currentVolume);
        tvMusicVolume.setText(currentVolume + "%");
    }
}