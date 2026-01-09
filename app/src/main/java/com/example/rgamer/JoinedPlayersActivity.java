package com.example.rgamer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class JoinedPlayersActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private JoinedPlayersAdapter adapter;

    private FirebaseFirestore db;
    private String tournamentId;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joined_players);

        /* ================= INIT ================= */
        recycler = findViewById(R.id.recyclerJoinedUsers);
        ImageView btnBack = findViewById(R.id.btnBack);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        /* ================= GET TOURNAMENT ID ================= */
        tournamentId = getIntent().getStringExtra("tournamentId");

        if (tournamentId == null) {
            Toast.makeText(this, "Invalid tournament", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* ================= REALTIME LISTENER ================= */
        listenJoinedUsers();

        /* ================= BACK ================= */
        btnBack.setOnClickListener(v -> finish());
    }

    private void listenJoinedUsers() {

        registration = db.collection("tournaments")
                .document(tournamentId)
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null || !snapshot.exists()) return;

                    FreeFireTournamentModel model =
                            snapshot.toObject(FreeFireTournamentModel.class);

                    if (model == null) return;

                    adapter = new JoinedPlayersAdapter(
                            model.getJoinedUsers()
                    );
                    recycler.setAdapter(adapter);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove(); // prevent memory leak
        }
    }
}
