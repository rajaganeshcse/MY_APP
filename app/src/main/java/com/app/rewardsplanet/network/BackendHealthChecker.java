package com.app.rewardsplanet.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BackendHealthChecker
 *
 * Utility class that pings the backend's /api/app-check endpoint and returns
 * a structured result indicating whether the backend, Firebase SDK, and
 * Firestore are all reachable.
 *
 * Usage (e.g. from SplashActivity or MainActivity):
 * <pre>
 *     BackendHealthChecker.check(new BackendHealthChecker.Callback() {
 *         {@literal @}Override
 *         public void onResult(HealthResult result) {
 *             if (result.isAllOk()) {
 *                 // proceed normally
 *             } else {
 *                 // show degraded-mode warning or retry
 *                 Log.w("Health", result.getSummary());
 *             }
 *         }
 *         {@literal @}Override
 *         public void onNetworkError(String error) {
 *             // backend unreachable — show offline message
 *         }
 *     });
 * </pre>
 */
public class BackendHealthChecker {

    private static final String TAG = "BackendHealthChecker";

    // =========================================================
    // PUBLIC CALLBACK INTERFACE
    // =========================================================

    public interface Callback {
        void onResult(HealthResult result);
        void onNetworkError(String error);
    }

    // =========================================================
    // HEALTH RESULT MODEL
    // =========================================================

    public static class HealthResult {

        private final String status;        // "OK" | "DEGRADED"
        private final String backend;       // "OK"
        private final String firebaseSdk;   // "OK" | "NOT_INITIALIZED"
        private final String firestore;     // "OK" | "TIMEOUT" | "ERROR: …"
        private final String timestamp;

        public HealthResult(String status, String backend, String firebaseSdk,
                            String firestore, String timestamp) {
            this.status      = status;
            this.backend     = backend;
            this.firebaseSdk = firebaseSdk;
            this.firestore   = firestore;
            this.timestamp   = timestamp;
        }

        public boolean isAllOk() {
            return "OK".equals(status);
        }

        public String getStatus()      { return status; }
        public String getBackend()     { return backend; }
        public String getFirebaseSdk() { return firebaseSdk; }
        public String getFirestore()   { return firestore; }
        public String getTimestamp()   { return timestamp; }

        /** One-line summary suitable for logging. */
        public String getSummary() {
            return "[AppCheck] status=" + status
                    + " | backend=" + backend
                    + " | firebase_sdk=" + firebaseSdk
                    + " | firestore=" + firestore
                    + " | ts=" + timestamp;
        }
    }

    // =========================================================
    // CHECK METHOD
    // =========================================================

    /**
     * Calls GET /api/app-check asynchronously.
     * The callback is delivered on the OkHttp thread pool (not main thread).
     * Wrap UI updates in runOnUiThread() if calling from an Activity.
     */
    public static void check(Callback callback) {
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.appCheck().enqueue(new retrofit2.Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String body = response.body() != null ? response.body().string() : "{}";
                    Log.d(TAG, "app-check response [" + response.code() + "]: " + body);

                    JSONObject json  = new JSONObject(body);
                    String status    = json.optString("status",       "UNKNOWN");
                    String backend   = json.optString("backend",      "UNKNOWN");
                    String fbSdk     = json.optString("firebase_sdk", "UNKNOWN");
                    String firestore = json.optString("firestore",    "UNKNOWN");
                    String ts        = json.optString("timestamp",    "");

                    callback.onResult(new HealthResult(status, backend, fbSdk, firestore, ts));

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing app-check response: " + e.getMessage());
                    // Treat parse failure as backend up, unknown Firebase status
                    callback.onResult(new HealthResult(
                            "DEGRADED", "OK", "UNKNOWN", "PARSE_ERROR", ""));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "app-check network failure: " + t.getMessage());
                callback.onNetworkError(t.getMessage());
            }
        });
    }

    // =========================================================
    // FAST LIVENESS PING
    // =========================================================

    /**
     * Calls GET /live/health asynchronously.
     * Invokes onAlive with true if HTTP 200, false on any error.
     */
    public static void ping(java.util.function.Consumer<Boolean> onAlive) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.liveness().enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                onAlive.accept(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "liveness ping failed: " + t.getMessage());
                onAlive.accept(false);
            }
        });
    }
}
