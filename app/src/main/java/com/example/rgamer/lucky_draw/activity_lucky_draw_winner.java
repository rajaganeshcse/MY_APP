package com.example.rgamer.lucky_draw;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;
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

        setContentView(com.example.rgamer.R.layout.activity_lucky_draw_winner);
        makeFullScreen();

        /* ---------- BACK BUTTON ---------- */
        findViewById(com.example.rgamer.R.id.btnBack).setOnClickListener(v -> finish());

        /* ---------- RECYCLER ---------- */
        recyclerView = findViewById(R.id.recyclerWinners);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawWinnerAdapter(list);
        recyclerView.setAdapter(adapter);

        /* ---------- FIRESTORE ---------- */
        db = FirebaseFirestore.getInstance();
        loadWinners();
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
