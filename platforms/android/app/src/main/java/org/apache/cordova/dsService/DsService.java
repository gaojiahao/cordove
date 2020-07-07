package org.apache.cordova.dsService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class DsService extends CordovaPlugin {
    public static final String TAG = "DsService";
    private Intent intent = new Intent("com.refordom.roletask.ACTION");
    private MsgReceiver msgReceiver;
    private CallbackContext msgCallbackContext = null;
    protected void pluginInitialize() {
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
        }
        return false;
    }

    private void handlerMsg(CallbackContext callbackContext) {
        msgCallbackContext = callbackContext;
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }
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
         intent.putExtra("action","login");
        intent.putExtra("url",url);
        intent.putExtra("uid",uid);
        getContext().sendBroadcast(intent);
        callbackContext.success("login");
    }
    private void close(CallbackContext callbackContext){
        intent.putExtra("action","close");
        getContext().sendBroadcast(intent);
        callbackContext.success();
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
    public void onDestroy(){
        cordova.getActivity().unregisterReceiver(msgReceiver);
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

            if(intent.hasExtra("msg")){ //得到的是消息
                String msg = intent.getStringExtra("msg");
                if (msgCallbackContext != null) {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
                    result.setKeepCallback(true);
                    msgCallbackContext.sendPluginResult(result);
                }
            }else {
                int progress = intent.getIntExtra("progress", 0);
                String status = intent.getStringExtra("status");
                String str = String.format("来自service%d,status:%s",progress,status);
                console(str);
            }
        }
     }
}
