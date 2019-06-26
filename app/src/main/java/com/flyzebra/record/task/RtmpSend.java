package com.flyzebra.record.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;

import com.flyzebra.record.utils.ByteArrayTools;
import com.flyzebra.rtmp.RtmpClient;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author FlyZebra
 * 2019/6/26 15:15
 * Describ:
 **/
public class RtmpSend {

    private static final HandlerThread sWorkerThread = new HandlerThread("screen-rtmp");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    private AtomicLong jniRtmpPointer = new AtomicLong(-1);

    public static RtmpSend getInstance() {
        return RtmpSend.RtmpSendHolder.sInstance;
    }


    private static class RtmpSendHolder {
        public static final RtmpSend sInstance = new RtmpSend();
    }

    public void open(final String url) {
        if (jniRtmpPointer.get() == -1) {
            tHandler.post(new Runnable() {
                @Override
                public void run() {
                    jniRtmpPointer.set(RtmpClient.open(url, true));
                }
            });
        }
    }

    public void sendsps(MediaFormat outputFormat) {
        tHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public void send(ByteBuffer outputBuffer, MediaCodec.BufferInfo mBufferInfo) {
        tHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }


    public void close() {
        final long rtmp = jniRtmpPointer.get();
        jniRtmpPointer.set(-1);
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                RtmpClient.close(rtmp);
            }
        });
    }

    public static class FLVPackager {
        public static final int FLV_TAG_LENGTH = 11;
        public static final int FLV_VIDEO_TAG_LENGTH = 5;
        public static final int FLV_AUDIO_TAG_LENGTH = 2;
        public static final int FLV_TAG_FOOTER_LENGTH = 4;
        public static final int NALU_HEADER_LENGTH = 4;

        public static void fillFlvVideoTag(byte[] dst, int pos, boolean isAVCSequenceHeader, boolean isIDR, int readDataLength) {
            //FrameType&CodecID
            dst[pos] = isIDR ? (byte) 0x17 : (byte) 0x27;
            //AVCPacketType
            dst[pos + 1] = isAVCSequenceHeader ? (byte) 0x00 : (byte) 0x01;
            //LAKETODO CompositionTime
            dst[pos + 2] = 0x00;
            dst[pos + 3] = 0x00;
            dst[pos + 4] = 0x00;
            if (!isAVCSequenceHeader) {
                //NALU HEADER
                ByteArrayTools.intToByteArrayFull(dst, pos + 5, readDataLength);
            }
        }

        public static void fillFlvAudioTag(byte[] dst, int pos, boolean isAACSequenceHeader) {
            /**
             * UB[4] 10=AAC
             * UB[2] 3=44kHz
             * UB[1] 1=16-bit
             * UB[1] 0=MonoSound
             */
            dst[pos] = (byte) 0xAE;
            dst[pos + 1] = isAACSequenceHeader ? (byte) 0x00 : (byte) 0x01;
        }
    }
}
