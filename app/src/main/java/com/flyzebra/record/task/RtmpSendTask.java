package com.flyzebra.record.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;

import com.flyzebra.record.bean.RtmpData;
import com.flyzebra.record.flvutils.FLvMetaData;
import com.flyzebra.record.flvutils.Packager;
import com.flyzebra.record.flvutils.RESCoreParameters;
import com.flyzebra.record.flvutils.RESFlvData;
import com.flyzebra.record.utils.ByteUtil;
import com.flyzebra.record.utils.FlyLog;
import com.flyzebra.rtmp.RtmpClient;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import static com.flyzebra.record.flvutils.RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
import static com.flyzebra.record.flvutils.RESFlvData.FLV_RTMP_PACKET_TYPE_INFO;
import static com.flyzebra.record.flvutils.RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;

/**
 * Author FlyZebra
 * 2019/6/26 15:15
 * Describ:
 **/
public class RtmpSendTask {
    private static final int MAX_QUEUE_CAPACITY = 50;
    private static final int MAX_QUEUE_LIST = 25;
    private LinkedBlockingDeque<RtmpData> frameQueue = new LinkedBlockingDeque<>(MAX_QUEUE_CAPACITY);
    private static final HandlerThread sWorkerThread = new HandlerThread("sendVideoFrame-rtmp");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    private AtomicLong jniRtmpPointer = new AtomicLong(-1);
    public static final String RTMP_ADDR = "rtmp://192.168.1.87/live/test";

    public static RtmpSendTask getInstance() {
        return RtmpSendTask.RtmpSendTaskHolder.sInstance;
    }


    private static class RtmpSendTaskHolder {
        public static final RtmpSendTask sInstance = new RtmpSendTask();
    }

    public Runnable runTask = new Runnable() {
        @Override
        public void run() {
            while (jniRtmpPointer.get() != -1) {
                if (frameQueue.size() > 0) {
                    RtmpData rtmpData = frameQueue.pop();
                    RtmpClient.write(jniRtmpPointer.get(), rtmpData.buffer, rtmpData.buffer.length, rtmpData.type, rtmpData.ts);
                    FlyLog.d("size=%d,send: %s", frameQueue.size(), ByteUtil.bytes2String(rtmpData.buffer, 10));
                }
            }
        }
    };


    public void open(final String url) {
        if (jniRtmpPointer.get() == -1) {
            jniRtmpPointer.set(RtmpClient.open(url, true));
            RESCoreParameters coreParameters = new RESCoreParameters();
            coreParameters.mediacodecAACBitRate = RESFlvData.AAC_BITRATE;
            coreParameters.mediacodecAACSampleRate = RESFlvData.AAC_SAMPLE_RATE;
            coreParameters.mediacodecAVCFrameRate = RESFlvData.FPS;
            coreParameters.videoWidth = RESFlvData.VIDEO_WIDTH;
            coreParameters.videoHeight = RESFlvData.VIDEO_HEIGHT;

            FLvMetaData fLvMetaData = new FLvMetaData(coreParameters);
            byte[] metaData = fLvMetaData.getMetaData();
            RtmpData rtmpData = new RtmpData();
            rtmpData.buffer = metaData;
            rtmpData.type = FLV_RTMP_PACKET_TYPE_INFO;
            rtmpData.ts = 0;
            frameQueue.add(rtmpData);
            tHandler.post(runTask);
        }

    }

    public void sendAudioSPS(MediaFormat format) {
        if (jniRtmpPointer.get() == -1) return;
        ByteBuffer realData = format.getByteBuffer("csd-0");
        int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH + realData.remaining();
        byte[] sendBytes = new byte[packetLen];
        realData.get(sendBytes, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH, realData.remaining());
        Packager.FLVPackager.fillFlvAudioTag(sendBytes, 0, true);
        RtmpData rtmpData = new RtmpData();
        rtmpData.buffer = sendBytes;
        rtmpData.type = FLV_RTMP_PACKET_TYPE_AUDIO;
        rtmpData.ts = 0;
        rtmpData.droppable = false;
        pushData(rtmpData);
    }

    public void sendVideoSPS(MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH + AVCDecoderConfigurationRecord.length;
        final byte[] sendBytes = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(sendBytes, 0, true, true, AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0, sendBytes, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RtmpData rtmpData = new RtmpData();
        rtmpData.buffer = sendBytes;
        rtmpData.type = FLV_RTMP_PACKET_TYPE_VIDEO;
        rtmpData.ts = 0;
        pushData(rtmpData);
    }

    public void sendVideoFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo mBufferInfo, final int ts) {
        if (jniRtmpPointer.get() == -1) return;
        //获取帧类型
        outputBuffer.mark();
        Byte type = outputBuffer.get(4);
        int frameType = type & 0x1F;
        outputBuffer.reset();

        //获取发送数据
        outputBuffer.mark();
        outputBuffer.position(mBufferInfo.offset + 4);
        int realDataLength = outputBuffer.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH + Packager.FLVPackager.NALU_HEADER_LENGTH + realDataLength;
        byte[] sendBytes = new byte[packetLen];
        outputBuffer.get(sendBytes, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH + Packager.FLVPackager.NALU_HEADER_LENGTH, realDataLength);
        Packager.FLVPackager.fillFlvVideoTag(sendBytes, 0, false, frameType == 5, realDataLength);
        outputBuffer.reset();
        RtmpData rtmpData = new RtmpData();
        rtmpData.buffer = sendBytes;
        rtmpData.type = FLV_RTMP_PACKET_TYPE_VIDEO;
        rtmpData.ts = ts;
        pushData(rtmpData);
    }

    public void sendAudioFrame(ByteBuffer realData, final int ts) {
        if (jniRtmpPointer.get() == -1) return;
        //获取帧类型
        int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH + realData.remaining();
        byte[] sendBytes = new byte[packetLen];
        realData.get(sendBytes, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH, realData.remaining());
        Packager.FLVPackager.fillFlvAudioTag(sendBytes, 0, false);
        RtmpData rtmpData = new RtmpData();
        rtmpData.buffer = sendBytes;
        rtmpData.type = FLV_RTMP_PACKET_TYPE_AUDIO;
        rtmpData.ts = ts;
        rtmpData.droppable = true;
        pushData(rtmpData);
    }


    private void pushData(RtmpData rtmpData) {
        if (frameQueue != null) {
            if (frameQueue.size() > MAX_QUEUE_LIST) {
                frameQueue.clear();
            }
            frameQueue.add(rtmpData);
        }
    }


    public void close() {
        final long rtmp = jniRtmpPointer.get();
        jniRtmpPointer.set(-1);
        tHandler.removeCallbacksAndMessages(null);
        RtmpClient.close(rtmp);
    }

}
