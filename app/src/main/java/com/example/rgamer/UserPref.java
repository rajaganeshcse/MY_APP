package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPref {

    private static final String PREF_NAME = "user_pref";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // UID
    public void setUid(String uid) {
        editor.putString("uid", uid);
        editor.apply();
    }

    public String getUid() {
        return pref.getString("uid", "");
    }

    // Name
    public void setName(String name) {
        editor.putString("name", name);
        editor.apply();
    }

    public String getName() {
        return pref.getString("name", "");
    }

    // Email
    public void setEmail(String email) {
        editor.putString("email", email);
        editor.apply();
    }

    public String getEmail() {
        return pref.getString("email", "");
    }

    // Coins
    public void setCoins(int coins) {
        editor.putInt("coins", coins);
        editor.apply();
    }

    public int getCoins() {
        return pref.getInt("coins", 0);
    }

    // Token
    public void setToken(String token) {
        editor.putString("token", token);
        editor.apply();
    }

    public String getToken() {
        return pref.getString("token", "0");
    }

    // ✅ Profile Image
    public void setProfileImage(String url) {
        editor.putString("profile_image", url);
        editor.apply();
    }

    public String getProfileImage() {
        return pref.getString("profile_image", "");
    }

    // Login status
    public void setLogin(boolean status) {
        editor.putBoolean("is_login", status);
        editor.apply();
    }

    public boolean isLogin() {
        return pref.getBoolean("is_login", false);
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}
