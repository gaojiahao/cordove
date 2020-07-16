package com.refordom.roletask;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

import io.deepstream.ConnectionState;
import io.deepstream.DeepstreamClient;
import io.deepstream.EventListener;
import io.deepstream.LoginResult;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.refordom.roletask.MainActivity;

public class DSService extends Service {
   private String TAG = "DSService";
   public static final int NOTICE_ID = 100;
   private String packageName;
   private Notification.Builder noticeBuiler = null;
   private String CacheTag = "dsCache";
   private DeepstreamClient client;
   private  int progress;
   private ActionReceiver actionReceiver;
   private final Random random = new Random();
   private Intent intent = new Intent("com.refordom.roletask.RECEIVER");

   @Override
   public void onCreate() {
      super.onCreate();
      Log.i(TAG, "onCreate");
      //注册一个接收器，接收cordova要执行的动作。
      packageName = getPackageName();
      registerReceive();
   }
   private void setForegroud(){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
         Notification.Builder builder = createBuilder("refordom.roletask.com.dsService","dsService");
         //  builder.setSmallIcon(R.mipmap.ic_launcher); 如果不设置图标，就不会显示真实标题和内容
         builder.setContentTitle("keep App alive");
         builder.setContentText("Deamon Service is Run");
         startForeground(1, builder.build());
      } else {
         startForeground(1,new Notification());
      }
   }
   public boolean isAppInForeground() {
      ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
      List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
      if (!tasks.isEmpty()) {
         ComponentName topActivity = tasks.get(0).topActivity;
         if (topActivity.getPackageName().equals(packageName)) {
            return true;
         }
      }
      return false;
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
   private void registerReceive(){
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
      setForegroud();//设置前台服务
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
      close();
      stopForeground(true);//取消前台通知
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
               } else {
                  autoLogin();
               }
               //发送Action为com.example.communication.RECEIVER的广播
               intent.putExtra("status",status);
               intent.putExtra("progress", progress);
               Log.i(TAG,"progress:" + String.format("%d",progress) + ",status:" + status);
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
   public void autoLogin(){
      SharedPreferences sp = getSharedPreferences(CacheTag, MODE_PRIVATE);
      String uid = sp.getString("uid",null);
      if(uid != null){
          String  url = sp.getString("url","");
          Log.i(TAG,"autoLogin,url:" + url + ",uid:" + uid);
          login(url,uid);
      }
   }
   public Boolean login(String url, String uid){
      SharedPreferences sp = getSharedPreferences(CacheTag, MODE_PRIVATE);
      sp.edit().putString("url",url).putString("uid",uid).commit();
      if (client != null){
         client.close();
      }
      try {
          Log.i(TAG,"connect,dsUrl:" + url);
         client = new DeepstreamClient(url);
         ConnectionState state = client.getConnectionState();
         Log.i(TAG,"connect Status:" + state.toString());
         Boolean rs = doLogin(client,uid);
         Log.i(TAG,String.format("login status:%b",rs));
         if(rs){
            subscribeEvent(client,uid);
            Log.i(TAG,"login success!");
         } else {
            Log.i(TAG,"login fail!");
            close();
         }
         return rs;
      } catch (URISyntaxException e) {
         //TODO: handle exception
         Log.i(TAG,"login Exception");
      }

      return false;
   }
   public void close(){
      SharedPreferences sp = getSharedPreferences(CacheTag, MODE_PRIVATE);
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
      int imType = msg.get("imType").getAsInt();
      if(imType != 1 && imType != 2 && imType != 3 && imType != 4) return;//这一类是消息
      String creatorName = msg.has("creatorName") ? msg.get("creatorName").getAsString() : "";
      String title = String.format("来自%s的消息：",creatorName);
      String content = msg.get("content").getAsString();
      String text = content2Text(content,imType);
      String groupId = msg.get("groupId").getAsString();
      Boolean isMySelf = msg.get("isMySelf").getAsBoolean();
      if(isMySelf){
         Log.i(TAG,"自已发的消息，不再发通知！");
         return;
      }
      Log.i(TAG,"msg:" + text);
      if (noticeBuiler == null){
         noticeBuiler = createBuilder("refordom.roletask.com.notice","消息通知");
         noticeBuiler.setSmallIcon(R.mipmap.ic_launcher);// 如果不设置图标，会引发异常
      }
      noticeBuiler.setContentTitle(title);
      noticeBuiler.setContentText(text);
      applyContentReceiver(noticeBuiler,groupId);
      //channelId为本条通知的id
      NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      manager.notify(NOTICE_ID,noticeBuiler.build());
   }
   private String content2Text(String content, int imType){
      String text = "";
      JsonElement contentObj = null;
      if(imType != 1){
         contentObj =  (new Gson()).fromJson(content,JsonElement.class);
      }
      switch(imType){
         case 1:
           text = content;
           break;
         case 2:
            text = "[图片]";
            JsonArray arr = contentObj.getAsJsonArray();
            for(JsonElement msg:arr){
               JsonObject m = msg.getAsJsonObject();
               int subImType = m.get("imType").getAsInt();
               if (subImType == '1'){
                  text += m.get("content").getAsString();
              } else if(subImType == '2'){
                  text += "[图片]";
              }  else if(subImType == '4'){
                  text += "[文件]" + m.get("content").getAsString();
              }
            }
            break;
         case 3:
            break;
         case 4:
            text = "[文件]" + contentObj.getAsJsonObject().get("content").getAsString();
            break;
      }
      return text;
   }
   private void applyContentReceiver(Notification.Builder builder,String groupId) {
      Context context = getApplicationContext();
      int reqCode = random.nextInt();
      Intent intent = new Intent(getBaseContext(),MainActivity.class);
      Bundle bundle = new Bundle();
      bundle.putString("groupId",groupId);
      intent.putExtras(bundle);
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
              if(!isAppInForeground()){
                 doNotification(parameters);
              } else {
                 Log.i(TAG,"app正在运行,不再发通知！");
              }
              intent.setAction("receiveMsg");
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
         Log.i(TAG,"onReceive ,action:" + act);
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