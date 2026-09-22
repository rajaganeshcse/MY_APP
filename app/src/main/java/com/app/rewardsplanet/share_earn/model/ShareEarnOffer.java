package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ShareEarnOffer implements Serializable {
    @SerializedName("offerId")
    private String offerId;

    @SerializedName("title")
    private String title;

    @SerializedName("shortDescription")
    private String shortDescription;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("logoUrl")
    private String logoUrl;

    @SerializedName("bannerUrl")
    private String bannerUrl;

    @SerializedName("destinationUrl")
    private String destinationUrl;

    @SerializedName("rewardCoins")
    private long rewardCoins;

    @SerializedName("conversionEvent")
    private String conversionEvent;

    @SerializedName("howItWorks")
    private List<String> howItWorks;

    @SerializedName("termsAndConditions")
    private List<String> termsAndConditions;

    @SerializedName("priority")
    private int priority;

    @SerializedName("status")
    private String status;

    public String getOfferId() { return offerId; }
    public String getTitle() { return title; }
    public String getShortDescription() { return shortDescription; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLogoUrl() { return logoUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public String getDestinationUrl() { return destinationUrl; }
    public long getRewardCoins() { return rewardCoins; }
    public String getConversionEvent() { return conversionEvent; }
    public List<String> getHowItWorks() { return howItWorks; }
    public List<String> getTermsAndConditions() { return termsAndConditions; }
    public int getPriority() { return priority; }
    public String getStatus() { return status; }
}
