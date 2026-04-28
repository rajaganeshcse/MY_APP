package com.example.rgamer.Game;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
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
        makeFullScreen();

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
