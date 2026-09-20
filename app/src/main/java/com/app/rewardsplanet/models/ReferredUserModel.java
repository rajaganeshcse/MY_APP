package com.app.rewardsplanet.models;

import com.google.firebase.Timestamp;

public class ReferredUserModel {
    private String uid;
    private String name;
    private String email;
    private String profilePic;
    private long joinedAt;

    public ReferredUserModel() {}

    public ReferredUserModel(String uid, String name, String email, String profilePic, long joinedAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profilePic = profilePic;
        this.joinedAt = joinedAt;
    }

    public String getUid() { return uid != null ? uid : ""; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name != null && !name.trim().isEmpty() ? name : "App User"; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getProfilePic() { return profilePic != null ? profilePic : ""; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}
