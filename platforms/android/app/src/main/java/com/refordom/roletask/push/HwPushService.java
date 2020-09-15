package com.refordom.roletask.push;

import android.util.Log;

import com.huawei.hms.push.HmsMessageService;

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
    }
    @Override
    public void onTokenError(Exception e) {
        super.onTokenError(e);
        Log.i(TAG,"onTokenError:" + e.getMessage());
    }
}
