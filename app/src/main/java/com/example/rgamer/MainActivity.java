package com.example.rgamer;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    LinearLayout navHome, navGame, navReward, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeFullScreen(); // ✅ SAFE fullscreen
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();

        // Default tab
        selectNav(navHome);
        loadFragment(new HomeFragment());
    }

    // ================= SAFE FULL SCREEN =================
    // ✔ Transparent status bar
    // ✔ Navigation bar visible
    // ✔ No touch / auto-close issues
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

    // ================= INIT =================
    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navProfile = findViewById(R.id.navProfile);
    }

    // ================= NAVIGATION =================
    private void setupNavigation() {

        navHome.setOnClickListener(v -> {
            selectNav(navHome);
            loadFragment(new HomeFragment());
        });

        navGame.setOnClickListener(v -> {
            selectNav(navGame);
            loadFragment(new GameFragment());
        });

        navReward.setOnClickListener(v -> {
            selectNav(navReward);
            loadFragment(new RewardFragment());
        });

        navProfile.setOnClickListener(v -> {
            selectNav(navProfile);
            loadFragment(new ProfileFragment());
        });
    }

    // ================= FOOTER EFFECT =================
    private void selectNav(LinearLayout selected) {
        resetNav();

        selected.setBackgroundResource(R.drawable.bg_nav_selected);
        selected.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(150)
                .start();
    }

    private void resetNav() {
        LinearLayout[] navs = {navHome, navGame, navReward, navProfile};

        for (LinearLayout nav : navs) {
            nav.setBackgroundResource(R.drawable.bg_nav_unselected);
            nav.setScaleX(1f);
            nav.setScaleY(1f);
        }
    }

    // ================= FRAGMENT LOAD =================
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
