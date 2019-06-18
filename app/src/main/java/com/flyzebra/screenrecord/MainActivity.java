package com.flyzebra.screenrecord;

import android.app.Activity;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Author FlyZebra
 * 2019/6/15 15:09
 * Describ:
 **/
public class MainActivity extends Activity {
    public static LocalSocket clientSocket, sendSocket;
    private LocalServerSocket serverSocket;
    public static Socket cSocket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        try {
//            serverSocket = new LocalServerSocket("flyzebra_screenrecord");
//            clientSocket = new LocalSocket();
//            clientSocket.connect(new LocalSocketAddress("flyzebra_screenrecord"));
//            clientSocket.setReceiveBufferSize(8 * 1024 * 1024);
//            clientSocket.setSoTimeout(3000);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    sendSocket = serverSocket.accept();
//                    sendSocket.setSendBufferSize(8 * 1024 * 1024);
//                    try {
//                        OutputStream ins = sendSocket.getOutputStream();
//                        while (true) {
//                            FlyLog.d("send date 1 2 3 4");
//                            ins.write(new byte[]{1, 2, 3, 4});
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    ServerSocket serverSocket = new ServerSocket(8001);
                    try {
                        cSocket = new Socket("127.0.0.1",8001);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Socket socket = serverSocket.accept();
                    InputStream inputStream = socket.getInputStream();
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = inputStream.read(bytes)) != -1) {
                        byte data[] = new byte[len];
                        System.arraycopy(bytes, 0, data, 0, len);
                        FlyLog.d("recv data:" + ByteUtil.bytes2HexString(data));
                    }
//                    System.out.println("get message from client: " + sb);
                    inputStream.close();
//                    try {
//                        byte buffer[] = new byte[8 * 1024 * 1024];
//                        InputStream ins = sendSocket.getInputStream();
//                        int len = -1;
//                        while ((len = ins.read(buffer)) != -1) {
//                            byte data[] = new byte[len];
//                            System.arraycopy(buffer, 0, data, 0, len);
//                            FlyLog.d("recv data:" + ByteUtil.bytes2HexString(data));
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });

        startActivity(new Intent(MainActivity.this, ScreenRecordActivity.class));

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    byte buffer[] = new byte[8 * 1024 * 1024];
//                    InputStream ins = clientSocket.getInputStream();
//                    int len = -1;
//                    while ((len = ins.read(buffer)) != -1) {
//                        byte data[] = new byte[len];
//                        System.arraycopy(buffer, 0, data, 0, len);
//                        FlyLog.d("recv data:" + ByteUtil.bytes2HexString(data));
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }).start();
    }
}
