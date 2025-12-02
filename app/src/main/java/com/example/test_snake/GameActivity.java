package com.example.test_snake;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private GameView gameView;
    private TextView scoreText;
    private Button btnUp, btnDown, btnLeft, btnRight;

    // Firebase: referencias a nodos que usamos (puntuaciones y monedas)
    private DatabaseReference scoresRef;
    private DatabaseReference globalScoresRef;
    private DatabaseReference countryScoresRef;
    private DatabaseReference coinsRef;
    private String username;
    private int lastSavedScore = 0;

    // Geolocalización
    private FusedLocationProviderClient fusedLocationClient;
    private String userCountry = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Tomamos el username guardado (si no hay, aparece 'Invitado')
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        username = prefs.getString("username", "Invitado");

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtener ubicación del usuario
        requestLocationPermissionAndGetCountry();

        // Conectamos a Firebase para los diferentes nodos que necesitamos
        scoresRef = FirebaseDatabase.getInstance()
                .getReference("user_scores")
                .child(username);
        globalScoresRef = FirebaseDatabase.getInstance()
                .getReference("global_scores");
        coinsRef = FirebaseDatabase.getInstance()
                .getReference("user_coins")
                .child(username);

        // Cargar las monedas desde Firebase (si hay)
        loadCoinsFromFirebase();

        // Inicializar vistas
        gameView = findViewById(R.id.gameView);
        scoreText = findViewById(R.id.scoreText);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);

        // Configurar los controles (flechas)
        setupControls();

        // Reinicia cuando tocas la pantalla si ya perdiste: antes guarda la puntuación
        gameView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gameView.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Guardamos el score actual antes de reiniciar la partida
                    saveScoreToFirebase(gameView.getScore());
                    gameView.restartGame();
                    updateScore();
                    return true;
                }
                return false;
            }
        });

        // Empezar el bucle que actualiza la puntuación en pantalla
        startScoreUpdate();
    }

    private void requestLocationPermissionAndGetCountry() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Ya tenemos permiso, obtener ubicación
            getUserCountry();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserCountry();
            } else {
                Log.w(TAG, "Permiso de ubicación denegado. País por defecto: Unknown");
                userCountry = "Unknown";
            }
        }
    }

    private void getUserCountry() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getCountryFromLocation(location);
                    } else {
                        Log.w(TAG, "No se pudo obtener ubicación. País por defecto: Unknown");
                        userCountry = "Unknown";
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo ubicación: " + e.getMessage());
                    userCountry = "Unknown";
                });
    }

    private void getCountryFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                String country = addresses.get(0).getCountryName();
                if (country != null && !country.isEmpty()) {
                    userCountry = country;
                    Log.d(TAG, "País detectado: " + userCountry);

                    // Guardar país en SharedPreferences para uso posterior
                    SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                    prefs.edit().putString("userCountry", userCountry).apply();
                } else {
                    userCountry = "Unknown";
                }
            } else {
                userCountry = "Unknown";
                Log.w(TAG, "No se pudo obtener el nombre del país");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error en Geocoder: " + e.getMessage());
            userCountry = "Unknown";
        }
    }

    private void loadCoinsFromFirebase() {
        coinsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer firebaseCoins = dataSnapshot.getValue(Integer.class);
                    if (firebaseCoins != null) {
                        // Sincronizamos las monedas con las que guardamos localmente
                        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                        prefs.edit().putInt("coins", firebaseCoins).apply();

                        // Avisamos al GameView por si necesita mostrarlas
                        if (gameView != null) {
                            gameView.updateCoinsFromFirebase(firebaseCoins);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Si falla Firebase, seguimos con las monedas locales (no rompemos la partida)
            }
        });
    }

    private void setupControls() {
        btnUp.setOnClickListener(v -> gameView.setDirectionUp());
        btnDown.setOnClickListener(v -> gameView.setDirectionDown());
        btnLeft.setOnClickListener(v -> gameView.setDirectionLeft());
        btnRight.setOnClickListener(v -> gameView.setDirectionRight());
    }

    private void startScoreUpdate() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100); // Actualiza cada 100ms
                    runOnUiThread(() -> {
                        updateScore();
                        // Si terminó la partida, guardamos el score (una sola vez por cambio)
                        if (gameView.isGameOver() && gameView.getScore() > 0 && lastSavedScore != gameView.getScore()) {
                            saveScoreToFirebase(gameView.getScore());
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateScore() {
        scoreText.setText("SCORE: " + gameView.getScore());
    }

    private void saveScoreToFirebase(final int score) {
        if (score == 0 || score == lastSavedScore) return;

        Log.d(TAG, "Guardando score: " + score + " para usuario: " + username + " en país: " + userCountry);

        lastSavedScore = score;
        final long timestamp = System.currentTimeMillis();

        // Guardar en el historial personal del usuario
        String scoreId = scoresRef.push().getKey();
        if (scoreId != null) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("score", score);
            scoreData.put("timestamp", timestamp);
            scoreData.put("username", username);
            scoreData.put("country", userCountry);

            scoresRef.child(scoreId).setValue(scoreData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Score personal guardado exitosamente"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando score personal: " + e.getMessage()));
        }

        // Verificar si entra al top 10 global y, si es así, guardarlo allí también
        checkAndSaveGlobalScore(score, timestamp);

        // Guardar en el ranking por país (top 100)
        saveCountryScore(score, timestamp);
    }

    private void saveCountryScore(final int score, final long timestamp) {
        if (userCountry.equals("Unknown")) {
            Log.w(TAG, "País desconocido, no se guarda en ranking local");
            return;
        }

        // Referencia al nodo de scores por país
        countryScoresRef = FirebaseDatabase.getInstance()
                .getReference("country_scores")
                .child(userCountry);

        // Leer los scores actuales del país
        countryScoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ScoreEntry> countryScores = new ArrayList<>();

                // Recolectar todos los scores del país
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long scoreValue = snapshot.child("score").getValue(Long.class);
                    if (scoreValue != null) {
                        countryScores.add(new ScoreEntry(snapshot.getKey(), scoreValue.intValue()));
                    }
                }

                // Añadir el nuevo score
                countryScores.add(new ScoreEntry(null, score));

                // Ordenar de mayor a menor
                Collections.sort(countryScores, (s1, s2) -> Integer.compare(s2.score, s1.score));

                // Mantener solo top 100
                boolean isTop100 = false;
                for (int i = 0; i < countryScores.size(); i++) {
                    if (i < 100 && countryScores.get(i).key == null) {
                        // Nuestro nuevo score está dentro del top 100
                        isTop100 = true;
                        Log.d(TAG, "Score " + score + " está en top 100 de " + userCountry);
                    } else if (i >= 100 && countryScores.get(i).key != null) {
                        // Este score ya no pertenece al top 100, lo borramos
                        String keyToRemove = countryScores.get(i).key;
                        countryScoresRef.child(keyToRemove).removeValue();
                    }
                }

                // Si entra al top 100, guardarlo
                if (isTop100) {
                    String scoreId = countryScoresRef.push().getKey();
                    if (scoreId != null) {
                        Map<String, Object> countryScoreData = new HashMap<>();
                        countryScoreData.put("score", score);
                        countryScoreData.put("timestamp", timestamp);
                        countryScoreData.put("username", username);
                        countryScoreData.put("country", userCountry);

                        countryScoresRef.child(scoreId).setValue(countryScoreData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Score guardado en top " + userCountry);
                                    runOnUiThread(() -> {
                                        Toast.makeText(GameActivity.this,
                                                "¡Top 100 de " + userCountry + "! Score: " + score,
                                                Toast.LENGTH_SHORT).show();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error guardando score en país: " + e.getMessage());
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error verificando scores por país: " + databaseError.getMessage());
            }
        });
    }

    private void checkAndSaveGlobalScore(final int score, final long timestamp) {
        Log.d(TAG, "Verificando si score " + score + " califica para top 10");

        // Leemos los scores globales actuales para decidir si el nuevo entra al top 10
        globalScoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Scores globales actuales: " + dataSnapshot.getChildrenCount());

                List<ScoreEntry> allScores = new ArrayList<>();

                // Recolectar todos los scores que hay
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long scoreValue = snapshot.child("score").getValue(Long.class);
                    if (scoreValue != null) {
                        allScores.add(new ScoreEntry(snapshot.getKey(), scoreValue.intValue()));
                        Log.d(TAG, "Score existente: " + scoreValue);
                    }
                }

                // Añadimos el score nuevo a la lista para comparar
                allScores.add(new ScoreEntry(null, score));

                // Ordenamos de mayor a menor
                Collections.sort(allScores, (s1, s2) -> Integer.compare(s2.score, s1.score));

                // Determinar si el nuevo score queda en el top 10 y eliminar los que sobren
                boolean isTop10 = false;

                for (int i = 0; i < allScores.size(); i++) {
                    if (i < 10 && allScores.get(i).key == null) {
                        // Nuestro nuevo score está dentro del top 10
                        isTop10 = true;
                        Log.d(TAG, "Score " + score + " está en posición " + (i + 1) + " del top 10");
                    } else if (i >= 10 && allScores.get(i).key != null) {
                        // Este score ya no pertenece al top 10, lo borramos
                        String keyToRemove = allScores.get(i).key;
                        Log.d(TAG, "Eliminando score fuera del top 10: " + allScores.get(i).score);
                        globalScoresRef.child(keyToRemove).removeValue();
                    }
                }

                // Si entra al top 10, lo guardamos con los datos necesarios
                if (isTop10) {
                    String scoreId = globalScoresRef.push().getKey();
                    if (scoreId != null) {
                        Map<String, Object> globalScoreData = new HashMap<>();
                        globalScoreData.put("score", score);
                        globalScoreData.put("timestamp", timestamp);
                        globalScoreData.put("username", username);
                        globalScoreData.put("country", userCountry);

                        Log.d(TAG, "Guardando en top 10 global: " + score);
                        globalScoresRef.child(scoreId).setValue(globalScoreData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Score guardado en top 10 global exitosamente");
                                    runOnUiThread(() -> {
                                        Toast.makeText(GameActivity.this,
                                                "¡Top 10 Global! Score: " + score,
                                                Toast.LENGTH_SHORT).show();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error guardando score global: " + e.getMessage());
                                });
                    }
                } else {
                    Log.d(TAG, "Score " + score + " NO califica para top 10");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error verificando scores globales: " + databaseError.getMessage());
            }
        });
    }

    // Pequeña clase auxiliar para manejar pares (key, score)
    private static class ScoreEntry {
        String key;
        int score;

        ScoreEntry(String key, int score) {
            this.key = key;
            this.score = score;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stopGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null && !gameView.isGameOver()) {
            // No hacemos nada especial aquí; el juego espera a la interacción
        }
    }
}