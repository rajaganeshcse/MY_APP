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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.Fragements.HomeFragment;
import com.app.rewardsplanet.Fragements.RewardFragment;
import com.app.rewardsplanet.Fragements.ShareEarnFragment;
import com.app.rewardsplanet.LeaderboardFragment;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;

import com.app.rewardsplanet.invite.activity_refer_earn;
import com.app.rewardsplanet.withdraws.TransactionHistoryActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.app.rewardsplanet.notifications.MyFirebaseMessagingService;

import java.util.HashMap;
import java.util.Map;
import com.app.rewardsplanet.share_earn.ui.InstallAttributionHelper;


public class MainActivity extends AppCompatActivity {

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    public LinearLayout navHome;
    private LinearLayout navShareEarn;
    public LinearLayout navReward;
    private LinearLayout navLeaderboard;

    private LinearLayout bottomNav;

    private LinearLayout navPillHome;
    private LinearLayout navPillShareEarn;
    private LinearLayout navPillReward;
    private LinearLayout navPillLeaderboard;

    private TextView txtNavHome;
    private TextView txtNavShareEarn;
    private TextView txtNavReward;
    private TextView txtNavLeaderboard;

    private int activeNavPosition = -1;


    // =========================================================
    // NAV IMAGES
    // =========================================================

    private ImageView imgNavHome;
    private ImageView imgNavShareEarn;
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
    private LinearLayout coinhistory;
    private LinearLayout tickethistory;

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

        // Install attribution & campaign deep link handling (idempotent)
        if (getIntent() != null && getIntent().getData() != null) {
            InstallAttributionHelper.storeClickIdFromUri(this, getIntent().getData());
        }
        InstallAttributionHelper.initializeAndRecordInstall(this);
        InstallAttributionHelper.recordRegistrationIfNeeded(this);

        initViews();

        initDrawer();

        setupNavigation();


        Log.d("RewardsFCM", "Firebase initialization started");
        try {
            if (com.google.firebase.FirebaseApp.getInstance() != null) {
                Log.d("RewardsFCM", "FirebaseApp initialized");
            }
        } catch (Exception e) {
            Log.e("RewardsFCM", "FirebaseApp check failed", e);
        }

