package com.app.rewardsplanet.withdraws;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardHistory extends AppCompatActivity {

    RecyclerView recyclerView;
    RewardAdapter adapter;

    List<CoinModel> rawList = new ArrayList<>();
    List<ListItem> finalList = new ArrayList<>();

    FirebaseFirestore db;
    String uid;

    String currentFilter = "all";

    TextView tabAll, tabAds, tabOffer, tabReferral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        recyclerView = findViewById(R.id.recyclerView);
        ImageView btnBack = findViewById(R.id.btnBack);

        tabAll = findViewById(R.id.tabAll);
        tabAds = findViewById(R.id.tabAds);
        tabOffer = findViewById(R.id.tabOffer);
        tabReferral = findViewById(R.id.tabReferral);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RewardAdapter(finalList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        btnBack.setOnClickListener(v -> finish());
makeFullScreen();
        setupTabs();
        loadData();
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {setFilter("all");
        });
        tabAds.setOnClickListener(v -> setFilter("ads"));
        tabOffer.setOnClickListener(v -> setFilter("offer"));
        tabReferral.setOnClickListener(v -> setFilter("referral"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateTabUI();
        applyFilter();
    }

    private void updateTabUI() {

        // ALL
        if (currentFilter.equals("all")) {
            tabAll.setBackgroundResource(R.drawable.bg_tab_selected);
            tabAll.setTextColor(Color.WHITE);
        } else {
            tabAll.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabAll.setTextColor(Color.BLACK);
        }

        // ADS
        if (currentFilter.equals("ads")) {
            tabAds.setBackgroundResource(R.drawable.bg_tab_selected);
            tabAds.setTextColor(Color.WHITE);
        } else {
            tabAds.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabAds.setTextColor(Color.BLACK);
        }

        // OFFER
        if (currentFilter.equals("offer")) {
            tabOffer.setBackgroundResource(R.drawable.bg_tab_selected);
            tabOffer.setTextColor(Color.WHITE);
        } else {
            tabOffer.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabOffer.setTextColor(Color.BLACK);
        }

        // REFERRAL
        if (currentFilter.equals("referral")) {
            tabReferral.setBackgroundResource(R.drawable.bg_tab_selected);
            tabReferral.setTextColor(Color.WHITE);
        } else {
            tabReferral.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabReferral.setTextColor(Color.BLACK);
        }
    }

    private void loadData() {

        db.collection("users")
                .document(uid)
                .collection("coinDetails")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null) return;

                    rawList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        CoinModel model = doc.toObject(CoinModel.class);
                        if (model != null) rawList.add(model);
                    }

                    applyFilter();
                });
    }

    private void applyFilter() {

        finalList.clear();
        String lastMonth = "";

        for (CoinModel m : rawList) {

            if (!currentFilter.equals("all") &&
                    (m.getType() == null || !m.getType().equalsIgnoreCase(currentFilter))) {
                continue;
            }

            String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(new Date(m.getTimeMillis()));

            if (!month.equals(lastMonth)) {
                finalList.add(new ListItem(month));
                lastMonth = month;
            }

            finalList.add(new ListItem(m));
        }

        adapter.notifyDataSetChanged();
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

}