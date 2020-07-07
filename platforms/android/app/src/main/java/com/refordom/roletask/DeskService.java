package com.refordom.roletask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 描述:
 * <p>
 * Created by allens on 2018/1/31.
 */

public class DeskService extends Service {

    private static final String TAG = "DaemonService";
    public static final int NOTICE_ID = 100;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DaemonService---->onCreate被调用，启动前台service");
        //如果API大于18，需要弹出一个可见通知
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // String CHANNEL_ID = "com.refordom.roletask.N0001";
            // String CHANNEL_NAME = "TEST";
            // Notification.Builder builder = new Notification.Builder(CHANNEL_ID,CHANNEL_NAME,NotificationManager.IMPORTANCE_HIGH);
            // builder.setSmallIcon(R.mipmap.ic_launcher);
            // builder.setContentTitle("KeepAppAlive");
            // builder.setContentText("DaemonService is runing...");
            // startForeground(NOTICE_ID, builder.build());
            // 如果觉得常驻通知栏体验不好
            // 可以通过启动CancelNoticeService，将通知移除，oom_adj值不变
            // Intent intent = new Intent(this, CancelNoticeService.class);
            // startService(intent);
        // } else {
        //     startForeground(NOTICE_ID, new Notification());
        // }

        String CHANNEL_ID = "com.refordom.roletask.N0001";
    	String CHANNEL_NAME = "TEST";
    	NotificationChannel notificationChannel = null;
    	if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
    	    notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
    	    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
     	   notificationManager.createNotificationChannel(notificationChannel);
   		}
        Intent intent = new Intent(this, MainActivity.class);
        Notification notification = new Notification.Builder(this,CHANNEL_ID).
                setContentTitle("This is content title").
                setContentText("This is content text").build();
        startForeground(1, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 如果Service被终止
        // 当资源允许情况下，重启service
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 如果Service被杀死，干掉通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mManager.cancel(NOTICE_ID);
        }
        Log.d(TAG, "DaemonService---->onDestroy，前台service被杀死");
        // 重启自己
        Intent intent = new Intent(getApplicationContext(), DeskService.class);
        startService(intent);
    }

}
