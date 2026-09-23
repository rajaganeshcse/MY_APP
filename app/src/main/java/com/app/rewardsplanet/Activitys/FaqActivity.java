package com.app.rewardsplanet.Activitys;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FaqActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFaq;
    private FaqAdapter adapter;
    private EditText edtSearchFaq;
    private ImageView btnClearSearch;
    private LinearLayout layoutEmptyFaq;
    private AdView adViewFaq;

    private TextView tabFaqAll, tabFaqCoins, tabFaqWithdraw, tabFaqRefer, tabFaqRules;

    private final List<FaqItem> allFaqList = new ArrayList<>();
    private final List<FaqItem> filteredList = new ArrayList<>();

    private String currentCategory = "all";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        setupStatusBar();
        initViews();
        setupFaqData();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupAd();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        edtSearchFaq = findViewById(R.id.edtSearchFaq);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        recyclerViewFaq = findViewById(R.id.recyclerViewFaq);
        layoutEmptyFaq = findViewById(R.id.layoutEmptyFaq);
        adViewFaq = findViewById(R.id.adViewFaq);

        tabFaqAll = findViewById(R.id.tabFaqAll);
        tabFaqCoins = findViewById(R.id.tabFaqCoins);
        tabFaqWithdraw = findViewById(R.id.tabFaqWithdraw);
        tabFaqRefer = findViewById(R.id.tabFaqRefer);
        tabFaqRules = findViewById(R.id.tabFaqRules);

        MaterialButton btnFaqContactSupport = findViewById(R.id.btnFaqContactSupport);
        if (btnFaqContactSupport != null) {
            btnFaqContactSupport.setOnClickListener(v -> {
                Intent intent = new Intent(FaqActivity.this, ContactUsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupFaqData() {
        allFaqList.clear();

        // Coins & Tasks
        allFaqList.add(new FaqItem(
                "How do I earn coins in DailyKash?",
                "You can earn coins by spinning the daily lucky wheel, scratching cards, completing partner tasks and offerwalls, sharing curated offers with your network, and referring your friends.",
                "Coins & Tasks"
        ));
        allFaqList.add(new FaqItem(
                "Why haven't I received coins after completing an offer?",
                "Offerwall partners typically take between 15 minutes to 24 hours to verify app installs or survey completions. Ensure you followed all instructions without switching networks or using VPNs.",
                "Coins & Tasks"
        ));
        allFaqList.add(new FaqItem(
                "What is the difference between Coins and Tickets?",
                "Coins represent real rewards currency that can be redeemed for UPI cash, Google Play gift cards, and vouchers. Tickets are special tokens used to participate in Lucky Draws and Esports tournaments.",
                "Coins & Tasks"
        ));
        allFaqList.add(new FaqItem(
                "When do daily spin and scratch limits reset?",
                "Daily spin and scratch limits reset automatically every midnight (12:00 AM) based on your local timezone.",
                "Coins & Tasks"
        ));

        // Withdrawals
        allFaqList.add(new FaqItem(
                "When will my payout request be approved?",
                "All payout requests are manually reviewed by our security team to prevent fraudulent abuse. Payouts are usually processed within 24 to 48 business hours.",
                "Withdrawals"
        ));
        allFaqList.add(new FaqItem(
                "What payout methods are supported in the app?",
                "We support direct UPI transfer, Google Play redeem gift codes, PhonePe, Paytm, and Amazon Pay voucher codes, depending on your region.",
                "Withdrawals"
        ));
        allFaqList.add(new FaqItem(
                "Why was my withdrawal request rejected?",
                "Requests may be rejected if invalid payment details were submitted (e.g. invalid UPI ID) or if multi-accounting/VPN usage was detected. If rejected, your coins are safely returned to your wallet.",
                "Withdrawals"
        ));

        // Refer & Earn
        allFaqList.add(new FaqItem(
                "How does the Refer & Earn bonus work?",
                "Share your unique referral code or link with friends. When they install DailyKash and sign in with your code, both you and your friend receive instant bonus coins.",
                "Refer & Earn"
        ));
        allFaqList.add(new FaqItem(
                "Where can I view my referred friends?",
                "Open the Refer & Earn screen from the navigation drawer or bottom bar, where you can see your total referrals, pending bonuses, and conversion history.",
                "Refer & Earn"
        ));

        // Account & Rules
        allFaqList.add(new FaqItem(
                "Is VPN or proxy allowed while using DailyKash?",
                "No. Using VPNs, proxy servers, emulators, auto-clickers, or cloned apps is strictly prohibited by our terms. Accounts violating these rules will be permanently suspended.",
                "Account & Rules"
        ));
        allFaqList.add(new FaqItem(
                "Can I create multiple accounts on the same phone?",
                "No. DailyKash enforces a strict policy of one account per device. Creating multiple accounts on a single device will result in automated bans.",
                "Account & Rules"
        ));
        allFaqList.add(new FaqItem(
                "How can I update my profile details?",
                "Tap your avatar or the edit icon in the top drawer menu to navigate to the Edit Profile screen, where you can update your phone number and display information.",
                "Account & Rules"
        ));
    }

    private void setupRecyclerView() {
        recyclerViewFaq.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FaqAdapter(filteredList);
        recyclerViewFaq.setAdapter(adapter);
        filterFaqList();
    }

    private void setupTabs() {
        tabFaqAll.setOnClickListener(v -> setCategory("all"));
        tabFaqCoins.setOnClickListener(v -> setCategory("Coins & Tasks"));
        tabFaqWithdraw.setOnClickListener(v -> setCategory("Withdrawals"));
        tabFaqRefer.setOnClickListener(v -> setCategory("Refer & Earn"));
        tabFaqRules.setOnClickListener(v -> setCategory("Account & Rules"));
    }

    private void setCategory(String category) {
        currentCategory = category;
        updateTabStyles();
        filterFaqList();
    }

    private void updateTabStyles() {
        updateTab(tabFaqAll, "all".equalsIgnoreCase(currentCategory));
        updateTab(tabFaqCoins, "Coins & Tasks".equalsIgnoreCase(currentCategory));
        updateTab(tabFaqWithdraw, "Withdrawals".equalsIgnoreCase(currentCategory));
        updateTab(tabFaqRefer, "Refer & Earn".equalsIgnoreCase(currentCategory));
        updateTab(tabFaqRules, "Account & Rules".equalsIgnoreCase(currentCategory));
    }

    private void updateTab(TextView tab, boolean selected) {
        if (tab == null) return;
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected);
            tab.setTextColor(Color.WHITE);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(Color.parseColor("#475569"));
        }
    }

    private void setupSearch() {
        edtSearchFaq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                filterFaqList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                edtSearchFaq.setText("");
            });
        }
    }

    private void filterFaqList() {
        filteredList.clear();

        for (FaqItem item : allFaqList) {
            boolean matchesCategory = "all".equalsIgnoreCase(currentCategory)
                    || item.getCategory().equalsIgnoreCase(currentCategory);

            boolean matchesSearch = currentQuery.isEmpty()
                    || item.getQuestion().toLowerCase(Locale.getDefault()).contains(currentQuery)
                    || item.getAnswer().toLowerCase(Locale.getDefault()).contains(currentQuery);

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (layoutEmptyFaq != null && recyclerViewFaq != null) {
            if (filteredList.isEmpty()) {
                layoutEmptyFaq.setVisibility(View.VISIBLE);
                recyclerViewFaq.setVisibility(View.GONE);
            } else {
                layoutEmptyFaq.setVisibility(View.GONE);
                recyclerViewFaq.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupAd() {
        if (adViewFaq != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewFaq.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewFaq != null) adViewFaq.resume();
    }

    @Override
    protected void onPause() {
        if (adViewFaq != null) adViewFaq.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adViewFaq != null) adViewFaq.destroy();
        super.onDestroy();
    }
}
