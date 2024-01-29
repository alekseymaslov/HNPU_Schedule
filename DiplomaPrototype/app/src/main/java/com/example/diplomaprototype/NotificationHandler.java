package com.example.diplomaprototype;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHandler {
    private String CHANNEL_ID = "100355";
    private Context context;
    private NotificationManager notificationManager = null;
    private NotificationChannel channel = null;
    private int currentId = 1;
    private int maxId = 10;
    public NotificationHandler(Context inContext) {
        context = inContext;
    }

    private void createNotificationChannel() {
        CharSequence name = "HNPU Schedule";
        String description = "Schedule of lessons for students";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
    }

    private void createNotificationManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (null == channel) {
                createNotificationChannel();
            }

            CharSequence name = "tick";
            String description = "test description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void createNotification(String name, String description) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(name)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (null == notificationManager) {
            createNotificationManager();
        }
        if (null != notificationManager) {
            notificationManager.notify(currentId, notification.build());
            currentId++;
            if (currentId > maxId) {
                currentId = 1;
            }
        }
    }
}
