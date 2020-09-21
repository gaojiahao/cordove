package com.refordom.roletask.push;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.refordom.roletask.Api;
import com.refordom.roletask.MainActivity;

public class HwPushService extends HmsMessageService{
    private static final String TAG = "HwPushService";

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        super.onCreate();
    }

    @Override
    public void onNewToken(String token){
        super.onNewToken(token);
        Log.i(TAG,"token:" + token);
        SharedPreferences sp = getSharedPreferences(MainActivity.REFORDOM_CACHE, MODE_PRIVATE);
        sp.edit().putString("pushToken",token);
        Api.refreshPushToken(Build.MANUFACTURER,token);
    }
    @Override
    public void onTokenError(Exception e) {
        super.onTokenError(e);
        Log.i(TAG,"onTokenError:" + e.getMessage());
    }
}
