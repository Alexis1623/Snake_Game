package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
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
    private TextView scoreText, coinsText;
    private Button btnUp, btnDown, btnLeft, btnRight;

    // Firebase: referencias a nodos que usamos (puntuaciones y monedas)
    private DatabaseReference scoresRef;
    private DatabaseReference globalScoresRef;
    private DatabaseReference coinsRef;
    private String username;
    private int lastSavedScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Log.d(TAG, "GameActivity creada");

        // Tomamos el username guardado (si no hay, aparece 'Invitado')
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        username = prefs.getString("username", "Invitado");

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

        try {
            // Inicializar vistas
            gameView = findViewById(R.id.gameView);
            scoreText = findViewById(R.id.scoreText);
            coinsText = findViewById(R.id.coinsText);
            btnUp = findViewById(R.id.btnUp);
            btnDown = findViewById(R.id.btnDown);
            btnLeft = findViewById(R.id.btnLeft);
            btnRight = findViewById(R.id.btnRight);

            if (gameView == null) {
                Log.e(TAG, "GameView es null!");
                finish();
                return;
            }

            if (scoreText == null || coinsText == null || btnUp == null || btnDown == null || btnLeft == null || btnRight == null) {
                Log.e(TAG, "Alguna vista es null!");
            }

            // Configurar controles
            setupControls();

            // Configurar toque para reiniciar
            gameView.setOnTouchListener((v, event) -> {
                if ((gameView.isGameOver() || gameView.isGameWon()) && event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Guardamos el score actual antes de reiniciar la partida
                    saveScoreToFirebase(gameView.getScore());
                    gameView.restartGame();
                    updateScore();
                    updateCoins();
                    return true;
                }
                return false;
            });

            // Iniciar actualización de score
            startScoreUpdate();

            Log.d(TAG, "GameActivity inicializada correctamente");

        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate: " + e.getMessage());
            e.printStackTrace();
            finish();
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
                Log.e(TAG, "Error cargando monedas de Firebase: " + databaseError.getMessage());
            }
        });
    }

    private void setupControls() {
        if (btnUp != null && gameView != null) {
            btnUp.setOnClickListener(v -> {
                Log.d(TAG, "Botón UP presionado");
                gameView.setDirectionUp();
            });
        }

        if (btnDown != null && gameView != null) {
            btnDown.setOnClickListener(v -> {
                Log.d(TAG, "Botón DOWN presionado");
                gameView.setDirectionDown();
            });
        }

        if (btnLeft != null && gameView != null) {
            btnLeft.setOnClickListener(v -> {
                Log.d(TAG, "Botón LEFT presionado");
                gameView.setDirectionLeft();
            });
        }

        if (btnRight != null && gameView != null) {
            btnRight.setOnClickListener(v -> {
                Log.d(TAG, "Botón RIGHT presionado");
                gameView.setDirectionRight();
            });
        }
    }

    private void startScoreUpdate() {
        new Thread(() -> {
            while (!isFinishing()) {
                try {
                    Thread.sleep(100);
                    runOnUiThread(() -> {
                        updateScore();
                        updateCoins();
                        // Si terminó la partida, guardamos el score (una sola vez por cambio)
                        if (gameView != null && gameView.isGameOver() && gameView.getScore() > 0 && lastSavedScore != gameView.getScore()) {
                            saveScoreToFirebase(gameView.getScore());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error en hilo de score: " + e.getMessage());
                    break;
                }
            }
        }).start();
    }

    private void updateScore() {
        if (scoreText != null && gameView != null) {
            scoreText.setText("SCORE: " + gameView.getScore() + "/250");
        }
    }

    private void updateCoins() {
        if (coinsText != null) {
            SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
            int coins = prefs.getInt("coins", 0);
            coinsText.setText("MONEDAS: " + coins);
        }
    }

    private void saveScoreToFirebase(final int score) {
        if (score == 0 || score == lastSavedScore) return;

        Log.d(TAG, "Guardando score: " + score + " para usuario: " + username);

        lastSavedScore = score;
        final long timestamp = System.currentTimeMillis();

        // Guardar en el historial personal del usuario
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

        // Verificar si entra al top 10 global y, si es así, guardarlo allí también
        checkAndSaveGlobalScore(score, timestamp);
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
        Log.d(TAG, "GameActivity en pausa");
        if (gameView != null) {
            gameView.stopGame();
            gameView.pauseBackgroundMusic();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "GameActivity en resume");
        if (gameView != null && !gameView.isGameWon()) {
            gameView.resumeBackgroundMusic();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GameActivity destruida");
        if (gameView != null) {
            gameView.stopGame();
        }
    }
}