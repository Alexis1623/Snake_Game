package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private GameView gameView;
    private TextView scoreText, coinsText;
    private Button btnUp, btnDown, btnLeft, btnRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Log.d(TAG, "GameActivity creada");

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
            scoreText.setText("SCORE: " + gameView.getScore() + "/250");}
    }

    private void updateCoins() {
        if (coinsText != null) {
            SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
            int coins = prefs.getInt("coins", 0);
            coinsText.setText("MONEDAS: " + coins);
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