package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShareEarnEarningsResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private EarningsData data;

    public boolean isSuccess() { return success; }
    public EarningsData getData() { return data; }

    public static class EarningsData {
        @SerializedName("totalEarnedCoins")
        private long totalEarnedCoins;

        @SerializedName("totalClicks")
        private int totalClicks;

        @SerializedName("totalConversions")
        private int totalConversions;

        @SerializedName("pendingConversions")
        private int pendingConversions;

        @SerializedName("offerPerformance")
        private List<OfferPerformance> offerPerformance;

        public long getTotalEarnedCoins() { return totalEarnedCoins; }
        public int getTotalClicks() { return totalClicks; }
        public int getTotalConversions() { return totalConversions; }
        public int getPendingConversions() { return pendingConversions; }
        public List<OfferPerformance> getOfferPerformance() { return offerPerformance; }
    }

    public static class OfferPerformance {
        @SerializedName("offerId")
        private String offerId;

        @SerializedName("title")
        private String title;

        @SerializedName("logoUrl")
        private String logoUrl;

        @SerializedName("clicks")
        private int clicks;

        @SerializedName("conversions")
        private int conversions;

        @SerializedName("earnedCoins")
        private long earnedCoins;

        public String getOfferId() { return offerId; }
        public String getTitle() { return title; }
        public String getLogoUrl() { return logoUrl; }
        public int getClicks() { return clicks; }
        public int getConversions() { return conversions; }
        public long getEarnedCoins() { return earnedCoins; }
    }
}
