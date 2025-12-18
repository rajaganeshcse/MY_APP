package com.example.rgamer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class activity_lucky_draw extends AppCompatActivity {

    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadLuckyDraws();
    }

    private void loadLuckyDraws() {

        db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((value, error) -> {

                    if (value == null || value.isEmpty()) return;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        setupCard(doc);
                    }
                });
    }

    private void setupCard(DocumentSnapshot doc) {

        String drawId = doc.getId();
        int reward = doc.getLong("rewardCoins").intValue();

        View card = findViewById(R.id.card_root_1); // demo card

        card.findViewById(R.id.btnFreeEntry)
                .setOnClickListener(v -> joinDraw(drawId));

        card.findViewById(R.id.btnCheckWinners)
                .setOnClickListener(v ->
                        Toast.makeText(this,
                                "Winner announced after draw completes",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void joinDraw(String drawId) {

        DocumentReference entryRef =
                db.collection("lucky_draw_entries")
                        .document(drawId)
                        .collection("users")
                        .document(uid);

        entryRef.get().addOnSuccessListener(doc -> {

            if (doc.exists()) {
                Toast.makeText(this,
                        "Already joined",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Add entry
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("joinedAt", FieldValue.serverTimestamp());

            entryRef.set(data);

            // Increase count
            db.collection("lucky_draws")
                    .document(drawId)
                    .update("filledSlots", FieldValue.increment(1));

            Toast.makeText(this,
                    "Free entry added!",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
