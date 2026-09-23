package com.app.rewardsplanet.withdraws;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.rewardsplanet.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardHistory extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RewardAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerTransactions;
    private LinearLayout layoutEmptyTransactions;
    private AdView adViewTransactions;

    private TextView txtHeaderCoins;
    private TextView txtTotalCredited, txtTotalDebited;
    private TextView tabAll, tabAds, tabOffer, tabReferral, tabSpin, tabScratch, tabWithdrawal;

    private final List<CoinModel> rawList = new ArrayList<>();
    private final List<ListItem> finalList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration coinDetailsListener;
    private ListenerRegistration userBalanceListener;

    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        setupStatusBar();
        initViews();
        setupTabs();

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        setupRecyclerView();
        setupAd();
        loadBalance();
        loadData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        txtHeaderCoins = findViewById(R.id.txtHeaderCoins);
        txtTotalCredited = findViewById(R.id.txtTotalCredited);
        txtTotalDebited = findViewById(R.id.txtTotalDebited);

        tabAll = findViewById(R.id.tabAll);
        tabAds = findViewById(R.id.tabAds);
        tabOffer = findViewById(R.id.tabOffer);
        tabReferral = findViewById(R.id.tabReferral);
        tabSpin = findViewById(R.id.tabSpin);
        tabScratch = findViewById(R.id.tabScratch);
        tabWithdrawal = findViewById(R.id.tabWithdrawal);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        shimmerTransactions = findViewById(R.id.shimmerTransactions);
        layoutEmptyTransactions = findViewById(R.id.layoutEmptyTransactions);
        adViewTransactions = findViewById(R.id.adViewTransactions);

        swipeRefresh.setColorSchemeColors(Color.parseColor("#4F46E5"));
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RewardAdapter(finalList, this::showTransactionDetailDialog);
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> setFilter("all"));
        tabAds.setOnClickListener(v -> setFilter("ads"));
        tabOffer.setOnClickListener(v -> setFilter("offer"));
        tabReferral.setOnClickListener(v -> setFilter("referral"));
        tabSpin.setOnClickListener(v -> setFilter("spin"));
        tabScratch.setOnClickListener(v -> setFilter("scratch"));
        tabWithdrawal.setOnClickListener(v -> setFilter("withdrawal"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateTabUI();
        applyFilter();
    }

    private void updateTabUI() {
        updateTabStyle(tabAll, "all".equals(currentFilter));
        updateTabStyle(tabAds, "ads".equals(currentFilter));
        updateTabStyle(tabOffer, "offer".equals(currentFilter));
        updateTabStyle(tabReferral, "referral".equals(currentFilter));
        updateTabStyle(tabSpin, "spin".equals(currentFilter));
        updateTabStyle(tabScratch, "scratch".equals(currentFilter));
        updateTabStyle(tabWithdrawal, "withdrawal".equals(currentFilter));
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (tab == null) return;
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected);
            tab.setTextColor(Color.WHITE);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(Color.parseColor("#475569"));
        }
    }

    private void loadBalance() {
        if (uid == null) return;
        userBalanceListener = db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        Long coins = doc.getLong("coin");
                        if (coins == null) {
                            coins = doc.getLong("coins");
                        }
                        if (coins != null && txtHeaderCoins != null) {
                            txtHeaderCoins.setText(NumberFormat.getInstance(Locale.US).format(coins));
                        }
                    }
                });
    }

    private void loadData() {
        if (uid == null) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        if (shimmerTransactions != null && rawList.isEmpty()) {
            shimmerTransactions.setVisibility(View.VISIBLE);
            shimmerTransactions.startShimmer();
            recyclerView.setVisibility(View.GONE);
            layoutEmptyTransactions.setVisibility(View.GONE);
        }

        if (coinDetailsListener != null) {
            coinDetailsListener.remove();
        }

        coinDetailsListener = db.collection("users")
                .document(uid)
                .collection("coinDetails")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (shimmerTransactions != null) {
                        shimmerTransactions.stopShimmer();
                        shimmerTransactions.setVisibility(View.GONE);
                    }
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }

                    if (value == null) {
                        applyFilter();
                        return;
                    }

                    rawList.clear();
                    long totalCredit = 0;
                    long totalDebit = 0;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        CoinModel model = doc.toObject(CoinModel.class);
                        if (model != null) {
                            rawList.add(model);
                            if ("Credit".equalsIgnoreCase(model.getStatus())) {
                                totalCredit += model.getAmount();
                            } else {
                                totalDebit += model.getAmount();
                            }
                        }
                    }

                    if (txtTotalCredited != null) {
                        txtTotalCredited.setText("+" + NumberFormat.getInstance(Locale.US).format(totalCredit));
                    }
                    if (txtTotalDebited != null) {
                        txtTotalDebited.setText("-" + NumberFormat.getInstance(Locale.US).format(totalDebit));
                    }

                    applyFilter();
                });
    }

    private void applyFilter() {
        finalList.clear();
        String lastMonth = "";

        for (CoinModel m : rawList) {
            if (!matchesFilter(m, currentFilter)) {
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

        if (finalList.isEmpty()) {
            layoutEmptyTransactions.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptyTransactions.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesFilter(CoinModel m, String filter) {
        if ("all".equalsIgnoreCase(filter)) return true;
        String type = m.getType();
        if (type == null) return false;

        switch (filter.toLowerCase(Locale.ROOT)) {
            case "ads":
                return type.equalsIgnoreCase("ads");
            case "offer":
                return type.equalsIgnoreCase("offer");
            case "referral":
                return type.equalsIgnoreCase("referral");
            case "spin":
                return type.equalsIgnoreCase("Daily spin") || type.equalsIgnoreCase("spin");
            case "scratch":
                return type.equalsIgnoreCase("scratch") || type.equalsIgnoreCase("Daily scratch");
            case "withdrawal":
                return type.equalsIgnoreCase("Withdrawal");
            default:
                return type.equalsIgnoreCase(filter);
        }
    }

    private void showTransactionDetailDialog(CoinModel model) {
        if (model == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_coin_transaction_details, null);
        dialog.setContentView(view);

        ImageView imgDetailIcon = view.findViewById(R.id.imgDetailIcon);
        TextView txtDetailAmount = view.findViewById(R.id.txtDetailAmount);
        ImageView imgDetailCurrency = view.findViewById(R.id.imgDetailCurrency);
        TextView txtDetailStatus = view.findViewById(R.id.txtDetailStatus);
        TextView txtDetailType = view.findViewById(R.id.txtDetailType);
        TextView txtDetailDate = view.findViewById(R.id.txtDetailDate);
        TextView txtDetailRewardKind = view.findViewById(R.id.txtDetailRewardKind);
        ImageView btnCloseDialog = view.findViewById(R.id.btnCloseDialog);
        Button btnDone = view.findViewById(R.id.btnDone);

        // Amount & Status
        boolean isCredit = "Credit".equalsIgnoreCase(model.getStatus());
        if (isCredit) {
            txtDetailAmount.setText("+" + model.getAmount());
            txtDetailAmount.setTextColor(Color.parseColor("#10B981"));
            txtDetailStatus.setText("CREDITED");
            txtDetailStatus.setBackgroundResource(R.drawable.bg_status_approved);
            txtDetailStatus.setTextColor(Color.parseColor("#065F46"));
        } else {
            txtDetailAmount.setText("-" + model.getAmount());
            txtDetailAmount.setTextColor(Color.parseColor("#EF4444"));
            txtDetailStatus.setText("DEBITED");
            txtDetailStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            txtDetailStatus.setTextColor(Color.parseColor("#991B1B"));
        }

        // Currency
        boolean isToken = "token".equalsIgnoreCase(model.getIstype());
        if (isToken) {
            imgDetailCurrency.setImageResource(R.drawable.ic_ticket);
            txtDetailRewardKind.setText("Tickets / Tokens");
        } else {
            imgDetailCurrency.setImageResource(R.drawable.ic_coin);
            txtDetailRewardKind.setText("Coins");
        }

        // Category & Icon
        String typeStr = model.getType() != null ? model.getType() : "Reward";
        txtDetailType.setText(formatTypeTitle(typeStr));

        if ("ads".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_watch);
        } else if ("offer".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_money);
        } else if ("referral".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_share);
        } else if ("Daily spin".equalsIgnoreCase(typeStr) || "spin".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_spinner);
        } else if ("Lucky Draw".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_lucky);
        } else if ("scratch".equalsIgnoreCase(typeStr) || "Daily scratch".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.ic_gift);
        } else if ("Withdrawal".equalsIgnoreCase(typeStr)) {
            imgDetailIcon.setImageResource(R.drawable.img_redeem);
        } else {
            imgDetailIcon.setImageResource(R.drawable.ic_reward);
        }

        // Date & Time
        String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(model.getTimeMillis()));
        txtDetailDate.setText(dateStr);

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String formatTypeTitle(String type) {
        if (type == null || type.isEmpty()) return "Reward";
        if (type.equalsIgnoreCase("ads")) return "Ad Reward";
        if (type.equalsIgnoreCase("offer")) return "Offer Task";
        if (type.equalsIgnoreCase("referral")) return "Referral Bonus";
        if (type.equalsIgnoreCase("Daily spin") || type.equalsIgnoreCase("spin")) return "Spin Wheel Reward";
        if (type.equalsIgnoreCase("Lucky Draw")) return "Lucky Draw Contest";
        if (type.equalsIgnoreCase("scratch") || type.equalsIgnoreCase("Daily scratch")) return "Scratch Card Reward";
        if (type.equalsIgnoreCase("Withdrawal")) return "Redeem Payout";
        return type.substring(0, 1).toUpperCase(Locale.getDefault()) + type.substring(1);
    }

    private void setupAd() {
        if (adViewTransactions != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewTransactions.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewTransactions != null) adViewTransactions.resume();
    }

    @Override
    protected void onPause() {
        if (adViewTransactions != null) adViewTransactions.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adViewTransactions != null) adViewTransactions.destroy();
        if (coinDetailsListener != null) coinDetailsListener.remove();
        if (userBalanceListener != null) userBalanceListener.remove();
        super.onDestroy();
    }
}