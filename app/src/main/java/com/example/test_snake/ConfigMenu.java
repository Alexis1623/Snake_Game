package com.example.test_snake;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class ConfigMenu extends AppCompatActivity {

    private Switch switchMusica;
    private SeekBar seekBarVolumen;
    private Button btnSalir, btnCreditos, btnReanudar;
    // testing de sebas commit
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_menu);

        // Inicializar vistassssssss
        switchMusica = findViewById(R.id.sw_musica);
        seekBarVolumen = findViewById(R.id.sbar_Volumen);
        btnSalir = findViewById(R.id.btn_salir);
        btnCreditos = findViewById(R.id.btn_creditos);
        btnReanudar = findViewById(R.id.btn_reanudar);

        // Configurar listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cerrar configuración
            }
        });

        btnReanudar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Volver al menú principal
            }
        });

        btnCreditos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // lógica para créditos
            }
        });

        switchMusica.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean musicaActivada = switchMusica.isChecked();
                //  controlar la música
            }
        });

        seekBarVolumen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Controlar volumen
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}