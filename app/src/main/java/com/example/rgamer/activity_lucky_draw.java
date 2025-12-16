package com.example.rgamer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class activity_lucky_draw extends AppCompatActivity {

    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        // Back button
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Since you are using <include layout="@layout/item_lucky_draw"/>
        // we handle clicks by finding buttons from included layouts

        setupLuckyDrawCards();
    }

    // ================= SETUP CARDS =================
    private void setupLuckyDrawCards() {

        // FIRST CARD
        View card1 = findViewById(R.id.card_root_1);
        if (card1 != null) {
            card1.findViewById(R.id.btnCheckWinners)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Checking winners...",
                                    Toast.LENGTH_SHORT).show());

            card1.findViewById(R.id.btnFreeEntry)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Free entry added!",
                                    Toast.LENGTH_SHORT).show());
        }

        // SECOND CARD
        View card2 = findViewById(R.id.card_root_2);
        if (card2 != null) {
            card2.findViewById(R.id.btnCheckWinners)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Checking winners...",
                                    Toast.LENGTH_SHORT).show());

            card2.findViewById(R.id.btnFreeEntry)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Free entry added!",
                                    Toast.LENGTH_SHORT).show());
        }

        // THIRD CARD
        View card3 = findViewById(R.id.card_root_3);
        if (card3 != null) {
            card3.findViewById(R.id.btnCheckWinners)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Checking winners...",
                                    Toast.LENGTH_SHORT).show());

            card3.findViewById(R.id.btnFreeEntry)
                    .setOnClickListener(v ->
                            Toast.makeText(this,
                                    "Free entry added!",
                                    Toast.LENGTH_SHORT).show());
        }
    }
}
