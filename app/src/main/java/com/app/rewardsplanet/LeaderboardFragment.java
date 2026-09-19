package com.app.rewardsplanet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private RecyclerView recycler;
    private LeaderboardAdapter adapter;

    private final List<User> list =
            new ArrayList<>();

    private FirebaseFirestore db;


    // =========================================================
    // TOP 3
    // =========================================================

    private TextView name1;
    private TextView score1;

    private TextView name2;
    private TextView score2;

    private TextView name3;
    private TextView score3;

    private ImageView img1;
    private ImageView img2;
    private ImageView img3;

    private ImageView backbtn;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public LeaderboardFragment() {
        // Required empty constructor
    }


    // =========================================================
    // ON CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_leaderboard,
                container,
                false
        );


        // =====================================================
        // FIRESTORE
        // =====================================================

        db = FirebaseFirestore.getInstance();


        // =====================================================
        // RECYCLER VIEW
        // =====================================================

        recycler =
                view.findViewById(
                        R.id.recycler
                );


        recycler.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );


        adapter =
                new LeaderboardAdapter(
                        requireContext(),
                        list
                );


        recycler.setAdapter(adapter);


        // =====================================================
        // TOP 1
        // =====================================================

        name1 =
                view.findViewById(
                        R.id.name1
                );

        score1 =
                view.findViewById(
                        R.id.score1
                );

        img1 =
                view.findViewById(
                        R.id.img1
                );


        // =====================================================
        // TOP 2
        // =====================================================

        name2 =
                view.findViewById(
                        R.id.name2
                );

        score2 =
                view.findViewById(
                        R.id.score2
                );

        img2 =
                view.findViewById(
                        R.id.img2
                );


        // =====================================================
        // TOP 3
        // =====================================================

        name3 =
                view.findViewById(
                        R.id.name3
                );

        score3 =
                view.findViewById(
                        R.id.score3
                );

        img3 =
                view.findViewById(
                        R.id.img3
                );


        // =====================================================
        // BACK BUTTON
        // =====================================================

        backbtn =
                view.findViewById(
                        R.id.backbtn
                );


        if (backbtn != null) {

            backbtn.setOnClickListener(v -> {

                requireActivity()
                        .onBackPressed();
            });
        }


        // =====================================================
        // LOAD LEADERBOARD
        // =====================================================

        loadLeaderboard();


        return view;
    }


    // =========================================================
    // LOAD LEADERBOARD
    // =========================================================

    private void loadLeaderboard() {

        db.collection("users")

                .orderBy(
                        "streak_count",
                        Query.Direction.DESCENDING
                )

                .limit(100)

                .addSnapshotListener(
                        (value, error) -> {

                            if (!isAdded()) {
                                return;
                            }


                            if (error != null ||
                                    value == null) {

                                return;
                            }


                            // Clear old data
                            list.clear();


                            // Add users
                            for (DocumentSnapshot doc :
                                    value.getDocuments()) {

                                User u =
                                        doc.toObject(
                                                User.class
                                        );


                                if (u != null) {

                                    list.add(u);
                                }
                            }


                            // Update RecyclerView
                            adapter.notifyDataSetChanged();


                            // Update Top 3
                            setTopThree();
                        }
                );
    }


    // =========================================================
    // SET TOP THREE
    // =========================================================

    private void setTopThree() {

        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        name1.setText("User");
        score1.setText("🔥 0");
        img1.setImageResource(
                R.drawable.ic_profile
        );


        name2.setText("User");
        score2.setText("🔥 0");
        img2.setImageResource(
                R.drawable.ic_profile
        );


        name3.setText("User");
        score3.setText("🔥 0");
        img3.setImageResource(
                R.drawable.ic_profile
        );


        // =====================================================
        // 1ST PLACE
        // =====================================================

        if (list.size() > 0) {

            User u =
                    list.get(0);


            if (u.name != null &&
                    !u.name.trim().isEmpty()) {

                name1.setText(
                        u.name
                );
            }


            score1.setText(
                    "🔥 " + u.streak_count
            );


            loadProfileImage(
                    img1,
                    u.profile_pic
            );
        }


        // =====================================================
        // 2ND PLACE
        // =====================================================

        if (list.size() > 1) {

            User u =
                    list.get(1);


            if (u.name != null &&
                    !u.name.trim().isEmpty()) {

                name2.setText(
                        u.name
                );
            }


            score2.setText(
                    "🔥 " + u.streak_count
            );


            loadProfileImage(
                    img2,
                    u.profile_pic
            );
        }


        // =====================================================
        // 3RD PLACE
        // =====================================================

        if (list.size() > 2) {

            User u =
                    list.get(2);


            if (u.name != null &&
                    !u.name.trim().isEmpty()) {

                name3.setText(
                        u.name
                );
            }


            score3.setText(
                    "🔥 " + u.streak_count
            );


            loadProfileImage(
                    img3,
                    u.profile_pic
            );
        }
    }


    // =========================================================
    // PROFILE IMAGE
    // =========================================================

    private void loadProfileImage(
            ImageView imageView,
            String imageUrl) {

        if (!isAdded()) {
            return;
        }


        // Always start with default
        imageView.setImageResource(
                R.drawable.ic_profile
        );


        if (imageUrl != null &&
                !imageUrl.trim().isEmpty()) {


            Glide.with(this)

                    .load(imageUrl)

                    .placeholder(
                            R.drawable.ic_profile
                    )

                    .error(
                            R.drawable.ic_profile
                    )

                    .circleCrop()

                    .into(imageView);
        }
    }


    // =========================================================
    // ON DESTROY VIEW
    // =========================================================

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        recycler = null;
        adapter = null;

        name1 = null;
        score1 = null;

        name2 = null;
        score2 = null;

        name3 = null;
        score3 = null;

        img1 = null;
        img2 = null;
        img3 = null;

        backbtn = null;
    }
}

