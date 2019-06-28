package com.flyzebra.record.task;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class AudioStream {
    private AudioRecord mAudioRecord;
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段
    private byte[] audioBuffer;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private static final HandlerThread sWorkerThread = new HandlerThread("screen-audio");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    public static AudioStream getInstance() {
        return AudioStreamHolder.sInstance;
    }

    private static class AudioStreamHolder {
        public static final AudioStream sInstance = new AudioStream();
    }

    private Runnable runTask = new Runnable() {
        @Override
        public void run() {
            while (isRunning.get()) {
                int size = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (size > 0) {
                }
            }
        }
    };


    public void start() {
        initAudioRecord();
    }

    private void initAudioRecord() {
        recordBufSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioBuffer = new byte[recordBufSize];
        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufSize);
        mAudioRecord.startRecording();
        isRunning.set(true);
        tHandler.post(runTask);
    }


    public void stop() {
        tHandler.removeCallbacksAndMessages(null);
    }

}
