package com.app.rewardsplanet.network;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthTokenHelper {

    public interface TokenCallback {
        void onSuccess(String bearerToken);
        void onError(Exception e);
    }

    public static void getBearerToken(TokenCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError(new Exception("User is not logged in"));
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    if (token != null && !token.isEmpty()) {
                        callback.onSuccess("Bearer " + token);
                    } else {
                        callback.onError(new Exception("Token is empty"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
