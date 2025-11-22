package com.example.test_snake;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private Button btnStartGame, btnSettings, btnScores, btnExit;
    private Button btnEditUser, btnLogout;
    private Button btnTienda; // Botón de TIENDA
    private TextView tvUsername; // Muestra: Hola, [usuario]

    private DatabaseReference databaseReference;
    private String currentUsername; // Guardar el username actual para control

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Inicializar vistas
        tvUsername   = findViewById(R.id.tvUsername);
        btnEditUser  = findViewById(R.id.btnEditUser);
        btnLogout    = findViewById(R.id.btnLogout);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnSettings  = findViewById(R.id.btnSettings);
        btnScores    = findViewById(R.id.btnScores);
        btnExit      = findViewById(R.id.btnExit);
        btnTienda    = findViewById(R.id.btnTienda); // IMPORTANTE: debe existir en activity_main.xml

        // --- NUEVO: cargar username desde Intent o SharedPreferences y mostrarlo inmediatamente ---
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = prefs.getString("username", "Invitado");
        }
        tvUsername.setText("Hola, " + currentUsername);
        // --------------------------------------------------------------------------------------

        // Cargar mensaje desde Firebase / SharedPreferences
        loadMessageFromFirebase();

        // Asignar listeners a los botones
        setupButtonListeners();
    }

    private void loadMessageFromFirebase() {
        DatabaseReference messageRef = databaseReference.child("message");

        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String message = dataSnapshot.getValue(String.class);
                    if (message != null && !message.isEmpty()) {
                        // Solo actualizar la UI con el valor de Firebase si el usuario actual es el por defecto
                        // o si aún no hay un username válido.
                        if (currentUsername == null || currentUsername.equals("Invitado") || currentUsername.isEmpty()) {
                            currentUsername = message;
                            tvUsername.setText("Hola, " + currentUsername);
                        }
                        // Si el usuario ya se logueó (tiene un nombre distinto a 'Invitado'), no sobrescribimos.
                    } else {
                        // Si no hay mensaje en Firebase, ya tenemos currentUsername (mostrado arriba)
                        // no hacemos nada adicional aquí.
                    }
                } else {
                    // Si no existe el nodo, usar currentUsername (ya cargado) y guardar en Firebase para futuras ejecuciones
                    if (currentUsername == null || currentUsername.isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                        currentUsername = prefs.getString("username", "Invitado");
                        tvUsername.setText("Hola, " + currentUsername);
                    }
                    messageRef.setValue(currentUsername);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Si falla Firebase, dejar el valor que ya mostramos (currentUsername)
                if (currentUsername == null || currentUsername.isEmpty()) {
                    SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                    currentUsername = prefs.getString("username", "Invitado");
                }
                tvUsername.setText("Hola, " + currentUsername);
            }
        });
    }

    private void setupButtonListeners() {

        // Iniciar juego
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });

        // Configuración
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Puntuaciones
        btnScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScoresActivity.class));
            }
        });

        // Salir
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Editar usuario
        btnEditUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("force_edit", true);
                startActivity(intent);
                finish();
            }
        });

        // Logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                prefs.edit().remove("username").apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // TIENDA
        btnTienda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Abrir la tienda de skins
                Intent intent = new Intent(MainActivity.this, StoreActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpieza si hace falta
    }
}
