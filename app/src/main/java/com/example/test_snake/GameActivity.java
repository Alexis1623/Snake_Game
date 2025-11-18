package com.example.test_snake;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private Button btnUp, btnDown, btnLeft, btnRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

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
                    runOnUiThread(() -> updateScore());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateScore() {
        scoreText.setText("SCORE: " + gameView.getScore());
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