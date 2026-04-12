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
        makeFullScreen();
        setContentView(R.layout.activity_main);
        initViews();
        setupNavigation();

        // Default fragment
        selectNav(navHome);
        loadFragment(new HomeFragment());
    }
    private void makeFullScreen() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.parseColor("#ffffff"));
        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navProfile = findViewById(R.id.navProfile);
    }

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

    private void selectNav(LinearLayout selected) {
        resetNav();
        selected.setBackgroundResource(R.drawable.bg_nav_selected);
    }
    private void resetNav() {
        LinearLayout[] navs = {navHome, navGame, navReward, navProfile};
        for (LinearLayout nav : navs) {
            nav.setBackgroundResource(R.drawable.bg_nav_unselected);
        }
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
