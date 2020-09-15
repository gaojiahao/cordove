package com.refordom.roletask.push;

import android.content.Context;
import android.util.Log;

import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;

public class VivoPushMessageReceiverImpl extends OpenClientPushMessageReceiver {
    private static  final String TAG = "VivoPushReceive";
    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage upsNotificationMessage) {
        Log.i(TAG,"onNotificationMessageClicked");
    }

    @Override
    public void onReceiveRegId(Context context, String s) {
        Log.i(TAG,"onReceiveRegId:" + s);
    }
}
