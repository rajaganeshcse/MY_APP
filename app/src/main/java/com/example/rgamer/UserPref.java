package com.example.rgamer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserPref {

    private static final String PREF_NAME = "user_pref";

    // ================= BASIC =================
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGIN = "is_login";

    // ================= WALLET =================
    private static final String KEY_COINS = "coins";          // ✅ long
    private static final String KEY_TICKETS = "tickets";
    private static final String KEY_WALLET_TOKEN = "wallet_token";

    // ================= PROFILE =================
    private static final String KEY_PROFILE_IMAGE = "profile_image";

    // ================= NOTIFICATIONS =================
    private static final String KEY_FCM_TOKEN = "fcm_token";

    // ================= DAILY BONUS =================
    private static final String KEY_DAILY_DATE = "daily_date";

    // ================= DAILY ADS =================
    private static final String KEY_AD_DATE = "ad_date";
    private static final String KEY_AD_COUNT = "ad_count";

    // ================= REFERRAL =================
    private static final String KEY_REF_COINS = "referral_coins";
    private static final String KEY_REF_TICKETS = "referral_tickets";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /* ==================================================
       BASIC INFO
       ================================================== */

    public void setUid(String uid) {
        editor.putString(KEY_UID, uid).apply();
    }

    public String getUid() {
        return pref.getString(KEY_UID, "");
    }

    public void setName(String name) {
        editor.putString(KEY_NAME, name).apply();
    }

    public String getName() {
        return pref.getString(KEY_NAME, "");
    }

    public void setEmail(String email) {
        editor.putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public void setLogin(boolean status) {
        editor.putBoolean(KEY_IS_LOGIN, status).apply();
    }

    public boolean isLogin() {
        return pref.getBoolean(KEY_IS_LOGIN, false);
    }

    /* ==================================================
       WALLET (✅ FIXED)
       ================================================== */

    public void setCoins(long coins) {          // ✅ long
        editor.putLong(KEY_COINS, coins).apply();
    }

    public long getCoins() {                    // ✅ long
        return pref.getLong(KEY_COINS, 0L);
    }

    public void deductCoins(long amount) {      // ✅ helper
        long current = getCoins();
        long updated = Math.max(0, current - amount);
        setCoins(updated);
    }

    public void addCoins(long amount) {
        setCoins(getCoins() + amount);
    }

    public void setTickets(int tickets) {
        editor.putInt(KEY_TICKETS, tickets).apply();
    }

    public int getTickets() {
        return pref.getInt(KEY_TICKETS, 0);
    }

    public void setWalletToken(int token) {
        editor.putInt(KEY_WALLET_TOKEN, token).apply();
    }

    public int getWalletToken() {
        return pref.getInt(KEY_WALLET_TOKEN, 0);
    }

    /* ==================================================
       PROFILE
       ================================================== */

    public void setProfileImage(String url) {
        editor.putString(KEY_PROFILE_IMAGE, url).apply();
    }

    public String getProfileImage() {
        return pref.getString(KEY_PROFILE_IMAGE, "");
    }

    /* ==================================================
       NOTIFICATIONS
       ================================================== */

    public void setFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token).apply();
    }

    public String getFcmToken() {
        return pref.getString(KEY_FCM_TOKEN, "");
    }

    /* ==================================================
       DAILY BONUS
       ================================================== */

    public boolean canClaimDailyBonus() {
        String lastDate = pref.getString(KEY_DAILY_DATE, "");
        return !getTodayDate().equals(lastDate);
    }

    public void setDailyBonusClaimed() {
        editor.putString(KEY_DAILY_DATE, getTodayDate()).apply();
    }

    public void setDailyClaimedDate(String date) {
        editor.putString(KEY_DAILY_DATE, date).apply();
    }

    public String getDailyClaimedDate() {
        return pref.getString(KEY_DAILY_DATE, "");
    }

    /* ==================================================
       DAILY ADS
       ================================================== */

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

    public void increaseAdCount() {
        editor.putInt(KEY_AD_COUNT, getTodayAdCount() + 1).apply();
    }

    /* ==================================================
       REFERRAL CACHE
       ================================================== */

    public void setReferralCoins(long coins) {
        editor.putLong(KEY_REF_COINS, coins).apply();
    }

    public long getReferralCoins() {
        return pref.getLong(KEY_REF_COINS, 0);
    }

    public void setReferralTickets(long tickets) {
        editor.putLong(KEY_REF_TICKETS, tickets).apply();
    }

    public long getReferralTickets() {
        return pref.getLong(KEY_REF_TICKETS, 0);
    }

    /* ==================================================
       HELPERS
       ================================================== */

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
    }

    public void clear() {
        editor.clear().apply();
    }
}
