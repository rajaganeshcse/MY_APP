package com.app.rewardsplanet.profile;

import java.util.UUID;

public class ReferralUtil {

    public static String generateCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}
