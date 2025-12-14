package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;

public class UserPref {

    private static final String PREF_NAME = "user_pref";
    private static final String KEY_DAILY_DATE = "daily_date";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /* ================= UID ================= */
    public void setUid(String uid) {
        editor.putString("uid", uid);
        editor.apply();
    }

    public String getUid() {
        return pref.getString("uid", "");
    }

    /* ================= NAME ================= */
    public void setName(String name) {
        editor.putString("name", name);
        editor.apply();
    }

    public String getName() {
        return pref.getString("name", "");
    }

    /* ================= EMAIL ================= */
    public void setEmail(String email) {
        editor.putString("email", email);
        editor.apply();
    }

    public String getEmail() {
        return pref.getString("email", "");
    }

    /* ================= COINS ================= */
    public void setCoins(int coins) {
        editor.putInt("coins", coins);
        editor.apply();
    }

    public int getCoins() {
        return pref.getInt("coins", 0);
    }

    /* ================= TOKEN ================= */
    public void setToken(String token) {
        editor.putString("token", token);
        editor.apply();
    }

    public String getToken() {
        return pref.getString("token", "0");
    }

    /* ================= PROFILE IMAGE ================= */
    public void setProfileImage(String url) {
        editor.putString("profile_image", url);
        editor.apply();
    }

    public String getProfileImage() {
        return pref.getString("profile_image", "");
    }

    /* ================= LOGIN STATUS ================= */
    public void setLogin(boolean status) {
        editor.putBoolean("is_login", status);
        editor.apply();
    }

    public boolean isLogin() {
        return pref.getBoolean("is_login", false);
    }

    /* ==================================================
       DAILY BONUS (ONCE PER DAY)
       ================================================== */

    /**
     * New preferred method
     */
    public boolean canClaimDailyBonus() {
        String lastDate = pref.getString(KEY_DAILY_DATE, "");

        String today;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            today = java.time.LocalDate.now().toString();
        } else {
            // Fallback for older Android versions
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            today = sdf.format(new java.util.Date());
        }

        return !today.equals(lastDate);
    }


    /**
     * New preferred method
     */
    public void setDailyBonusClaimed() {
        String today;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            today = java.time.LocalDate.now().toString();
        } else {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            today = sdf.format(new java.util.Date());
        }

        editor.putString(KEY_DAILY_DATE, today);
        editor.apply();
    }


    /* --------------------------------------------------
       BACKWARD-COMPATIBLE METHODS
       (So old code will NOT break)
       -------------------------------------------------- */

    public boolean canClaimDaily() {
        return canClaimDailyBonus();
    }

    public void setDailyClaimed() {
        setDailyBonusClaimed();
    }

    /* ================= CLEAR ================= */
    public void clear() {
        editor.clear();
        editor.apply();
    }
}
