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
    @Override
    public void onCreate(){
        super.onCreate();
        //initVivoPush();
    }
    private void initVivoPush(){
        // 在当前工程入口函数，建议在Application的onCreate函数中，添加以下代码

        PushClient pushClient = PushClient.getInstance(getApplicationContext());
        pushClient.initialize();

        // 打开push开关, 关闭为turnOffPush，详见api接入文档

//        PushClient.getInstance(getApplicationContext()).turnOnPush(new IPushActionListener() {
//
//            @Override
//
//            public void onStateChanged(int state) {
//
//                // TODO: 开关状态处理， 0代表成功
//                Log.i(TAG,"stat:"+ state);
//                String regId = PushClient.getInstance(getApplicationContext()).getRegId();
//                Log.i(TAG,"regId:" + regId);
//
//            }
//
//        });
        try {
            pushClient.checkManifest();
        }catch (VivoPushException e){
            Log.i(TAG,e.getMessage());
        }
        if(pushClient.isSupport()){
            Log.i(TAG,"支持推送");
            // 打开push开关, 关闭为turnOffPush，详见api接入文档
            pushClient.turnOnPush(new IPushActionListener() {
                @Override
                public void onStateChanged(int state) {

                    // TODO: 开关状态处理， 0代表成功
                    Log.i(TAG,"push 功能打开" + (state == 0 ? "成功" : "失败"));
                    Log.i(TAG,"state:" + state);
                    String regId = pushClient.getRegId();
                    if(!TextUtils.isEmpty(regId)){
                        Log.i(TAG,"regId:" + regId);
                    }

                }

            });
        }
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