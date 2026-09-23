package com.app.rewardsplanet.invite;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.profile.MyEarningsFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class activity_refer_earn extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View btnBack;
    private TextView txtCoins;
    private AdView adViewRefer;

    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refer_earn);
        makeFullScreen();

        userPref = new UserPref(this);
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        txtCoins = findViewById(R.id.txtCoins);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        adViewRefer = findViewById(R.id.adViewRefer);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (txtCoins != null && userPref != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }

        listenUserData();

        // Setup ViewPager2 with Adapter for smooth left/right drag gesture
        ReferPagerAdapter adapter = new ReferPagerAdapter(this);
        if (viewPager != null) {
            viewPager.setAdapter(adapter);

            // Connect TabLayout with ViewPager2
            if (tabLayout != null) {
                new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                    if (position == 0) {
                        tab.setText("INVITE FRIENDS");
                    } else {
                        tab.setText("MY EARNINGS");
                    }
                }).attach();
            }
        }

        if (adViewRefer != null) {
            try {
                AdRequest adRequest = new AdRequest.Builder().build();
                adViewRefer.loadAd(adRequest);
            } catch (Exception ignored) {}
        }
    }

    private static class ReferPagerAdapter extends FragmentStateAdapter {
        public ReferPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new layout_invite();
            } else {
                return new MyEarningsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    private void listenUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        userListener = db.collection("users")
                .document(uid)
                .addSnapshotListener(this, (snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Long coins = snapshot.getLong("coins");
                        if (coins != null && txtCoins != null) {
                            txtCoins.setText(String.valueOf(coins));
                            if (userPref != null) userPref.setCoins(coins);
                        }
                    }
                });
    }

    private void makeFullScreen() {
        Window window = getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onPause() {
        if (adViewRefer != null) {
            adViewRefer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewRefer != null) {
            adViewRefer.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adViewRefer != null) {
            adViewRefer.destroy();
        }
        if (userListener != null) {
            userListener.remove();
        }
        super.onDestroy();
    }
}
