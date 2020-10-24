package com.flyzebra.record.service;

import com.flyzebra.record.utils.FlyLog;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecvSocketTask implements Runnable {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void run() {
        FlyLog.d("RecvSocketTask running...");
        isRunning.set(true);
        try {
            String host = "192.168.8.140";
            int port = 9008;
            Socket socket = new Socket(host, port);
            InputStream inputStream = socket.getInputStream();
            byte[] recv = new byte[1024];
            while (!isStop.get()){
                FlyLog.d("RecvSocketTask recv...");
                int len = inputStream.read(recv);
                FlyLog.d("recv data len=%d", len);
            }
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
        isRunning.set(false);
        FlyLog.d("RecvSocketTask exit...");
    }

    public void start() {
        isStop.set(false);
        Thread mThread = new Thread(this, "Contorller-recv");
        mThread.setDaemon(true);
        mThread.start();
    }

    public void stop() {
        isStop.set(true);
    }
}
