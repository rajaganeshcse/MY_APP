package com.example.rgamer;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rgamer.GameFragment;
import com.example.rgamer.HomeFragment;
import com.example.rgamer.ProfileFragment;
import com.example.rgamer.RewardFragment;

public class MainActivity extends AppCompatActivity {

    UserPref userPref;

    TextView txtCoins;
    ImageView imgProfile, imgBottomProfile;

    LinearLayout navHome, navGame, navReward, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent status bar (safe)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        setContentView(R.layout.activity_main);

        userPref = new UserPref(this);

        initViews();
        loadUserData();
        setupBottomNavigation();

        // Load HomeFragment by default
        loadFragment(new HomeFragment());
    }

    private void initViews() {
        txtCoins = findViewById(R.id.txtCoins);
        imgProfile = findViewById(R.id.imgProfile);
        imgBottomProfile = findViewById(R.id.imgBottomProfile);

        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navProfile = findViewById(R.id.navProfile);
    }

    private void loadUserData() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));

        Glide.with(this)
                .load(userPref.getProfileImage())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);

        Glide.with(this)
                .load(userPref.getProfileImage())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgBottomProfile);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> loadFragment(new HomeFragment()));
        navGame.setOnClickListener(v -> loadFragment(new GameFragment()));
        navReward.setOnClickListener(v -> loadFragment(new RewardFragment()));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment()));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
