package com.example.rgamer.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class UserModel {

    /* ================= BASIC INFO ================= */
    private String uid;
    private String name;
    private String email;

    /* ================= WALLET ================= */
    private long coins;
    private int tickets;
    private int walletToken;

    /* ================= NOTIFICATIONS ================= */
    private String fcmToken;

    /* ================= PROFILE ================= */
    private String profile_image;
    private Timestamp created_at;

    /* ================= DAILY BONUS ================= */
    private String dailyBonusClaimedDate;

    /* ================= REFERRAL SYSTEM ================= */
    private String referralCode;
    private String referredBy;
    private boolean referralUsed;

    private long totalReferralCoins;
    private long totalReferralTickets;

    /* ================= EMPTY CONSTRUCTOR ================= */
    public UserModel() {}

    /* ================= FULL CONSTRUCTOR ================= */
    public UserModel(
            String uid,
            String name,
            String email,
            long coins,
            int tickets,
            int walletToken,
            String fcmToken,
            String profile_image,
            String dailyBonusClaimedDate,
            String referralCode,
            String referredBy,
            boolean referralUsed,
            long totalReferralCoins,
            long totalReferralTickets,
            Timestamp created_at
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.coins = coins;
        this.tickets = tickets;
        this.walletToken = walletToken;
        this.fcmToken = fcmToken;
        this.profile_image = profile_image;
        this.dailyBonusClaimedDate = dailyBonusClaimedDate;
        this.referralCode = referralCode;
        this.referredBy = referredBy;
        this.referralUsed = referralUsed;
        this.totalReferralCoins = totalReferralCoins;
        this.totalReferralTickets = totalReferralTickets;
        this.created_at = created_at;
    }

    /* ================= GETTERS ================= */
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public long getCoins() { return coins; }
    public int getTickets() { return tickets; }
    public int getWalletToken() { return walletToken; }

    public String getFcmToken() { return fcmToken; }
    public String getProfileImage() { return profile_image; }
    public String getDailyBonusClaimedDate() { return dailyBonusClaimedDate; }

    public String getReferralCode() { return referralCode; }
    public String getReferredBy() { return referredBy; }
    public boolean isReferralUsed() { return referralUsed; }

    public long getTotalReferralCoins() { return totalReferralCoins; }
    public long getTotalReferralTickets() { return totalReferralTickets; }

    public long getCreatedAt() {
        return created_at != null ? created_at.toDate().getTime() : 0;
    }

    public String getFormattedCreatedAt() {
        if (created_at == null) return "";
        return android.text.format.DateFormat
                .format("dd MMM yyyy", created_at.toDate())
                .toString();
    }

    /* ================= SETTERS (ADDED) ================= */
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    public void setCoins(long coins) { this.coins = coins; }
    public void setTickets(int tickets) { this.tickets = tickets; }
    public void setWalletToken(int walletToken) { this.walletToken = walletToken; }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setProfileImage(String profile_image) { this.profile_image = profile_image; }

    public void setDailyBonusClaimedDate(String date) { this.dailyBonusClaimedDate = date; }

    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public void setReferralUsed(boolean referralUsed) { this.referralUsed = referralUsed; }

    public void setTotalReferralCoins(long coins) { this.totalReferralCoins = coins; }
    public void setTotalReferralTickets(long tickets) { this.totalReferralTickets = tickets; }

    public void setCreatedAt(Timestamp created_at) { this.created_at = created_at; }

    /* ================= EXTRA METHODS (ADDED) ================= */

    public void safeInit() {
        if (coins < 0) coins = 0;
        if (tickets < 0) tickets = 0;
        if (walletToken < 0) walletToken = 0;

        if (referralCode == null) referralCode = "";
        if (referredBy == null) referredBy = "";
        if (profile_image == null) profile_image = "";
    }

    public boolean isNewUser() {
        return coins == 0 && tickets == 0;
    }

    public Map<String, Object> toSafeMap() {
        Map<String, Object> map = new HashMap<>();

        if (name != null) map.put("name", name);
        if (email != null) map.put("email", email);

        map.put("coins", coins);
        map.put("tickets", tickets);

        return map;
    }

    /* ================= FIRESTORE MAP ================= */
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);

        map.put("coins", coins);
        map.put("tickets", tickets);
        map.put("walletToken", walletToken);

        map.put("fcmToken", fcmToken);
        map.put("profile_image", profile_image);

        map.put("dailyBonusClaimedDate", dailyBonusClaimedDate);

        map.put("referralCode", referralCode);
        map.put("referredBy", referredBy);
        map.put("referralUsed", referralUsed);
        map.put("totalReferralCoins", totalReferralCoins);
        map.put("totalReferralTickets", totalReferralTickets);

        map.put("created_at", created_at);

        return map;
    }
}