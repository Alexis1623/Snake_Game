package com.example.test_snake;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.test_snake.MusicService;

/**
 * Pantalla de configuración con controles de audio, juego, visual, controles y cuenta.
 * Permite alternar entre modo noche y modo día guardando la preferencia.
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    // Audio
    private SeekBar seekBarMusic, seekBarSFX;
    private TextView tvMusicVolume, tvSFXVolume;

    // Juego
    private RadioGroup rgDifficulty;
    private SeekBar seekBarSpeed;
    private TextView tvSpeedValue;
    private SwitchCompat switchVibration;

    // Visual
    private Spinner spinnerTheme;
    private SwitchCompat switchFPS, switchParticles, switchNightMode;

    // Controles
    private RadioGroup rgControls;
    private SeekBar seekBarSensitivity;
    private TextView tvSensitivityValue;

    // Botones
    private Button btnChangeUser, btnClearData, btnAbout, btnResetDefaults, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        // Audio
        seekBarMusic = findViewById(R.id.seekBarMusic);
        seekBarSFX   = findViewById(R.id.seekBarSFX);
        tvMusicVolume = findViewById(R.id.tvMusicVolume);
        tvSFXVolume   = findViewById(R.id.tvSFXVolume);

        // Juego
        rgDifficulty = findViewById(R.id.rgDifficulty);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        switchVibration = findViewById(R.id.switchVibration);

        // Visual
        spinnerTheme    = findViewById(R.id.spinnerTheme);
        switchFPS       = findViewById(R.id.switchFPS);
        switchParticles = findViewById(R.id.switchParticles);
        switchNightMode = findViewById(R.id.switchNightMode);

        // Controles
        rgControls      = findViewById(R.id.rgControls);
        seekBarSensitivity = findViewById(R.id.seekBarSensitivity);
        tvSensitivityValue = findViewById(R.id.tvSensitivityValue);

        // Botones
        btnChangeUser    = findViewById(R.id.btnChangeUser);
        btnClearData     = findViewById(R.id.btnClearData);
        btnAbout         = findViewById(R.id.btnAbout);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnBack          = findViewById(R.id.btnBack);

        // Llenar spinner de temas
        String[] themes = {"Clásico Verde", "Azul Neón", "Rojo Fuego", "Morado Oscuro", "Arcoíris"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, themes);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerTheme.setAdapter(adapter);
    }

    private void loadSettings() {
        // Volúmenes
        int musicVol = prefs.getInt("music_volume", 70);
        int sfxVol   = prefs.getInt("sfx_volume", 85);
        seekBarMusic.setProgress(musicVol);
        seekBarSFX.setProgress(sfxVol);
        tvMusicVolume.setText(musicVol + "%");
        tvSFXVolume.setText(sfxVol + "%");

        // Dificultad
        String difficulty = prefs.getString("difficulty", "normal");
        switch (difficulty) {
            case "easy":  rgDifficulty.check(R.id.rbEasy);   break;
            case "hard":  rgDifficulty.check(R.id.rbHard);   break;
            default:      rgDifficulty.check(R.id.rbNormal); break;
        }

        // Velocidad inicial
        int speed = prefs.getInt("game_speed", 5);
        seekBarSpeed.setProgress(speed);
        tvSpeedValue.setText(String.valueOf(speed));

        // Vibración
        switchVibration.setChecked(prefs.getBoolean("vibration", true));

        // Tema visual
        int themeIndex = prefs.getInt("theme_index", 0);
        spinnerTheme.setSelection(themeIndex);

        // Mostrar FPS y partículas
        switchFPS.setChecked(prefs.getBoolean("show_fps", false));
        switchParticles.setChecked(prefs.getBoolean("particles", true));

        // Modo noche / día
        switchNightMode.setChecked(prefs.getBoolean("night_mode", true));

        // Control
        String control = prefs.getString("control_type", "swipe");
        if ("buttons".equals(control)) {
            rgControls.check(R.id.rbButtons);
        } else if ("tilt".equals(control)) {
            rgControls.check(R.id.rbTilt);
        } else {
            rgControls.check(R.id.rbSwipe);
        }

        // Sensibilidad
        int sensitivity = prefs.getInt("sensitivity", 50);
        seekBarSensitivity.setProgress(sensitivity);
        tvSensitivityValue.setText(sensitivity + "%");
    }

    private void setupListeners() {
        // Música
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvMusicVolume.setText(progress + "%");
                prefs.edit().putInt("music_volume", progress).apply();
                MusicService.updateVolume(SettingsActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // SFX
        seekBarSFX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvSFXVolume.setText(progress + "%");
                prefs.edit().putInt("sfx_volume", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Dificultad
        rgDifficulty.setOnCheckedChangeListener((group, id) -> {
            String diff = "normal";
            if (id == R.id.rbEasy) diff = "easy";
            else if (id == R.id.rbHard) diff = "hard";
            prefs.edit().putString("difficulty", diff).apply();
            Toast.makeText(this, "Dificultad: " + diff, Toast.LENGTH_SHORT).show();
        });

        // Velocidad
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvSpeedValue.setText(String.valueOf(progress));
                prefs.edit().putInt("game_speed", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Vibración
        switchVibration.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("vibration", isChecked).apply());

        // Tema visual
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("theme_index", position).apply();
                String themeName = parent.getItemAtPosition(position).toString();
                Toast.makeText(SettingsActivity.this, "Tema: " + themeName, Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FPS
        switchFPS.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("show_fps", isChecked).apply());

        // Partículas
        switchParticles.setOnCheckedChangeListener((v, isChecked) ->
                prefs.edit().putBoolean("particles", isChecked).apply());

        // Modo noche / día
        switchNightMode.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("night_mode", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Modo noche activado" : "Modo día activado",
                    Toast.LENGTH_SHORT).show();
        });

        // Tipo de control
        rgControls.setOnCheckedChangeListener((group, id) -> {
            String ctrl = "swipe";
            if (id == R.id.rbButtons) ctrl = "buttons";
            else if (id == R.id.rbTilt) ctrl = "tilt";
            prefs.edit().putString("control_type", ctrl).apply();
            Toast.makeText(this, "Control: " + ctrl, Toast.LENGTH_SHORT).show();
        });

        // Sensibilidad
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvSensitivityValue.setText(progress + "%");
                prefs.edit().putInt("sensitivity", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Botón: cambiar usuario
        btnChangeUser.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("force_edit", true);
            startActivity(i);
            finish();
        });

        // Botón: borrar datos
        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Confirmar")
                    .setMessage("¿Borrar TODOS los datos? Esta acción no se puede deshacer.")
                    .setPositiveButton("Borrar", (dialog, which) -> {
                        prefs.edit().clear().apply();
                        Toast.makeText(this, "Datos borrados", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón: acerca de
        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("🐍 Snake Game")
                    .setMessage("Versión 1.0.0\n\nDesarrollado por Grupo 5\n\n¡Disfruta del juego!")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Botón: restaurar predeterminados
        btnResetDefaults.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Restaurar configuración")
                    .setMessage("¿Restaurar todos los valores a sus ajustes predeterminados?")
                    .setPositiveButton("Restaurar", (dialog, which) -> {
                        resetToDefaults();
                        Toast.makeText(this, "Configuración restaurada", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón: volver
        btnBack.setOnClickListener(v -> finish());
    }

    private void resetToDefaults() {
        String username = prefs.getString("username", "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putString("username", username);
        editor.putInt("music_volume", 70);
        editor.putInt("sfx_volume", 85);
        editor.putString("difficulty", "normal");
        editor.putInt("game_speed", 5);
        editor.putBoolean("vibration", true);
        editor.putInt("theme_index", 0);
        editor.putBoolean("show_fps", false);
        editor.putBoolean("particles", true);
        editor.putString("control_type", "swipe");
        editor.putInt("sensitivity", 50);
        editor.putBoolean("night_mode", true); // dejar activado por defecto
        editor.apply();
        loadSettings();
    }
}