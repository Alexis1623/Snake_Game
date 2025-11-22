package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoresActivity extends AppCompatActivity {

    private static final String TAG = "ScoresActivity";
    private RecyclerView rvScores;
    private ScoresAdapter adapter;
    private DatabaseReference globalScoresRef;
    private DatabaseReference userScoresRef;
    private List<Score> scoresList;
    private Button btnGlobal, btnLocal;
    private TextView tvTitle;
    private String username;
    private boolean showingGlobal = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        // Tomamos el username guardado para poder mostrar tus puntuaciones locales
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        username = prefs.getString("username", "Invitado");

        // Vistas
        tvTitle = findViewById(R.id.tvTitle);
        rvScores = findViewById(R.id.rvScores);
        btnGlobal = findViewById(R.id.btnGlobal);
        btnLocal = findViewById(R.id.btnLocal);

        rvScores.setLayoutManager(new LinearLayoutManager(this));

        // Preparamos la lista y el adaptador vacíos
        scoresList = new ArrayList<>();
        adapter = new ScoresAdapter(scoresList);
        rvScores.setAdapter(adapter);

        // Botones para cambiar entre Top global y tus puntajes
        setupButtons();

        // Por defecto mostramos el TOP 10 global
        loadGlobalScores();
    }

    private void setupButtons() {
        // Botón GLOBAL: si no está activo, lo activamos
        btnGlobal.setOnClickListener(v -> {
            if (!showingGlobal) {
                showingGlobal = true;
                updateButtonStyles();
                loadGlobalScores();
            }
        });

        // Botón LOCAL: tus puntuaciones personales
        btnLocal.setOnClickListener(v -> {
            if (showingGlobal) {
                showingGlobal = false;
                updateButtonStyles();
                loadLocalScores();
            }
        });

        // Estilo inicial de botones
        updateButtonStyles();
    }

    private void updateButtonStyles() {
        if (showingGlobal) {
            // GLOBAL activo (más visible)
            btnGlobal.setBackgroundColor(0xFF9D4EDD); // Morado brillante
            btnGlobal.setTextColor(0xFFFFFFFF); // Blanco

            // LOCAL inactivo
            btnLocal.setBackgroundColor(0xFF6A0DAD); // Morado oscuro
            btnLocal.setTextColor(0xFF00FF00); // Verde
        } else {
            // LOCAL activo
            btnLocal.setBackgroundColor(0xFF9D4EDD); // Morado brillante
            btnLocal.setTextColor(0xFFFFFFFF); // Blanco

            // GLOBAL inactivo
            btnGlobal.setBackgroundColor(0xFF6A0DAD); // Morado oscuro
            btnGlobal.setTextColor(0xFF00FF00); // Verde
        }
    }

    private void loadGlobalScores() {
        tvTitle.setText("TOP 10 GLOBAL");
        Log.d(TAG, "Cargando scores globales...");

        globalScoresRef = FirebaseDatabase.getInstance()
                .getReference("global_scores");

        // Escuchar cambios en el nodo global_scores y actualizar UI
        globalScoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Global - onDataChange. Hijos: " + dataSnapshot.getChildrenCount());

                scoresList.clear();

                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    showNoScoresMessage();
                    return;
                }

                List<Score> tempList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long scoreValue = snapshot.child("score").getValue(Long.class);
                    String username = snapshot.child("username").getValue(String.class);

                    if (scoreValue != null && username != null) {
                        Score score = new Score(0, username, scoreValue.intValue());
                        tempList.add(score);
                    }
                }

                if (tempList.isEmpty()) {
                    showNoScoresMessage();
                    return;
                }

                // Ordenar desc y quedar con los top 10 para mostrar
                tempList.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

                for (int i = 0; i < Math.min(10, tempList.size()); i++) {
                    Score score = tempList.get(i);
                    score.setPosition(i + 1);
                    scoresList.add(score);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Si Firebase falla, mostramos un mensaje amigable y una sugerencia
                handleFirebaseError(databaseError);
            }
        });
    }

    private void loadLocalScores() {
        tvTitle.setText("MIS PUNTUACIONES - " + username);
        Log.d(TAG, "Cargando scores locales para: " + username);

        userScoresRef = FirebaseDatabase.getInstance()
                .getReference("user_scores")
                .child(username);

        // Escuchamos el historial del usuario y lo mostramos ordenado
        userScoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Local - onDataChange. Hijos: " + dataSnapshot.getChildrenCount());

                scoresList.clear();

                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    showNoPersonalScoresMessage();
                    return;
                }

                List<Score> tempList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long scoreValue = snapshot.child("score").getValue(Long.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (scoreValue != null) {
                        Score score = new Score(0, username, scoreValue.intValue());
                        tempList.add(score);
                    }
                }

                if (tempList.isEmpty()) {
                    showNoPersonalScoresMessage();
                    return;
                }

                // Ordenar por puntuación descendente
                tempList.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

                // Mostrar todos tus scores (sin límite de 10)
                for (int i = 0; i < tempList.size(); i++) {
                    Score score = tempList.get(i);
                    score.setPosition(i + 1);
                    scoresList.add(score);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                handleFirebaseError(databaseError);
            }
        });
    }

    private void handleFirebaseError(DatabaseError databaseError) {
        Log.e(TAG, "Error de Firebase: " + databaseError.getMessage());

        String errorMsg = "Error al cargar scores";

        if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
            errorMsg = "Permiso denegado. Configura las reglas de Firebase";
            Toast.makeText(this,
                "⚠️ Error de permisos de Firebase",
                Toast.LENGTH_LONG).show();
        } else if (databaseError.getCode() == DatabaseError.NETWORK_ERROR) {
            errorMsg = "Error de conexión. Verifica tu internet";
        }

        // Mostrar un mensaje claro en la lista para que el usuario entienda qué pasó
        showErrorMessage(errorMsg);
    }

    private void showNoScoresMessage() {
        scoresList.clear();
        scoresList.add(new Score(1, "No hay puntuaciones aún", 0));
        scoresList.add(new Score(2, "¡Sé el primero!", 0));
        adapter.notifyDataSetChanged();
    }

    private void showNoPersonalScoresMessage() {
        scoresList.clear();
        scoresList.add(new Score(1, "No tienes puntuaciones", 0));
        scoresList.add(new Score(2, "¡Juega para crear tu historial!", 0));
        adapter.notifyDataSetChanged();
    }

    private void showErrorMessage(String error) {
        scoresList.clear();
        scoresList.add(new Score(1, "Error al cargar", 0));
        scoresList.add(new Score(2, error, 0));
        adapter.notifyDataSetChanged();
    }
}