        // Ensure notification channel is registered
        MyFirebaseMessagingService.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("RewardsFCM", "Notification permission granted");
            } else {
                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        101
                );
            }
        }

        // =====================================================
        // FIREBASE FCM TOKEN
        // =====================================================

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.e("RewardsFCM", "Failed to get FCM token", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    if (token == null || token.trim().isEmpty()) {
                        Log.w("RewardsFCM", "Retrieved FCM token is empty");
                        return;
                    }

                    Log.d("RewardsFCM", "FCM token received successfully");
                    userPref.setFcmToken(token);

                    String uid = userPref.getUid();
                    if (uid == null || uid.isEmpty()) {
                        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (fUser != null) uid = fUser.getUid();
                    }

                    if (uid != null && !uid.isEmpty()) {
                        Map<String, Object> tokenData = new HashMap<>();
                        tokenData.put("fcmToken", token);
                        tokenData.put("notificationEnabled", true);
                        tokenData.put("updatedAt", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("RewardsFCM", "FCM token saved successfully to Firestore");
                                    InstallAttributionHelper.recordRegistrationIfNeeded(MainActivity.this);
                                })
                                .addOnFailureListener(e -> Log.e("RewardsFCM", "Failed to update FCM token in Firestore", e));
                    } else {
                        Log.w("RewardsFCM", "FirebaseAuth user unavailable when saving FCM token");
                    }
                });

        // =====================================================
        // USER DATA
        // =====================================================

        loadUserFromPref();

        // =====================================================
        // DEFAULT FRAGMENT / NOTIFICATION NAV
        // =====================================================

        selectNav(navHome);
        loadFragment(new HomeFragment());

        handleNotificationNavigation(getIntent());

        // Check if redirected from ProfileActivity after saving changes
        checkProfileSuccessDialog(getIntent());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("RewardsFCM", "Notification permission granted");
            } else {
                Log.d("RewardsFCM", "Notification permission denied");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getData() != null) {
            InstallAttributionHelper.storeClickIdFromUri(this, intent.getData());
        }
        checkProfileSuccessDialog(intent);
        handleNotificationNavigation(intent);
    }

    private void handleNotificationNavigation(Intent intent) {
        if (intent == null) return;

        String screen = intent.getStringExtra("notification_screen");
        if (screen == null || screen.trim().isEmpty()) {
            screen = intent.getStringExtra("screen");
        }
        if (screen == null || screen.trim().isEmpty()) {
            return;
        }

        Log.d("RewardsFCM", "Navigating to destination screen: " + screen);

        switch (screen.toUpperCase()) {
            case "DAILY_BONUS":
                selectNav(navHome);
                loadFragment(new HomeFragment());
                break;
            case "SPINNER":
                startActivity(new Intent(this, com.app.rewardsplanet.lucky_draw.activity_daily_spin.class));
                break;
            case "SCRATCH_CARD":
                startActivity(new Intent(this, com.app.rewardsplanet.lucky_draw.ScratchActivity.class));
                break;
            case "GAMES":
            case "SHARE_EARN":
                selectNavTab(1);
                loadFragment(new ShareEarnFragment());
                break;
            case "REDEEM":
                selectNav(navReward);
                loadFragment(new RewardFragment());
                break;
            case "LEADERBOARD":
                selectNav(navLeaderboard);
                loadFragment(new LeaderboardFragment());
                break;
            case "TASKS":
                startActivity(new Intent(this, com.app.rewardsplanet.lucky_draw.HitRewardzActivity.class));
                break;
            case "PROFILE":
                startActivity(new Intent(this, ProfileActivity.class));
                break;
            case "OFFER_HISTORY":
            case "SHARE_EARN_HISTORY":
                startActivity(new Intent(this, com.app.rewardsplanet.share_earn.ui.OfferHistoryActivity.class));
                break;
            case "SHARE_EARN_EARNINGS":
                startActivity(new Intent(this, com.app.rewardsplanet.share_earn.ui.ShareEarnEarningsActivity.class));
                break;
            case "HOME":
            default:
                selectNav(navHome);
                loadFragment(new HomeFragment());
                break;
        }
        intent.removeExtra("notification_screen");
        intent.removeExtra("screen");
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
        navShareEarn = findViewById(R.id.navShareEarn);
        navReward = findViewById(R.id.navReward);
        navLeaderboard = findViewById(R.id.navLeaderboard);

        navPillHome = findViewById(R.id.navPillHome);
        navPillShareEarn = findViewById(R.id.navPillShareEarn);
        navPillReward = findViewById(R.id.navPillReward);
        navPillLeaderboard = findViewById(R.id.navPillLeaderboard);

        imgNavHome = findViewById(R.id.imgNavHome);
        imgNavShareEarn = findViewById(R.id.imgNavShareEarn);
        imgNavReward = findViewById(R.id.imgNavReward);
        imgNavLeaderboard = findViewById(R.id.imgNavLeaderboard);

        txtNavHome = findViewById(R.id.txtNavHome);
        txtNavShareEarn = findViewById(R.id.txtNavShareEarn);
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


        btnEditProfile = findViewById(R.id.btnEditProfile);

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
        // ACCOUNT HISTORY & TRANSACTION HISTORY CLICKS
        // =====================================================

        View.OnClickListener openHistoryListener = v -> {
            closeDrawer();
            Intent intent = new Intent(MainActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        };

        if (menuWallet != null) {
            menuWallet.setOnClickListener(openHistoryListener);
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
        // EDIT PROFILE / PROFILE IMAGE CLICK
        // =====================================================

        View.OnClickListener openProfileListener = v -> {
            closeDrawer();
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        };

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(openProfileListener);
        }

        if (imgProfile != null) {
            imgProfile.setOnClickListener(openProfileListener);
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

        if (navShareEarn != null) {
            navShareEarn.setOnClickListener(v -> {
                closeDrawer();
                selectNavTab(1);
                loadFragment(new ShareEarnFragment());
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
        } else if (selected == navShareEarn) {
            selectNavTab(1);
            loadFragment(new ShareEarnFragment());
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

        LinearLayout[] navContainers = {navHome, navShareEarn, navReward, navLeaderboard};
        LinearLayout[] pills = {navPillHome, navPillShareEarn, navPillReward, navPillLeaderboard};
        ImageView[] icons = {imgNavHome, imgNavShareEarn, imgNavReward, imgNavLeaderboard};
        TextView[] texts = {txtNavHome, txtNavShareEarn, txtNavReward, txtNavLeaderboard};
        int[] bgGradients = {
                R.drawable.bg_nav_pill_home,
                R.drawable.bg_nav_pill_share_earn,
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid() != null) {
            Map<String, Object> tokenRemove = new HashMap<>();
            tokenRemove.put("fcmToken", FieldValue.delete());
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .update(tokenRemove)
                    .addOnFailureListener(e -> Log.e("LOGOUT", "Error removing FCM token", e));
        }

        if (userPref != null) {
            userPref.logout();
        }

        FirebaseAuth.getInstance().signOut();


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
