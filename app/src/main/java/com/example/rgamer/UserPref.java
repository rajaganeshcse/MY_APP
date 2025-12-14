package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
       DAILY BONUS (ONCE PER DAY – SAFE FOR ALL ANDROID)
       ================================================== */

    /**
     * Preferred method
     */
    public boolean canClaimDailyBonus() {
        String lastDate = pref.getString(KEY_DAILY_DATE, "");
        String today = getTodayDate();
        return !today.equals(lastDate);
    }

    /**
     * Preferred method
     */
    public void setDailyBonusClaimed() {
        editor.putString(KEY_DAILY_DATE, getTodayDate());
        editor.apply();
    }

    /* --------------------------------------------------
       BACKWARD COMPATIBILITY (so old code works)
       -------------------------------------------------- */

    public boolean canClaimDaily() {
        return canClaimDailyBonus();
    }

    public void setDailyClaimed() {
        setDailyBonusClaimed();
    }

    /* ================= DATE HELPER ================= */
    private String getTodayDate() {
        // Works on ALL Android versions
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /* ================= CLEAR ================= */
    public void clear() {
        editor.clear();
        editor.apply();
    }
}
