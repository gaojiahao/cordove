package com.refordom.roletask.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.refordom.roletask.R;

/**
 * 账号同步代码，添加帐号用
 */
public class AccountHelper {
    /**
     * 添加Account，需要"android.permission.GET_ACCOUNTS"权限
     * @param context
     */
    public  static void addAccount(Context context){
        String ACCOUNT_TYPE = context.getResources().getString(R.string.account_type);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accountsType = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if(accountsType.length>0){
            Log.e("AccountHelper","账户已经存在");
            return;
        }
        //给这个账户类型添加账户
        Account account=new Account("deamon",ACCOUNT_TYPE);
        //需要"android.permission.AUTHENTICATE_ACCOUNTS"权限
        accountManager.addAccountExplicitly(account,"password",new Bundle());
    }

    /**
     * 设置账户同步，即告知系统我们需要系统为我们来进行账户同步，只有设置了之后系统才会自动去
     * 触发SyncAdapter#onPerformSync方法
     */
    public static void autoSyncAccount(Context context){
        Resources resource = context.getResources();
        String accountType = resource.getString(R.string.account_type);
        String provider = resource.getString(R.string.account_provide);
        Account account=new Account("deamon",accountType);
        //设置可同步
        ContentResolver.setIsSyncable(account,provider,1);
        //设置自动同步
        ContentResolver.setSyncAutomatically(account,provider,true);
        ContentResolver.setMasterSyncAutomatically(true);
        //设置同步周期参考值，不受开发者控制完全由系统决定何时同步，测试下来最长等了差不多几十分钟才同步一次，不同系统表现不同
        ContentResolver.addPeriodicSync(account,provider,new Bundle(),300);

    }
}
