package com.example.rgamer.withdraws;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private static final String TAG = "HISTORY";

    private RecyclerView recyclerHistory;
    private WithdrawHistoryAdapter adapter;
    private final List<WithdrawHistoryModel> list = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration historyListener;

    private ImageView btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history_fragment);

        // Views
        btnBack = findViewById(R.id.btnBack);
        recyclerHistory = findViewById(R.id.recyclerHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));


        // Adapter
        adapter = new WithdrawHistoryAdapter(list, model -> {
            Intent intent = new Intent(
                    TransactionHistoryActivity.this,
                    activity_withdraw_success.class
            );
            intent.putExtra(activity_withdraw_success.EXTRA_TYPE, model.getType());
            intent.putExtra(activity_withdraw_success.EXTRA_DATE, model.getFormattedDate());

            intent.putExtra(
                    activity_withdraw_success.EXTRA_AMOUNT,
                    "₹ " + model.getAmount()
            );
            intent.putExtra(activity_withdraw_success.EXTRA_REQUEST_ID,model.getRequest_id());
            startActivity(intent);
        });

        recyclerHistory.setAdapter(adapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
        makeFullScreen();
        loadWithdrawHistory();
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

    // ================= LOAD HISTORY =================


    private void loadWithdrawHistory() {

        if (uid == null) {
            Log.e(TAG, "UID is null");
            return;
        }

        historyListener = db.collection("redeem_requests")
                .whereEqualTo("uid", uid)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Firestore error", error);
                        return;
                    }

                    if (value == null) return;

                    list.clear();

                    for (var doc : value.getDocuments()) {

                        WithdrawHistoryModel model =
                                doc.toObject(WithdrawHistoryModel.class);

                        if (model == null) continue;

// 🔥 FORCE SET TIMESTAMP
                        model.setCreated_at(doc.getTimestamp("created_at"));

                        model.setRequest_id(doc.getId());
                        list.add(model);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) {
            historyListener.remove();
        }
    }
}