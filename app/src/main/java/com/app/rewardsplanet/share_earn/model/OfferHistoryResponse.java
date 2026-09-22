package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OfferHistoryResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<HistoryItem> data;

    public boolean isSuccess() { return success; }
    public List<HistoryItem> getData() { return data; }

    public static class HistoryItem {
        @SerializedName("clickId")
        private String clickId;

        @SerializedName("offerId")
        private String offerId;

        @SerializedName("title")
        private String title;

        @SerializedName("logoUrl")
        private String logoUrl;

        @SerializedName("category")
        private String category;

        @SerializedName("rewardCoins")
        private long rewardCoins;

        @SerializedName("status")
        private String status;

        @SerializedName("createdAt")
        private long createdAt;

        public String getClickId() { return clickId; }
        public String getOfferId() { return offerId; }
        public String getTitle() { return title; }
        public String getLogoUrl() { return logoUrl; }
        public String getCategory() { return category; }
        public long getRewardCoins() { return rewardCoins; }
        public String getStatus() { return status; }
        public long getCreatedAt() { return createdAt; }
    }
}
