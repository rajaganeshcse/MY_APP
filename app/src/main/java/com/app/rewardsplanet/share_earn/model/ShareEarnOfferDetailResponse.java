package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;

public class ShareEarnOfferDetailResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ShareEarnOffer data;

    public boolean isSuccess() { return success; }
    public ShareEarnOffer getData() { return data; }
}
