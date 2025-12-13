package com.example.rgamer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;



import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    UserPref userPref;

    TextView txtCoins, txtToken;
    ImageView imgProfile;

    // Cards
    LinearLayout cardWatchEarn, cardLuckyDraw, cardSpinner, cardTasks, cardSurveys, cardInvite;

    // Bottom Nav
    LinearLayout navHome, navGame, navReward, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userPref = new UserPref(this);

        initViews();
        loadUserData();
        setupCardClickActions();
        setupBottomNav();
    }

    private void initViews() {

        txtCoins = findViewById(R.id.txtCoins);
        txtToken = findViewById(R.id.txtToken);
        imgProfile = findViewById(R.id.imgProfile);

        // Main cards
        cardWatchEarn = findViewById(R.id.card_watch_earn);
        cardLuckyDraw = findViewById(R.id.card_lucky_draw);
        cardSpinner = findViewById(R.id.card_spinner);
        cardTasks = findViewById(R.id.card_tasks);
        cardSurveys = findViewById(R.id.card_surveys);
        cardInvite = findViewById(R.id.card_invite);

        // Footer Navigation
        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navProfile = findViewById(R.id.navProfile);
    }

    private void loadUserData() {

        // Load coins & tokens
        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(userPref.getToken());

        // Load Google profile image (from FirebaseAuth or Saved Pref)
        Glide.with(this)
                .load("https://lh3.googleusercontent.com/a/default_profile") // dynamic url later
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);
    }

    // ---------------------------------------------
    // 🚀 CARD CLICK ACTIONS
    // ---------------------------------------------
    private void setupCardClickActions() {

        // Watch & Earn
        cardWatchEarn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WatchActivity.class))
        );

        // Lucky Draw
        cardLuckyDraw.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LuckyDrawActivity.class))
        );

        // Spinner
        cardSpinner.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SpinnerActivity.class))
        );

        // Tasks & Offers
        cardTasks.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TaskActivity.class))
        );

        // Surveys
        cardSurveys.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SurveyActivity.class))
        );

        // Invite Friends
        cardInvite.setOnClickListener(v ->
                inviteFriend()
        );
    }

    // ---------------------------------------------
    // 📩 Invite Friend Intent
    // ---------------------------------------------
    private void inviteFriend() {

        String referralCode = userPref.getUid().substring(0, 6);

        String message =
                "🔥 Earn free coins! Download this app and use my referral code: "
                        + referralCode +
                        "\nDownload now: https://play.google.com/store/apps/details?id=com.example.rgamer";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    // ---------------------------------------------
    // 🚀 Bottom Navigation Click Actions
    // ---------------------------------------------
    private void setupBottomNav() {

        navHome.setOnClickListener(v -> {
            // Already in Home
        });

        navGame.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GameActivity.class))
        );

        navReward.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RewardActivity.class))
        );

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class))
        );
    }
}
