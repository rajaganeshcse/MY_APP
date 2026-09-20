package com.app.rewardsplanet.Activitys;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.Fragements.GameFragment;
import com.app.rewardsplanet.Fragements.HomeFragment;
import com.app.rewardsplanet.Fragements.RewardFragment;
import com.app.rewardsplanet.LeaderboardFragment;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;

import com.app.rewardsplanet.invite.activity_refer_earn;
import com.app.rewardsplanet.withdraws.TransactionHistoryActivity;

import com.bumptech.glide.Glide;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;


public class MainActivity extends AppCompatActivity {

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    public LinearLayout navHome;
    private LinearLayout navGame;
    public LinearLayout navReward;
    private LinearLayout navLeaderboard;

    private LinearLayout bottomNav;

    private LinearLayout navPillHome;
    private LinearLayout navPillGame;
    private LinearLayout navPillReward;
    private LinearLayout navPillLeaderboard;

    private TextView txtNavHome;
    private TextView txtNavGame;
    private TextView txtNavReward;
    private TextView txtNavLeaderboard;

    private int activeNavPosition = -1;


    // =========================================================
    // NAV IMAGES
    // =========================================================

    private ImageView imgNavHome;
    private ImageView imgNavGame;
    private ImageView imgNavReward;
    private ImageView imgNavLeaderboard;



    // =========================================================
    // USER PREFERENCE
    // =========================================================

    private UserPref userPref;


    // =========================================================
    // DRAWER
    // =========================================================

    private DrawerLayout drawerLayout;


    // =========================================================
    // DRAWER USER DETAILS
    // =========================================================

    private TextView txtName;
    private TextView txtEmail;
    private TextView txtUid;
    private TextView txtCoins;
    private TextView txtTickets;

    private ImageView imgProfile;


    // =========================================================
    // DRAWER MENU ITEMS
    // =========================================================

    private LinearLayout menuWallet;
    private LinearLayout menuLeaderboard;
    private LinearLayout menuActivity;
    private LinearLayout menuRefer;

    private ImageView btnCopy;

    ImageView btnEditProfile;

    private LinearLayout menuRate;
    private LinearLayout menuFeedback;
    private LinearLayout menuContact;
    private LinearLayout menuFaq;
    private LinearLayout menuPrivacy;

    private LinearLayout btnLogout1;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        makeFullScreen();

        userPref = new UserPref(this);

        initViews();

        initDrawer();

        setupNavigation();


        // =====================================================
        // NOTIFICATION PERMISSION
        // =====================================================

