package com.flyzebra.record.service;

import com.flyzebra.record.utils.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecvSocketTask implements Runnable {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private InputStream inputStream = null;
    private Socket socket = null;
    @Override
    public void run() {
        FlyLog.d("RecvSocketTask running...");
        isRunning.set(true);
        while (!isStop.get()) {
            try {
                FlyLog.e("try connect controller server...");
                String host = "192.168.8.140";
                int port = 9008;
                socket = new Socket(host, port);
                inputStream = socket.getInputStream();
                byte[] recv = new byte[1024];
                while (!isStop.get()) {
                    FlyLog.d("RecvSocketTask recv...");
                    int len = inputStream.read(recv);
                    FlyLog.d("recv data len=%d", len);
                    if(len<=0){
                        throw new Exception("socket recv error lenght!");
                    }
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(isStop.get()){
                break;
            }else{
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
