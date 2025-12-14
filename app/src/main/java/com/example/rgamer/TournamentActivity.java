package com.example.rgamer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TournamentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        TextView txtGame = findViewById(R.id.txtGame);

        // Get game name safely
        String game = getIntent().getStringExtra("game");

        if (game != null && !game.isEmpty()) {
            txtGame.setText(game + " Tournament");
        } else {
            txtGame.setText("Tournament");
        }
    }
}
