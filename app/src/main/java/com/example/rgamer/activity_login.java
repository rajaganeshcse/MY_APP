package com.example.rgamer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.rgamer.models.UserModel;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    GoogleSignInClient googleSignInClient;
    FirebaseAuth auth;
    FirebaseFirestore db;
    UserPref userPref;

    LinearLayout btnGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userPref = new UserPref(this);

        btnGoogle = findViewById(R.id.btnGoogle);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        startActivityForResult(
                googleSignInClient.getSignInIntent(),
                RC_SIGN_IN
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);

                firebaseAuth(account);

            } catch (Exception e) {
                Toast.makeText(this,
                        "Login Failed",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(GoogleSignInAccount account) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        account.getIdToken(),
                        null
                );

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser =
                            auth.getCurrentUser();

                    if (firebaseUser == null) return;

                    String uid = firebaseUser.getUid();
                    String name = account.getDisplayName();
                    String email = account.getEmail();

                    String profileImage = "";
                    if (account.getPhotoUrl() != null) {
                        profileImage = account.getPhotoUrl().toString();
                    }

                    checkUserInFirestore(
                            uid,
                            name,
                            email,
                            profileImage
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Auth Failed",
                                Toast.LENGTH_SHORT).show());
    }

    /* ================= FIRESTORE USER CHECK ================= */

    private void checkUserInFirestore(
            String uid,
            String name,
            String email,
            String profileImage) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        // ✅ EXISTING USER
                        UserModel user =
                                doc.toObject(UserModel.class);

                        if (user != null) {
                            saveUserToPref(user);
                        }

                    } else {
                        // 🆕 NEW USER
                        UserModel newUser =
                                createNewUser(
                                        uid,
                                        name,
                                        email,
                                        profileImage
                                );

                        db.collection("users")
                                .document(uid)
                                .set(newUser.toMap());

                        saveUserToPref(newUser);
                    }

                    userPref.setLogin(true);
                    startActivity(
                            new Intent(
                                    this,
                                    MainActivity.class
                            )
                    );
                    finish();
                });
    }

    /* ================= CREATE NEW USER ================= */

    private UserModel createNewUser(
            String uid,
            String name,
            String email,
            String profileImage) {

        return new UserModel(
                uid,
                name,
                email,
                100,        // coins
                0,          // tickets
                0,          // walletToken
                "",         // fcmToken
                profileImage,
                "",         // dailyBonusClaimedDate
                generateReferralCode(uid),
                "",         // referredBy
                false,      // referralUsed
                0,          // referral coins
                0,          // referral tickets
                System.currentTimeMillis()
        );
    }

    private String generateReferralCode(String uid) {
        return uid.substring(0, 6).toUpperCase();
    }

    /* ================= SAVE TO USER PREF ================= */

    private void saveUserToPref(UserModel user) {

        userPref.setUid(user.getUid());
        userPref.setName(user.getName());
        userPref.setEmail(user.getEmail());
        userPref.setProfileImage(user.getProfileImage());

        userPref.setCoins(user.getCoins());
        userPref.setTickets(user.getTickets());
        userPref.setWalletToken(user.getWalletToken());

        userPref.setDailyClaimedDate(
                user.getDailyBonusClaimedDate()
        );

        userPref.setLogin(true);
    }
}
