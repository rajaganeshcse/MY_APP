package com.example.rgamer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class TournamentActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView btnHistory;

    RecyclerView recyclerTournament;
    FreeFireTournamentAdapter adapter;
    List<FreeFireTournamentModel> list;

    FirebaseFirestore db;
    FirebaseAuth auth;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        // INIT
        btnBack = findViewById(R.id.btnBack);
        btnHistory = findViewById(R.id.btnHistory);
        recyclerTournament = findViewById(R.id.recyclerTournament);

        recyclerTournament.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        adapter = new FreeFireTournamentAdapter(this, list, this::joinTournament);
        recyclerTournament.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnHistory.setOnClickListener(v ->
                Toast.makeText(this, "Tournament History", Toast.LENGTH_SHORT).show());

        loadTournaments();
    }

    // 🔥 Load tournaments (Admin controlled)
    private void loadTournaments() {

        db.collection("freefire_tournaments")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    list.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        FreeFireTournamentModel model =
                                doc.toObject(FreeFireTournamentModel.class);
                        list.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // 🔥 JOIN TOURNAMENT (SAFE)
    private void joinTournament(FreeFireTournamentModel tournament) {

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    UserModel user = snapshot.toObject(UserModel.class);

                    if (user.getCoins() < tournament.getEntryTickets()) {
                        Toast.makeText(this,
                                "Not enough tickets",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (tournament.getJoinedSlots() >= tournament.getTotalSlots()) {
                        Toast.makeText(this,
                                "Tournament Full",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    // Deduct tickets
                    DocumentReference userRef =
                            db.collection("users").document(uid);
                    batch.update(userRef,
                            "coins",
                            FieldValue.increment(-tournament.getEntryTickets()));

                    // Increase joined slots
                    DocumentReference tourRef =
                            db.collection("freefire_tournaments")
                                    .document(tournament.getTournamentId());

                    batch.update(tourRef,
                            "joined_slots",
                            FieldValue.increment(1));

                    // Save participant
                    DocumentReference partRef =
                            tourRef.collection("participants").document(uid);

                    batch.set(partRef, new Participant(uid));

                    batch.commit().addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    "Joined Free Fire Tournament",
                                    Toast.LENGTH_SHORT).show());
                });
    }

    // 🔹 Participant inner model
    static class Participant {
        public String uid;
        public long joined_at;

        Participant(String uid) {
            this.uid = uid;
            this.joined_at = System.currentTimeMillis();
        }
    }
}
