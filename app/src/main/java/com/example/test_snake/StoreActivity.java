package com.example.test_snake;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    private TextView coinsAmount;
    private RecyclerView recyclerView;
    private SkinAdapter adapter;
    private List<Skin> skinList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        coinsAmount = findViewById(R.id.coinsAmount);
        recyclerView = findViewById(R.id.skinsRecyclerView);

        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        int coins = prefs.getInt("coins", 0);
        coinsAmount.setText(String.valueOf(coins));

        // Crear lista de skins
        skinList = new ArrayList<>();

        // Skin por defecto (siempre equipada / gratis)
        skinList.add(new Skin(
                "skin_default",
                "Clásica",
                0,
                getResources().getIdentifier("skin_default", "drawable", getPackageName()),
                true
        ));

        // Skin azul
        skinList.add(new Skin(
                "skin_blue",
                "Azul",
                10, // precio en monedas
                getResources().getIdentifier("skin_blue", "drawable", getPackageName()),
                prefs.getBoolean("skin_purchased_skin_blue", false)
        ));

        // Skin roja
        skinList.add(new Skin(
                "skin_red",
                "Roja",
                15, // precio en monedas
                getResources().getIdentifier("skin_red", "drawable", getPackageName()),
                prefs.getBoolean("skin_purchased_skin_red", false)
        ));

        // Configurar RecyclerView
        adapter = new SkinAdapter(this, skinList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar monedas cada vez que se entra de nuevo a la tienda
        SharedPreferences prefs = getSharedPreferences("SnakePrefs", MODE_PRIVATE);
        int coins = prefs.getInt("coins", 0);
        coinsAmount.setText(String.valueOf(coins));
    }
}
