package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    private GameView gameView;
    private TextView scoreText;
    private Button btnUp, btnDown, btnLeft, btnRight;

    // Firebase
    private DatabaseReference scoresRef;
    private DatabaseReference globalScoresRef;
    private DatabaseReference coinsRef;
    private String username;
    private int lastSavedScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Obtener username
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        username = prefs.getString("username", "Invitado");

        // Inicializar Firebase
        scoresRef = FirebaseDatabase.getInstance()
                .getReference("user_scores")
                .child(username);
        globalScoresRef = FirebaseDatabase.getInstance()
                .getReference("global_scores");
        coinsRef = FirebaseDatabase.getInstance()
                .getReference("user_coins")
                .child(username);

        // Cargar monedas desde Firebase
        loadCoinsFromFirebase();

        // Inicializar vistas
        gameView = findViewById(R.id.gameView);
        scoreText = findViewById(R.id.scoreText);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);

        // Configurar controles
        setupControls();

        // Configurar el touch listener para reiniciar
        gameView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gameView.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Guardar score antes de reiniciar
                    saveScoreToFirebase(gameView.getScore());
                    gameView.restartGame();
                    updateScore();
                    return true;
                }
                return false;
            }
        });

        // Iniciar actualización de puntuación
        startScoreUpdate();
    }

    private void loadCoinsFromFirebase() {
        coinsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer firebaseCoins = dataSnapshot.getValue(Integer.class);
                    if (firebaseCoins != null) {
                        // Sincronizar con SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
                        prefs.edit().putInt("coins", firebaseCoins).apply();

                        // Actualizar en GameView
                        if (gameView != null) {
                            gameView.updateCoinsFromFirebase(firebaseCoins);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Error al cargar monedas, usar las locales
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
                    Thread.sleep(100); // Actualizar cada 100ms
                    runOnUiThread(() -> {
                        updateScore();
                        // Detectar game over y guardar score
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

        Log.d(TAG, "Guardando score: " + score + " para usuario: " + username);

        lastSavedScore = score;
        final long timestamp = System.currentTimeMillis();

        // Guardar en scores personales del usuario
        String scoreId = scoresRef.push().getKey();
        if (scoreId != null) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("score", score);
            scoreData.put("timestamp", timestamp);
            scoreData.put("username", username);

            scoresRef.child(scoreId).setValue(scoreData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Score personal guardado exitosamente"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando score personal: " + e.getMessage()));
        }

        // Verificar si es top score y guardar en global
        checkAndSaveGlobalScore(score, timestamp);
    }

    private void checkAndSaveGlobalScore(final int score, final long timestamp) {
        Log.d(TAG, "Verificando si score " + score + " califica para top 10");

        // Primero verificar cuántos scores hay en total
        globalScoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Scores globales actuales: " + dataSnapshot.getChildrenCount());

                List<ScoreEntry> allScores = new ArrayList<>();

                // Leer todos los scores actuales
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long scoreValue = snapshot.child("score").getValue(Long.class);
                    if (scoreValue != null) {
                        allScores.add(new ScoreEntry(snapshot.getKey(), scoreValue.intValue()));
                        Log.d(TAG, "Score existente: " + scoreValue);
                    }
                }

                // Agregar el nuevo score
                allScores.add(new ScoreEntry(null, score));

                // Ordenar de mayor a menor
                Collections.sort(allScores, (s1, s2) -> Integer.compare(s2.score, s1.score));

                // Verificar si el nuevo score está en el top 10
                boolean isTop10 = false;

                for (int i = 0; i < allScores.size(); i++) {
                    if (i < 10 && allScores.get(i).key == null) {
                        // El nuevo score está en el top 10
                        isTop10 = true;
                        Log.d(TAG, "Score " + score + " está en posición " + (i + 1) + " del top 10");
                    } else if (i >= 10 && allScores.get(i).key != null) {
                        // Este score debe ser eliminado
                        String keyToRemove = allScores.get(i).key;
                        Log.d(TAG, "Eliminando score fuera del top 10: " + allScores.get(i).score);
                        globalScoresRef.child(keyToRemove).removeValue();
                    }
                }

                // Si está en el top 10, guardarlo
                if (isTop10) {
                    String scoreId = globalScoresRef.push().getKey();
                    if (scoreId != null) {
                        Map<String, Object> globalScoreData = new HashMap<>();
                        globalScoreData.put("score", score);
                        globalScoreData.put("timestamp", timestamp);
                        globalScoreData.put("username", username);

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

    // Clase auxiliar para manejar scores con sus keys
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
            // El juego se reinicia automáticamente al tocar
        }
    }
}