package com.app.rewardsplanet.invite;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.profile.MyEarningsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class activity_refer_earn extends AppCompatActivity {

    private TabLayout tabLayout;
    private View btnBack;
    private TextView txtCoins;

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

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (txtCoins != null && userPref != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }

        listenUserData();

        // Default tab
        loadFragment(new layout_invite());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadFragment(new layout_invite());
                } else {
                    loadFragment(new MyEarningsFragment());
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void makeFullScreen() {
        Window window = getWindow();
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
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
