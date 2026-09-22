package com.app.rewardsplanet.share_earn.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.app.rewardsplanet.network.AuthTokenHelper;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * InstallAttributionHelper
 *
 * Handles first-install detection and campaign attribution.
 * Called from MainActivity on every launch — idempotent via SharedPreferences guards.
 *
 * Flow:
 *   1. On first ever launch: calls POST /api/v1/attribution/install
 *   2. After user logs in: calls POST /api/v1/attribution/register (once per install)
 */
public class InstallAttributionHelper {

    private static final String TAG = "InstallAttrib";
    private static final String PREFS_NAME = "install_attribution_prefs";
    private static final String KEY_INSTALL_RECORDED = "install_recorded";
    private static final String KEY_CLICK_ID = "attr_click_id";
    private static final String KEY_REGISTER_RECORDED = "register_recorded";
    private static final String BACKEND_URL = "https://app-backend-lutn.onrender.com";

    /**
     * Call from MainActivity.onCreate().
     * Detects first launch, records install attribution once.
     *
     * @param context  Application or Activity context
     * @param clickId  click_id extracted from deep link / intent data (may be null)
     */
    public static void recordInstallIfFirstLaunch(Context context, String clickId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_INSTALL_RECORDED, false)) {
            Log.d(TAG, "Install already attributed, skipping.");
            return;
        }
        // Store click_id for later registration step
        if (clickId != null && !clickId.isEmpty()) {
            prefs.edit().putString(KEY_CLICK_ID, clickId).apply();
        }
        prefs.edit().putBoolean(KEY_INSTALL_RECORDED, true).apply();

        final String storedClickId = prefs.getString(KEY_CLICK_ID, "");
        final String deviceId = getDeviceId(context);
        final String appVersion = getAppVersion(context);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("clickId", storedClickId != null ? storedClickId : "");
                body.put("referrer", "");
                body.put("deviceId", deviceId != null ? deviceId : "");
                body.put("appVersion", appVersion != null ? appVersion : "");
                int code = postJson(BACKEND_URL + "/api/v1/attribution/install", null, body.toString());
                Log.d(TAG, "Install attribution response: " + code + ", clickId=" + storedClickId);
            } catch (Exception e) {
                Log.e(TAG, "Install attribution failed", e);
                // Un-mark so it retries next launch
                prefs.edit().putBoolean(KEY_INSTALL_RECORDED, false).apply();
            }
        }).start();
    }

    /**
     * Call after user successfully authenticates (signs in / registers).
     * Posts registration attribution once per installation.
     *
     * @param context Application or Activity context
     */
    public static void recordRegistrationIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REGISTER_RECORDED, false)) {
            Log.d(TAG, "Registration attribution already recorded.");
            return;
        }
        prefs.edit().putBoolean(KEY_REGISTER_RECORDED, true).apply();

        final String clickId = prefs.getString(KEY_CLICK_ID, "");

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("clickId", clickId != null ? clickId : "");
                        int code = postJson(BACKEND_URL + "/api/v1/attribution/register", bearerToken, body.toString());
                        Log.d(TAG, "Registration attribution response: " + code + ", clickId=" + clickId);
                    } catch (Exception e) {
                        Log.e(TAG, "Registration attribution failed", e);
                        prefs.edit().putBoolean(KEY_REGISTER_RECORDED, false).apply();
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Auth unavailable for registration attribution: " + e.getMessage());
                prefs.edit().putBoolean(KEY_REGISTER_RECORDED, false).apply();
            }
        });
    }

    /**
     * Store click_id extracted from a tracking URL or referrer string.
     * Call this when the app receives a deep link like /r/CLK_XXXXX.
     */
    public static void storeClickIdFromUrl(Context context, String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isEmpty()) return;
        String clickId = urlOrPath;
        try {
            Uri uri = Uri.parse(urlOrPath);
            String path = uri.getPath();
            if (path != null && (path.startsWith("/r/") || path.startsWith("/track/"))) {
                String[] parts = path.split("/");
                if (parts.length >= 3) clickId = parts[parts.length - 1];
            }
            String qParam = uri.getQueryParameter("click_id");
            if (qParam != null && !qParam.isEmpty()) clickId = qParam;
        } catch (Exception ignored) {}

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CLICK_ID, clickId).apply();
        Log.d(TAG, "Stored click_id: " + clickId);
    }

    private static int postJson(String urlString, String bearerToken, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (bearerToken != null && !bearerToken.isEmpty()) {
            conn.setRequestProperty("Authorization", bearerToken);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private static String getDeviceId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }
}
