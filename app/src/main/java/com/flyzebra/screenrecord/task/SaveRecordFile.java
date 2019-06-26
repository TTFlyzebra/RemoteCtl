package com.flyzebra.screenrecord.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Author FlyZebra
 * 2019/6/20 10:26
 * Describ:
 **/
public class SaveRecordFile {
    private MediaMuxer mediaMuxer;
    private boolean isSetFormat = false;
    private MediaFormat mFormat;
    private int indexTrack;

    private static final HandlerThread sWorkerThread = new HandlerThread("screen-save");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    public static SaveRecordFile getInstance() {
        return SaveRecordFileHolder.sInstance;
    }

    private static class SaveRecordFileHolder {
        public static final SaveRecordFile sInstance = new SaveRecordFile();
    }

    /**
     * 创建文件
     * @param fileName
     */
    public void createNewFile(final String fileName, final int format) {
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    isSetFormat = false;
                    mediaMuxer = new MediaMuxer(fileName, format);
                    if(mFormat!=null){
                        setFileFormat(mFormat);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public void setFileFormat(final MediaFormat format) {
        mFormat = format;
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isSetFormat) {
                    indexTrack = mediaMuxer.addTrack(format);
                    mediaMuxer.start();
                    isSetFormat = true;
                }
            }
        });

    }

    public void save(final ByteBuffer outputBuffer, final MediaCodec.BufferInfo mBufferInfo) {
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaMuxer != null) {
                    mediaMuxer.writeSampleData(indexTrack, outputBuffer, mBufferInfo);
                }
            }
        });

    }

    public void close() {
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                mFormat = null;
                isSetFormat = false;
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
            }
        });
    }
}
