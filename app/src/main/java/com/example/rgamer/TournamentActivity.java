package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.List;

public class TournamentActivity extends AppCompatActivity {

    private static final String TAG = "TOUR";

    /* ================= UI ================= */
    private ImageView btnBack, imgBanner;
    private TextView txtTitle;
    private RecyclerView recyclerTournament;

    /* ================= ADAPTER ================= */
    private FreeFireTournamentAdapter adapter;
    private final List<FreeFireTournamentModel> list = new ArrayList<>();

    /* ================= FIREBASE ================= */
    private FirebaseFirestore db;
    private String uid;
    private String game;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        Log.d(TAG, "TournamentActivity started");

        /* ================= INTENT ================= */
        game = getIntent().getStringExtra("game");
        String title = getIntent().getStringExtra("title");
        int banner = getIntent().getIntExtra("banner", 0);

        if (game == null) {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* ================= UI INIT ================= */
        btnBack = findViewById(R.id.btnBack);
        imgBanner = findViewById(R.id.imgBanner);
        txtTitle = findViewById(R.id.txtTitle);
        recyclerTournament = findViewById(R.id.recyclerTournament);

        txtTitle.setText(title);
        imgBanner.setImageResource(banner);

        recyclerTournament.setLayoutManager(
                new LinearLayoutManager(this)
        );

        /* ================= ADAPTER ================= */
        adapter = new FreeFireTournamentAdapter(
                this,
                list,
                new FreeFireTournamentAdapter.Listener() {
                    @Override
                    public void onJoin(FreeFireTournamentModel model) {
                        joinTournament(model);
                    }

                    @Override
                    public void onCheckWinners(FreeFireTournamentModel model) {
                        openWinners(model);
                    }
                }
        );

        recyclerTournament.setAdapter(adapter);

        /* ================= FIREBASE ================= */
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());

        /* ================= LOAD DATA ================= */
        loadTournaments();
    }

    /* ================= LOAD TOURNAMENTS ================= */

    private void loadTournaments() {

        db.collection("tournaments")
                .whereEqualTo("game", game)
                .orderBy("startTimeMillis", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        Log.e(TAG, "Firestore error", e);
                        return;
                    }

                    if (snap == null) return;

                    list.clear();

                    long now = System.currentTimeMillis();

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        FreeFireTournamentModel m =
                                d.toObject(FreeFireTournamentModel.class);

                        if (m == null) continue;

                        m.setId(d.getId());

                        /* 🔥 AUTO UPDATE STATUS: scheduled → live */
                        if ("scheduled".equalsIgnoreCase(m.getStatus())
                                && m.getStartTimeMillis() <= now) {

                            db.collection("tournaments")
                                    .document(m.getId())
                                    .update("status", "live");
                        }

                        list.add(m);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    /* ================= JOIN TOURNAMENT (SAFE) ================= */

    private void joinTournament(FreeFireTournamentModel t) {

        db.runTransaction(transaction -> {

            DocumentReference userRef =
                    db.collection("users").document(uid);

            DocumentReference tourRef =
                    db.collection("tournaments").document(t.getId());

            DocumentReference joinRef =
                    tourRef.collection("participants").document(uid);

            /* ===== ALREADY JOINED CHECK ===== */
            if (transaction.get(joinRef).exists()) {
                throw new RuntimeException("Already joined");
            }

            Long tickets =
                    transaction.get(userRef).getLong("tickets");

            Long joined =
                    transaction.get(tourRef).getLong("joinedSlots");

            Long total =
                    transaction.get(tourRef).getLong("totalSlots");

            Long startTime =
                    transaction.get(tourRef).getLong("startTimeMillis");

            if (tickets == null || joined == null
                    || total == null || startTime == null) {
                throw new RuntimeException("Data error");
            }

            /* 🔥 BLOCK JOIN AFTER START */
            if (System.currentTimeMillis() >= startTime) {
                throw new RuntimeException("Tournament already started");
            }

            if (tickets < t.getEntryTickets()) {
                throw new RuntimeException("Not enough tickets");
            }

            if (joined >= total) {
                throw new RuntimeException("Slots full");
            }

            /* ===== UPDATE DATA ===== */

            transaction.update(
                    userRef,
                    "tickets",
                    tickets - t.getEntryTickets()
            );

            transaction.update(
                    tourRef,
                    "joinedSlots",
                    FieldValue.increment(1)
            );

            transaction.set(
                    joinRef,
                    new Participant(uid)
            );

            return null;

        }).addOnSuccessListener(unused ->
                Toast.makeText(
                        this,
                        "Joined Successfully",
                        Toast.LENGTH_SHORT
                ).show()
        ).addOnFailureListener(e ->
                Toast.makeText(
                        this,
                        e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    /* ================= OPEN WINNERS ================= */

    private void openWinners(FreeFireTournamentModel model) {
        Intent i = new Intent(this, activity_winners.class);
        i.putExtra("tournamentId", model.getId());
        startActivity(i);
    }

    /* ================= PARTICIPANT MODEL ================= */

    static class Participant {
        public String uid;
        public long joined_at;

        Participant(String uid) {
            this.uid = uid;
            this.joined_at = System.currentTimeMillis();
        }
    }
}
