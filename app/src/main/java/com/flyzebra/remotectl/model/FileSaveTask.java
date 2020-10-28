package com.flyzebra.remotectl.model;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;

import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/20 10:26
 * Describ:
 **/
public class FileSaveTask {
    public static final int OPEN_VIDEO = 1;
    public static final int OPEN_AUDIO = 2;
    private long ONE_RECORD_TIME = 60000;
    private static final int OUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
    private static final String SAVA_PATH = "/sdcard/flyrecord";
    private static final String FILE_FORMAT = "yyyyMMdd_HHmmss";
    private MediaMuxer mediaMuxer;
    private AtomicBoolean isStartMediaMuxer = new AtomicBoolean(false);
    private AtomicBoolean isAddVideoTrack = new AtomicBoolean(false);
    private AtomicBoolean isAddAudioTrack = new AtomicBoolean(false);
    private boolean isRecordAudio = true;
    private int videoTrack;
    private int audioTrack;
    private MediaFormat mVideoMediaFormat;
    private MediaFormat mAudioMediaFormat;
    private long lastRecordTime = 0;


    private final Object synObj = new Object();

    public static FileSaveTask getInstance() {
        return SaveRecordFileHolder.sInstance;
    }

    private static class SaveRecordFileHolder {
        public static final FileSaveTask sInstance = new FileSaveTask();
    }

    private void initMediaMuxer() {
        try {
            File file = new File(SAVA_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = SAVA_PATH + File.separator + TimeUtil.getCurrentTime(FILE_FORMAT) + ".mp4";
            mediaMuxer = new MediaMuxer(fileName, OUT_FORMAT);
            lastRecordTime = System.currentTimeMillis() / ONE_RECORD_TIME;
            FlyLog.d("create new MediaMuxer: %s", fileName);
        } catch (IOException e) {
            FlyLog.e("initMediaMuxer failed! error:" + e.toString());
        }

    }


    public synchronized void open(int type, MediaFormat format) {
        if (!isRecordAudio) {
            isAddAudioTrack.set(true);
        }
        if (mediaMuxer == null) {
            initMediaMuxer();
        }
        if (type == OPEN_VIDEO) {
            addVideoTrack(format);
        } else if (type == OPEN_AUDIO) {
            addAudioTrack(format);
        }
    }


    private synchronized void addVideoTrack(final MediaFormat format) {
        if (!isAddVideoTrack.get() && format != null) {
            mVideoMediaFormat = format;
            videoTrack = mediaMuxer.addTrack(format);
            isAddVideoTrack.set(true);
            startMediaMuxer();
        }

    }

    private synchronized void addAudioTrack(final MediaFormat format) {
        if (!isAddAudioTrack.get() && format != null) {
            mAudioMediaFormat = format;
            audioTrack = mediaMuxer.addTrack(format);
            isAddAudioTrack.set(true);
            startMediaMuxer();
        }
    }


    private void startMediaMuxer() {
        if (!isStartMediaMuxer.get()) {
            FlyLog.d("MediaMuxer start");
            mediaMuxer.start();
            isStartMediaMuxer.set(true);
        }
    }


    public void writeVideoTrack(final ByteBuffer outputBuffer, final MediaCodec.BufferInfo mBufferInfo) {
        writeFrame(videoTrack, outputBuffer, mBufferInfo);
    }

    public void writeAudioTrack(final ByteBuffer outputBuffer, final MediaCodec.BufferInfo mBufferInfo) {
        writeFrame(audioTrack, outputBuffer, mBufferInfo);
    }

    public synchronized void writeFrame(int indexTrack, final ByteBuffer outputBuffer, final MediaCodec.BufferInfo mBufferInfo) {
        synchronized (synObj) {
            if (mediaMuxer != null) {
                //获取帧类型
                outputBuffer.mark();
                Byte type = outputBuffer.get(4);
                int frameType = type & 0x1F;
                outputBuffer.reset();

                long time = System.currentTimeMillis();
                if (time / ONE_RECORD_TIME - lastRecordTime > 0 && frameType == 5) {
                    close();
                    initMediaMuxer();
                    addVideoTrack(mVideoMediaFormat);
                    addAudioTrack(mAudioMediaFormat);
                }
                if (isStartMediaMuxer.get()) {
                    long systemTime = SystemClock.uptimeMillis();
                    try {
                        mediaMuxer.writeSampleData(indexTrack, outputBuffer, mBufferInfo);
                    } catch (Exception e) {
                        FlyLog.e("xxxx time=xx" + mBufferInfo.presentationTimeUs + ",index=%d,systemTime=" + systemTime, indexTrack);
                    }
                }
            }
        }

    }

    public void close() {
        isAddVideoTrack.set(false);
        isAddAudioTrack.set(!isRecordAudio);
        if (mediaMuxer != null) {
            if(isStartMediaMuxer.get()){
                mediaMuxer.stop();
            }
            mediaMuxer.release();
            mediaMuxer = null;
            FlyLog.d("MediaMuxer release");
        }
        isStartMediaMuxer.set(false);
    }
}
