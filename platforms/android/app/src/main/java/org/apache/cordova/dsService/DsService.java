package org.apache.cordova.dsService;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.refordom.roletask.MainActivity;
import com.refordom.roletask.R;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Random;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * This class echoes a string called from JavaScript.
 */
public class DsService extends CordovaPlugin {
    public static final String TAG = "DsService";
    private Intent intent = new Intent("com.refordom.roletask.ACTION");
    private MsgReceiver msgReceiver;
    public static final int NOTICE_ID = 101;
    private final Random random = new Random();
    private SharedPreferences statusSp;
    private SharedPreferences cacheSp;
    private CallbackContext msgCallbackContext = null;
    private CallbackContext notificationClickCallbackContext = null;
    private String notificationClickGroupId = null;
    protected void pluginInitialize() {
        Intent intent = cordova.getActivity().getIntent();
        Log.i(TAG,"pluginInitialize");
        Context c = getContext();
        cacheSp = c.getSharedPreferences("dsPluginCache", c.MODE_PRIVATE);//非共享模式，避免多个进程用一个名字，否则不会更新xml文件。
        statusSp = c.getSharedPreferences("activityStatus", c.MODE_MULTI_PROCESS);//共享模式，可以跨进程使用。
        statusSp.edit().putBoolean("isPause",false).commit();
        handlerRouter(intent);
        registryDsEvent();
    }
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.i(TAG, "execute:" + action);
        if (action.equals("getDsMsg")) {
            this.handlerMsg(callbackContext);
            return true;
        } else if(action.equals("login")){
            Log.i(TAG,"login action");
            this.login(args,callbackContext);
            return true;
        } else if(action.equals("close")){
            this.close(callbackContext);
            return true;
        } else if(action.equals("onNotificationClick")){
            this.bindNotificationClick(callbackContext);
            return true;
        } else if(action.equals("dsStatus")){
            this.getDsStatus(callbackContext);
            return  true;
        } else if(action.equals("getToken")){
            this.getToken(callbackContext);
            return  true;
        } else if(action.equals("getCache")){
            this.getCache(args,callbackContext);
        } else if(action.equals("setCache")){
            this.setCache(args,callbackContext);
        }
        return false;
    }

    private void handlerMsg(CallbackContext callbackContext) {
        msgCallbackContext = callbackContext;
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }
    private  void getDsStatus(CallbackContext callbackContext){

    }
    private void getToken(CallbackContext callbackContext){
        String token = cacheSp.getString("token",null);
        PluginResult result = new PluginResult(PluginResult.Status.OK, token);
        callbackContext.sendPluginResult(result);
    }
    private void getCache(JSONArray args,CallbackContext callbackContext) throws JSONException{
        String key = args.getString(0);
        String value = cacheSp.getString(key,null);
        Log.i(TAG,"getCache," + key + ":" + value);
        PluginResult result = new PluginResult(PluginResult.Status.OK, value);
        callbackContext.sendPluginResult(result);
    }
    private void setCache(JSONArray args,CallbackContext callbackContext) throws JSONException{
        String key = args.getString(0);
        String value = args.getString(1);
        SharedPreferences.Editor editor = cacheSp.edit();
        boolean rs = editor.putString(key, value).commit();
        Log.i(TAG,"setCache rs:" + rs);
        PluginResult result = new PluginResult(rs ? PluginResult.Status.OK : PluginResult.Status.ERROR);
        callbackContext.sendPluginResult(result);
    }
    private void handlerRouter(Intent intent){
        Bundle bundle = intent.getExtras();
        if(bundle != null && !bundle.isEmpty()){
            String groupId = bundle.getString("groupId");
            if(groupId != null) {
                if (notificationClickCallbackContext != null) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, groupId);
                    result.setKeepCallback(true);
                    notificationClickCallbackContext.sendPluginResult(result);
                    Log.i(TAG, "notificationClickCallbackContext is not null");
                } else {
                    notificationClickGroupId = groupId;
                }
                console("被拉起");
            }
            Log.i(TAG, "bundle:" + bundle.toString());

        } else {
            console("自启动");
        }
    }
    /**
     * 接收服务发过来的消息
     * */
    private void registryDsEvent(){
         //动态注册广播接收器 
         msgReceiver = new MsgReceiver(); 
         IntentFilter intentFilter = new IntentFilter();
         intentFilter.addAction("com.refordom.roletask.RECEIVER");
         cordova.getActivity().registerReceiver(msgReceiver, intentFilter); 
    }
    private void login(JSONArray args, CallbackContext callbackContext) throws JSONException {
         String url = args.getString(0);
         String uid = args.getString(1);
         String token = args.getString(2);
         intent.putExtra("action","login");
        intent.putExtra("url",url);
        intent.putExtra("uid",uid);
        intent.putExtra("t",random.nextInt());
        Log.i(TAG,"set token:" + token);
        cacheSp.edit().putString("token",token).commit();
        getContext().sendBroadcast(intent);
        Log.i(TAG,"send login Broadcast");
        callbackContext.success("login");
    }
    private void close(CallbackContext callbackContext){
        intent.putExtra("action","close");
        getContext().sendBroadcast(intent);
        callbackContext.success();
    }
    private Notification.Builder createBuilder(String title, String text){
        Context context = getContext();//Activity
        Notification.Builder builder = new Notification.Builder(context);
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
            NotificationManager manager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId(CHANNEL_ONE_ID);
            if(notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannel.getId());
                context.startActivity(intent);
                Toast.makeText(context, "通知不能关闭，请手动将通知打开", Toast.LENGTH_SHORT).show();
            }
        }
        return builder;
    }
    private void bindNotificationClick(CallbackContext callbackContext){
        notificationClickCallbackContext = callbackContext;
        PluginResult pluginResult;
        if (notificationClickGroupId != null){
            pluginResult = new PluginResult(PluginResult.Status.OK,notificationClickGroupId);
        } else {
            pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        }
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }
    /**
     * If the app is running in foreground.
     */
    private boolean isInForeground() {
        if (webView == null)
            return false;

        KeyguardManager km = (KeyguardManager) webView.getContext()
                .getSystemService(Context.KEYGUARD_SERVICE);

        //noinspection SimplifiableIfStatement
        if (km != null && km.isKeyguardLocked())
            return false;

        return webView.getView().getWindowVisibility() == View.VISIBLE;
    }
    public void console(String msg){
        String js =  String.format("console.log('%s')",msg);
        ((Activity)getContext()).runOnUiThread(new Runnable() {
            public void run() {
                webView.loadUrl("javascript:" + js);
            }
        });
    }
    @Override
    public void onNewIntent(Intent intent){
        Log.i(TAG,"onNewIntent");
        handlerRouter(intent);
    }
    @Override
    public void onDestroy(){
        cordova.getActivity().unregisterReceiver(msgReceiver);
    }
    public void onPause(boolean multitasking) {
        statusSp.edit().putBoolean("isPause",true).commit();
        Log.i("DSService","on Pause:");
    }
    public void onResume(boolean multitasking) {
        statusSp.edit().putBoolean("isPause",false).commit();
        Log.i("DSService","on Resume");
    }
    /**
     * Returns the context of the activity.
     */
    private Context getContext () {
        return cordova.getActivity();
    }
     /**
      * 广播接收器
      *
      */
      public class MsgReceiver extends BroadcastReceiver {
  
        @Override
        public void onReceive(Context context, Intent intent) {
            //

            if(intent.getAction().equals("receiveMsg")) { //得到的是消息
                String msg = intent.getStringExtra("msg");
                if (msgCallbackContext != null) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
                    result.setKeepCallback(true);
                    msgCallbackContext.sendPluginResult(result);
                }
            }
        }
     }
}
