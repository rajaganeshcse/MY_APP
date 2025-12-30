package com.example.rgamer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class TournamentActivity extends AppCompatActivity {

    private RecyclerView recyclerTournament;
    private FreeFireTournamentAdapter adapter;
    private final List<FreeFireTournamentModel> list = new ArrayList<>();

    private FirebaseFirestore db;
    private String game;
    private boolean showNew;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        /* ================= INTENT ================= */
        game = getIntent().getStringExtra("game");
        String title = getIntent().getStringExtra("title");
        int banner = getIntent().getIntExtra("banner", 0);
        showNew = getIntent().getBooleanExtra("showNew", false);

        /* ================= UI ================= */
        ImageView imgBanner = findViewById(R.id.imgBanner);
        TextView txtTitle = findViewById(R.id.txtTitle);

        imgBanner.setImageResource(banner);
        txtTitle.setText(title);

        recyclerTournament = findViewById(R.id.recyclerTournament);
        recyclerTournament.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new FreeFireTournamentAdapter(
                this,
                list,
                new FreeFireTournamentAdapter.Listener() {
                    @Override
                    public void onJoin(FreeFireTournamentModel model) {
                        // later join logic
                    }

                    @Override
                    public void onCheckWinners(FreeFireTournamentModel model) {
                        // later winners logic
                    }
                });

        recyclerTournament.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadMatches();
    }

    /* ================= LOAD MATCHES ================= */
    private void loadMatches() {

        Query query = db.collection("tournaments")
                .whereEqualTo("game", game);

        if (showNew) {
            // 🔥 NEW MATCHES FIRST
            query = query.orderBy(
                    "created_at",
                    Query.Direction.DESCENDING
            );
        }

        query.addSnapshotListener((snap, e) -> {

            if (e != null || snap == null) return;

            list.clear();

            for (DocumentSnapshot d : snap.getDocuments()) {

                FreeFireTournamentModel m =
                        d.toObject(FreeFireTournamentModel.class);

                if (m != null) {
                    m.setId(d.getId());
                    list.add(m);
                }
            }

            adapter.notifyDataSetChanged();
        });
    }
}
