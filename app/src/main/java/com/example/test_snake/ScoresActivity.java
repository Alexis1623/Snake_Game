package com.example.test_snake;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ScoresActivity extends AppCompatActivity {

    private RecyclerView rvScores;
    private ScoresAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        rvScores = findViewById(R.id.rvScores);
        rvScores.setLayoutManager(new LinearLayoutManager(this));

        // Datos de ejemplo
        List<Score> scores = new ArrayList<>();
        scores.add(new Score(1, "djWoody", 30001));
        scores.add(new Score(2, "repose", 30000));
        scores.add(new Score(3, "SVGAMER", 25151));
        scores.add(new Score(4, "ZOROxJPN", 23183));
        scores.add(new Score(5, "Barcode", 22400));
        scores.add(new Score(6, "5744Suika", 18040));
        scores.add(new Score(7, "GangsterEttienne", 17860));
        scores.add(new Score(8, "GT", 17820));

        adapter = new ScoresAdapter(scores);
        rvScores.setAdapter(adapter);
    }
}
