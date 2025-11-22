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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

// IMPORTS FIREBASE
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private DatabaseReference settingsRef; // referencia a Realtime DB
    private String username; // nombre de usuario para la ruta

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

        // obtener username guardado (si no existe, no conectamos a Firebase)
        username = prefs.getString("username", "");
        if (!username.isEmpty()) {
            settingsRef = FirebaseDatabase.getInstance()
                    .getReference("user_settings")
                    .child(username);
            loadFromFirebase();
        }

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

    // NUEVO: cargar desde Firebase (una sola vez al iniciar)
    private void loadFromFirebase() {
        if (settingsRef == null) return;

        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    SharedPreferences.Editor editor = prefs.edit();

                    if (snapshot.child("music_volume").exists())
                        editor.putInt("music_volume", snapshot.child("music_volume").getValue(Integer.class));
                    if (snapshot.child("sfx_volume").exists())
                        editor.putInt("sfx_volume", snapshot.child("sfx_volume").getValue(Integer.class));
                    if (snapshot.child("difficulty").exists())
                        editor.putString("difficulty", snapshot.child("difficulty").getValue(String.class));
                    if (snapshot.child("game_speed").exists())
                        editor.putInt("game_speed", snapshot.child("game_speed").getValue(Integer.class));
                    if (snapshot.child("vibration").exists())
                        editor.putBoolean("vibration", snapshot.child("vibration").getValue(Boolean.class));
                    if (snapshot.child("theme_index").exists())
                        editor.putInt("theme_index", snapshot.child("theme_index").getValue(Integer.class));
                    if (snapshot.child("show_fps").exists())
                        editor.putBoolean("show_fps", snapshot.child("show_fps").getValue(Boolean.class));
                    if (snapshot.child("particles").exists())
                        editor.putBoolean("particles", snapshot.child("particles").getValue(Boolean.class));
                    if (snapshot.child("control_type").exists())
                        editor.putString("control_type", snapshot.child("control_type").getValue(String.class));
                    if (snapshot.child("sensitivity").exists())
                        editor.putInt("sensitivity", snapshot.child("sensitivity").getValue(Integer.class));

                    editor.apply();
                    loadSettings(); // actualizar UI con lo obtenido
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this,
                        "Error al cargar configuración", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NUEVO: guardar un par clave/valor en Firebase
    private void saveToFirebase(String key, Object value) {
        if (settingsRef != null) {
            settingsRef.child(key).setValue(value);
        }
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
                saveToFirebase("music_volume", progress); // guardar en Firebase
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
                saveToFirebase("sfx_volume", progress); // guardar en Firebase
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
                saveToFirebase("difficulty", difficulty);
                Toast.makeText(SettingsActivity.this, "Dificultad: " + difficulty, Toast.LENGTH_SHORT).show();
            }
        });

        // Velocidad
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedValue.setText(String.valueOf(progress));
                prefs.edit().putInt("game_speed", progress).apply();
                saveToFirebase("game_speed", progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Vibración
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibration", isChecked).apply();
            saveToFirebase("vibration", isChecked);
        });

        // Tema
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("theme_index", position).apply();
                saveToFirebase("theme_index", position);
                String themeName = parent.getItemAtPosition(position).toString();
                Toast.makeText(SettingsActivity.this, "Tema: " + themeName, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FPS
        switchFPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("show_fps", isChecked).apply();
            saveToFirebase("show_fps", isChecked);
        });

        // Partículas
        switchParticles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("particles", isChecked).apply();
            saveToFirebase("particles", isChecked);
        });

        // Tipo de Control
        rgControls.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String controlType = "swipe";
                if (checkedId == R.id.rbButtons) controlType = "buttons";
                else if (checkedId == R.id.rbTilt) controlType = "tilt";
                prefs.edit().putString("control_type", controlType).apply();
                saveToFirebase("control_type", controlType);
                Toast.makeText(SettingsActivity.this, "Control: " + controlType, Toast.LENGTH_SHORT).show();
            }
        });

        // Sensibilidad
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSensitivityValue.setText(progress + "%");
                prefs.edit().putInt("sensitivity", progress).apply();
                saveToFirebase("sensitivity", progress);
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
                            // Borrar de Firebase si existe
                            if (settingsRef != null) {
                                settingsRef.removeValue();
                            }
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

        // Guardar defaults en Firebase (si corresponde)
        if (settingsRef != null) {
            saveToFirebase("music_volume", 70);
            saveToFirebase("sfx_volume", 85);
            saveToFirebase("difficulty", "normal");
            saveToFirebase("game_speed", 5);
            saveToFirebase("vibration", true);
            saveToFirebase("theme_index", 0);
            saveToFirebase("show_fps", false);
            saveToFirebase("particles", true);
            saveToFirebase("control_type", "swipe");
            saveToFirebase("sensitivity", 50);
        }

        // Recargar la configuración en la UI
        loadSettings();
    }
}