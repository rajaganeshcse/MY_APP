package com.example.rgamer.Activitys;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    LinearLayout navHome, navGame, navReward, navProfile;
    ImageView imgNavProfile;
    UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeFullScreen();

        initViews();
        setupNavigation();
        userPref = new UserPref(this);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{
                    android.Manifest.permission.POST_NOTIFICATIONS


            }, 1);
        }
        // ✅ FIRST

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    String token = task.getResult();
                    Log.d("FCM_TOKEN", token);

                    String uid = userPref.getUid();

                    if (uid != null && !uid.isEmpty()) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("fcmToken", token);
                    }
                });

        // Load profile image
        userPref = new UserPref(this);
        loadProfileImage();

        // Default fragment
        selectNav(navHome);
        loadFragment(new HomeFragment());
    }

    private void makeFullScreen() {
        Window window = getWindow();

        // 🔥 Make content go behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

                // Optional: hide bars (remove if you only want transparent top)
                // controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 🔥 Make status bar transparent (TOP FIX)
        window.setStatusBarColor(Color.TRANSPARENT);

        // 🔥 Optional: make navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgNavProfile);

        }
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