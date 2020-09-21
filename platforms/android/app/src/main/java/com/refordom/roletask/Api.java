package com.refordom.roletask;

import android.util.Log;

import com.google.gson.JsonObject;

public class Api extends PostGetUtil {
    private final static String TAG = "refordom_Api";
    public static String refreshPushToken(String manufacturer,String token){
        if(PostGetUtil.token == null){
            Log.i(TAG,"登录才能使用refreshPushToken！" );
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("phoneType",manufacturer);
        json.addProperty("pushToken",token);
        Log.i("PostGetUtil",json.toString());
        String res = PostGetUtil.sendPost("/H_roleplay-si/im/refreshPushToken", json.toString(),true);
        return res;
    }
    public static String getMyGroups(){
        return PostGetUtil.sendGET("/H_roleplay-si/im/group/getMyGroups",null);
    }
}
