package com.example.diplomaprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends Service {
    private Timer timer;
    private TimerTask timerTask;
    private NotificationHandler notificationHandler;
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private static HandlerThread thread;
    public NotificationService() {
        notificationHandler = new NotificationHandler(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public static final int delay = 1000 * 30;
        public static final int interval = 1000 * 60 * 45;
        Context context;
        public ServiceHandler(Looper looper, Context context) {
            super(looper);
            this.context = context;
        }

        public void checkScheduler() {
            NotifyUpdate notifyUpdate = new NotifyUpdate();
            String scheduleSave = context.getResources().getString(R.string.scheduleSave);
            try {
                if(!DaysDataLoader.getInstance().currentGroup.isEmpty()) {
                    DaysDataLoader.getInstance().Load(notifyUpdate, context, scheduleSave, serviceLooper);
                }
            } catch (Exception e) {
                Log.e("NotificationService", "Fail to open file output stream");
            }
        }
    }

    @Override
    public void onCreate() {
    }

    class NotifyUpdate implements DaysDataLoader.UpdateUICallback {
        @Override
        public void callingBack(boolean isChanged) {
            if(isChanged) {
                notificationHandler.createNotification("ХНПУ", "Розклад оновлено");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == thread) {
            thread = new HandlerThread("NotificationServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            serviceLooper = thread.getLooper();
            serviceHandler = new ServiceHandler(serviceLooper, this);
            startTask();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTask();
        if (null != thread) {
            thread.quit();
            thread = null;
        }
    }

    public void startTask() {
        if(timer == null) {
            timer = new Timer();
            initializeTimerTask();
            timer.scheduleAtFixedRate(timerTask, ServiceHandler.delay, ServiceHandler.interval);
        }
    }

    public void stopTask() {
        if(timer != null) {
            timer.cancel();
        }
        if(timerTask != null) {
            timerTask.cancel();
        }
    }

    public void initializeTimerTask() {
        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    serviceHandler.checkScheduler();
                }
            };
        }
    }
}