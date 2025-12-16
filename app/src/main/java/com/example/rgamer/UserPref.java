package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserPref {

    private static final String PREF_NAME = "user_pref";
    private static final String KEY_DAILY_DATE = "daily_date";

    // 🔹 NEW (Ads)
    private static final String KEY_AD_DATE = "ad_date";
    private static final String KEY_AD_COUNT = "ad_count";

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
       DAILY BONUS (ONCE PER DAY – SAFE)
       ================================================== */

    public boolean canClaimDailyBonus() {
        String lastDate = pref.getString(KEY_DAILY_DATE, "");
        String today = getTodayDate();
        return !today.equals(lastDate);
    }

    public void setDailyBonusClaimed() {
        editor.putString(KEY_DAILY_DATE, getTodayDate());
        editor.apply();
    }

    // Backward compatibility
    public boolean canClaimDaily() {
        return canClaimDailyBonus();
    }

    public void setDailyClaimed() {
        setDailyBonusClaimed();
    }

    /* ==================================================
       🆕 DAILY ADS LOGIC (ANTI-CHEAT SAFE)
       ================================================== */

    /** Get today ad count (auto reset on new day) */
    public int getTodayAdCount() {
        String today = getTodayDate();
        String savedDate = pref.getString(KEY_AD_DATE, "");

        if (!today.equals(savedDate)) {
            editor.putString(KEY_AD_DATE, today);
            editor.putInt(KEY_AD_COUNT, 0);
            editor.apply();
            return 0;
        }
        return pref.getInt(KEY_AD_COUNT, 0);
    }

    /** Increase ad count safely */
    public void increaseAdCount() {
        int count = getTodayAdCount();
        editor.putInt(KEY_AD_COUNT, count + 1);
        editor.apply();
    }

    /** Reset ads manually (admin/debug use) */
    public void resetAdCount() {
        editor.putInt(KEY_AD_COUNT, 0);
        editor.putString(KEY_AD_DATE, getTodayDate());
        editor.apply();
    }

    /* ================= DATE HELPER ================= */
    private String getTodayDate() {
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
