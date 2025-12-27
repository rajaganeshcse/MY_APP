package com.example.rgamer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class activity_lucky_draw_winner extends AppCompatActivity {

    /* ================= UI ================= */

    RecyclerView recyclerView;
    LuckyDrawWinnerAdapter adapter;
    List<LuckyDrawModel> list = new ArrayList<>();

    /* ================= FIREBASE ================= */

    FirebaseFirestore db;
    ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw_winner);

        /* ---------- BACK BUTTON ---------- */
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        /* ---------- RECYCLER ---------- */
        recyclerView = findViewById(R.id.recyclerWinners);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawWinnerAdapter(list);
        recyclerView.setAdapter(adapter);

        /* ---------- FIRESTORE ---------- */
        db = FirebaseFirestore.getInstance();
        loadWinners();
    }

    /* ================= LOAD WINNERS ================= */

    private void loadWinners() {

        listener = db.collection("lucky_draws")
                .whereEqualTo("status", "COMPLETED")
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || e != null) return;

                    list.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        LuckyDrawModel model =
                                d.toObject(LuckyDrawModel.class);

                        if (model != null) {
                            list.add(model);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
