package com.refordom.roletask;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

import io.deepstream.ConnectionState;
import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamRuntimeErrorHandler;
import io.deepstream.Event;
import io.deepstream.EventListener;
import io.deepstream.LoginResult;
import io.deepstream.Topic;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class DSService extends Service {
   private String TAG = "DSService";
   private PowerManager.WakeLock wakeLock;
   public static final int NOTICE_ID = 1;
   private String packageName;
   private Notification.Builder noticeBuiler = null;
   private SharedPreferences cacheSp;
   private String lastEvent;//订阅的事件
   private EventListener lastEventLitener;
   private DeepstreamClient client;
   private  int progress;
   private ActionReceiver actionReceiver;
   private final Random random = new Random();
   private Intent intent = new Intent("com.refordom.roletask.RECEIVER");

    @Override
   public void onCreate() {
      super.onCreate();
      Log.i(TAG, "onCreate");
       packageName = getPackageName();
       cacheSp = getSharedPreferences("dsCache", MODE_PRIVATE); //缓存
       PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
       wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
       wakeLock.acquire();
      //注册一个接收器，接收cordova要执行的动作。
       registerReceive();
   }
   /**
    * 注册前台服务
    */
   private void setForegroud(){

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
         Notification.Builder builder = createBuilder(packageName + ".dsService","dsService");
//         builder.setSmallIcon(R.mipmap.ic_launcher); //如果不设置图标，就不会显示真实标题和内容
//         builder.setContentTitle("keep App alive");
//         builder.setContentText("Deamon Service is Run");
         startForeground(NOTICE_ID, builder.build());
         // 可以通过启动CancelNoticeService，将通知移除，oom_adj值不变
         Intent intent = new Intent(getApplicationContext(), CancelNoticeService.class);
         startService(intent);
         Log.i(TAG,"start CancalNoticeService");
      } else {
         startForeground(NOTICE_ID,new Notification());
      }
   }
   /**
    * 判断是否运行在前台
    * @return
    */
   public boolean isAppInForeground() {
      ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
      List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
      if (!tasks.isEmpty()) {
         ComponentName topActivity = tasks.get(0).topActivity;
//         Log.i(TAG,"topActivity:" + topActivity.getClassName() + ",packageName:"+packageName);
         if (topActivity.getPackageName().equals(packageName) && topActivity.getClassName().equals("com.refordom.roletask.MainActivity")) {
            SharedPreferences sp = getSharedPreferences("activityStatus", MODE_MULTI_PROCESS);
//            Boolean hasKey = sp.contains("isPause");
            Boolean isPause = sp.getBoolean("isPause",true);
//            Log.i(TAG,"haskey:" + hasKey.toString());
//            Log.i(TAG,"isPause:" + isPause.toString());
             return !isPause;
         }
      }
      return false;
   }
   /**
    * 消息构造器
    * @param CHANNEL_ONE_ID
    * @param CHANNEL_ONE_NAME
    * @return
    */
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
   /**
    * 注册一个接收器，接收登陆退出指示。
    */
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
      startThead();//判断用户登陆状态，用于登陆
      Log.i(TAG,"onStartCommand");
      setForegroud();//设置前台服务
      return START_STICKY;
   }
   @Override
   public void onDestroy() {
      wakeLock.release();
      unregisterReceiver(actionReceiver);
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
      // 如果Service被杀死，干掉通知
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//         NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//         mManager.cancel(NOTICE_ID);
//     }
      close();
     // 重启自己
     Intent intent = new Intent(getApplicationContext(), DSService.class);
     startService(intent);
   }
   /**
    * 加密用户id.
    * @param dataStr
    * @return
    */
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
               Log.i(TAG,"progress:" + String.format("%d",progress) + ",status:" + status);
               try {
                  Thread.sleep(5000); 
               } catch (InterruptedException e) { 
                  e.printStackTrace(); 
               } 

            }
          } 
        }).start();
   }
   /**
    * 自动登陆
    */
   public void autoLogin(){
      String uid = cacheSp.getString("uid",null);
      if(uid != null){
          String  url = cacheSp.getString("url","");
          Log.i(TAG,"autoLogin,url:" + url + ",uid:" + uid);
          login(url,uid);
      } else {
         Log.i(TAG,"uid is null");
      }
   }
   public Boolean login(String url, String uid){
      cacheSp.edit().putString("url",url).putString("uid",uid).commit();
      if (client != null){
         //client.event.unsubscribe(lastEvent,lastEventLitener);
         client.close();
      }
      try {
          Log.i(TAG,"connect,dsUrl:" + url);
         client = new DeepstreamClient(url);
         ConnectionState state = client.getConnectionState();
         client.setRuntimeErrorHandler(new DeepstreamRuntimeErrorHandler() {
             @Override
             public void onException(Topic topic, Event event, String s) {
                Log.i(TAG,"ds error: event," + event.name() + "| string," + s);
             }
         });
         Log.i(TAG,"connect Status:" + state.toString());
         Boolean rs = doLogin(client,uid);
         Log.i(TAG,String.format("login status:%b",rs));
         if(rs){
            subscribeEvent(client,uid);
            Log.i(TAG,"login success!");
         } else {
            Log.i(TAG,"login fail!");
            close(true);
         }
         return rs;
      } catch (URISyntaxException e) {
         //TODO: handle exception
         Log.i(TAG,"login Exception");
      }

      return false;
   }
   public void close(){
       close(false);
   }
   public void close(boolean isRemoveUid){
      if (isRemoveUid) cacheSp.edit().remove("uid").commit();
      if (client != null){
         client.event.unsubscribe(lastEvent,lastEventLitener);
         client.close();
         client = null;
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
   /**
    * 根据消息显示通知
    * @param msg
    */
   private void doNotification(JsonObject msg){
      int imType = msg.get("imType").getAsInt();
      if(imType != 1 && imType != 2 && imType != 3 && imType != 4) return;//这一类是消息
      String creatorName = msg.has("creatorName") ? msg.get("creatorName").getAsString() : "";
      String title = String.format("来自%s的消息：",creatorName);
      String content = msg.get("content").getAsString();
      String text = content2Text(content,imType);
      String groupId = msg.get("groupId").getAsString();
      int isMySelf = msg.get("isMySelf").getAsInt();
      if(isMySelf == 1){
         Log.i(TAG,"自已发的消息，不再发通知！");
         return;
      } else {
          Log.i(TAG,"msg:" + msg.toString());
      }
      Log.i(TAG,"msg:" + text);
      if (noticeBuiler == null){
         noticeBuiler = createBuilder(packageName + ".notice","消息通知");
         noticeBuiler.setSmallIcon(R.mipmap.ic_launcher);// 如果不设置图标，会引发异常
      }
      noticeBuiler.setContentTitle(title);
      noticeBuiler.setContentText(text);
      applyContentReceiver(noticeBuiler,groupId);
      //channelId为本条通知的id
      NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      manager.notify(100,noticeBuiler.build());
   }
   /**
    * 消息内容转通知内容
    * @param content
    * @param imType
    * @return
    */
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
            break;
         case 3:
            JsonArray arr = contentObj.getAsJsonArray();
            for(JsonElement msg:arr){
               JsonObject m = msg.getAsJsonObject();
               int subImType = m.get("imType").getAsInt();
               if (subImType == 1){
                  text += m.get("content").getAsString();
               } else if(subImType == 2){
                  text += "[图片]";
               }  else if(subImType == 4){
                  text += "[文件]" + m.get("content").getAsJsonObject().get("content").getAsString();
               }
            }
            break;
         case 4:
            text = "[文件]" + contentObj.getAsJsonObject().get("content").getAsString();
            break;
      }
      return text;
   }
   /**
    * 设置通知被点击时要发送的意图
    * @param builder
    * @param groupId
    */
   private void applyContentReceiver(Notification.Builder builder,String groupId){
       Context context = getApplicationContext();

       int reqCode = random.nextInt();
       Intent intent = new Intent(getBaseContext(), MainActivity.class);

       Bundle bundle = new Bundle();
       bundle.putString("groupId", groupId);
       intent.putExtras(bundle);

       PendingIntent contentIntent = PendingIntent.getActivity(
               context, reqCode, intent, FLAG_UPDATE_CURRENT);
       builder.setContentIntent(contentIntent);
   }
   /**
    * 订阅deepstream并根据消息显示通知。
    * @param client
    * @param uid
    */
   private void subscribeEvent(DeepstreamClient client,String uid) {
      String md5Str = encrypt(uid);
      lastEvent = "roletaskIm/" + md5Str;
      client.event.subscribe(lastEvent, lastEventLitener = new EventListener() {
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
            close(true);
         } else {
            Log.i(TAG, "action is null");
         }
      }
   }
}