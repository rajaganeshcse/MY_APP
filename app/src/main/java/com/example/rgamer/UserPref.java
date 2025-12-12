package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPref {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUser(String uid, String name, String email, int coins, String token) {
        editor.putString("uid", uid);
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putInt("coins", coins);
        editor.putString("token", token);
        editor.apply();
    }

    public String getUid() { return pref.getString("uid", null); }
    public String getName() { return pref.getString("name", null); }
    public String getEmail() { return pref.getString("email", null); }
    public int getCoins() { return pref.getInt("coins", 0); }
    public String getToken() { return pref.getString("token", ""); }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}
