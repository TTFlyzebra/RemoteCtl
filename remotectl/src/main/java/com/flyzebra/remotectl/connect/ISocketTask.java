package com.flyzebra.remotectl.connect;

public interface ISocketTask {
    void start();
    void send(byte[] buffer,int offset, int lenght);
    void stop();
}
