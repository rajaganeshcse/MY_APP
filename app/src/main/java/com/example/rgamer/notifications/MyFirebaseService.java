package com.example.rgamer.notifications;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.rgamer.Activitys.MainActivity;
import com.example.rgamer.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.*;

public class MyFirebaseService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "reward_channel";
    private static final String CHANNEL_NAME = "Rewards";
    private static final String GROUP_KEY = "reward_group";

    // Store last few rewards (for stacked UI)
    private static final List<String> rewardHistory = new ArrayList<>();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData().isEmpty()) return;

        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");
        String amount = remoteMessage.getData().get("amount");

        if (title == null) title = "Reward Update 🎯";
        if (message == null) message = "Reward credited 💰";
        if (amount == null) amount = "0";

        showRewardNotification(title, message, amount);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("TOKEN_CHECK", token);

        // TODO: Send token to backend
    }

    private void showRewardNotification(String title, String message, String amount) {

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // Maintain last 5 reward entries
        rewardHistory.add("₹" + amount + " added 💰");
        if (rewardHistory.size() > 5) {
            rewardHistory.remove(0);
        }

        // Custom Layout
        @SuppressLint("RemoteViewLayout") RemoteViews remoteViews =
                new RemoteViews(getPackageName(), R.layout.notification_reward);

        remoteViews.setTextViewText(R.id.txtTitle, title);
        remoteViews.setTextViewText(R.id.txtMessage, message);

        remoteViews.setImageViewResource(R.id.imgIcon, R.drawable.ic_coin);
        remoteViews.setImageViewResource(R.id.imgRight, R.drawable.ic_coin);

        // Click → Open App
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Individual Notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_facebook)
                        .setCustomContentView(remoteViews)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setGroup(GROUP_KEY)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(new Random().nextInt(), builder.build());

        // Group Summary (Inbox Style)
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                .setSummaryText("Recent rewards");

        for (String line : rewardHistory) {
            inboxStyle.addLine(line);
        }

        NotificationCompat.Builder summary =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_phonepe)
                        .setContentTitle("Play Games, Earn Money 🎮")
                        .setStyle(inboxStyle)
                        .setGroup(GROUP_KEY)
                        .setGroupSummary(true)
                        .setAutoCancel(true);

        manager.notify(1000, summary.build());
    }
}