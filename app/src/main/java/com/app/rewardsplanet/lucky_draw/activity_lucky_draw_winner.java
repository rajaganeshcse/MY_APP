package com.app.rewardsplanet.lucky_draw;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.LuckyDrawModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class activity_lucky_draw_winner extends AppCompatActivity {

    private static final String TAG = "LuckyDrawWinner";

    RecyclerView recyclerView;
    LuckyDrawWinnerAdapter adapter;
    List<LuckyDrawModel> list = new ArrayList<>();

    FirebaseFirestore db;
    ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw_winner);

        makeFullScreen();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerWinners);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LuckyDrawWinnerAdapter(list);
            recyclerView.setAdapter(adapter);
        }

        db = FirebaseFirestore.getInstance();

        loadWinners();
    }

    private void loadWinners() {
        if (db == null) return;

        listener = db.collection("lucky_draws")
                .whereIn("status", Arrays.asList("CLOSED", "COMPLETED"))
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        Log.e(TAG, "Error fetching winner history: " + e.getMessage());
                        return;
                    }

                    if (snap == null) return;

                    list.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        try {
                            LuckyDrawModel model = d.toObject(LuckyDrawModel.class);
                            if (model != null) {
                                model.setId(d.getId());
                                list.add(model);
                            }
                        } catch (Exception parseEx) {
                            Log.e(TAG, "Error parsing draw document " + d.getId() + ": " + parseEx.getMessage());
                        }
                    }

                    // Sort descending by completedAt / createdAt timestamp in memory
                    Collections.sort(list, (a, b) -> {
                        Timestamp tA = a.getCompletedAt() != null ? a.getCompletedAt() : a.getCreatedAt();
                        Timestamp tB = b.getCompletedAt() != null ? b.getCompletedAt() : b.getCreatedAt();
                        if (tA == null && tB == null) return 0;
                        if (tA == null) return 1;
                        if (tB == null) return -1;
                        return tB.compareTo(tA);
                    });

                    if (adapter != null) {
                        adapter.setDrawList(list);
                    }
                });
    }

    private void makeFullScreen() {
        try {
            Window window = getWindow();
            if (window == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);

                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                //noinspection deprecation
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }

            window.setStatusBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
        } catch (Exception e) {
            Log.w(TAG, "makeFullScreen error: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }
}