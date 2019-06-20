package com.flyzebra.screenrecord.module;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

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

    /**
     * 创建文件
     * @param fileName
     */
    public void createNewFile(String fileName) {
        try {
            isSetFormat = false;
            mediaMuxer = new MediaMuxer(fileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setFileFormat(MediaFormat format) {
        if (!isSetFormat) {
            indexTrack = mediaMuxer.addTrack(format);
            mediaMuxer.start();
            isSetFormat = true;
        }
    }

    public void save(ByteBuffer outputBuffer, MediaCodec.BufferInfo mBufferInfo) {
        if (mediaMuxer != null) {
            mediaMuxer.writeSampleData(indexTrack, outputBuffer, mBufferInfo);
        }
    }

    public void close() {
        mediaMuxer.stop();
        mediaMuxer.release();
    }
}
