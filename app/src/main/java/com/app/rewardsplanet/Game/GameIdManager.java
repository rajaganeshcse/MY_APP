package com.app.rewardsplanet.Game;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GameIdManager {

    public interface Callback {
        void onResult(String gameId);
    }

    /* ================= GET GAME ID ================= */
    public static void getGameId(String game, Callback callback) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onResult(null);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()
                            && doc.contains("game_ids." + game)) {

                        callback.onResult(
                                doc.getString("game_ids." + game)
                        );
                    } else {
                        callback.onResult(null);
                    }
                });
    }

    /* ================= SAVE GAME ID ================= */
    public static void saveGameId(
            String game,
            @NonNull String gameId
    ) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("game_ids." + game, gameId);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(map);
    }
}
