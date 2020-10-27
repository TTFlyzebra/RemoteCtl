package com.flyzebra.record.net;

import com.flyzebra.util.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecvSocketTask implements Runnable, ISocketListenter {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private InputStream inputStream = null;
    private Socket socket = null;
    private LocalSocketClient mVideoClient;
    private LocalSocketClient mControllerClient;

    public RecvSocketTask() {
    }

    public void start() {
        isStop.set(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ScrcpyServer.start("1.14", "DEBUG", "0", "8000000", "12", "-1", "true", "-", "true", "ture", "0", "true", "true", "-");
                    FlyLog.e("conect local socket fly_touch ok..");
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }, "ScrcpyServer").start();

        mVideoClient = new LocalSocketClient("video");
        mVideoClient.start();
        mControllerClient = new LocalSocketClient("controller");
        mControllerClient.start();

        Thread mThread = new Thread(this, "Contorller-recv");
        mThread.setDaemon(true);
        mThread.start();
    }

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
                    int len = inputStream.read(recv);
                    FlyLog.d("recv data len=%d", len);
                    if (len <= 0) {
                        throw new Exception("socket recv error lenght!");
                    }
                    mControllerClient.send(recv, 0, len);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        inputStream = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                        socket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (isStop.get()) {
                break;
            } else {
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

    public void stop() {
        isStop.set(true);
        mVideoClient.stop();
        mControllerClient.stop();
    }

    @Override
    public void disConnect() {

    }
}
