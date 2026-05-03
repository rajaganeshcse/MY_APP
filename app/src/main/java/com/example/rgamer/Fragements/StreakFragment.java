package com.example.rgamer.Fragements;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rgamer.R;
import com.example.rgamer.network.ApiClient;
import com.example.rgamer.network.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.*;

public class StreakFragment extends Fragment {

    private GridLayout streakContainer;
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

        streakContainer = view.findViewById(R.id.streakContainer);
        btnClaim = view.findViewById(R.id.btnClaim);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // 🔥 SHOW SHIMMER FIRST
        showShimmer();

        // 🔥 LOAD DATA
        loadStreakStatus();

        btnClaim.setOnClickListener(v -> claimStreak());

        return view;
    }

    // 🔥 SHIMMER UI
    private void showShimmer() {

        streakContainer.removeAllViews();

        for(int i = 0; i < 6; i++) {

            View shimmerItem = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_shimmer_streak, streakContainer, false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8,8,8,8);

            shimmerItem.setLayoutParams(params);

            streakContainer.addView(shimmerItem);
        }
    }

    // 🔥 LOAD STATUS
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

                                    streakContainer.removeAllViews();
                                    setupStreakUI();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(),"Error loading",Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // 🔥 REAL UI
    private void setupStreakUI() {

        int[] rewards = {10,20,30,40,50,75,100};

        for(int i=0; i<7; i++){

            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_streak, streakContainer, false);

            TextView day = item.findViewById(R.id.txtDay);
            TextView reward = item.findViewById(R.id.txtReward);
            ImageView fire = item.findViewById(R.id.imgFire);

            day.setText("Day " + (i+1));
            reward.setText(String.valueOf(rewards[i]));

            if(i < currentStreak - 1){
                fire.setColorFilter(Color.parseColor("#FF6F00"));
            }
            else if(i == currentStreak - 1){

                item.setBackgroundResource(R.drawable.bg_streak_active);

                if(isClaimedToday){
                    fire.setColorFilter(Color.parseColor("#FF6F00"));
                } else {
                    fire.setColorFilter(Color.parseColor("#FFA000"));
                }
            }
            else {
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
                                    String res = response.body().string();
                                    JSONObject json = new JSONObject(res);

                                    currentStreak = json.getInt("streak");
                                    isClaimedToday = true;

                                    streakContainer.removeAllViews();
                                    setupStreakUI();

                                    Toast.makeText(getContext(),"🔥 Reward Claimed!",Toast.LENGTH_SHORT).show();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Toast.makeText(getContext(),"Already claimed",Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(),"Server error",Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}