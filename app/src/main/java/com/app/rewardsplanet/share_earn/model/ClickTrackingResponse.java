package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;

public class ClickTrackingResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ClickData data;

    public boolean isSuccess() { return success; }
    public ClickData getData() { return data; }

    public static class ClickData {
        @SerializedName("clickId")
        private String clickId;

        @SerializedName("trackingUrl")
        private String trackingUrl;

        @SerializedName("offerId")
        private String offerId;

        @SerializedName("expiresAt")
        private long expiresAt;

        public String getClickId() { return clickId; }
        public String getTrackingUrl() { return trackingUrl; }
        public String getOfferId() { return offerId; }
        public long getExpiresAt() { return expiresAt; }
    }
}
