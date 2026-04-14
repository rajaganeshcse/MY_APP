package com.example.rgamer.lucky_draw;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    /* ================= FIREBASE ================= */

    FirebaseFirestore db;
    String uid;

    /* ================= UI ================= */

    RecyclerView recyclerView;
    LuckyDrawAdapter adapter;
    List<LuckyDrawModel> list = new ArrayList<>();

    /* ================= LISTENERS ================= */

    ListenerRegistration drawListener;
    ListenerRegistration joinedListener;

    Set<String> joinedDrawIds = new HashSet<>();

    /* ================= REWARDED AD ================= */

    private RewardedAd rewardedAd;
    private boolean isAdLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(com.example.rgamer.R.layout.activity_lucky_draw);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        /* ---------- BACK ---------- */
        findViewById(com.example.rgamer.R.id.btnBack).setOnClickListener(v -> finish());

        /* ---------- HISTORY CARD ---------- */
        if (findViewById(com.example.rgamer.R.id.cardLuckyDrawHistory) != null) {
            findViewById(com.example.rgamer.R.id.cardLuckyDrawHistory)
                    .setOnClickListener(v ->
                            startActivity(new Intent(
                                    activity_lucky_draw.this,
                                    activity_lucky_draw_winner.class
                            ))
                    );
        }

        /* ---------- RECYCLER ---------- */
        recyclerView = findViewById(R.id.luckyDrawRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawAdapter(list, this);
        recyclerView.setAdapter(adapter);

        /* ---------- LOAD AD ---------- */
        loadRewardedAd();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenJoinedDraws();
        loadLuckyDraws();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (drawListener != null) drawListener.remove();
        if (joinedListener != null) joinedListener.remove();
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


    /* ================= JOINED DRAWS ================= */

    private void listenJoinedDraws() {

        if (uid == null) return;

        joinedListener = db.collection("users")
                .document(uid)
                .collection("joinedLuckyDraws")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || e != null) return;

                    joinedDrawIds.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        joinedDrawIds.add(d.getId());
                    }
                    applyJoinedState();
                });
    }

    /* ================= READ DRAWS ================= */

    private void loadLuckyDraws() {

        drawListener = db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || e != null) return;

                    list.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        LuckyDrawModel model =
                                doc.toObject(LuckyDrawModel.class);

                        if (model == null) continue;

                        model.setId(doc.getId());
                        model.setJoinedByMe(
                                joinedDrawIds.contains(doc.getId())
                        );

                        list.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void applyJoinedState() {
        for (LuckyDrawModel m : list) {
            m.setJoinedByMe(joinedDrawIds.contains(m.getId()));
        }
        adapter.notifyDataSetChanged();
    }

    /* ================= ADAPTER CALLBACKS ================= */

    @Override
    public void onJoin(LuckyDrawModel model) {
        joinDraw(model.getId());
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        startActivity(new Intent(
                activity_lucky_draw.this,
                activity_lucky_draw_winner.class
        ));
    }

    /* ================= SAFE JOIN ================= */

    private void joinDraw(String drawId) {

        if (uid == null) return;

        DocumentReference drawRef =
                db.collection("lucky_draws").document(drawId);

        DocumentReference entryRef =
                db.collection("lucky_draw_entries")
                        .document(drawId)
                        .collection("users")
                        .document(uid);

        DocumentReference userJoinRef =
                db.collection("users")
                        .document(uid)
                        .collection("joinedLuckyDraws")
                        .document(drawId);

        db.runTransaction(transaction -> {

            DocumentSnapshot drawSnap = transaction.get(drawRef);

            long filled = drawSnap.getLong("filledSlots");
            long total = drawSnap.getLong("totalSlots");
            String status = drawSnap.getString("status");

            if (!"OPEN".equals(status))
                throw new RuntimeException("Draw closed");

            if (filled >= total)
                throw new RuntimeException("Draw full");

            if (transaction.get(entryRef).exists())
                throw new RuntimeException("Already joined");

            Map<String, Object> data = new HashMap<>();
            data.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(entryRef, data);
            transaction.set(userJoinRef, data);
            transaction.update(drawRef,
                    "filledSlots", FieldValue.increment(1));

            return null;

        }).addOnSuccessListener(v -> {
            Toast.makeText(this,
                    "Joined Lucky Draw",
                    Toast.LENGTH_SHORT).show();
            showRewardedAd();
        }).addOnFailureListener(e ->
                Toast.makeText(this,
                        e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    /* ================= REWARDED AD ================= */

    private void loadRewardedAd() {

        if (isAdLoading || rewardedAd != null) return;
        isAdLoading = true;

        RewardedAd.load(
                this,
                "ca-app-pub-3940256099942544/5224354917",
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isAdLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        isAdLoading = false;
                    }
                }
        );
    }

    private void showRewardedAd() {

        if (rewardedAd == null) {
            loadRewardedAd();
            return;
        }

        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(
                            AdError adError) {
                        rewardedAd = null;
                        loadRewardedAd();
                    }
                }
        );

        rewardedAd.show(this, rewardItem -> giveFreeReward());
    }

    /* ================= FREE REWARD ================= */

    private void giveFreeReward() {

        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .update("coins", FieldValue.increment(0));

        Toast.makeText(
                this,
                "🎉 You earned free coins!",
                Toast.LENGTH_SHORT
        ).show();
    }
}
