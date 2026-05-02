package com.example.rgamer.Game;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
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

    private TextView txtGameName,txtTickets;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament);

        makeFullScreen();

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
        txtGameName = findViewById(R.id.txtGameName);
        txtTickets=findViewById(R.id.txtTickets);



        db.collection("users")
                .document(uid).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long tickets = documentSnapshot.getLong("tickets");
                        txtTickets.setText(""+tickets);
                    }
                });

        // Set banner and title


        imgBanner.setImageResource(banner);
        txtTitle.setText(title);

        // Default text
        txtGameName.setText("ENTER GAME ID :");

        recyclerTournament = findViewById(R.id.recyclerTournament);
        recyclerTournament.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FreeFireTournamentAdapter(
                this,
                list,
                new FreeFireTournamentAdapter.Listener() {

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
                        // TODO
                    }
                });

        recyclerTournament.setAdapter(adapter);

        loadMatches();
    }

    /* ================= FULL SCREEN ================= */
    private void makeFullScreen() {
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        window.setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    /* ================= LOAD MATCHES ================= */
    private void loadMatches() {
        Query query = db.collection("tournaments")
                .whereEqualTo("game", game)
        .whereEqualTo("status", "scheduled");

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
                    list.add(m);

                    // check join status
                    checkIfJoined(m);
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

                        txtGameName.setText("Joined Tournament");

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
    private void joinTournament(FreeFireTournamentModel model, String gameId) {

        if (uid == null) return;

        DocumentReference tournamentRef =
                db.collection("tournaments")
                        .document(model.getId());

        DocumentReference tournamentUserRef =
                tournamentRef
                        .collection("joined_users")
                        .document(uid);

        DocumentReference userJoinRef =
                db.collection("users")
                        .document(uid)
                        .collection("joined_tournaments")
                        .document(model.getId());

        String username = "USER";

        db.runTransaction(transaction -> {

            if (transaction.get(tournamentUserRef).exists()) {
                throw new RuntimeException("Already joined");
            }

            DocumentSnapshot snap = transaction.get(tournamentRef);

            long joined = snap.getLong("joinedSlots") == null
                    ? 0
                    : snap.getLong("joinedSlots");

            long totalSlots = snap.getLong("totalSlots") == null
                    ? 0
                    : snap.getLong("totalSlots");

            // ✅ prevent over join
            if (joined >= totalSlots) {
                throw new RuntimeException("Slots Full");
            }

            transaction.update(tournamentRef, "joinedSlots", joined + 1);

            Map<String, Object> tournamentUser = new HashMap<>();
            tournamentUser.put("username", username);
            tournamentUser.put("gameId", gameId);
            tournamentUser.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(tournamentUserRef, tournamentUser);

            Map<String, Object> userJoin = new HashMap<>();
            userJoin.put("game", game);
            userJoin.put("gameId", gameId);
            userJoin.put("username", username);
            userJoin.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(userJoinRef, userJoin);

            return null;

        }).addOnSuccessListener(unused -> {

            model.setJoined(true);
            model.setJoinedGameId(gameId);

            txtGameName.setText("GAME ID : " + gameId);

            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Joined Successfully", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}