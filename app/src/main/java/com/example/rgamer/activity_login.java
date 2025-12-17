package com.example.rgamer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;

public class activity_login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    GoogleSignInClient googleSignInClient;
    FirebaseAuth auth;
    UserPref userPref;

    LinearLayout btnGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
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
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);

                firebaseAuth(account);

            } catch (Exception e) {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(GoogleSignInAccount account) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user = auth.getCurrentUser();

                    String uid = user.getUid();
                    String name = account.getDisplayName();
                    String email = account.getEmail();

                    String profileImage = "";
                    if (account.getPhotoUrl() != null) {
                        profileImage = account.getPhotoUrl().toString();
                    }

                    // ✅ Save all data
                    userPref.setUid(uid);
                    userPref.setName(name);
                    userPref.setEmail(email);
                    userPref.setProfileImage(profileImage);
                    userPref.setCoins(100);
                    userPref.setLogin(true);

                    startActivity(new Intent(this, MainActivity.class));
                    finish();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show());
    }
}
