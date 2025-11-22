package com.example.test_snake;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private Button btnPlay;
    private Button btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);

        etUsername = findViewById(R.id.etUsername);
        btnPlay = findViewById(R.id.btnPlay);
        btnGuest = findViewById(R.id.btnGuest);

        // Cargar el último usuario guardado en el campo (si existe)
        String savedUsername = prefs.getString("username", "");
        if (!TextUtils.isEmpty(savedUsername)) {
            etUsername.setText(savedUsername);
            etUsername.setSelection(savedUsername.length()); // Cursor al final
        }

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();

                if (TextUtils.isEmpty(username)) {
                    username = "Invitado";
                }

                if (username.length() > 20) {
                    etUsername.setError("Máximo 20 caracteres");
                    Toast.makeText(LoginActivity.this, "El nombre no puede superar 20 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Guardar el nombre de usuario en SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", username);
                editor.apply();

                Toast.makeText(LoginActivity.this, "Bienvenido " + username, Toast.LENGTH_SHORT).show();

                // Ir al menú principal
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish();
            }
        });

        btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Jugar como Invitado y guardar como username temporal
                String guest = "Invitado";

                // Guardar invitado en SharedPreferences para sincronización con Firebase si es necesario
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", guest);
                editor.apply();

                Toast.makeText(LoginActivity.this, "Entrando como Invitado", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("username", guest);
                startActivity(intent);
                finish();
            }
        });
    }
}
