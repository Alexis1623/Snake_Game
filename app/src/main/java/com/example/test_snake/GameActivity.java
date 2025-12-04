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

    // Referencias a Firebase
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

        // Cargamos el nombre de usuario o “Invitado”
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        username = prefs.getString("username", "Invitado");

        // Inicializar cliente de ubicación y pedir permiso
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermissionAndGetCountry();

        // Inicializar referencias de Firebase
        scoresRef  = FirebaseDatabase.getInstance().getReference("user_scores").child(username);
        globalScoresRef = FirebaseDatabase.getInstance().getReference("global_scores");
        coinsRef = FirebaseDatabase.getInstance().getReference("user_coins").child(username);

        // Cargar monedas si las hay guardadas en Firebase
        loadCoinsFromFirebase();

        // Inicializar vistas
        gameView = findViewById(R.id.gameView);
        scoreText = findViewById(R.id.scoreText);
        btnUp   = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight= findViewById(R.id.btnRight);

        setupControls();

        // Reiniciar la partida al tocar si el juego terminó
        gameView.setOnTouchListener((v, event) -> {
            if (gameView.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
                saveScoreToFirebase(gameView.getScore());
                gameView.restartGame();
                updateScore();
                return true;
            }
            return false;
        });

        // Actualizar el marcador en pantalla periódicamente
        startScoreUpdate();
    }

    /** Pide permisos de ubicación y, si están concedidos, obtiene el país */
    private void requestLocationPermissionAndGetCountry() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserCountry();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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

    /** Recupera la última ubicación conocida y traduce a nombre de país */
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

    /** Usa Geocoder para obtener el país a partir de la ubicación */
    private void getCountryFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String country = addresses.get(0).getCountryName();
                if (country != null && !country.isEmpty()) {
                    userCountry = country;
                    SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                    prefs.edit().putString("userCountry", userCountry).apply();
                    Log.d(TAG, "País detectado: " + userCountry);
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

    /** Carga el número de monedas desde Firebase y actualiza GameView */
    private void loadCoinsFromFirebase() {
        coinsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer firebaseCoins = snapshot.getValue(Integer.class);
                    if (firebaseCoins != null) {
                        // Guardamos en SharedPreferences por si acaso
                        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                        prefs.edit().putInt("coins", firebaseCoins).apply();
                        // Informamos a GameView (este método debes implementarlo en GameView)
                        if (gameView != null) {
                            gameView.updateCoinsFromFirebase(firebaseCoins);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Si falla Firebase, seguimos usando las monedas locales
            }
        });
    }

    /** Configura los botones de dirección */
    private void setupControls() {
        btnUp.setOnClickListener(v -> gameView.setDirectionUp());
        btnDown.setOnClickListener(v -> gameView.setDirectionDown());
        btnLeft.setOnClickListener(v -> gameView.setDirectionLeft());
        btnRight.setOnClickListener(v -> gameView.setDirectionRight());
    }

    /** Hilo que actualiza el marcador cada 100 ms y guarda el score al terminar */
    private void startScoreUpdate() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    runOnUiThread(() -> {
                        updateScore();
                        if (gameView.isGameOver()
                                && gameView.getScore() > 0
                                && lastSavedScore != gameView.getScore()) {
                            saveScoreToFirebase(gameView.getScore());
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error en hilo de actualización de score", e);
                }
            }
        }).start();
    }

    /** Muestra el marcador en pantalla */
    private void updateScore() {
        scoreText.setText("SCORE: " + gameView.getScore());
    }

    /** Guarda el score en Firebase en el historial personal, top global y top por país */
    private void saveScoreToFirebase(final int score) {
        if (score == 0 || score == lastSavedScore) return;

        lastSavedScore = score;
        final long timestamp = System.currentTimeMillis();
        Log.d(TAG, "Guardando score: " + score
                + " para usuario: " + username + " en país: " + userCountry);

        // Historial personal
        String scoreId = scoresRef.push().getKey();
        if (scoreId != null) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("score", score);
            scoreData.put("timestamp", timestamp);
            scoreData.put("username", username);
            scoreData.put("country", userCountry);
            scoresRef.child(scoreId).setValue(scoreData)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Score personal guardado exitosamente"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error guardando score personal: " + e.getMessage()));
        }

        // Top global y top por país
        checkAndSaveGlobalScore(score, timestamp);
        saveCountryScore(score, timestamp);
    }

    /** Guarda el score en el top 100 por país si corresponde */
    private void saveCountryScore(final int score, final long timestamp) {
        if (userCountry.equals("Unknown")) {
            Log.w(TAG, "País desconocido, no se guarda en ranking local");
            return;
        }
        countryScoresRef = FirebaseDatabase.getInstance()
                .getReference("country_scores")
                .child(userCountry);

        countryScoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ScoreEntry> countryScores = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Long scoreValue = snap.child("score").getValue(Long.class);
                    if (scoreValue != null) {
                        countryScores.add(
                                new ScoreEntry(snap.getKey(), scoreValue.intValue()));
                    }
                }
                // Añadir el nuevo score y ordenar
                countryScores.add(new ScoreEntry(null, score));
                Collections.sort(countryScores,
                        (s1, s2) -> Integer.compare(s2.score, s1.score));

                boolean isTop100 = false;
                for (int i = 0; i < countryScores.size(); i++) {
                    if (i < 100 && countryScores.get(i).key == null) {
                        isTop100 = true;
                    } else if (i >= 100 && countryScores.get(i).key != null) {
                        String keyToRemove = countryScores.get(i).key;
                        countryScoresRef.child(keyToRemove).removeValue();
                    }
                }

                if (isTop100) {
                    String newScoreId = countryScoresRef.push().getKey();
                    if (newScoreId != null) {
                        Map<String, Object> countryScoreData = new HashMap<>();
                        countryScoreData.put("score", score);
                        countryScoreData.put("timestamp", timestamp);
                        countryScoreData.put("username", username);
                        countryScoreData.put("country", userCountry);
                        countryScoresRef.child(newScoreId).setValue(countryScoreData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Score guardado en top " + userCountry);
                                    runOnUiThread(() ->
                                            Toast.makeText(GameActivity.this,
                                                    "¡Top 100 de "
                                                            + userCountry + "! Score: " + score,
                                                    Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error guardando score en país: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error verificando scores por país: " + error.getMessage());
            }
        });
    }

    /** Comprueba si el score entra en el top 10 global */
    private void checkAndSaveGlobalScore(final int score, final long timestamp) {
        Log.d(TAG, "Verificando si score " + score + " califica para top 10");
        globalScoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ScoreEntry> allScores = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Long scoreValue = snap.child("score").getValue(Long.class);
                    if (scoreValue != null) {
                        allScores.add(new ScoreEntry(snap.getKey(),
                                scoreValue.intValue()));
                    }
                }
                allScores.add(new ScoreEntry(null, score));
                Collections.sort(allScores,
                        (s1, s2) -> Integer.compare(s2.score, s1.score));

                boolean isTop10 = false;
                for (int i = 0; i < allScores.size(); i++) {
                    if (i < 10 && allScores.get(i).key == null) {
                        isTop10 = true;
                    } else if (i >= 10 && allScores.get(i).key != null) {
                        String keyToRemove = allScores.get(i).key;
                        globalScoresRef.child(keyToRemove).removeValue();
                    }
                }
                if (isTop10) {
                    String newScoreId = globalScoresRef.push().getKey();
                    if (newScoreId != null) {
                        Map<String, Object> globalScoreData = new HashMap<>();
                        globalScoreData.put("score", score);
                        globalScoreData.put("timestamp", timestamp);
                        globalScoreData.put("username", username);
                        globalScoreData.put("country", userCountry);
                        globalScoresRef.child(newScoreId).setValue(globalScoreData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Score guardado en top 10 global");
                                    runOnUiThread(() ->
                                            Toast.makeText(GameActivity.this,
                                                    "¡Top 10 Global! Score: " + score,
                                                    Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error guardando score global: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error verificando scores globales: " + error.getMessage());
            }
        });
    }

    /** Clase auxiliar para pares clave–score */
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
        // No es necesario reanudar nada especial; la lógica está en GameView
    }
}
