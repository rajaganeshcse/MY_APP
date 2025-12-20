package com.example.rgamer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends AppCompatActivity {

    RecyclerView recyclerHistory;
    WithdrawHistoryAdapter adapter;
    List<WithdrawHistoryModel> list;

    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔹 FULL SCREEN STATUS BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history_fragment);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();

        adapter = new WithdrawHistoryAdapter(list, model -> {

            // 🔥 OPEN WITHDRAW DETAIL SCREEN
            Intent intent = new Intent(
                    TransactionHistoryFragment.this,
                    activity_withdraw_success.class
            );

            intent.putExtra(activity_withdraw_success.EXTRA_TYPE, model.getType());
            intent.putExtra(activity_withdraw_success.EXTRA_AMOUNT, model.getAmount());
            intent.putExtra(activity_withdraw_success.EXTRA_REQUEST_ID, model.getRequest_id());

            startActivity(intent);
        });

        recyclerHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        loadWithdrawHistory();
    }

    private void loadWithdrawHistory() {

        db.collection("redeem_requests")
                .whereEqualTo("uid", uid)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null) return;

                    list.clear();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        value.getDocuments().forEach(doc -> {
                            WithdrawHistoryModel model =
                                    doc.toObject(WithdrawHistoryModel.class);

                            if (model != null) {
                                model.setRequest_id(doc.getId()); // 🔑 IMPORTANT
                                list.add(model);
                            }
                        });
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
