package com.flyzebra.record.net;

public interface ISocketTask {
    void start();
    void send(byte[] buffer,int offset, int lenght);
    void stop();
}
