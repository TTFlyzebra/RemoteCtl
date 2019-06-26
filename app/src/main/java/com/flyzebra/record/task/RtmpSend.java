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
    public static final String RTMP_ADDR = "rtmp://192.168.1.87/live/test";

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

    public void sendsps(MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        final byte[] send = new byte[packetLen];
        FLVPackager.fillFlvVideoTag(send,
                0,
                true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                send, FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                final int res = RtmpClient.write(jniRtmpPointer.get(), send, send.length, 9, 0);
            }
        });
    }

    public void send(ByteBuffer outputBuffer, MediaCodec.BufferInfo mBufferInfo,final int ts) {
        if(jniRtmpPointer.get()==-1) return;
        //获取帧类型
        outputBuffer.mark();
        Byte type = outputBuffer.get(4);
        int frameType = type & 0x1F;
        outputBuffer.reset();
        //获取发送数据
        outputBuffer.mark();
        int lengh = 5 + 4 + outputBuffer.remaining() - 4;
        final byte send[] = new byte[lengh];
        outputBuffer.get(send, 9, outputBuffer.remaining() - 4);
        FLVPackager.fillFlvVideoTag(send,
                0,
                false,
                frameType == 5,
                outputBuffer.remaining() - 4);
        outputBuffer.reset();
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                final int res = RtmpClient.write(jniRtmpPointer.get(), send, send.length, 9, ts);
            }
        });
    }


    public void close() {
        final long rtmp = jniRtmpPointer.get();
        jniRtmpPointer.set(-1);
        tHandler.removeCallbacksAndMessages(null);
        tHandler.post(new Runnable() {
            @Override
            public void run() {
                RtmpClient.close(rtmp);
            }
        });
    }

    public static class H264Packager {

        public static byte[] generateAVCDecoderConfigurationRecord(MediaFormat mediaFormat) {
            ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
            SPSByteBuff.position(4);
            ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
            PPSByteBuff.position(4);
            int spslength = SPSByteBuff.remaining();
            int ppslength = PPSByteBuff.remaining();
            int length = 11 + spslength + ppslength;
            byte[] result = new byte[length];
            SPSByteBuff.get(result, 8, spslength);
            PPSByteBuff.get(result, 8 + spslength + 3, ppslength);
            /**
             * UB[8]configurationVersion
             * UB[8]AVCProfileIndication
             * UB[8]profile_compatibility
             * UB[8]AVCLevelIndication
             * UB[8]lengthSizeMinusOne
             */
            result[0] = 0x01;
            result[1] = result[9];
            result[2] = result[10];
            result[3] = result[11];
            result[4] = (byte) 0xFF;
            /**
             * UB[8]numOfSequenceParameterSets
             * UB[16]sequenceParameterSetLength
             */
            result[5] = (byte) 0xE1;
            ByteArrayTools.intToByteArrayTwoByte(result, 6, spslength);
            /**
             * UB[8]numOfPictureParameterSets
             * UB[16]pictureParameterSetLength
             */
            int pos = 8 + spslength;
            result[pos] = (byte) 0x01;
            ByteArrayTools.intToByteArrayTwoByte(result, pos + 1, ppslength);

            return result;
        }
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
