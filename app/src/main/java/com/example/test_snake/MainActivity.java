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
                String username;
                if (dataSnapshot.exists()) {
                    String message = dataSnapshot.getValue(String.class);
                    if (message != null && !message.isEmpty()) {
                        username = message;
                    } else {
                        // Si no hay mensaje en Firebase, usar SharedPreferences o Intent
                        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                        username = getIntent().getStringExtra("username");
                        if (username == null || username.isEmpty()) {
                            username = prefs.getString("username", "Invitado");
                        }
                    }
                } else {
                    // Si no existe el nodo, usar SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                    username = getIntent().getStringExtra("username");
                    if (username == null || username.isEmpty()) {
                        username = prefs.getString("username", "Invitado");
                    }
                    // Guardar en Firebase para futuras ejecuciones
                    messageRef.setValue(username);
                }

                tvUsername.setText("Hola, " + username);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Si falla Firebase, usar SharedPreferences
                SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                String username = getIntent().getStringExtra("username");
                if (username == null || username.isEmpty()) {
                    username = prefs.getString("username", "Invitado");
                }
                tvUsername.setText("Hola, " + username);
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
