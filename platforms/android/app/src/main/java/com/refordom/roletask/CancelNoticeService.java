package com.refordom.roletask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * 描述:
 * <p>
 * Created by allens on 2018/1/31.
 */

public class CancelNoticeService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("CancelNoticeService", "onCreate");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Notification.Builder builder = createBuilder(getPackageName() + ".dsService","dsService");
//            builder.setSmallIcon(R.mipmap.ic_launcher);
//            builder.setContentTitle("cancel");
//            builder.setContentText("cancel Service");
            startForeground(1, builder.build());
            // 开启一条线程，去移除DaemonService弹出的通知
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 延迟1s
                    SystemClock.sleep(1000);
                    // 取消CancelNoticeService的前台
                    stopForeground(true);
                    Log.i("CancleNoticeService","cancal Notice");
                    // 移除DaemonService弹出的通知
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.cancel(1);
                    // 任务完成，终止自己
                    stopSelf();
                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }
    private Notification.Builder createBuilder(String CHANNEL_ONE_ID ,String CHANNEL_ONE_NAME){
        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId(CHANNEL_ONE_ID);
            if(notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannel.getId());
                startActivity(intent);
                Toast.makeText(this, "通知不能关闭，请手动将通知打开", Toast.LENGTH_SHORT).show();
            }
        }
        return builder;
    }
}
