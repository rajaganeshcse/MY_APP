package com.example.rgamer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
