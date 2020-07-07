package com.refordom.roletask;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Random;

import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamFactory;
import io.deepstream.EventListener;
import io.deepstream.LoginResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.refordom.roletask.MainActivity;

public class DSService extends Service {
   private String TAG = "DSService";
   public static final int NOTICE_ID = 100;
   private DeepstreamClient client;
   private  int progress;
   private ActionReceiver actionReceiver;
   private final Random random = new Random();
   private Intent intent = new Intent("com.refordom.roletask.RECEIVER");

   @Override
   public void onCreate() {
      super.onCreate();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
         Notification.Builder builder = createBuilder("keep App alive","Deamon Service is Run");
         startForeground(NOTICE_ID, builder.build());
      } else {
         startForeground(NOTICE_ID,new Notification());
      }
      Log.i(TAG, "onCreate");
      //注册一个服务
      registerServerReceive();
   }
   private Notification.Builder createBuilder(String title,String text){
      Notification.Builder builder = new Notification.Builder(this);
      builder.setSmallIcon(R.mipmap.ic_launcher);
      builder.setContentTitle(title);
      builder.setContentText(text);
      builder.setWhen(System.currentTimeMillis());
     //builder.setLargeIcon(R.mipmap.ic_launcher);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
         String CHANNEL_ONE_ID = "refordom.roletask.com";
         String CHANNEL_ONE_NAME = "Channel One";
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
   private void registerServerReceive(){
      actionReceiver = new ActionReceiver();
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction("com.refordom.roletask.ACTION");
      registerReceiver(actionReceiver, intentFilter);
   }
   @Nullable
   @Override
   public IBinder onBind(Intent intent) {
      Log.i(TAG, "onBind");
      return null;
   }
   @Override
   public int onStartCommand(Intent intent,int flag, int startId){
      startThead();
      return START_STICKY;
   }
   @Override
   public void onDestroy() {
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
      // 如果Service被杀死，干掉通知
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
         NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
         mManager.cancel(NOTICE_ID);
     }
     // 重启自己
     Intent intent = new Intent(getApplicationContext(), DSService.class);
     startService(intent);
   }
   public String encrypt(String dataStr) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.update(dataStr.getBytes("UTF8"));
			byte s[] = m.digest();
			String result = "";
			for (int i = 0; i < s.length; i++) {
				result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
   public void startThead(){
      new Thread(new Runnable() { 
     
         @Override
         public void run() { 
            while(true){
               String status = "null";
               progress += 1;
               if (client != null){
                  status = client.getConnectionState().toString();
               }
               //发送Action为com.example.communication.RECEIVER的广播
               intent.putExtra("status",status);
               intent.putExtra("progress", progress); 
               sendBroadcast(intent); 
                  
               try { 
                  Thread.sleep(5000); 
               } catch (InterruptedException e) { 
                  e.printStackTrace(); 
               } 

            }
          } 
        }).start();
   }
   public Boolean login(String url, String uid){
      DeepstreamFactory factory = DeepstreamFactory.getInstance();
      if (client != null){
         client.close();
      }
      try {
         client = factory.getClient(url);
         Boolean rs = doLogin(client,uid);
         if(rs){
            subscribeEvent(client,uid);
         }
         return rs;
      } catch (URISyntaxException e) {
         //TODO: handle exception
      }

      return false;
   }
   public void close(){
      if (client != null){
         client.close();
      }
   }
   private Boolean doLogin(DeepstreamClient client,String uid){
      JsonObject credentials = new JsonObject();
      credentials.addProperty("username", uid);
      LoginResult loginRs = client.login(credentials);
      if (loginRs.loggedIn()){
//         Toast.makeText(this, "登陆成功", Toast.LENGTH_LONG).show();
         return true;
      } else {
         return false;
      }
   }
   private void doNotification(JsonObject msg){
      String creatorName = msg.get("creatorName").getAsString();
      String title = String.format("来自%s的消息：",creatorName);
      String text = msg.get("content").getAsString();
      Notification.Builder builder = createBuilder(title,text);
      Notification notification = builder.build() ;
      //channelId为本条通知的id
      NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      manager.notify(NOTICE_ID,notification);
   }
   private void applyContentReceiver(Notification.Builder builder) {
      Context context = getApplicationContext();
      Intent intent = new Intent(getApplicationContext(), MainActivity.class)
              .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

      int reqCode = random.nextInt();

      PendingIntent contentIntent = PendingIntent.getActivity(
              context, reqCode, intent, FLAG_UPDATE_CURRENT);

      builder.setContentIntent(contentIntent);
   }
   private void subscribeEvent(DeepstreamClient client,String uid) {
      String md5Str = encrypt(uid);
      client.event.subscribe("roletaskIm/" + md5Str, new EventListener() {
          @Override
          public void onEvent(String eventName, Object args) {
              JsonObject parameters = (JsonObject) args;
              doNotification(parameters);
              intent.putExtra("msg", parameters.toString());
              sendBroadcast(intent);
              intent.removeExtra("msg");
          }
      });
  }
   /**
    * 广播接收器
    *
    */
   public class ActionReceiver extends BroadcastReceiver {

      @Override
      public void onReceive(Context context, Intent intent) {
         //
         String act = intent.getStringExtra("action");
         if (act != null && act.equals("login")){
            String url = intent.getStringExtra("url");
            String uid = intent.getStringExtra("uid");
            Boolean rs = login(url,uid);
            Log.i(TAG,"login rs:" + rs);
         } else if(act.equals("close")){
            close();
         } else {
            Log.i(TAG, "action is null");
         }
      }
   }
}