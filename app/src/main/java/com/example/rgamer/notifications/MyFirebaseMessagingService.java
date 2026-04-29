package com.example.rgamer.notifications;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.rgamer.R;
import com.example.rgamer.withdraws.activity_withdraw_success;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.*;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "reward_channel";
    private static final String GROUP_KEY = "reward_group";

    // Store last few rewards (for grouped UI)
    private static final List<String> history = new ArrayList<>();

    /* ================= TOKEN ================= */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    private void saveToken(String token) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Save locally if user not logged in
            getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token);
    }

    /* ================= MESSAGE ================= */
    @Override
    public void onMessageReceived(RemoteMessage message) {

        if (message.getData().isEmpty()) return;

        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String amount = message.getData().get("amount");

        String requestId = message.getData().get("requestId");
        String type = message.getData().get("type");

        if (title == null) title = "Reward Update 🎯";
        if (body == null) body = "Something updated 💰";
        if (amount == null) amount = "0";

        showNotification(title, body, amount, requestId, type);
    }

    /* ================= NOTIFICATION ================= */
    private void showNotification(
            String title,
            String body,
            String amount,
            String requestId,
            String type
    ) {

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createChannel(manager);

        // Save history (max 5)
        history.add("₹" + amount + " added 💰");
        if (history.size() > 5) {
            history.remove(0);
        }

        // Custom Layout
        RemoteViews views = new RemoteViews(
                getPackageName(),
                R.layout.notification_ui
        );

        views.setTextViewText(R.id.txtTitle, title);
        views.setTextViewText(R.id.txtMessage, body);
        views.setImageViewResource(R.id.imgIcon, R.drawable.app_icon);
        views.setImageViewResource(R.id.imgRight, R.drawable.app_icon);

        // Click Intent
        Intent intent = new Intent(this, activity_withdraw_success.class);
        intent.putExtra(activity_withdraw_success.EXTRA_REQUEST_ID, requestId);
        intent.putExtra(activity_withdraw_success.EXTRA_TYPE, type);
        intent.putExtra(activity_withdraw_success.EXTRA_AMOUNT, "₹" + amount);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Individual Notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.app_icon)
                        .setCustomContentView(views)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setGroup(GROUP_KEY)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify(new Random().nextInt(), builder.build());

        // Group Summary
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                .setSummaryText("Recent rewards");

        for (String line : history) {
            inboxStyle.addLine(line);
        }

        NotificationCompat.Builder summary =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.app_icon)
                        .setContentTitle("Rewards Update 💰")
                        .setStyle(inboxStyle)
                        .setGroup(GROUP_KEY)
                        .setGroupSummary(true)
                        .setAutoCancel(true);

        manager.notify(1000, summary.build());
    }

    /* ================= CHANNEL ================= */
    private void createChannel(NotificationManager manager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rewards",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.enableVibration(true);
            channel.setDescription("Reward and Withdraw updates");

            manager.createNotificationChannel(channel);
        }
    }
}