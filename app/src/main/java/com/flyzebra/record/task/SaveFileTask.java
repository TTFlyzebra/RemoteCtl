package com.flyzebra.record.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;

import com.flyzebra.record.bean.RtmpData;
import com.flyzebra.record.utils.FlyLog;
import com.flyzebra.record.utils.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/20 10:26
 * Describ:
 **/
public class SaveFileTask {
    private MediaMuxer mediaMuxer;
    private AtomicBoolean isStart = new AtomicBoolean(false);
    private int indexTrack;
    private long lastRecordTime = 0;
    private long ONE_RECORD_TIME = 60000;
    private int OUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
    private String SAVA_PATH = "/sdcard/flyrecord";
    private String FILE_FORMAT = "yyyyMMdd_HHmmss";
    private MediaFormat mMediaFormat;
    private static final int MAX_QUEUE_CAPACITY = 100;
    private LinkedBlockingDeque<RtmpData> fileQuque = new LinkedBlockingDeque<>(MAX_QUEUE_CAPACITY);

    private static final HandlerThread sWorkerThread = new HandlerThread("save-file");
    static {
        sWorkerThread.start();
    }
    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    public static SaveFileTask getInstance() {
        return SaveRecordFileHolder.sInstance;
    }

    private static class SaveRecordFileHolder {
        public static final SaveFileTask sInstance = new SaveFileTask();
    }

    public Runnable runTask = new Runnable() {
        @Override
        public void run() {
            while (isStart.get()) {
                if (fileQuque.size() > 0) {
                }
            }
        }
    };


    /**
     * 创建文件
     */
    private void newFile() {
        try {
            lastRecordTime = System.currentTimeMillis() / ONE_RECORD_TIME;
            File file = new File(SAVA_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = SAVA_PATH + File.separator + TimeUtil.getCurrentTime(FILE_FORMAT) + ".mp4";
            mediaMuxer = new MediaMuxer(fileName, OUT_FORMAT);
            FlyLog.d("create new file: %s", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void open(MediaFormat outputFormat) {
        newFile();
        writeFormat(outputFormat);
    }


    public void writeFormat(final MediaFormat format) {
        if(format==null)  return;
        mMediaFormat = format;
        if (!isStart.get()) {
            indexTrack = mediaMuxer.addTrack(format);
            mediaMuxer.start();
            isStart.set(true);
        }

    }

    public void write(final ByteBuffer outputBuffer, final MediaCodec.BufferInfo mBufferInfo) {
        if (mediaMuxer != null) {
            //获取帧类型
            outputBuffer.mark();
            Byte type = outputBuffer.get(4);
            int frameType = type & 0x1F;
            outputBuffer.reset();

            long time = System.currentTimeMillis();
            if (time / ONE_RECORD_TIME - lastRecordTime > 0 && frameType == 5) {
                close();
                newFile();
                writeFormat(mMediaFormat);
            }
            mediaMuxer.writeSampleData(indexTrack, outputBuffer, mBufferInfo);
        }

    }

    public void close() {
        isStart.set(false);
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaMuxer = null;
    }
}
