package com.refordom.roletask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;
import android.util.Log;

public class DSService extends Service {
   private String TAG = "DSService";
   @Override
   public void onCreate() {
      Log.i(TAG, "onCreate HOHO");
      Toast.makeText(this, "服务已经启动", Toast.LENGTH_LONG).show();
   }

   @Nullable
   @Override
   public IBinder onBind(Intent intent) {
      Log.i(TAG, "onBind");
      return null;
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
   }
}