package com.refordom.roletask;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.config.LazyInputStream;
import com.vivo.push.IPushActionListener;
import com.vivo.push.PushClient;
import com.vivo.push.util.VivoPushException;

import java.io.IOException;
import java.io.InputStream;

public class CustomApplication extends Application{
    private static final String TAG = "CustomApplication";
    private static boolean pause = false;
    @Override
    public void onCreate(){
        super.onCreate();
        //initVivoPush();
    }
    public boolean isPause(){
        return pause;
    }
    public void setPause(boolean isPause){
        pause = isPause;
    }
    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        if(Build.MANUFACTURER.equals("HUAWEI")){ //必须写在这里晚了，可能不行。
            AGConnectServicesConfig config = AGConnectServicesConfig.fromContext(context);
            config.overlayWith(new LazyInputStream(context) {
                public InputStream get(Context context) {
                    try {
                        return context.getAssets().open("agconnect-services.json");
                    } catch (IOException e) {
                        return null;
                    }
                }
            });
        }

    }
}