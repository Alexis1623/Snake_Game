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
    private Button btnTienda;  // Botón de la tienda
    private TextView tvUsername;

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
        btnTienda    = findViewById(R.id.btnTienda); // Inicializar botón de tienda

        // Cargar mensaje desde Firebase
        loadMessageFromFirebase();

        // Configurar listeners para cada botón
        setupButtonListeners();
    }

    /** Lee el mensaje "Hola, [username]" desde Firebase o SharedPreferences y lo muestra. */
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
                        // Si no hay mensaje en Firebase, usar SharedPreferences o intent
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
                    // Guardar el valor por defecto en Firebase
                    messageRef.setValue(username);
                }
                tvUsername.setText("Hola, " + username);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // En caso de error, usar SharedPreferences
                SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                String username = getIntent().getStringExtra("username");
                if (username == null || username.isEmpty()) {
                    username = prefs.getString("username", "Invitado");
                }
                tvUsername.setText("Hola, " + username);
            }
        });
    }

    /** Asigna las acciones de cada botón del menú principal. */
    private void setupButtonListeners() {
        // Iniciar el juego
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });

        // Abrir la configuración
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Abrir puntuaciones
        btnScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScoresActivity.class));
            }
        });

        // Salir de la app
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

        // Cerrar sesión
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

        // Abrir la tienda de skins
        btnTienda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StoreActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Puedes limpiar recursos adicionales aquí si es necesario
    }
}
