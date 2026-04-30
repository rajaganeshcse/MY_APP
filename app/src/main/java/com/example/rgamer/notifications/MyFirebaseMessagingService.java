package com.example.rgamer.notifications;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.rgamer.R;
import com.example.rgamer.withdraws.activity_withdraw_success;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "reward_channel";

    @Override
    public void onMessageReceived(RemoteMessage message) {

        Log.d("FCM_TEST", "Message received: " + message.getData());

        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) return;

        String title = get(data, "title", "Reward Update 🎯");
        String body = get(data, "body", "Something updated 💰");
        String amount = get(data, "amount", "0");
        String type = get(data, "type", "general");
        String requestId = get(data, "requestId", "");


        try {
            showNotification(title, body, amount, type, requestId);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private String get(Map<String, String> d, String k, String def) {
        return d.get(k) == null ? def : d.get(k);
    }

    private void showNotification(String title, String body,
                                  String amount, String type, String requestId) throws ClassNotFoundException {

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createChannel(manager);

        /* -------- Custom Layout -------- */
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_ui);
        views.setTextViewText(R.id.txtTitle, title);
        views.setTextViewText(R.id.txtMessage, body);
        views.setTextViewText(R.id.txtAmount, amount);

        /* -------- Click Action -------- */
        Intent intent = new Intent(this,activity_withdraw_success.class);
        intent.putExtra("amount", "₹" + amount);
        intent.putExtra("type", type);
        intent.putExtra("requestId", requestId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                new Random().nextInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        /* -------- Notification -------- */
        NotificationCompat.Builder builder
                = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.app_icon);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setCustomContentView(views);
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentIntent(pi);// ✅ FIXED ICON (IMPORTANT)
// fallback
// fallback

        manager.notify(new Random().nextInt(), builder.build());
    }

    private void createChannel(NotificationManager manager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rewards",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reward notifications");
            channel.enableVibration(true);

            manager.createNotificationChannel(channel);
        }
    }
}