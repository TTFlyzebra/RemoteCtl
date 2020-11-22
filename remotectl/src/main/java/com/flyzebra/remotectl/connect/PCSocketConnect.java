package com.flyzebra.remotectl.connect;


import com.flyzebra.remotectl.model.FlvRtmpClient;
import com.flyzebra.scrcpy.ScrcpyServer;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.SystemPropTools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class PCSocketConnect implements Runnable, ISocketListenter, FlvRtmpClient.IRtmpListener {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Socket socket = null;
    private LocalSocketClient mScreenVideoClient;
    private LocalSocketClient mControllerClient;

    public PCSocketConnect() {
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
        mScreenVideoClient = new LocalSocketClient("video");
        mScreenVideoClient.start();
        mControllerClient = new LocalSocketClient("controller");
        mControllerClient.start();
        FlyLog.e("scrcpy server start succes!");
    }

    @Override
    public void run() {
        FlyLog.d("RecvSocketTask running...");
        while (isRunning.get()) {
            FlyLog.e("RecvSocketTask is running...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isRunning.set(true);
        while (!isStop.get()) {
            try {
                String host = SystemPropTools.get("persist.sys.remotectl.ip", "192.168.1.87");
                String port = SystemPropTools.get("persist.sys.remotectl.port", "9008");
                FlyLog.d("try connect controller server:[%s:%s]...", host, port);
                socket = new Socket(host, Integer.parseInt(port));
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                byte[] recv = new byte[1024];
                FlvRtmpClient.getInstance().open();
                FlvRtmpClient.getInstance().setListener(PCSocketConnect.this);
                startScrcpyServer();
                while (!isStop.get()) {
                    int len = inputStream.read(recv);
                    if (len <= 0) {
                        throw new Exception("socket recv error lenght!");
                    }
                    mControllerClient.send(recv, 0, len);
                }
            } catch (Exception e) {
                FlyLog.e("controller connect server failed! error:" + e.toString());
            } finally {
                FlvRtmpClient.getInstance().close();
                if (mScreenVideoClient != null) {
                    mScreenVideoClient.stop();
                    mScreenVideoClient = null;
                }
                if (mControllerClient != null) {
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
                if (outputStream != null) {
                    try {
                        outputStream.close();
                        outputStream = null;
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
                    Thread.sleep(5000);
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
        while (isRunning.get()) {
            try {
                FlyLog.e("RecvSocketTask is running....");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mScreenVideoClient != null) {
            mScreenVideoClient.stop();
        }
        if (mControllerClient != null) {
            mControllerClient.stop();
        }
    }

    @Override
    public void disConnect() {

    }

    @Override
    public void writeError(int error) {
        FlyLog.e("will reset connect!");
        stop();
        start();
    }
}
