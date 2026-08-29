package com.app.rewardsplanet.Fragements;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.Activitys.MainActivity;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreakFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private LinearLayout contentLayout;

    private GridLayout shimmerGrid;
    private GridLayout streakContainer;

    private MaterialButton btnClaim;
    private ImageView btnBack;

    private ApiService apiService;

    private int currentStreak = 0;
    private boolean isClaimedToday = false;

    private OnBackPressedCallback backPressedCallback;


    // =========================================================
    // CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_fragment_streak,
                container,
                false
        );


        // =====================================================
        // FIND VIEWS
        // =====================================================

        shimmerContainer =
                view.findViewById(
                        R.id.shimmerContainer
                );

        contentLayout =
                view.findViewById(
                        R.id.contentLayout
                );

        shimmerGrid =
                view.findViewById(
                        R.id.shimmerGrid
                );

        streakContainer =
                view.findViewById(
                        R.id.streakContainer
                );

        btnClaim =
                view.findViewById(
                        R.id.btnClaim
                );

        btnBack =
                view.findViewById(
                        R.id.btnBack
                );


        // =====================================================
        // API
        // =====================================================

        apiService =
                ApiClient
                        .getClient()
                        .create(ApiService.class);


        // =====================================================
        // TOOLBAR BACK BUTTON
        // =====================================================

        btnBack.setOnClickListener(v -> {

            loadHomeFragment();

        });


        // =====================================================
        // ANDROID SYSTEM BACK
        // =====================================================

        setupBackPressed();


        // =====================================================
        // SHIMMER
        // =====================================================

        setupShimmerGrid();

        applyGreyShimmer();

        shimmerContainer.startShimmer();


        // =====================================================
        // LOAD STREAK
        // =====================================================

        loadStreakStatus();


        // =====================================================
        // CLAIM BUTTON
        // =====================================================

        btnClaim.setOnClickListener(
                v -> claimStreak()
        );


        return view;
    }


    // =========================================================
    // SYSTEM BACK
    // =========================================================

    private void setupBackPressed() {

        backPressedCallback =
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        loadHomeFragment();
                    }
                };


        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        backPressedCallback
                );
    }


    // =========================================================
    // LOAD HOME FRAGMENT
    // =========================================================

    private void loadHomeFragment() {

        MainActivity activity =
                (MainActivity) requireActivity();


        // Select HOME navigation
        activity.selectNav(
                activity.navHome
        );


        // Load HomeFragment
        activity.loadFragment(
                new HomeFragment()
        );
    }


    // =========================================================
    // SHIMMER GRID
    // =========================================================

    private void setupShimmerGrid() {

        if (shimmerGrid == null) {
            return;
        }


        shimmerGrid.removeAllViews();


        for (int i = 0; i < 7; i++) {

            View item =
                    LayoutInflater
                            .from(requireContext())
                            .inflate(
                                    R.layout.item_shimmer_streak,
                                    shimmerGrid,
                                    false
                            );


            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();


            params.width = 0;

            params.columnSpec =
                    GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                    );


            params.setMargins(
                    4,
                    4,
                    4,
                    4
            );


            item.setLayoutParams(params);

            shimmerGrid.addView(item);
        }
    }


    // =========================================================
    // GREY SHIMMER
    // =========================================================

    private void applyGreyShimmer() {

        Shimmer shimmer =
                new Shimmer.ColorHighlightBuilder()

                        .setBaseColor(
                                Color.parseColor(
                                        "#858E96"
                                )
                        )

                        .setHighlightColor(
                                Color.parseColor(
                                        "#E3E3E3"
                                )
                        )

                        .setDuration(1000)

                        .setDirection(
                                Shimmer.Direction.RIGHT_TO_LEFT
                        )

                        .setAutoStart(true)

                        .build();


        shimmerContainer.setShimmer(
                shimmer
        );
    }


    // =========================================================
    // LOAD STREAK STATUS
    // =========================================================

    private void loadStreakStatus() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (user == null) {

            showContent();

            return;
        }


        user.getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token =
                            result.getToken();


                    if (token == null) {

                        showContent();

                        return;
                    }


                    apiService
                            .getStreakStatus(token)
                            .enqueue(
                                    new Callback<ResponseBody>() {

                                        @Override
                                        public void onResponse(
                                                Call<ResponseBody> call,
                                                Response<ResponseBody> response) {

                                            if (response.isSuccessful()
                                                    && response.body() != null) {

                                                try {

                                                    String res =
                                                            response.body()
                                                                    .string();


                                                    JSONObject json =
                                                            new JSONObject(res);


                                                    currentStreak =
                                                            json.optInt(
                                                                    "streak",
                                                                    0
                                                            );


                                                    isClaimedToday =
                                                            json.optBoolean(
                                                                    "claimedToday",
                                                                    false
                                                            );


                                                } catch (Exception e) {

                                                    e.printStackTrace();
                                                }
                                            }


                                            showContent();
                                        }


                                        @Override
                                        public void onFailure(
                                                Call<ResponseBody> call,
                                                Throwable t) {

                                            Toast.makeText(
                                                    requireContext(),
                                                    "Error loading streak",
                                                    Toast.LENGTH_SHORT
                                            ).show();


                                            showContent();
                                        }
                                    }
                            );
                });
    }


    // =========================================================
    // SHOW CONTENT
    // =========================================================

    private void showContent() {

        if (!isAdded()) {
            return;
        }


        shimmerContainer.stopShimmer();

        shimmerContainer.setVisibility(
                View.GONE
        );


        contentLayout.setVisibility(
                View.VISIBLE
        );


        setupStreakUI();
    }


    // =========================================================
    // STREAK UI
    // =========================================================

    private void setupStreakUI() {

        if (streakContainer == null) {
            return;
        }


        streakContainer.removeAllViews();


        int[] rewards = {
                10,
                20,
                30,
                40,
                50,
                75,
                100
        };


        for (int i = 0; i < 7; i++) {

            View item =
                    LayoutInflater
                            .from(requireContext())
                            .inflate(
                                    R.layout.item_streak,
                                    streakContainer,
                                    false
                            );


            TextView day =
                    item.findViewById(
                            R.id.txtDay
                    );


            TextView reward =
                    item.findViewById(
                            R.id.txtReward
                    );


            ImageView fire =
                    item.findViewById(
                            R.id.imgFire
                    );


            day.setText(
                    "Day " + (i + 1)
            );


            reward.setText(
                    String.valueOf(
                            rewards[i]
                    )
            );


            // =================================================
            // COMPLETED DAYS
            // =================================================

            if (i < currentStreak - 1) {

                // COMPLETED
                day.setText("Completed");
                day.setTextColor(Color.parseColor("#000000"));

                fire.setImageResource(
                        R.drawable.ic_success
                );
                fire.setColorFilter(Color.GREEN);
                reward.setTextColor(Color.YELLOW);

                item.setAlpha(1f);

            }
            else if (i == currentStreak - 1) {

                // CURRENT DAY
                day.setText("Day " + (i + 1));
                day.setTextColor(Color.parseColor("#39FF14"));
                day.setText("Today");
                reward.setTextColor(Color.YELLOW);
                fire.setImageResource(
                        R.drawable.flame
                );




                if (isClaimedToday) {

                    fire.setColorFilter(
                            Color.GREEN
                    );
                    btnClaim.setEnabled(false);
                    btnClaim.setBackgroundColor(Color.GREEN);
                    btnClaim.setText(" Today Claimed");
                    btnClaim.setTextColor(Color.parseColor("#000000"));
                    fire.setImageResource(
                            R.drawable.ic_success
                    );

                } else {

                    fire.setColorFilter(
                            Color.parseColor("#FFA000")
                    );
                }

                item.setAlpha(1f);

            }
            else {

                // FUTURE DAY
                day.setText("Day " + (i + 1));
                day.setTextColor(Color.parseColor("#000000"));

                fire.setImageResource(
                        R.drawable.ic_strike
                );
                day.setTextColor(Color.parseColor("#000000"));

                fire.setColorFilter(Color.RED);
                reward.setTextColor(Color.YELLOW);

                item.setAlpha(1f);
            }

            // =================================================
            // LOCKED DAYS
            // =================================================




            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();


            params.width = 0;


            params.columnSpec =
                    GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                    );


            params.setMargins(
                    4,
                    4,
                    4,
                    4
            );


            item.setLayoutParams(
                    params
            );


            streakContainer.addView(
                    item
            );
        }


        btnClaim.setEnabled(
                !isClaimedToday
        );
    }


    // =========================================================
    // CLAIM STREAK
    // =========================================================

    private void claimStreak() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (user == null) {

            Toast.makeText(
                    requireContext(),
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        btnClaim.setEnabled(false);


        user.getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token =
                            result.getToken();


                    if (token == null) {

                        btnClaim.setEnabled(true);

                        return;
                    }


                    apiService
                            .claimStreak(token)
                            .enqueue(
                                    new Callback<ResponseBody>() {

                                        @Override
                                        public void onResponse(
                                                Call<ResponseBody> call,
                                                Response<ResponseBody> response) {

                                            if (response.isSuccessful()
                                                    && response.body() != null) {

                                                try {

                                                    JSONObject json =
                                                            new JSONObject(
                                                                    response.body()
                                                                            .string()
                                                            );


                                                    currentStreak =
                                                            json.optInt(
                                                                    "streak",
                                                                    currentStreak
                                                            );


                                                    isClaimedToday =
                                                            true;


                                                    setupStreakUI();


                                                    Toast.makeText(
                                                            requireContext(),
                                                            "Reward Claimed",
                                                            Toast.LENGTH_SHORT
                                                    ).show();


                                                } catch (Exception e) {

                                                    btnClaim.setEnabled(true);

                                                    e.printStackTrace();
                                                }

                                            } else {

                                                btnClaim.setEnabled(true);


                                                Toast.makeText(
                                                        requireContext(),
                                                        "Unable to claim reward",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            }
                                        }


                                        @Override
                                        public void onFailure(
                                                Call<ResponseBody> call,
                                                Throwable t) {

                                            btnClaim.setEnabled(true);


                                            Toast.makeText(
                                                    requireContext(),
                                                    "Network error",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );
                })
                .addOnFailureListener(e -> {

                    btnClaim.setEnabled(true);


                    Toast.makeText(
                            requireContext(),
                            "Authentication error",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    @Override
    public void onDestroyView() {

        if (shimmerContainer != null) {

            shimmerContainer.stopShimmer();
        }


        super.onDestroyView();
    }
}