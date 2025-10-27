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
    private SwitchCompat switchFPS, switchParticles;

    // Controles
    private RadioGroup rgControls;
    private SeekBar seekBarSensitivity;
    private TextView tvSensitivityValue;

    // Botones
    private Button btnChangeUser, btnClearData, btnAbout, btnResetDefaults;

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
        seekBarSFX = findViewById(R.id.seekBarSFX);
        tvMusicVolume = findViewById(R.id.tvMusicVolume);
        tvSFXVolume = findViewById(R.id.tvSFXVolume);

        // Juego
        rgDifficulty = findViewById(R.id.rgDifficulty);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        switchVibration = findViewById(R.id.switchVibration);

        // Visual
        spinnerTheme = findViewById(R.id.spinnerTheme);
        switchFPS = findViewById(R.id.switchFPS);
        switchParticles = findViewById(R.id.switchParticles);

        // Controles
        rgControls = findViewById(R.id.rgControls);
        seekBarSensitivity = findViewById(R.id.seekBarSensitivity);
        tvSensitivityValue = findViewById(R.id.tvSensitivityValue);

        // Botones
        btnChangeUser = findViewById(R.id.btnChangeUser);
        btnClearData = findViewById(R.id.btnClearData);
        btnAbout = findViewById(R.id.btnAbout);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);

        // Configurar Spinner de temas
        String[] themes = {"Clásico Verde", "Azul Neón", "Rojo Fuego", "Morado Oscuro", "Arcoíris"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, themes);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerTheme.setAdapter(adapter);
    }

    private void loadSettings() {
        // Cargar Audio
        int musicVolume = prefs.getInt("music_volume", 70);
        int sfxVolume = prefs.getInt("sfx_volume", 85);
        seekBarMusic.setProgress(musicVolume);
        seekBarSFX.setProgress(sfxVolume);
        tvMusicVolume.setText(musicVolume + "%");
        tvSFXVolume.setText(sfxVolume + "%");

        // Cargar Dificultad
        String difficulty = prefs.getString("difficulty", "normal");
        switch (difficulty) {
            case "easy":
                rgDifficulty.check(R.id.rbEasy);
                break;
            case "hard":
                rgDifficulty.check(R.id.rbHard);
                break;
            default:
                rgDifficulty.check(R.id.rbNormal);
                break;
        }

        // Cargar Velocidad
        int speed = prefs.getInt("game_speed", 5);
        seekBarSpeed.setProgress(speed);
        tvSpeedValue.setText(String.valueOf(speed));

        // Cargar Vibración
        boolean vibration = prefs.getBoolean("vibration", true);
        switchVibration.setChecked(vibration);

        // Cargar Tema
        int themeIndex = prefs.getInt("theme_index", 0);
        spinnerTheme.setSelection(themeIndex);

        // Cargar FPS y Partículas
        boolean showFPS = prefs.getBoolean("show_fps", false);
        boolean particles = prefs.getBoolean("particles", true);
        switchFPS.setChecked(showFPS);
        switchParticles.setChecked(particles);

        // Cargar Tipo de Control
        String controlType = prefs.getString("control_type", "swipe");
        switch (controlType) {
            case "buttons":
                rgControls.check(R.id.rbButtons);
                break;
            case "tilt":
                rgControls.check(R.id.rbTilt);
                break;
            default:
                rgControls.check(R.id.rbSwipe);
                break;
        }

        // Cargar Sensibilidad
        int sensitivity = prefs.getInt("sensitivity", 50);
        seekBarSensitivity.setProgress(sensitivity);
        tvSensitivityValue.setText(sensitivity + "%");
    }

    private void setupListeners() {
        // SeekBar Música
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMusicVolume.setText(progress + "%");
                prefs.edit().putInt("music_volume", progress).apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // SeekBar SFX
        seekBarSFX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSFXVolume.setText(progress + "%");
                prefs.edit().putInt("sfx_volume", progress).apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Dificultad
        rgDifficulty.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String difficulty = "normal";
                if (checkedId == R.id.rbEasy) difficulty = "easy";
                else if (checkedId == R.id.rbHard) difficulty = "hard";
                prefs.edit().putString("difficulty", difficulty).apply();
                Toast.makeText(SettingsActivity.this, "Dificultad: " + difficulty, Toast.LENGTH_SHORT).show();
            }
        });

        // Velocidad
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedValue.setText(String.valueOf(progress));
                prefs.edit().putInt("game_speed", progress).apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Vibración
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibration", isChecked).apply();
        });

        // Tema
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("theme_index", position).apply();
                String themeName = parent.getItemAtPosition(position).toString();
                Toast.makeText(SettingsActivity.this, "Tema: " + themeName, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FPS
        switchFPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("show_fps", isChecked).apply();
        });

        // Partículas
        switchParticles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("particles", isChecked).apply();
        });

        // Tipo de Control
        rgControls.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String controlType = "swipe";
                if (checkedId == R.id.rbButtons) controlType = "buttons";
                else if (checkedId == R.id.rbTilt) controlType = "tilt";
                prefs.edit().putString("control_type", controlType).apply();
                Toast.makeText(SettingsActivity.this, "Control: " + controlType, Toast.LENGTH_SHORT).show();
            }
        });

        // Sensibilidad
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSensitivityValue.setText(progress + "%");
                prefs.edit().putInt("sensitivity", progress).apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Cambiar Usuario
        btnChangeUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.putExtra("force_edit", true);
                startActivity(intent);
                finish();
            }
        });

        // Borrar Datos
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("⚠️ Confirmar")
                    .setMessage("¿Estás seguro de borrar TODOS los datos? Esta acción no se puede deshacer.")
                    .setPositiveButton("Borrar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().clear().apply();
                            Toast.makeText(SettingsActivity.this, "Todos los datos borrados", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            }
        });

        // Acerca de
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("🐍 Snake Game")
                    .setMessage("Versión: 1.0.0\n\n" +
                            "Desarrollado por: ULATINA\n" +
                            "Año: 2025\n\n" +
                            "Un clásico juego de serpiente con controles modernos y configuración completa.\n\n" +
                            "¡Disfruta del juego!")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });

        // Restaurar Valores
        btnResetDefaults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Restaurar configuración")
                    .setMessage("¿Restaurar todos los valores a sus ajustes predeterminados?")
                    .setPositiveButton("Restaurar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetToDefaults();
                            Toast.makeText(SettingsActivity.this, "Configuración restaurada", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            }
        });
    }

    private void resetToDefaults() {
        // Guardar solo el username
        String username = prefs.getString("username", "");

        // Limpiar todas las preferencias de configuración
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putString("username", username); // Restaurar username

        // Valores predeterminados
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
        editor.apply();

        // Recargar la configuración en la UI
        loadSettings();
    }
}