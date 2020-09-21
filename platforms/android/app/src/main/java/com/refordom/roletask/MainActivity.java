/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.refordom.roletask;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import org.apache.cordova.*;
import org.json.JSONObject;

import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessaging;
import com.refordom.roletask.authentication.AccountHelper;
import com.tencent.smtt.sdk.QbSdk;
import com.vivo.push.IPushActionListener;
import com.vivo.push.PushClient;
import com.vivo.push.util.VivoPushException;
import com.xiaomi.mipush.sdk.MiPushClient;


public class MainActivity extends CordovaActivity
{
    public static String TAG = "MainActivity";
    public static final String REFORDOM_CACHE = "dsPluginCache";
    private AlertDialog dialog = null;
    private static final int NOT_NOTICE = 2;//如果勾选了不再询问
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;//写入权限requestCode
    private HashMap<String,String> permisssionMap = new HashMap<String, String>();;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }
        initPermission();//动态权限申请
//        requestWriteSettings();//申请修改设置权限
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
        //addScripts();
       // keepAlive();//保活
       // x5test();
        initPush();//初始化厂商推送
        AccountHelper.addAccount(this);//添加账户
        AccountHelper.autoSyncAccount(this);//调用系统自动同步
    }
    private void keepAlive(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, DeskService.class)); //前台服务
        } else {
            startService(new Intent(this, DeskService.class));
        }
    }
    private void initPush(){
        PostGetUtil.baseUrl  = getResources().getString(R.string.baseURL);//初始化url;
        String manufacturer = Build.MANUFACTURER;
        Log.i(TAG,"manufacturer:" + manufacturer);
        if(manufacturer.equals("HUAWEI")){
            initHwPush();
        }
        else if(manufacturer.equals("vivo")){
            initVivoPush();
        } else if(manufacturer.equals("Xiaomi") && shouldInit()){
            Resources res = getResources();
            String APP_ID = res.getString(R.string.mi_app_id);
            String APP_KEY = res.getString(R.string.mi_app_key);
            MiPushClient.registerPush(this, APP_ID, APP_KEY);
        } else {
            startService(); //显式启动,前台保活代码。
        }
    }
    private void initPermission(){
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.READ_PHONE_STATE
        };
        permisssionMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,"存储权限");//tbs需要的
        permisssionMap.put(Manifest.permission.READ_PHONE_STATE,"电话(读取手机状态)权限");//
        permisssionMap.put(Manifest.permission.GET_ACCOUNTS,"联系人(读取账户信息,用于帐号同步)权限");//tbs需要的

        checkPremissions(permissions,null);
    }
    /**
     * 申请权限
     */
    private void requestWriteSettings()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            //大于等于23 请求权限
            if ( !Settings.System.canWrite(getApplicationContext()))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("提示");
                builder.setMessage("我们的应用需要您授权\"修改系统设置\"的权限,请点击\"设置\"确认开启");

                // 拒绝, 退出应用
                builder.setNegativeButton("取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });

                builder.setPositiveButton("去设置",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS );
                            }
                        });

                builder.setCancelable(false);

                builder.show();

            }
        }else{
            //小于23直接设置
        }
    }
    private Boolean checkPremissions(String[] permissions,int[] grantResults){
        List<String> alertPermisson = new ArrayList<String>();
        List<String> requestPermission = new ArrayList<String>();
        for (int i = 0; i < permissions.length; i++){
            int grantResult = grantResults == null ? ContextCompat.checkSelfPermission(this,permissions[i]) : grantResults[i];
            if(grantResult != PackageManager.PERMISSION_GRANTED){

                //如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])){
                    alertPermisson.add(permisssionMap.get(permissions[i]));
                } else {
                    requestPermission.add(permissions[i]);
                }
            }
        }
        int requestPermissionSize = requestPermission.size();
        if(alertPermisson.size() != 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            StringBuilder permissionString = new StringBuilder();
            for(String str : alertPermisson){
                permissionString.append(str +"\n");
            }
            builder.setTitle("警告")
                    .setMessage("缺少以下权限：\n" + permissionString.toString() +  "请手动打开权限。")
                    .setPositiveButton("去允许", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(dialog != null && dialog.isShowing()){
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);//注意就是"package",不用改成自己的包名
                                intent.setData(uri);
                                startActivityForResult(intent, NOT_NOTICE);
                            }
                        }
                    });

            dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return false;
        } else if(requestPermissionSize != 0){
            String[] requestPermissionArray =  (String[]) requestPermission.toArray(new String[requestPermissionSize]);
            ActivityCompat.requestPermissions(this,requestPermissionArray,1);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
           checkPremissions(permissions,grantResults);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==NOT_NOTICE){
            initPermission();//由于不知道是否选择了允许所以需要再次判断
        } else if(requestCode == REQUEST_CODE_WRITE_SETTINGS){
            requestWriteSettings();
        }
    }

    private void initHwPush(){//hw
        MainActivity content = this;
        new Thread() {
            @Override
            public void run() {
                try {

                    AGConnectServicesConfig agConnectServicesConfig = AGConnectServicesConfig.fromContext(content);
//                    try {
//                        InputStream localInputStream = getResources().getAssets().open("agconnect-services.json");
//                        agConnectServicesConfig.overlayWith(localInputStream);
//                    } catch (IOException localIOException) {
//                        Log.i(TAG,localIOException.getLocalizedMessage());
//                    }
//                    agConnectServicesConfig.overlayWith(new LazyInputStream(getBaseContext()) {
//                        public InputStream get(Context context) {
//                            try {
//                                return context.getAssets().open("agconnect-services.json");
//                            } catch (IOException e) {
//                                return null;
//                            }
//                        }
//                    });
                    String appId = agConnectServicesConfig.getString("client/app_id");
                    Log.i(TAG,"appid:" + appId);
                    String token = HmsInstanceId.getInstance(getApplicationContext()).getToken(appId, "HCM");
                    Log.i(TAG, "get token:" + token);
                    if(!TextUtils.isEmpty(token)) {
                        SharedPreferences sp = getSharedPreferences(MainActivity.REFORDOM_CACHE, MODE_PRIVATE);
                        sp.edit().putString("pushToken",token);
                       Api.refreshPushToken(Build.MANUFACTURER,token);
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "get token failed, " + e);
                }
            }
        }.start();
        HmsMessaging hmsMessaging = HmsMessaging.getInstance(this);
        if(hmsMessaging.isAutoInitEnabled()){
            Log.i(TAG,"hmsMessaging autoInitEnabled");
        } else {
            hmsMessaging.setAutoInitEnabled(true);
        }
    }
    private void refreashPushToken(String manufacturer,String token){
        SharedPreferences cacheSp = getSharedPreferences(REFORDOM_CACHE, MODE_PRIVATE);//非共享模式，避免多个进程用一个名字，否则不会更新xml文件。
        String tokenJson = cacheSp.getString("token",null);
        if(tokenJson != null){
            JsonObject contentObj = (new Gson()).fromJson(tokenJson, JsonObject.class);
            String httpToken = contentObj.get("token").getAsString();
            PostGetUtil.token = httpToken;
//            AsyncTask<String, Void, String> rs = new RefreshTokenTask();
//            rs.execute(manufacturer, token);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    String res = Api.refreshPushToken(manufacturer,token);
                    Log.i(TAG,"post rs:" + res);
                }
            }).start();
        } else {
            Log.i(TAG,"token is null");
        }

    }
    private void initVivoPush(){
        // 在当前工程入口函数，建议在Application的onCreate函数中，添加以下代码
        PushClient pushClient = PushClient.getInstance(getApplicationContext());
        pushClient.initialize();
        try {
            pushClient.checkManifest();
        }catch (VivoPushException e){
             Log.i(TAG,e.getMessage());
        }
        if(pushClient.isSupport()){
            Log.i(TAG,"支持vivo推送");
            // 打开push开关, 关闭为turnOffPush，详见api接入文档
            pushClient.turnOnPush(new IPushActionListener() {
                @Override
                public void onStateChanged(int state) {

                    // TODO: 开关状态处理， 0代表成功
                    Log.i(TAG,"push 功能打开" + (state == 0 ? "成功" : "失败"));
                    String regId = pushClient.getRegId();
                    if(!TextUtils.isEmpty(regId)){
                        Log.i(TAG,"regId:" + regId);
                        refreashPushToken("vivo",regId);
                    }
                }
            });
        }
    }
    private boolean shouldInit() {//小米需要的
        ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = getApplicationInfo().processName;
        int myPid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }
    public void x5test(){
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

            @Override
            public void onViewInitFinished(boolean arg0) {
              // TODO Auto-generated method stub
              //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
              Log.d("app", " onViewInitFinished is " + arg0);
            }
      
            @Override
            public void onCoreInitFinished() {
              // TODO Auto-generated method stub
            }
          };
      
      QbSdk.initX5Environment(this, cb);
    }
    //添加js变量支持
    private void addScripts(){
        Properties properties = new Properties();
        try{
            InputStream in = this.getAssets().open("appConfig");
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String serverUrl = properties.getProperty("serverUrl","");
        String js = String.format("javascript:console.log('token:%s');",serverUrl);
        appView.loadUrlIntoView(js,false);
    }
    public void startService(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getBaseContext(), DSService.class));
        } else {
            startService(new Intent(getApplicationContext(), DSService.class));
        }
    }
    public  void stopService(){
        stopService(new Intent(getBaseContext(),DSService.class));
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event){
//
//        if(keyCode == KeyEvent.KEYCODE_BACK){
//            moveTaskToBack(true);
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//    public class RefreshTokenTask extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            String test = PostGetUtil.sendGET("/H_roleplay-si/im/group/getMyGroups",null);
//            Log.i(TAG,"test:" + test);
//            String res = PostGetUtil.sendPost("/refreshPushToken", "manufacturer=" + strings[0] + "&token=" + strings[1]);
//            return res;
//        }
//    }
}
