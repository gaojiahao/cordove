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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import org.apache.cordova.*;
import android.content.Intent;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.KeyEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.appplant.cordova.plugin.notification.Builder;

public class MainActivity extends CordovaActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
        addScripts();
       // keepAlive();//保活
        startService(); //显式启动，后面会修改成隐式启动
    }
    private void keepAlive(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, DeskService.class)); //前台服务
        } else {
            startService(new Intent(this, DeskService.class));
        }
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
            startService(new Intent(getBaseContext(), DSService.class));
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

}
