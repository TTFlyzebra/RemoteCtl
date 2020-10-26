package com.flyzebra.record.service;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.flyzebra.util.FlyLog;
import com.genymobile.scrcpy.DesktopConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecvSocketTask implements Runnable {
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Socket socket = null;
    @Override
    public void run() {
        FlyLog.d("RecvSocketTask running...");
        isRunning.set(true);
        //SCRCPY_VERSION,
        //log_level_to_server_string(params->log_level),
        //max_size_string,
        //bit_rate_string,
        //max_fps_string,
        //lock_video_orientation_string,
        //server->tunnel_forward ? "true" : "false",
        //params->crop ? params->crop : "-",
        //"true", // always send frame meta (packet boundaries + timestamp)
        //params->control ? "true" : "false",
        //display_id_string,
        //params->show_touches ? "true" : "false",
        //params->stay_awake ? "true" : "false",
        //params->codec_options ? params->codec_options : "-",
        while (!isStop.get()) {
            try {
                if(outputStream==null) {
                    FlyLog.e("try connect localserver fly_touch video...");
                    LocalSocket localSocket1 = new LocalSocket();
                    LocalSocketAddress address1 = new LocalSocketAddress(DesktopConnection.SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT);
                    localSocket1.connect(address1);

                    FlyLog.e("try connect localserver fly_touch keyevent...");
                    LocalSocket localSocket2 = new LocalSocket();
                    LocalSocketAddress address2 = new LocalSocketAddress(DesktopConnection.SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT);
                    localSocket2.connect(address2);
                    outputStream = localSocket2.getOutputStream();
                }

                FlyLog.e("try connect controller server...");
                String host = "192.168.8.140";
                int port = 9008;
                socket = new Socket(host, port);
                inputStream = socket.getInputStream();
                byte[] recv = new byte[1024];
                while (!isStop.get()) {
                    FlyLog.d("RecvSocketTask recv...");
                    int len = inputStream.read(recv);
                    if(outputStream!=null){
                        outputStream.write(recv,0,len);
                    }
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                    while (!isStop.get()) {
                        try {
                            ScrcpyServer.start("1.14", "DEBUG", "0", "8000000", "12", "-1", "true", "-", "true", "ture", "0", "true", "true", "-");
                            FlyLog.e("conect local socket fly_touch ok..");
                        } catch (Exception e) {
                            FlyLog.e(e.toString());
                        }
                    }
            }
        },"ScrcpyServer").start();

        Thread mThread = new Thread(this, "Contorller-recv");
        mThread.setDaemon(true);
        mThread.start();

    }

    public void stop() {
        isStop.set(true);
    }
}
