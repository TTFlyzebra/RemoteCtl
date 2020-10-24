package com.flyzebra.record.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.flyzebra.record.utils.FlyLog;

public class ControllerService extends Service {
    public static final String MAIN_ACTION_BROADCAST_EXIT = "MAIN_ACTION_BROADCAST_EXIT";
    private RecvSocketTask recvSocketTask;

    @Override
    public void onCreate() {
        super.onCreate();
        recvSocketTask = new RecvSocketTask();
        recvSocketTask.start();
    }

    @Override
    public void onDestroy() {
        recvSocketTask.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ServiceBroadCast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            FlyLog.d("onReceive="+intent);
            String action = intent.getAction();
            if (MAIN_ACTION_BROADCAST_EXIT.equals(action)) {
                stopSelf();
            }
        }
    }

}
