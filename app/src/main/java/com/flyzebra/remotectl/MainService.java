package com.flyzebra.remotectl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.flyzebra.remotectl.connect.PCSocketTask;
import com.flyzebra.utils.FlyLog;


public class MainService extends Service {
    private PCSocketTask mPCSocketTask;

    private static boolean isStop = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlyLog.e("+++++++++++++++++++++++++++++++++++");
        FlyLog.e("+++++version 1.01---2020.10.28+++++");
        FlyLog.e("+++++++++++++++++++++++++++++++++++");
        FlyLog.e("++remote control sevice is start!++");
        mPCSocketTask = new PCSocketTask();
        mPCSocketTask.start();
        isStop = false;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isStop) {
                    FlyLog.d("service is running.....");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        },"ServiceDaemon");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        FlyLog.d();
        isStop = true;
        mPCSocketTask.stop();
        super.onDestroy();
    }
}
