package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPref {

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences("USER_PREF", Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // ------------------ SAVE USER DATA ------------------
    public void saveUser(String uid, String name, String email, int coins, String token) {
        editor.putString("uid", uid);
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putInt("coins", coins);
        editor.putString("token", token);
        editor.apply();
    }

    // ------------------ GET USER DATA ------------------
    public String getUid() {
        return pref.getString("uid", "");
    }

    public String getName() {
        return pref.getString("name", "");
    }

    public String getEmail() {
        return pref.getString("email", "");
    }

    public int getCoins() {
        return pref.getInt("coins", 0);
    }

    public String getToken() {
        return pref.getString("token", "");
    }

    // ------------------ UPDATE COINS ------------------
    public void updateCoins(int newCoins) {
        editor.putInt("coins", newCoins);
        editor.apply();
    }

    // ------------------ CLEAR USER DATA (LOGOUT) ------------------
    public void clearUser() {
        editor.clear();
        editor.apply();
    }
}
