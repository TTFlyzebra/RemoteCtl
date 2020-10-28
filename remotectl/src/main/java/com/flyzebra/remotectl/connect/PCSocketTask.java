package com.flyzebra.remotectl.connect;


import com.flyzebra.remotectl.model.FlvRtmpClient;
import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class PCSocketTask implements Runnable, ISocketListenter {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private InputStream inputStream = null;
    private Socket socket = null;
    private LocalSocketClient mVideoClient;
    private LocalSocketClient mControllerClient;

    public PCSocketTask() {
    }

    public void start() {
        isStop.set(false);
        Thread mThread = new Thread(this, "Contorller-recv");
        mThread.setDaemon(true);
        mThread.start();
    }

    private void startScrcpyServer() {
        FlyLog.e("scrcpy server start...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ScrcpyServer.start("1.14", "DEBUG", "0", "8000000", "12", "-1", "true", "-", "true", "true", "0", "true", "true", "-");
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
        FlyLog.e("scrcpy server start succes!");
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
                FlvRtmpClient.getInstance().open(FlvRtmpClient.RTMP_ADDR);
                startScrcpyServer();
                while (!isStop.get()) {
                    int len = inputStream.read(recv);
                    if (len <= 0) {
                        throw new Exception("socket recv error lenght!");
                    }
                    mControllerClient.send(recv, 0, len);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            } finally {
                FlvRtmpClient.getInstance().close();
                if(mVideoClient!=null){
                    mVideoClient.stop();
                    mVideoClient = null;
                }if(mControllerClient!=null){
                    mControllerClient.stop();
                    mControllerClient = null;
                }
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
        if (mVideoClient != null) {
            mVideoClient.stop();
        }
        if (mControllerClient != null) {
            mControllerClient.stop();
        }
    }

    @Override
    public void disConnect() {

    }
}
