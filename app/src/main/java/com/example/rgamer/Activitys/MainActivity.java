package com.example.rgamer.Activitys;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rgamer.Fragements.GameFragment;
import com.example.rgamer.Fragements.HomeFragment;
import com.example.rgamer.Fragements.RewardFragment;
import com.example.rgamer.Fragements.ProfileFragment;
import com.example.rgamer.R;
import com.example.rgamer.UserPref;

public class MainActivity extends AppCompatActivity {

    LinearLayout navHome, navGame, navReward, navProfile;
    ImageView imgNavProfile;
    UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();

        // Load profile image
        userPref = new UserPref(this);
        loadProfileImage();

        // Default fragment
        selectNav(navHome);
        loadFragment(new HomeFragment());
    }

    private void makeFullScreen() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
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

        imgNavProfile = findViewById(R.id.imgNavProfile); // ✅ important
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

    private void loadProfileImage() {
        String profileUrl = userPref.getProfileImage();

        Glide.with(this)
                .load(profileUrl == null || profileUrl.isEmpty()
                        ? R.drawable.ic_profile
                        : profileUrl)
                .circleCrop()
                .into(imgNavProfile);
    }

    private void selectNav(View selected) {
        resetNav();
        selected.setBackgroundResource(R.drawable.bg_nav_selected);
    }

    private void resetNav() {
        View[] navs = {navHome, navGame, navReward, navProfile};
        for (View nav : navs) {
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