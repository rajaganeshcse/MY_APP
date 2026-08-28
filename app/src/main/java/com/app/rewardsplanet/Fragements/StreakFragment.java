package com.app.rewardsplanet.Fragements;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import retrofit2.*;

public class StreakFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private LinearLayout contentLayout;
    private GridLayout shimmerGrid, streakContainer;
    private MaterialButton btnClaim;
    private ImageView btnBack;

    private ApiService apiService;

    private int currentStreak = 0;
    private boolean isClaimedToday = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fragment_streak, container, false);

        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        contentLayout = view.findViewById(R.id.contentLayout);
        shimmerGrid = view.findViewById(R.id.shimmerGrid);

        streakContainer = view.findViewById(R.id.streakContainer);
        btnClaim = view.findViewById(R.id.btnClaim);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupShimmerGrid();
        applyGreyShimmer();
        shimmerContainer.startShimmer();

        loadStreakStatus();

        btnClaim.setOnClickListener(v -> claimStreak());

        return view;
    }

    // 🔥 create shimmer cells
    private void setupShimmerGrid() {
        for(int i=0; i<6; i++){
            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_shimmer_streak, shimmerGrid, false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8,8,8,8);

            item.setLayoutParams(params);
            shimmerGrid.addView(item);
        }
    }

    // 🔥 GOLD SHIMMER
    private void applyGreyShimmer() {

        Shimmer shimmer = new Shimmer.ColorHighlightBuilder()
                .setBaseColor(Color.parseColor("#858e96"))     // base grey
                .setHighlightColor(Color.parseColor("#e3e3e3")) // light grey shine
                .setDuration(3000)
                .setDirection(Shimmer.Direction.TOP_TO_BOTTOM)
                .setAutoStart(true)
                .build();

        shimmerContainer.setShimmer(shimmer);
    }

    // 🔥 LOAD DATA
    private void loadStreakStatus() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {

            apiService.getStreakStatus(result.getToken())
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {

                            if(response.isSuccessful()){
                                try {
                                    String res = response.body().string();
                                    JSONObject json = new JSONObject(res);

                                    currentStreak = json.getInt("streak");
                                    isClaimedToday = json.getBoolean("claimedToday");

                                    showContent();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(),"Error",Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // 🔥 SWITCH UI
    private void showContent() {
        shimmerContainer.stopShimmer();
        shimmerContainer.setVisibility(View.GONE);

        contentLayout.setVisibility(View.VISIBLE);

        setupStreakUI();
    }

    // 🔥 REAL GRID
    private void setupStreakUI() {

        int[] rewards = {10,20,30,40,50,75,100};

        for(int i=0;i<7;i++){

            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_streak, streakContainer, false);

            TextView day = item.findViewById(R.id.txtDay);
            TextView reward = item.findViewById(R.id.txtReward);
            ImageView fire = item.findViewById(R.id.imgFire);

            day.setText("Day " + (i+1));
            reward.setText(String.valueOf(rewards[i]));

            if(i < currentStreak - 1){
                fire.setColorFilter(Color.parseColor("#FF6F00"));
            } else if(i == currentStreak - 1){
                fire.setColorFilter(isClaimedToday ? Color.parseColor("#FF6F00")
                        : Color.parseColor("#FFA000"));
            } else {
                fire.setColorFilter(Color.GRAY);
                item.setAlpha(0.5f);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8,8,8,8);

            item.setLayoutParams(params);
            streakContainer.addView(item);
        }

        btnClaim.setEnabled(!isClaimedToday);
    }

    // 🔥 CLAIM
    private void claimStreak() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {

            apiService.claimStreak(result.getToken())
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {

                            if(response.isSuccessful()){
                                try {
                                    JSONObject json = new JSONObject(response.body().string());

                                    currentStreak = json.getInt("streak");
                                    isClaimedToday = true;

                                    streakContainer.removeAllViews();
                                    setupStreakUI();

                                    Toast.makeText(getContext(),"Reward Claimed",Toast.LENGTH_SHORT).show();

                                } catch (Exception e) {}
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {}
                    });
        });
    }
}