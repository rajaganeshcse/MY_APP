package com.example.rgamer.lucky_draw;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.JoinResponse;
import com.example.rgamer.models.LuckyDrawModel;
import com.example.rgamer.network.ApiClient;
import com.example.rgamer.network.ApiService;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

import retrofit2.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    FirebaseFirestore db;
    ApiService api;
    TextView tickets;
    MaterialCardView cardLuckyDrawHistory;

    List<LuckyDrawModel> list = new ArrayList<>();
    LuckyDrawAdapter adapter;

    String uid;

    int userTickets = 0;

    private RewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        tickets = findViewById(R.id.tickets);

        cardLuckyDrawHistory = findViewById(R.id.cardLuckyDrawHistory);
        cardLuckyDrawHistory.setOnClickListener(v ->
                startActivity(new Intent(this, activity_lucky_draw_winner.class))
        );

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        api = ApiClient.getClient().create(ApiService.class);
        uid = FirebaseAuth.getInstance().getUid();

        RecyclerView rv = findViewById(R.id.luckyDrawRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawAdapter(list, this, userTickets);
        rv.setAdapter(adapter);

        MobileAds.initialize(this);
        loadAd();

        loadDraws();
        makeFullScreen();

        // ✅ SINGLE Firestore listener (lifecycle aware)
        db.collection("users")
                .document(uid)
                .addSnapshotListener(this, (snap, e) -> {

                    if (e != null) {
                        Log.e("FIRESTORE", "Listen failed", e);
                        return;
                    }

                    if (snap != null && snap.exists()) {
                        Long t = snap.getLong("tickets");
                        userTickets = t == null ? 0 : t.intValue();

                        adapter.updateUserTickets(userTickets);
                        tickets.setText(String.valueOf(userTickets));
                    }
                });
    }

    private void makeFullScreen() {
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    /* ================= LOAD DRAWS ================= */

    private void loadDraws() {

        db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null) return;

                    list.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        LuckyDrawModel m = d.toObject(LuckyDrawModel.class);
                        if (m == null) continue;

                        m.setId(d.getId());

                        checkIfJoined(m);
                        list.add(m);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    /* ================= CHECK FREE ENTRY USED ================= */

    private void checkIfJoined(LuckyDrawModel model) {

        db.collection("lucky_draw_tickets")
                .document(model.getId())
                .collection("tickets")
                .whereEqualTo("uid", uid)
                .whereEqualTo("type", "AD")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        model.setJoinedByMe(true);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /* ================= LOAD AD ================= */

    private void loadAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this,
                "ca-app-pub-3940256099942544/5224354917",
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d("AD", "Loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        Log.e("AD", "Failed: " + error.getMessage());
                    }
                });
    }

    /* ================= CLICK EVENTS ================= */

    @Override
    public void onJoin(LuckyDrawModel model) {
        showAdThenJoin(model);
    }

    @Override
    public void onJoinWithTickets(LuckyDrawModel model) {
        join(model.getId(), "TICKET");
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        Toast.makeText(this, "Winner screen coming soon", Toast.LENGTH_SHORT).show();
    }

    /* ================= AD FLOW ================= */

    private void showAdThenJoin(LuckyDrawModel model) {

        if (rewardedAd != null) {

            adapter.setLoading(model.getId(), true);

            rewardedAd.show(this, rewardItem -> {

                Toast.makeText(this, "Ad watched 🎉", Toast.LENGTH_SHORT).show();

                join(model.getId(), "AD");
                loadAd();
            });

        } else {

            Toast.makeText(this, "Ad not ready, try again", Toast.LENGTH_SHORT).show();
            loadAd();
        }
    }

    /* ================= JOIN API ================= */

    private void join(String drawId, String type) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("AD".equals(type)) {
            adapter.setLoading(drawId, true);
        }

        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token = result.getToken();

                    Map<String, Object> body = new HashMap<>();
                    body.put("drawId", drawId);
                    body.put("type", type);

                    api.joinDraw("Bearer " + token, body)
                            .enqueue(new Callback<JoinResponse>() {

                                @Override
                                public void onResponse(Call<JoinResponse> call,
                                                       Response<JoinResponse> response) {

                                    adapter.clearLoading(drawId);

                                    if (response.isSuccessful() && response.body() != null) {

                                        Toast.makeText(
                                                activity_lucky_draw.this,
                                                response.body().message,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        if ("TICKET".equals(type)) {
                                            userTickets--;
                                            adapter.updateUserTickets(userTickets);
                                            tickets.setText(String.valueOf(userTickets));
                                        }

                                    } else {
                                        Toast.makeText(
                                                activity_lucky_draw.this,
                                                "Join failed",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JoinResponse> call, Throwable t) {

                                    adapter.clearLoading(drawId);

                                    Toast.makeText(
                                            activity_lucky_draw.this,
                                            t.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                });
    }
}