        if (Build.VERSION.SDK_INT >= 33) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    1
            );
        }


        // =====================================================
        // FIREBASE FCM TOKEN
        // =====================================================

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        Log.e(
                                "FCM_TOKEN",
                                "Failed to get FCM token"
                        );

                        return;
                    }

                    String token =
                            task.getResult();

                    Log.d(
                            "FCM_TOKEN",
                            token
                    );

                    String uid =
                            userPref.getUid();

                    if (uid != null &&
                            !uid.isEmpty()) {

                        FirebaseFirestore
                                .getInstance()
                                .collection("users")
                                .document(uid)
                                .update(
                                        "fcmToken",
                                        token
                                )
                                .addOnFailureListener(e ->
                                        Log.e(
                                                "FCM_TOKEN",
                                                "Failed to update token",
                                                e
                                        )
                                );
                    }
                });


        // =====================================================
        // USER DATA
        // =====================================================

        loadUserFromPref();


        // =====================================================
        // DEFAULT FRAGMENT
        // =====================================================

        selectNav(navHome);

        loadFragment(
                new HomeFragment()
        );

        // Check if redirected from ProfileActivity after saving changes
        checkProfileSuccessDialog(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkProfileSuccessDialog(intent);
    }

    private void checkProfileSuccessDialog(Intent intent) {
        if (intent != null && intent.getBooleanExtra("SHOW_PROFILE_SUCCESS_DIALOG", false)) {
            intent.putExtra("SHOW_PROFILE_SUCCESS_DIALOG", false);
            showProfileSavedSuccessDialog();
        }
    }

    private void showProfileSavedSuccessDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        View view = getLayoutInflater().inflate(R.layout.dialog_profile_success, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.86f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        com.app.rewardsplanet.utils.SuccessAnimationHelper.animate(dialog);

        View btnOk = view.findViewById(R.id.btnProfileSuccessOk);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }


    // =========================================================
    // INITIALIZE MAIN VIEWS
    // =========================================================

    private void initViews() {

        // =====================================================
        // NAV CONTAINERS
        // =====================================================

        bottomNav = findViewById(R.id.bottomNav);

        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navLeaderboard = findViewById(R.id.navLeaderboard);

        navPillHome = findViewById(R.id.navPillHome);
        navPillGame = findViewById(R.id.navPillGame);
        navPillReward = findViewById(R.id.navPillReward);
        navPillLeaderboard = findViewById(R.id.navPillLeaderboard);

        imgNavHome = findViewById(R.id.imgNavHome);
        imgNavGame = findViewById(R.id.imgNavGame);
        imgNavReward = findViewById(R.id.imgNavReward);
        imgNavLeaderboard = findViewById(R.id.imgNavLeaderboard);

        txtNavHome = findViewById(R.id.txtNavHome);
        txtNavGame = findViewById(R.id.txtNavGame);
        txtNavReward = findViewById(R.id.txtNavReward);
        txtNavLeaderboard = findViewById(R.id.txtNavLeaderboard);
    }


    // =========================================================
    // INITIALIZE DRAWER
    // =========================================================

    private void initDrawer() {

        drawerLayout =
                findViewById(
                        R.id.drawerLayout
                );


        // =====================================================
        // DRAWER USER DETAILS
        // =====================================================

        txtName =
                findViewById(
                        R.id.txtDrawerName
                );

        txtEmail =
                findViewById(
                        R.id.txtDrawerEmail
                );

        txtUid =
                findViewById(
                        R.id.txtDrawerPhone
                );

        txtCoins =
                findViewById(
                        R.id.txtCoins
                );

        txtTickets =
                findViewById(
                        R.id.txtTickets
                );

        imgProfile =
                findViewById(
                        R.id.profileImage
                );


        // =====================================================
        // DRAWER MENU
        // =====================================================

        btnEditProfile =
                findViewById(
                        R.id.btnEditProfile
                );

        menuWallet =
                findViewById(
                        R.id.menuWallet
                );

        menuLeaderboard =
                findViewById(
                        R.id.menuLeaderboard
                );

        menuActivity =
                findViewById(
                        R.id.menuActivity
                );

        menuRefer =
                findViewById(
                        R.id.menuRefer
                );

        btnCopy =
                findViewById(
                        R.id.btnCopy
                );

        menuRate =
                findViewById(
                        R.id.menuRate
                );

        menuFeedback =
                findViewById(
                        R.id.menuFeedback
                );

        menuContact =
                findViewById(
                        R.id.menuContact
                );

        menuFaq =
                findViewById(
                        R.id.menuFaq
                );

        menuPrivacy =
                findViewById(
                        R.id.menuPrivacy
                );

        btnLogout1 =
                findViewById(
                        R.id.btnLogout1
                );


        // =====================================================
        // ACCOUNT HISTORY (FORMERLY WALLET)
        // =====================================================

        if (menuWallet != null) {

            menuWallet.setOnClickListener(v -> {

                closeDrawer();

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                TransactionHistoryActivity.class
                        );

                startActivity(intent);
            });
        }

        // =====================================================
        // LEADERBOARD
        // =====================================================

        if (menuLeaderboard != null) {

            menuLeaderboard.setOnClickListener(v -> {

                closeDrawer();

                selectNavTab(3);

                loadFragment(
                        new LeaderboardFragment()
                );
            });
        }


        // =====================================================
        // EDIT PROFILE
        // =====================================================

        if (btnEditProfile != null) {

            btnEditProfile.setOnClickListener(v -> {

                closeDrawer();

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                ProfileActivity.class
                        );

                startActivity(intent);
            });
        }


        // =====================================================
        // COPY REFERRAL CODE
        // =====================================================

        if (btnCopy != null) {

            btnCopy.setOnClickListener(v ->
                    copyReferralCode()
            );
        }


        // =====================================================
        // MY ACTIVITY
        // =====================================================

        if (menuActivity != null) {

            menuActivity.setOnClickListener(v -> {

                closeDrawer();

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                TransactionHistoryActivity.class
                        );

                startActivity(intent);
            });
        }


        // =====================================================
        // REFER AND EARN
        // =====================================================

        if (menuRefer != null) {

            menuRefer.setOnClickListener(v -> {

                closeDrawer();

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                activity_refer_earn.class
                        );

                startActivity(intent);
            });
        }


        // =====================================================
        // RATE US
        // =====================================================

        if (menuRate != null) {

            menuRate.setOnClickListener(v -> {

                closeDrawer();

                openUrl(
                        "https://yourwebsite.com/rate-us"
                );
            });
        }


        // =====================================================
        // FEEDBACK
        // =====================================================

        if (menuFeedback != null) {

            menuFeedback.setOnClickListener(v -> {

                closeDrawer();

                openUrl(
                        "https://yourwebsite.com/feedback"
                );
            });
        }


        // =====================================================
        // CONTACT US
        // =====================================================

        if (menuContact != null) {

            menuContact.setOnClickListener(v -> {

                closeDrawer();

                openUrl(
                        "https://yourwebsite.com/contact"
                );
            });
        }


        // =====================================================
        // FAQ
        // =====================================================

        if (menuFaq != null) {

            menuFaq.setOnClickListener(v -> {

                closeDrawer();

                openUrl(
                        "https://yourwebsite.com/faq"
                );
            });
        }


        // =====================================================
        // PRIVACY POLICY
        // =====================================================

        if (menuPrivacy != null) {

            menuPrivacy.setOnClickListener(v -> {

                closeDrawer();

                openUrl(
                        "https://yourwebsite.com/privacy-policy"
                );
            });
        }


        // =====================================================
        // LOGOUT
        // =====================================================

        if (btnLogout1 != null) {

            btnLogout1.setOnClickListener(v -> {

                closeDrawer();

                logoutUser();
            });
        }
    }


    // =========================================================
    // COPY REFERRAL CODE
    // =========================================================

    private void copyReferralCode() {

        if (userPref == null) {

            Toast.makeText(
                    MainActivity.this,
                    "User data unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String referralCode =
                userPref.getReferralCode();


        if (referralCode == null ||
                referralCode.trim().isEmpty()) {

            Toast.makeText(
                    MainActivity.this,
                    "Referral code not available",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        referralCode =
                referralCode.trim();


        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );


        if (clipboard == null) {

            Toast.makeText(
                    MainActivity.this,
                    "Unable to copy code",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ClipData clip =
                ClipData.newPlainText(
                        "Referral Code",
                        referralCode
                );


        clipboard.setPrimaryClip(clip);


        Toast.makeText(
                MainActivity.this,
                "Referral code copied",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // LOAD USER FROM PREF
    // =========================================================

    private void loadUserFromPref() {

        // =====================================================
        // NAME
        // =====================================================

        if (txtName != null) {

            String name =
                    userPref.getName();


            if (name != null &&
                    !name.isEmpty()) {

                txtName.setText(
                        name
                );

            } else {

                txtName.setText(
                        "Hi, User"
                );
            }
        }


        // =====================================================
        // EMAIL
        // =====================================================

        if (txtEmail != null) {

            String email = userPref.getEmail();

            if (email == null || email.trim().isEmpty()) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null) {
                    email = currentUser.getEmail();
                }
            }

            if (email != null && !email.trim().isEmpty()) {
                txtEmail.setText(email.trim());
                txtEmail.setVisibility(View.VISIBLE);
            } else {
                txtEmail.setVisibility(View.GONE);
            }
        }


        // =====================================================
        // REFERRAL CODE
        // =====================================================

        if (txtUid != null) {

            String referralCode =
                    userPref.getReferralCode();


            if (referralCode != null &&
                    !referralCode.isEmpty()) {

                txtUid.setText(
                        referralCode
                );

            } else {

                txtUid.setText(
                        "Referral Code: -"
                );
            }
        }


        // =====================================================
        // COINS
        // =====================================================

        if (txtCoins != null) {

            txtCoins.setText(
                    String.valueOf(
                            userPref.getCoins()
                    )
            );
        }


        // =====================================================
        // TICKETS
        // =====================================================

        if (txtTickets != null) {

            txtTickets.setText(
                    String.valueOf(
                            userPref.getTickets()
                    )
            );
        }


        // =====================================================
        // FIREBASE USER PROFILE
        // =====================================================

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (user != null &&
                user.getPhotoUrl() != null &&
                imgProfile != null) {

            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(
                            R.drawable.ic_profile
                    )
                    .error(
                            R.drawable.ic_profile
                    )
                    .circleCrop()
                    .into(imgProfile);

        } else if (imgProfile != null) {

            imgProfile.setImageResource(
                    R.drawable.ic_profile
            );
        }
    }


    // =========================================================
    // OPEN DRAWER
    // =========================================================

    public void openDrawer() {

        if (drawerLayout != null &&
                !drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            drawerLayout.openDrawer(
                    GravityCompat.START
            );
        }
    }


    // =========================================================
    // CLOSE DRAWER
    // =========================================================

    public void closeDrawer() {

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }
    }


    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupNavigation() {

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                closeDrawer();
                selectNavTab(0);
                loadFragment(new HomeFragment());
            });
        }

        if (navGame != null) {
            navGame.setOnClickListener(v -> {
                closeDrawer();
                selectNavTab(1);
                loadFragment(new GameFragment());
            });
        }

        if (navReward != null) {
            navReward.setOnClickListener(v -> {
                closeDrawer();
                selectNavTab(2);
                loadFragment(new RewardFragment());
            });
        }

        if (navLeaderboard != null) {
            navLeaderboard.setOnClickListener(v -> {
                closeDrawer();
                selectNavTab(3);
                loadFragment(new LeaderboardFragment());
            });
        }
    }


    // =========================================================
    // SELECT NAVIGATION
    // =========================================================

    public void selectNav(View selected) {
        if (selected == navHome) {
            selectNavTab(0);
        } else if (selected == navGame) {
            selectNavTab(1);
        } else if (selected == navReward) {
            selectNavTab(2);
        } else if (selected == navLeaderboard) {
            selectNavTab(3);
        }
    }

    public void selectNavTab(int position) {
        if (activeNavPosition == position) {
            return;
        }

        if (bottomNav != null) {
            android.transition.AutoTransition autoTransition = new android.transition.AutoTransition();
            autoTransition.setDuration(280);
            autoTransition.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            android.transition.TransitionManager.beginDelayedTransition(bottomNav, autoTransition);
        }

        activeNavPosition = position;

        LinearLayout[] navContainers = {navHome, navGame, navReward, navLeaderboard};
        LinearLayout[] pills = {navPillHome, navPillGame, navPillReward, navPillLeaderboard};
        ImageView[] icons = {imgNavHome, imgNavGame, imgNavReward, imgNavLeaderboard};
        TextView[] texts = {txtNavHome, txtNavGame, txtNavReward, txtNavLeaderboard};
        int[] bgGradients = {
                R.drawable.bg_nav_pill_home,
                R.drawable.bg_nav_pill_game,
                R.drawable.bg_nav_pill_reward,
                R.drawable.bg_nav_pill_leaderboard
        };

        for (int i = 0; i < 4; i++) {
            final int index = i;
            if (navContainers[i] == null || pills[i] == null || icons[i] == null || texts[i] == null) continue;

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) navContainers[i].getLayoutParams();

            if (i == position) {
                // Expand selected container weight to 1.75f
                lp.weight = 1.75f;
                navContainers[i].setLayoutParams(lp);

                // Active item expanding pill
                pills[i].setBackgroundResource(bgGradients[i]);
                texts[i].setVisibility(View.VISIBLE);
                texts[i].setAlpha(0f);
                texts[i].animate().alpha(1f).setDuration(280).start();

                icons[i].setColorFilter(Color.WHITE);
                icons[i].animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            if (icons[index] != null) {
                                icons[index].animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                            }
                        })
                        .start();
            } else {
                // Contract unselected containers weight to 0.85f
                lp.weight = 0.85f;
                navContainers[i].setLayoutParams(lp);

                // Unselected items collapsed icon
                pills[i].setBackground(null);
                texts[i].setVisibility(View.GONE);
                icons[i].setColorFilter(Color.parseColor("#64748B"));
                icons[i].setScaleX(1.0f);
                icons[i].setScaleY(1.0f);
            }
        }
    }


    // =========================================================
    // LOAD FRAGMENT
    // =========================================================

    public void loadFragment(
            Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        fragment
                )
                .commit();
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private void logoutUser() {

        if (userPref != null) {

            userPref.logout();
        }


        FirebaseAuth
                .getInstance()
                .signOut();


        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                        .build();


        GoogleSignInClient googleSignInClient =
                GoogleSignIn.getClient(
                        MainActivity.this,
                        gso
                );


        googleSignInClient
                .signOut()
                .addOnCompleteListener(task -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Logged out",
                            Toast.LENGTH_SHORT
                    ).show();


                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    activity_login.class
                            );


                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );


                    startActivity(intent);

                    finish();
                });
    }


    // =========================================================
    // FULL SCREEN
    // =========================================================

    private void makeFullScreen() {

        Window window =
                getWindow();


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(
                    false
            );


            WindowInsetsController controller =
                    window.getInsetsController();


            if (controller != null) {

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            window.getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
        }


        window.setStatusBarColor(
                Color.TRANSPARENT
        );


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            window.setNavigationBarColor(
                    Color.TRANSPARENT
            );
        }
    }


    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            closeDrawer();

        } else {

            super.onBackPressed();
        }
    }


    // =========================================================
    // OPEN URL
    // =========================================================

    private void openUrl(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );


            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Unable to open link",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
