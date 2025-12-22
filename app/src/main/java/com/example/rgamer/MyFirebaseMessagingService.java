package com.example.rgamer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "redeem_status_channel";

    /* ================= TOKEN ================= */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    private void saveToken(String token) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token);
    }

    /* ================= MESSAGE ================= */
    @Override
    public void onMessageReceived(RemoteMessage message) {

        if (message.getNotification() == null) return;

        String title = message.getNotification().getTitle();
        String body = message.getNotification().getBody();

        // Data payload (from Cloud Function)
        String requestId = message.getData().get("requestId");
        String type = message.getData().get("type");
        String amount = message.getData().get("amount");

        showNotification(title, body, requestId, type, amount);
    }

    /* ================= NOTIFICATION ================= */
    private void showNotification(
            String title,
            String body,
            String requestId,
            String type,
            String amount
    ) {

        createNotificationChannel();

        Intent intent = new Intent(this, activity_withdraw_success.class);
        intent.putExtra(activity_withdraw_success.EXTRA_REQUEST_ID, requestId);
        intent.putExtra(activity_withdraw_success.EXTRA_TYPE, type);
        intent.putExtra(activity_withdraw_success.EXTRA_AMOUNT, "₹" + amount);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.app_icon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    /* ================= CHANNEL ================= */
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Redeem Status",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Redeem and Withdraw status updates");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
