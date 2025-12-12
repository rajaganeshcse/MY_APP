package com.example.rgamer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class activity_login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    GoogleSignInClient googleSignInClient;
    FirebaseAuth auth;
    FirebaseFirestore db;

    LinearLayout btnGoogle;
    UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Set status and nav bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#6A1BFF"));
        window.setNavigationBarColor(Color.parseColor("#FFFFFF"));

        super.onCreate(savedInstanceState);

        userPref = new UserPref(this);

        // AUTO LOGIN
        if (!userPref.getUid().isEmpty()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogle);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google Sign-in setup
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(this::firebaseLogin)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Google Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }


    private void firebaseLogin(GoogleSignInAccount account) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    if (user != null) {
                        checkUserInFirestore(
                                user.getUid(),
                                user.getDisplayName(),
                                user.getEmail()
                        );
                    }

                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Firebase Auth failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void checkUserInFirestore(String uid, String name, String email) {

        DocumentReference ref = db.collection("users").document(uid);

        ref.get().addOnSuccessListener(doc -> {

            if (doc.exists()) {
                // SAFE READ
                String savedName = doc.getString("name") != null ? doc.getString("name") : name;
                String savedEmail = doc.getString("email") != null ? doc.getString("email") : email;

                Long coinValue = doc.getLong("coins");
                int coins = Math.toIntExact(coinValue != null ? coinValue : 100);

                String token = doc.getString("token");
                if (token == null || token.isEmpty()) {
                    token = UUID.randomUUID().toString();
                    ref.update("token", token);
                }

                userPref.saveUser(uid, savedName, savedEmail, coins, token);

            } else {

                // NEW USER
                int initialCoins = 100;
                String token = UUID.randomUUID().toString();

                Map<String, Object> map = new HashMap<>();
                map.put("uid", uid);
                map.put("name", name != null ? name : "User");
                map.put("email", email != null ? email : "unknown");
                map.put("coins", initialCoins);
                map.put("token", token);
                map.put("created_at", System.currentTimeMillis());

                ref.set(map);
                userPref.saveUser(uid, name, email, initialCoins, token);
            }

            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
            goToMain();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void goToMain() {
        startActivity(new Intent(activity_login.this, MainActivity.class));
        finish();
    }
}
