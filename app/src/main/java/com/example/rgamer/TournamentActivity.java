package com.example.rgamer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentActivity extends AppCompatActivity {

    private RecyclerView recyclerTournament;
    private FreeFireTournamentAdapter adapter;
    private final List<FreeFireTournamentModel> list = new ArrayList<>();

    private FirebaseFirestore db;
    private String game;
    private boolean showNew;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        uid = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();

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
        recyclerTournament.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FreeFireTournamentAdapter(
                this,
                list,
                new FreeFireTournamentAdapter.Listener() {

                    /* ================= JOIN ================= */
                    @Override
                    public void onJoin(FreeFireTournamentModel model) {

                        GameIdManager.getGameId(game, gameId -> {

                            if (gameId == null) {
                                showGameIdBottomSheet(model);
                            } else {
                                joinTournament(model, gameId);
                            }
                        });
                    }

                    @Override
                    public void onCheckWinners(FreeFireTournamentModel model) {
                        // unchanged
                    }
                });

        recyclerTournament.setAdapter(adapter);

        loadMatches();
    }

    /* ================= LOAD MATCHES ================= */
    private void loadMatches() {

        Query query = db.collection("tournaments")
                .whereEqualTo("game", game);

        if (showNew) {
            query = query.orderBy("created_at", Query.Direction.DESCENDING);
        }

        query.addSnapshotListener((snap, e) -> {

            if (e != null || snap == null) return;

            list.clear();

            for (DocumentSnapshot d : snap.getDocuments()) {

                FreeFireTournamentModel m =
                        d.toObject(FreeFireTournamentModel.class);

                if (m != null) {
                    m.setId(d.getId());

                    // 🔥 CHECK IF USER JOINED THIS MATCH
                    checkIfJoined(m);

                    list.add(m);
                }
            }

            adapter.notifyDataSetChanged();
        });
    }

    /* ================= CHECK JOINED ================= */
    private void checkIfJoined(FreeFireTournamentModel model) {

        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("joined_tournaments")
                .document(model.getId())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        model.setJoined(true);
                        model.setJoinedGameId(doc.getString("gameId"));
                        model.setJoinedUsername(
                                doc.getString("username")
                        );
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /* ================= ASK GAME ID ================= */
    private void showGameIdBottomSheet(FreeFireTournamentModel model) {

        JoinGameIdBottomSheet sheet =
                new JoinGameIdBottomSheet(game, gameId -> {

                    GameIdManager.saveGameId(game, gameId);
                    joinTournament(model, gameId);
                });

        sheet.show(getSupportFragmentManager(), "GAME_ID_SHEET");
    }

    /* ================= JOIN TOURNAMENT ================= */
    private void joinTournament(
            FreeFireTournamentModel model,
            String gameId
    ) {

        if (uid == null) return;

        DocumentReference tournamentRef =
                db.collection("tournaments")
                        .document(model.getId());

        DocumentReference userJoinRef =
                db.collection("users")
                        .document(uid)
                        .collection("joined_tournaments")
                        .document(model.getId());

        db.runTransaction(transaction -> {

            DocumentSnapshot snap =
                    transaction.get(tournamentRef);

            long joined =
                    snap.getLong("joinedSlots") == null
                            ? 0
                            : snap.getLong("joinedSlots");

            transaction.update(
                    tournamentRef,
                    "joinedSlots",
                    joined + 1
            );

            Map<String, Object> joinData = new HashMap<>();
            joinData.put("game", game);
            joinData.put("gameId", gameId);
            joinData.put("username", "USER"); // replace with real username
            joinData.put("joinedAt",
                    FieldValue.serverTimestamp());

            transaction.set(userJoinRef, joinData);

            return null;
        }).addOnSuccessListener(unused -> {

            // UPDATE UI
            model.setJoined(true);
            model.setJoinedGameId(gameId);
            model.setJoinedUsername("USER"); // same username
            adapter.notifyDataSetChanged();

            Toast.makeText(
                    this,
                    "Joined Successfully",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(e ->
                Toast.makeText(
                        this,
                        e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
