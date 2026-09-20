package com.app.rewardsplanet.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class UserModel {

    /* ================= BASIC INFO ================= */
    private String uid;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String dob;
    private String profile_pic;
    private String referralCode;
    private String referredBy;
    private String referredByName;
    private boolean referralUsed;
    private String fcmToken;

    /* ================= WALLET ================= */
    private long coins;
    private long tickets;

    /* ================= REFERRAL STATS ================= */
    private long totalReferralCoins;
    private long totalReferralTickets;

    /* ================= COUNTERS & STREAK ================= */
    private long dailySpinCount;
    private long dailyScratchCount;
    private long daily_ads_count;
    private long streak_count;

    /* ================= DATES ================= */
    private String dailyBonusClaimDate;
    private String dailyScratchDate;
    private String lastSpinDate;
    private Map<String, Object> daily_bonus;

    /* ================= TIMESTAMPS ================= */
    private Timestamp created_at;
    private Timestamp loginTime;

    /* ================= EMPTY CONSTRUCTOR ================= */
    public UserModel() {}

    /* ================= FULL CONSTRUCTOR ================= */
    public UserModel(
            String uid,
            String name,
            String email,
            String profile_pic,
            String referralCode,
            String referredBy,
            boolean referralUsed,
            String fcmToken,
            long coins,
            long tickets,
            long totalReferralCoins,
            long totalReferralTickets,
            long dailySpinCount,
            long dailyScratchCount,
            long daily_ads_count,
            long streak_count,
            String dailyBonusClaimDate,
            String dailyScratchDate,
            String lastSpinDate,
            Map<String, Object> daily_bonus,
            Timestamp created_at,
            Timestamp loginTime
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profile_pic = profile_pic;
        this.referralCode = referralCode;
        this.referredBy = referredBy;
        this.referralUsed = referralUsed;
        this.fcmToken = fcmToken;
        this.coins = coins;
        this.tickets = tickets;
        this.totalReferralCoins = totalReferralCoins;
        this.totalReferralTickets = totalReferralTickets;
        this.dailySpinCount = dailySpinCount;
        this.dailyScratchCount = dailyScratchCount;
        this.daily_ads_count = daily_ads_count;
        this.streak_count = streak_count;
        this.dailyBonusClaimDate = dailyBonusClaimDate;
        this.dailyScratchDate = dailyScratchDate;
        this.lastSpinDate = lastSpinDate;
        this.daily_bonus = daily_bonus;
        this.created_at = created_at;
        this.loginTime = loginTime;
    }

    /* ================= GETTERS ================= */
    public String getUid() { return uid != null ? uid : ""; }
    public String getName() { return name != null ? name : ""; }
    public String getPhone() { return phone != null ? phone : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getGender() { return gender != null ? gender : ""; }
    public String getDob() { return dob != null ? dob : ""; }
    public String getProfile_pic() { return profile_pic != null ? profile_pic : ""; }
    public String getReferralCode() { return referralCode != null ? referralCode : ""; }
    public String getReferredBy() { return referredBy != null ? referredBy : ""; }
    public String getReferredByName() { return referredByName != null ? referredByName : ""; }
    public boolean isReferralUsed() { return referralUsed; }
    public String getFcmToken() { return fcmToken != null ? fcmToken : ""; }

    public long getCoins() { return Math.max(0, coins); }
    public long getTickets() { return Math.max(0, tickets); }
    public long getTotalReferralCoins() { return Math.max(0, totalReferralCoins); }
    public long getTotalReferralTickets() { return Math.max(0, totalReferralTickets); }

    public long getDailySpinCount() { return Math.max(0, dailySpinCount); }
    public long getDailyScratchCount() { return Math.max(0, dailyScratchCount); }
    public long getDaily_ads_count() { return Math.max(0, daily_ads_count); }
    public long getStreak_count() { return Math.max(0, streak_count); }

    public String getDailyBonusClaimDate() { return dailyBonusClaimDate != null ? dailyBonusClaimDate : ""; }
    public String getDailyScratchDate() { return dailyScratchDate != null ? dailyScratchDate : ""; }
    public String getLastSpinDate() { return lastSpinDate != null ? lastSpinDate : ""; }
    public Map<String, Object> getDaily_bonus() { return daily_bonus; }

    public Timestamp getCreated_at() { return created_at; }
    public Timestamp getLoginTime() { return loginTime; }

    /* ================= BACKWARDS COMPATIBILITY HELPERS ================= */
    public String getProfileImage() { return getProfile_pic(); }
    public void setProfileImage(String profile_pic) { this.profile_pic = profile_pic; }

    public String getDailyBonusClaimedDate() { return getDailyBonusClaimDate(); }
    public void setDailyBonusClaimedDate(String date) { this.dailyBonusClaimDate = date; }

    public int getWalletToken() { return (int) getTickets(); }
    public void setWalletToken(int token) { this.tickets = token; }

    /* ================= SETTERS ================= */
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setGender(String gender) { this.gender = gender; }
    public void setDob(String dob) { this.dob = dob; }
    public void setProfile_pic(String profile_pic) { this.profile_pic = profile_pic; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public void setReferredByName(String referredByName) { this.referredByName = referredByName; }
    public void setReferralUsed(boolean referralUsed) { this.referralUsed = referralUsed; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public void setCoins(long coins) { this.coins = coins; }
    public void setTickets(long tickets) { this.tickets = tickets; }
    public void setTotalReferralCoins(long totalReferralCoins) { this.totalReferralCoins = totalReferralCoins; }
    public void setTotalReferralTickets(long totalReferralTickets) { this.totalReferralTickets = totalReferralTickets; }

    public void setDailySpinCount(long dailySpinCount) { this.dailySpinCount = dailySpinCount; }
    public void setDailyScratchCount(long dailyScratchCount) { this.dailyScratchCount = dailyScratchCount; }
    public void setDaily_ads_count(long daily_ads_count) { this.daily_ads_count = daily_ads_count; }
    public void setStreak_count(long streak_count) { this.streak_count = streak_count; }

    public void setDailyBonusClaimDate(String date) { this.dailyBonusClaimDate = date; }
    public void setDailyScratchDate(String date) { this.dailyScratchDate = date; }
    public void setLastSpinDate(String date) { this.lastSpinDate = date; }
    public void setDaily_bonus(Map<String, Object> daily_bonus) { this.daily_bonus = daily_bonus; }

    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
    public void setLoginTime(Timestamp loginTime) { this.loginTime = loginTime; }

    /* ================= HELPER METHODS ================= */
    public String getFormattedCreatedAt() {
        if (created_at == null) return "";
        return android.text.format.DateFormat
                .format("dd MMM yyyy", created_at.toDate())
                .toString();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("phone", phone);
        map.put("email", email);
        map.put("gender", gender);
        map.put("dob", dob);
        map.put("profile_pic", profile_pic);
        map.put("referralCode", referralCode);
        map.put("referredBy", referredBy);
        map.put("referralUsed", referralUsed);
        map.put("fcmToken", fcmToken);

        map.put("coins", coins);
        map.put("tickets", tickets);
        map.put("totalReferralCoins", totalReferralCoins);
        map.put("totalReferralTickets", totalReferralTickets);

        map.put("dailySpinCount", dailySpinCount);
        map.put("dailyScratchCount", dailyScratchCount);
        map.put("daily_ads_count", daily_ads_count);
        map.put("streak_count", streak_count);

        map.put("dailyBonusClaimDate", dailyBonusClaimDate);
        map.put("dailyScratchDate", dailyScratchDate);
        map.put("lastSpinDate", lastSpinDate);
        map.put("daily_bonus", daily_bonus);

        map.put("created_at", created_at);
        map.put("loginTime", loginTime);

        return map;
    }
}