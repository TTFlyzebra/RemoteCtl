package com.flyzebra.record.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.flyzebra.record.utils.FlyLog;
import com.flyzebra.record.utils.TimeUtil;

import java.io.File;
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
    private int indexTrack;
    private long lastRecordTime = 0;
    private long ONE_RECORD_TIME = 60000;
    private int OUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
    private String SAVA_PATH = "/sdcard/flyrecord";
    private String FILE_FORMAT = "yyyyMMdd_HHmmss";
    private MediaFormat mMediaFormat;


    public static SaveRecordFile getInstance() {
        return SaveRecordFileHolder.sInstance;
    }

    private static class SaveRecordFileHolder {
        public static final SaveRecordFile sInstance = new SaveRecordFile();
    }

    /**
     * 创建文件
     */
    public void open() {
        try {
            lastRecordTime = System.currentTimeMillis() / ONE_RECORD_TIME;
            File file = new File(SAVA_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = SAVA_PATH + File.separator + TimeUtil.getCurrentTime(FILE_FORMAT) + ".mp4";
            mediaMuxer = new MediaMuxer(fileName, OUT_FORMAT);
            isSetFormat = false;
            FlyLog.d("create new file: %s", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void open(MediaFormat outputFormat) {
        open();
        writeFormat(outputFormat);
    }


    public void writeFormat(final MediaFormat format) {
        mMediaFormat = format;
        if (!isSetFormat) {
            indexTrack = mediaMuxer.addTrack(format);
            mediaMuxer.start();
            isSetFormat = true;
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
                open();
                writeFormat(mMediaFormat);
            }
            mediaMuxer.writeSampleData(indexTrack, outputBuffer, mBufferInfo);
        }

    }

    public void close() {
        isSetFormat = false;
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaMuxer = null;
    }
}
