package com.app.rewardsplanet;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserPref {

    private static final String PREF_NAME = "user_pref";

    /* ================= BASIC ================= */
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_DOB = "dob";
    private static final String KEY_IS_LOGIN = "is_login";

    /* ================= WALLET ================= */
    private static final String KEY_COINS = "coins";
    private static final String KEY_TICKETS = "tickets";
    private static final String KEY_WALLET_TOKEN = "wallet_token";

    /* ================= PROFILE ================= */
    private static final String KEY_PROFILE_IMAGE = "profile_image";

    /* ================= NOTIFICATIONS ================= */
    private static final String KEY_FCM_TOKEN = "fcm_token";

    /* ================= DAILY BONUS ================= */
    private static final String KEY_DAILY_DATE = "daily_date";

    /* ================= DAILY ADS ================= */
    private static final String KEY_AD_DATE = "ad_date";
    private static final String KEY_AD_COUNT = "ad_count";

    /* ================= REFERRAL ================= */
    private static final String KEY_REF_COINS = "referral_coins";
    private static final String KEY_REF_TICKETS = "referral_tickets";
    private static final String KEY_REFERRAL_CODE = "referral_code";

    /* ================= DAILY SPIN ================= */
    private static final String KEY_SPIN_DATE = "spin_date";
    private static final String KEY_SPIN_COUNT = "spin_count";

    /* ================= DAILY SCRATCH ================= */
    private static final String KEY_SCRATCH_DATE  = "scratch_date";
    private static final String KEY_SCRATCH_COUNT = "scratch_count";

    /* ================= DAILY STREAK ================= */
    private static final String KEY_STREAK_DATE   = "streak_date";
    private static final String KEY_STREAK_COUNT  = "streak_count";

    /* ================= DAILY QUIZ ================= */
    private static final String KEY_QUIZ_DATE     = "quiz_date";
    private static final String KEY_QUIZ_COUNT    = "quiz_count";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public UserPref(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /* ==================================================
       BASIC
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

    public void setPhone(String phone) {
        editor.putString(KEY_PHONE, phone).apply();
    }

    public String getPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public void setGender(String gender) {
        editor.putString(KEY_GENDER, gender).apply();
    }

    public String getGender() {
        return pref.getString(KEY_GENDER, "");
    }

    public void setDob(String dob) {
        editor.putString(KEY_DOB, dob).apply();
    }

    public String getDob() {
        return pref.getString(KEY_DOB, "");
    }

    public void setLogin(boolean status) {
        editor.putBoolean(KEY_IS_LOGIN, status).apply();
    }

    public boolean isLogin() {
        return pref.getBoolean(KEY_IS_LOGIN, false);
    }

    /* ================= LOGIN VALIDATION (ADDED) ================= */

    public boolean isUserValid() {
        return isLogin() && !getUid().isEmpty();
    }

    /* ==================================================
       DAILY BONUS
       ================================================== */

    public void setDailyClaimedDate(String date) {
        editor.putString(KEY_DAILY_DATE, date).apply();
    }

    public String getDailyClaimedDate() {
        return pref.getString(KEY_DAILY_DATE, "");
    }

    public boolean canClaimDailyBonus() {
        return !getTodayDate().equals(pref.getString(KEY_DAILY_DATE, ""));
    }

    public void setDailyBonusClaimed() {
        editor.putString(KEY_DAILY_DATE, getTodayDate()).apply();
    }

    /* ==================================================
       WALLET (SAFE)
       ================================================== */

    public void setCoins(long coins) {
        editor.putLong(KEY_COINS, coins).apply();
    }

    public long getCoins() {
        try {
            return pref.getLong(KEY_COINS, 0L);
        } catch (ClassCastException e) {
            int oldCoins = pref.getInt(KEY_COINS, 0);
            setCoins(oldCoins);
            return oldCoins;
        }
    }

    public void addCoins(long amount) {
        setCoins(getCoins() + amount);
    }

    public void deductCoins(long amount) {
        setCoins(Math.max(0, getCoins() - amount));
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
       DAILY SPIN
       ================================================== */

    public int getTodaySpinCount() {
        String today = getTodayDate();
        String savedDate = pref.getString(KEY_SPIN_DATE, "");

        if (!today.equals(savedDate)) {
            editor.putString(KEY_SPIN_DATE, today);
            editor.putInt(KEY_SPIN_COUNT, 0);
            editor.apply();
            return 0;
        }
        return pref.getInt(KEY_SPIN_COUNT, 0);
    }

    public void increaseSpinCount() {
        editor.putInt(KEY_SPIN_COUNT, getTodaySpinCount() + 1).apply();
    }

    private static final String KEY_SCRATCH_LAST_REWARD = "scratch_last_reward";

    public int getTodayScratchRemaining() {
        String today = getTodayDate();
        String savedDate = pref.getString(KEY_SCRATCH_DATE, "");
        if (!today.equals(savedDate)) {
            // New day - reset (will be overwritten by backend anyway)
            editor.putString(KEY_SCRATCH_DATE, today);
            editor.putInt(KEY_SCRATCH_COUNT, 0);
            editor.apply();
            return 0;
        }
        return pref.getInt(KEY_SCRATCH_COUNT, 0);
    }

    public void saveScratchRemaining(int remaining) {
        editor.putString(KEY_SCRATCH_DATE, getTodayDate());
        editor.putInt(KEY_SCRATCH_COUNT, remaining);
        editor.apply();
    }

    public int getLastScratchReward() {
        return pref.getInt(KEY_SCRATCH_LAST_REWARD, 0);
    }

    public void saveLastScratchReward(int reward) {
        editor.putInt(KEY_SCRATCH_LAST_REWARD, reward).apply();
    }

    /* ==================================================
       DAILY STREAK
       ================================================== */

    public int getStreakCount() {
        return pref.getInt(KEY_STREAK_COUNT, 0);
    }

    public String getStreakClaimedDate() {
        return pref.getString(KEY_STREAK_DATE, "");
    }

    public void saveStreak(int streakCount, String claimedDate) {
        editor.putInt(KEY_STREAK_COUNT, streakCount);
        editor.putString(KEY_STREAK_DATE, claimedDate);
        editor.apply();
    }

    /* ==================================================
       REFERRAL (ADDED)
       ================================================== */

    public void setReferralCode(String referralCode) {
        editor.putString(KEY_REFERRAL_CODE, referralCode).apply();
    }

    public String getReferralCode() {
        return pref.getString(KEY_REFERRAL_CODE, "");
    }

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
       DAILY QUIZ
       ================================================== */

    public int getTodayQuizCount() {
        String today = getTodayDate();
        String savedDate = pref.getString(KEY_QUIZ_DATE, "");

        if (!today.equals(savedDate)) {
            editor.putString(KEY_QUIZ_DATE, today);
            editor.putInt(KEY_QUIZ_COUNT, 0);
            editor.apply();
            return 0;
        }
        return pref.getInt(KEY_QUIZ_COUNT, 0);
    }

    public void increaseQuizCount() {
        editor.putInt(KEY_QUIZ_COUNT, getTodayQuizCount() + 1).apply();
    }

    public boolean canPlayDailyQuiz() {
        return getTodayQuizCount() < 1;
    }

    /* ==================================================
       HELPERS
       ================================================== */

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());
    }

    /* ==================================================
       LOGOUT / CLEAR
       ================================================== */

    public void logout() {
        boolean isDarkMode = pref.getBoolean("dark_mode", false);

        editor.clear();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();
    }

    public void clear() {
        editor.clear().apply();
    }
}