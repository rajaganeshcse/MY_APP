package com.example.rgamer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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
        super.onCreate(savedInstanceState);

        userPref = new UserPref(this);

        // AUTO LOGIN
        if (userPref.getUid() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogle);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            Intent intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(account -> firebaseLogin(account))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Google Login Failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void firebaseLogin(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    checkUserInFirestore(user.getUid(), user.getDisplayName(), user.getEmail());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Firebase Login Failed", Toast.LENGTH_SHORT).show());
    }

    private void checkUserInFirestore(String uid, String name, String email) {

        DocumentReference ref = db.collection("users").document(uid);

        ref.get().addOnSuccessListener(document -> {

            if (document.exists()) {
                // EXISTING USER → Load data
                String savedName = document.getString("name");
                String savedEmail = document.getString("email");
                int coins = document.getLong("coins").intValue();
                String token = document.getString("token");

                // Save to SharedPref
                userPref.saveUser(uid, savedName, savedEmail, coins, token);

            } else {
                // NEW USER → create data
                int initialCoins = 100;
                String token = UUID.randomUUID().toString(); // generate unique token

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("uid", uid);
                userMap.put("name", name);
                userMap.put("email", email);
                userMap.put("coins", initialCoins);
                userMap.put("token", token);
                userMap.put("created_at", System.currentTimeMillis());

                ref.set(userMap);

                // Save to SharedPref
                userPref.saveUser(uid, name, email, initialCoins, token);
            }

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(activity_login.this, MainActivity.class));
            finish();
        });
    }
}
