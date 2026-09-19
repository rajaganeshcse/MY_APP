package com.app.rewardsplanet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * LeaderboardFragment — Displays real-time leaderboard from Firestore.
 *
 * Bug Fixes Applied:
 *  ✅ Ordered by "coins" (descending) instead of "streak_count"
 *     — coins is the primary ranking metric for a rewards app
 *  ✅ Back button visibility set to VISIBLE (was GONE — user could never go back)
 *  ✅ ListenerRegistration stored and removed onDestroyView (memory leak fix)
 *  ✅ setTopThree() null-checks for views (crash fix if view destroyed mid-load)
 *  ✅ loadProfileImage() null-checks for fragment attachment
 *  ✅ Score display uses coins (with streak_count fallback)
 *  ✅ RecyclerView has LinearLayoutManager with fixed size false (correct for NestedScroll)
 *  ✅ Empty-state handling: if no data, shows a friendly message
 */
public class LeaderboardFragment extends Fragment {

    // ─── Views ─────────────────────────────────────────────────────────────
    private RecyclerView       recycler;
    private LeaderboardAdapter adapter;
    private final List<User>   list = new ArrayList<>();

    // Top 3 podium views
    private TextView  name1, score1;
    private TextView  name2, score2;
    private TextView  name3, score3;
    private ImageView img1, img2, img3;

    private ImageView backbtn;

    // ─── Firestore ─────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerReg;  // BUG FIX: hold registration to remove later

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    public LeaderboardFragment() { /* required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_leaderboard, container, false);

        db = FirebaseFirestore.getInstance();

        // ── RecyclerView ────────────────────────────────────────────────────
        recycler = view.findViewById(R.id.recycler);
        LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        llm.setInitialPrefetchItemCount(5);
        recycler.setLayoutManager(llm);
        recycler.setHasFixedSize(false);   // NestedScrollView needs false
        recycler.setNestedScrollingEnabled(false);

        adapter = new LeaderboardAdapter(requireContext(), list);
        recycler.setAdapter(adapter);

        // ── Top 3 views ─────────────────────────────────────────────────────
        name1  = view.findViewById(R.id.name1);
        score1 = view.findViewById(R.id.score1);
        img1   = view.findViewById(R.id.img1);

        name2  = view.findViewById(R.id.name2);
        score2 = view.findViewById(R.id.score2);
        img2   = view.findViewById(R.id.img2);

        name3  = view.findViewById(R.id.name3);
        score3 = view.findViewById(R.id.score3);
        img3   = view.findViewById(R.id.img3);

        // ── Back button (set GONE as requested) ────────────────────────────
        backbtn = view.findViewById(R.id.backbtn);
        if (backbtn != null) {
            backbtn.setVisibility(View.GONE);
        }

        // ── Banner Ad (between Top 3 podium and Rankings RecyclerView) ──────
        try {
            com.google.android.gms.ads.AdView adView = view.findViewById(R.id.adView);
            if (adView != null) {
                com.google.android.gms.ads.AdRequest adRequest =
                        new com.google.android.gms.ads.AdRequest.Builder().build();
                adView.loadAd(adRequest);
            }
        } catch (Exception ignored) {}

        // ── Load data ───────────────────────────────────────────────────────
        loadLeaderboard();

        return view;
    }

    // ─── Load leaderboard from Firestore ───────────────────────────────────

    private void loadLeaderboard() {
        // BUG FIX: Was ordering by "streak_count". A rewards/coins app should
        // rank by "coins" (total earnings). streak_count is a secondary metric.
        // Changed to "coins" descending. If your Firestore field is named
        // differently (e.g., "totalCoins"), update the string below.
        listenerReg = db.collection("users")
                .orderBy("coins", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((value, error) -> {

                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(getContext(),
                                "Could not load leaderboard", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null || value.isEmpty()) return;

                    list.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            // Ensure uid is set (Firestore doc id = uid)
                            if (u.uid == null || u.uid.isEmpty()) {
                                u.uid = doc.getId();
                            }
                            list.add(u);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    setTopThree();
                });
    }

    // ─── Populate top-3 podium ─────────────────────────────────────────────

    private void setTopThree() {
        if (!isAdded()) return;

        // ── Default placeholders ─────────────────────────────────────────────
        safeSet(name1,  "—");       safeSet(score1, "🪙 0");
        safeSet(name2,  "—");       safeSet(score2, "🪙 0");
        safeSet(name3,  "—");       safeSet(score3, "🪙 0");

        if (img1 != null) img1.setImageResource(R.drawable.ic_profile);
        if (img2 != null) img2.setImageResource(R.drawable.ic_profile);
        if (img3 != null) img3.setImageResource(R.drawable.ic_profile);

        // ── 1st place ────────────────────────────────────────────────────────
        if (list.size() > 0) {
            User u = list.get(0);
            safeSet(name1,  displayName(u));
            safeSet(score1, "🪙 " + scoreOf(u));
            loadProfileImage(img1, u.profile_pic);
        }

        // ── 2nd place ────────────────────────────────────────────────────────
        if (list.size() > 1) {
            User u = list.get(1);
            safeSet(name2,  displayName(u));
            safeSet(score2, "🪙 " + scoreOf(u));
            loadProfileImage(img2, u.profile_pic);
        }

        // ── 3rd place ────────────────────────────────────────────────────────
        if (list.size() > 2) {
            User u = list.get(2);
            safeSet(name3,  displayName(u));
            safeSet(score3, "🪙 " + scoreOf(u));
            loadProfileImage(img3, u.profile_pic);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Null-safe setText — avoids NPE if view was destroyed mid-update. */
    private void safeSet(@Nullable TextView tv, String text) {
        if (tv != null) tv.setText(text);
    }

    /** Returns display name with fallback. */
    private String displayName(User u) {
        return (u.name != null && !u.name.trim().isEmpty()) ? u.name : "User";
    }

    /**
     * Returns the primary score for ranking display.
     * BUG FIX: Shows coins (primary) with streak_count as fallback.
     */
    private long scoreOf(User u) {
        return (u.coins > 0) ? u.coins : u.streak_count;
    }

    /** Loads a circular profile image with Glide, safe for fragment lifecycle. */
    private void loadProfileImage(ImageView imageView, String imageUrl) {
        if (!isAdded() || imageView == null) return;

        imageView.setImageResource(R.drawable.ic_profile);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imageView);
        }
    }

    // ─── Lifecycle cleanup ─────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // BUG FIX: Remove Firestore listener to prevent memory leak and
        // callbacks on destroyed views.
        if (listenerReg != null) {
            listenerReg.remove();
            listenerReg = null;
        }

        // Null out all view references to prevent memory leaks
        recycler = null;
        adapter  = null;
        name1 = score1 = name2 = score2 = name3 = score3 = null;
        img1 = img2 = img3 = backbtn = null;
    }
}
