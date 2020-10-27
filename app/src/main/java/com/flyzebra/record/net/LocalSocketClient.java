package com.flyzebra.record.net;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.flyzebra.scrcpy.DesktopConnection;
import com.flyzebra.util.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalSocketClient implements ISocketTask {
    private Thread recvThread;
    private Thread sendThread;
    private InputStream inputStream;
    private OutputStream outputStream;
    private byte[] recvBuffer = new byte[1280 * 720 * 3 / 2];
    private byte[] sendBuffer = new byte[4096];
    private ByteBuffer sendByteBuffer = ByteBuffer.allocateDirect(1280 * 720 * 3 / 2 * 5);
    private final Object sendLock = new Object();
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private String tag;

    public LocalSocketClient(String tag){
        this.tag = tag;
    }

    @Override
    public void start() {
        isStop.set(false);
        while (!isStop.get()) {
            FlyLog.d("try connect localserver %s...", tag);
            LocalSocket localSocket = new LocalSocket();
            LocalSocketAddress address1 = new LocalSocketAddress(DesktopConnection.SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT);
            try {
                localSocket.connect(address1);
                inputStream = localSocket.getInputStream();
                outputStream = localSocket.getOutputStream();
                break;
            } catch (IOException e) {
                FlyLog.e(e.toString());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        recvThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!isStop.get()) {
                        int len = inputStream.read(recvBuffer);
                        if (len <= 0) {
                            FlyLog.e("recv len -1");
                            break;
                        }
//                        FlyLog.v("recv data len=%d", len);
                    }
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }, tag + "-recv");

        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int sendLen = 0;
                try {
                    while (!isStop.get()) {
                        synchronized (sendLock) {
                            if (sendByteBuffer.position() < 1) {
                                sendLock.wait();
                                if (isStop.get()) break;
                            }
                            sendLen = Math.min(sendBuffer.length, sendByteBuffer.position());
                            sendByteBuffer.flip();
                            sendByteBuffer.get(sendBuffer, 0, sendLen);
                            sendByteBuffer.compact();
                        }
                        outputStream.write(sendBuffer, 0, sendLen);
                        FlyLog.d("send data len=%d", sendLen);
                    }
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }, tag + "-send");
        recvThread.start();
        sendThread.start();
    }

    @Override
    public void send(byte[] buffer, int offset, int lenght) {
        synchronized (sendLock) {
            int position = sendByteBuffer.position();
            if (sendByteBuffer.remaining() < lenght) {
                FlyLog.e("send buffer is full!");
            } else {
                sendByteBuffer.put(buffer, 0, lenght);
            }
            if (position < 1) {
                sendLock.notify();
            }
        }
    }

    @Override
    public void stop() {
        isStop.set(true);
        synchronized (sendLock) {
            sendLock.notify();
        }
    }
}
