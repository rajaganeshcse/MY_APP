package com.app.rewardsplanet.share_earn.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.app.rewardsplanet.network.AuthTokenHelper;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * InstallAttributionHelper
 *
 * Handles first-install detection, Google Play Store Install Referrer extraction,
 * and user registration attribution.
 *
 * Flow:
 *   1. On first ever launch (SplashActivity):
 *      - Query Google Play InstallReferrerClient for 'referrer' query string (containing click_id)
 *      - Call POST /api/v1/attribution/install
 *   2. After user authenticates (activity_login or MainActivity):
 *      - Call POST /api/v1/attribution/register
 */
public class InstallAttributionHelper {

    private static final String TAG = "InstallAttrib";
    private static final String PREFS_NAME = "install_attribution_prefs";
    private static final String KEY_INSTALL_RECORDED = "install_recorded";
    private static final String KEY_CLICK_ID = "attr_click_id";
    private static final String KEY_REFERRER_RAW = "attr_referrer_raw";
    private static final String KEY_REGISTER_RECORDED = "register_recorded";
    private static final String BACKEND_URL = "https://app-backend-lutn.onrender.com";

    /**
     * Initializes install attribution on app start (e.g. from SplashActivity.onCreate()).
     * Reads Google Play Install Referrer if available on first launch.
     */
    public static void initializeAndRecordInstall(Context context) {
        final Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_INSTALL_RECORDED, false)) {
            Log.d(TAG, "Install attribution already completed, skipping.");
            return;
        }

        // Try reading Google Play Install Referrer
        try {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(appContext).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    String extractedClickId = null;
                    String rawReferrer = null;
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            ReferrerDetails details = referrerClient.getInstallReferrer();
                            rawReferrer = details.getInstallReferrer();
                            Log.d(TAG, "Play Install Referrer received: " + rawReferrer);
                            extractedClickId = extractClickId(rawReferrer);
                            referrerClient.endConnection();
                        } catch (Exception e) {
                            Log.w(TAG, "Error reading referrer details", e);
                        }
                    } else {
                        Log.d(TAG, "InstallReferrer response code: " + responseCode);
                    }

                    if (extractedClickId != null && !extractedClickId.isEmpty()) {
                        prefs.edit().putString(KEY_CLICK_ID, extractedClickId).apply();
                    }
                    if (rawReferrer != null && !rawReferrer.isEmpty()) {
                        prefs.edit().putString(KEY_REFERRER_RAW, rawReferrer).apply();
                    }

                    dispatchInstallAttribution(appContext);
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    Log.d(TAG, "InstallReferrer service disconnected. Dispatching fallback.");
                    dispatchInstallAttribution(appContext);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "InstallReferrerClient initialization failed, falling back", e);
            dispatchInstallAttribution(appContext);
        }
    }

    /**
     * Legacy / Direct method to record install with explicit clickId.
     */
    public static void recordInstallIfFirstLaunch(Context context, String clickId) {
        if (clickId != null && !clickId.trim().isEmpty()) {
            storeClickId(context, clickId.trim());
        }
        initializeAndRecordInstall(context);
    }

    /**
     * Posts install attribution to the backend.
     */
    private static void dispatchInstallAttribution(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_INSTALL_RECORDED, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_INSTALL_RECORDED, true).apply();

        final String storedClickId = prefs.getString(KEY_CLICK_ID, "");
        final String rawReferrer = prefs.getString(KEY_REFERRER_RAW, "");
        final String deviceId = getDeviceId(context);
        final String appVersion = getAppVersion(context);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("clickId", storedClickId != null ? storedClickId : "");
                body.put("referrer", rawReferrer != null ? rawReferrer : "");
                body.put("deviceId", deviceId != null ? deviceId : "");
                body.put("appVersion", appVersion != null ? appVersion : "");

                int code = postJson(BACKEND_URL + "/api/v1/attribution/install", null, body.toString());
                Log.d(TAG, "Install attribution response: " + code + ", clickId=" + storedClickId);
            } catch (Exception e) {
                Log.e(TAG, "Install attribution dispatch failed", e);
                // Allow retry on subsequent launches
                prefs.edit().putBoolean(KEY_INSTALL_RECORDED, false).apply();
            }
        }).start();
    }

    /**
     * Call after user successfully authenticates (signs in or registers).
     * Posts registration attribution once per installation.
     */
    public static void recordRegistrationIfNeeded(Context context) {
        final Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REGISTER_RECORDED, false)) {
            Log.d(TAG, "Registration attribution already recorded.");
            return;
        }
        prefs.edit().putBoolean(KEY_REGISTER_RECORDED, true).apply();

        final String clickId = prefs.getString(KEY_CLICK_ID, "");
        final String deviceId = getDeviceId(appContext);

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("clickId", clickId != null ? clickId : "");
                        body.put("deviceId", deviceId != null ? deviceId : "");

                        int code = postJson(BACKEND_URL + "/api/v1/attribution/register", bearerToken, body.toString());
                        Log.d(TAG, "Registration attribution response: " + code + ", clickId=" + clickId);
                    } catch (Exception e) {
                        Log.e(TAG, "Registration attribution dispatch failed", e);
                        prefs.edit().putBoolean(KEY_REGISTER_RECORDED, false).apply();
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Auth token unavailable for registration attribution: " + e.getMessage());
                prefs.edit().putBoolean(KEY_REGISTER_RECORDED, false).apply();
            }
        });
    }

    /**
     * Store clickId directly.
     */
    public static void storeClickId(Context context, String clickId) {
        if (clickId == null || clickId.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CLICK_ID, clickId.trim()).apply();
        Log.d(TAG, "Stored click_id: " + clickId.trim());
    }

    /**
     * Store click_id extracted from a tracking URL or Uri data.
     */
    public static void storeClickIdFromUri(Context context, Uri uri) {
        if (uri == null) return;
        String clickId = null;
        try {
            // Check query parameter ?click_id=... or ?clickId=...
            clickId = uri.getQueryParameter("click_id");
            if (clickId == null || clickId.isEmpty()) {
                clickId = uri.getQueryParameter("clickId");
            }
            if (clickId == null || clickId.isEmpty()) {
                clickId = uri.getQueryParameter("sub_id");
            }

            // Check if URI is path-based: /r/{clickId} or /track/{clickId}
            if (clickId == null || clickId.isEmpty()) {
                String path = uri.getPath();
                if (path != null && (path.startsWith("/r/") || path.startsWith("/track/"))) {
                    String[] parts = path.split("/");
                    if (parts.length >= 3) {
                        clickId = parts[parts.length - 1];
                    }
                }
            }

            // Check if referrer param is inside query
            if (clickId == null || clickId.isEmpty()) {
                String referrerParam = uri.getQueryParameter("referrer");
                if (referrerParam != null && !referrerParam.isEmpty()) {
                    clickId = extractClickId(referrerParam);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed parsing click_id from URI: " + uri, e);
        }

        if (clickId != null && !clickId.trim().isEmpty()) {
            storeClickId(context, clickId.trim());
        }
    }

    public static void storeClickIdFromUrl(Context context, String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isEmpty()) return;
        try {
            storeClickIdFromUri(context, Uri.parse(urlOrPath));
        } catch (Exception ignored) {}
    }

    public static String extractClickId(String referrer) {
        if (referrer == null || referrer.trim().isEmpty()) return null;
        try {
            String decoded = URLDecoder.decode(referrer, StandardCharsets.UTF_8.name());
            String[] params = decoded.split("&");
            for (String param : params) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim();
                    String v = kv[1].trim();
                    if ("click_id".equalsIgnoreCase(k) || "clickId".equalsIgnoreCase(k) || "sub_id".equalsIgnoreCase(k)) {
                        return v;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
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
