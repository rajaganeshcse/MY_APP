package com.example.rgamer.Game;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rgamer.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class activity_winners extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtInfo;

    private FirebaseFirestore db;
    private String tournamentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winners);
        makeFullScreen();

        // ================= INIT UI =================
        btnBack = findViewById(R.id.btnBack);
        txtInfo = findViewById(R.id.txtInfo);

        db = FirebaseFirestore.getInstance();

        // ================= GET INTENT =================
        tournamentId = getIntent().getStringExtra("tournamentId");

        if (tournamentId == null) {
            txtInfo.setText("Invalid tournament");
            return;
        }

        btnBack.setOnClickListener(v -> finish());

        loadWinners();
    }


    private void makeFullScreen() {
        Window window = getWindow();

        // 🔥 Make content go behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

                // Optional: hide bars (remove if you only want transparent top)
                // controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 🔥 Make status bar transparent (TOP FIX)
        window.setStatusBarColor(Color.TRANSPARENT);

        // 🔥 Optional: make navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    // ================= LOAD WINNERS =================
    private void loadWinners() {

        db.collection("tournaments")
                .document(tournamentId)
                .collection("winners")
                .get()
                .addOnSuccessListener(this::handleWinners)
                .addOnFailureListener(e ->
                        txtInfo.setText("Failed to load winners"));
    }

    private void handleWinners(QuerySnapshot snap) {

        if (snap == null || snap.isEmpty()) {
            txtInfo.setText("Winners not announced yet");
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (DocumentSnapshot d : snap.getDocuments()) {
            sb.append("Winner UID: ")
                    .append(d.getString("uid"))
                    .append("\n");
        }

        txtInfo.setText(sb.toString());
    }
}
