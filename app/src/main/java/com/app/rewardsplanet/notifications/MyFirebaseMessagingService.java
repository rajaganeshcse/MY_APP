package com.app.rewardsplanet.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.app.rewardsplanet.Activitys.MainActivity;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.withdraws.activity_withdraw_success;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RewardsFCM";
    public static final String CHANNEL_ID = "earning_notifications";
    public static final String CHANNEL_NAME = "Rewards Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FirebaseMessagingService loaded");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken called");
        updateUserFcmToken(this, token);
    }

    public static void updateUserFcmToken(Context context, String token) {
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "FCM token is empty or null");
            return;
        }

        Log.d(TAG, "FCM token received successfully");

        if (context != null) {
            new UserPref(context).setFcmToken(token);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);
            updates.put("notificationEnabled", true);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore write failed for FCM token", e));
        } else {
            Log.w(TAG, "FirebaseAuth user unavailable for token registration");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(TAG, "onMessageReceived called");

        Map<String, String> data = message.getData();

        String title = get(data, "title", message.getNotification() != null ? message.getNotification().getTitle() : "Rewards Planet 🌟");
        String body = get(data, "message", get(data, "body", message.getNotification() != null ? message.getNotification().getBody() : "Claim your daily rewards now!"));
        String body1 = get(data, "body1", "");
        String imageUrl = get(data, "imageUrl", get(data, "image", ""));
        String notificationType = get(data, "notificationType", get(data, "type", "PROMOTION"));
        String screen = get(data, "screen", "HOME");
        String deepLink = get(data, "deepLink", "");
        String notificationId = get(data, "notificationId", "");
        String customData = get(data, "customData", "");
        String amount = get(data, "amount", "");
        String requestId = get(data, "requestId", "");

        if (title == null || title.trim().isEmpty()) {
            title = "Rewards Planet 🌟";
        }
        if (body == null || body.trim().isEmpty()) {
            body = "Check out your new rewards!";
        }

        Log.d(TAG, "message received - notificationId: " + notificationId + ", screen: " + screen + ", notificationType: " + notificationType);

        showFcmNotification(title, body, body1, imageUrl, notificationType, screen, deepLink, notificationId, customData, amount, requestId);
    }

    private String get(Map<String, String> d, String k, String def) {
        if (d == null) return def;
        String val = d.get(k);
        return (val != null && !val.isEmpty()) ? val : def;
    }

    public void showFcmNotification(String title, String body, String body1, String imageUrl,
                                    String notificationType, String screen, String deepLink,
                                    String notificationId, String customData, String amount, String requestId) {
        try {
            createNotificationChannel(this);

            Intent intent;
            if ("withdraw".equalsIgnoreCase(notificationType) && amount != null && !amount.isEmpty()) {
                intent = new Intent(this, activity_withdraw_success.class);
                intent.putExtra("amount", amount.startsWith("₹") ? amount : "₹" + amount);
                intent.putExtra("type", notificationType);
                intent.putExtra("requestId", requestId);
            } else {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("notification_screen", screen);
                intent.putExtra("screen", screen);
                intent.putExtra("notificationType", notificationType);
                intent.putExtra("title", title);
                intent.putExtra("message", body);
                intent.putExtra("deepLink", deepLink);
                intent.putExtra("notificationId", notificationId);
                intent.putExtra("customData", customData);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int requestCode = (int) (System.currentTimeMillis() & 0xfffffff);

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            int iconRes = R.drawable.ic_notification;

            String fullMessage = body;
            if (body1 != null && !body1.trim().isEmpty()) {
                fullMessage = body + "\n" + body1;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(iconRes)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pi);

            Bitmap imageBitmap = downloadImageSafely(imageUrl);

            if (imageBitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(imageBitmap)
                        .setBigContentTitle(title)
                        .setSummaryText(body));
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(fullMessage)
                        .setBigContentTitle(title));
            }

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                int notifyId = (notificationId != null && !notificationId.isEmpty())
                        ? Math.abs(notificationId.hashCode())
                        : (int) (System.currentTimeMillis() & 0xfffffff);
                manager.notify(notifyId, builder.build());
                Log.d(TAG, "notification displayed");
            }
        } catch (Exception e) {
            Log.e(TAG, "notification display failed", e);
        }
    }

    private Bitmap downloadImageSafely(String urlString) {
        if (urlString == null || urlString.trim().isEmpty() || urlString.startsWith("blob:")) {
            return null;
        }
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Failed to download notification image: " + urlString, e);
            return null;
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Rewards app notifications");
                    channel.enableVibration(true);
                    channel.setShowBadge(true);
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel ready");
                }
            }
        }
    }
}