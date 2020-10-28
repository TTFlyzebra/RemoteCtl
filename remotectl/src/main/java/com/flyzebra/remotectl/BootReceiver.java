package com.flyzebra.remotectl;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.flyzebra.utils.FlyLog;


/**
 * Created by FlyZebra on 2018/1/4.
 */
public class BootReceiver extends BroadcastReceiver {
    //重写onReceive方法
    @Override
    public void onReceive(Context context, Intent intent) {
        //开机自启动
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            FlyLog.d( "recvRatdMessage broadcast(boot completed) start >>>>>>>>>>>");
            startMyself(context);
        }
        //接收广播：安装更新后，自动启动自己。
        else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            FlyLog.d(intent.getData()+"");
            if((intent.getData() + "").contains("com.flyzebra.remotectl")){
                FlyLog.d( "recvRatdMessage broadcast(boot package replaced) start >>>>>>>>>>>");
                startMyself(context);
            }

        }
    }

    private void startMyself(Context context){
        Intent mainintent = new Intent();
        mainintent.setClass(context, MainService.class);
        mainintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(mainintent);
    }
}

