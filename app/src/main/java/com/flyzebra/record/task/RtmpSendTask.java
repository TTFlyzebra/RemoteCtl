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
import com.flyzebra.rtmp.RtmpClient;

import java.nio.ByteBuffer;
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
    private static final HandlerThread sWorkerThread = new HandlerThread("sendVideoFrame-rtmp");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    private AtomicLong jniRtmpPointer = new AtomicLong(-1);
    public static final String RTMP_ADDR = "rtmp://192.168.8.244/live/screen";

    public static RtmpSendTask getInstance() {
        return RtmpSendTask.RtmpSendTaskHolder.sInstance;
    }


    private static class RtmpSendTaskHolder {
        public static final RtmpSendTask sInstance = new RtmpSendTask();
    }


    public void open(final String url) {
        if (jniRtmpPointer.get() == -1) {
            jniRtmpPointer.set(RtmpClient.open(url, true));
            RESCoreParameters coreParameters = new RESCoreParameters();
            coreParameters.mediacodecAACBitRate = RESFlvData.AAC_BITRATE;
            coreParameters.mediacodecAACSampleRate = RESFlvData.AAC_SAMPLE_RATE;
            coreParameters.videoWidth = RESFlvData.VIDEO_WIDTH;
            coreParameters.videoHeight = RESFlvData.VIDEO_HEIGHT;
            coreParameters.mediacodecAVCFrameRate = RESFlvData.VIDEO_FPS;

            FLvMetaData fLvMetaData = new FLvMetaData(coreParameters);
            byte[] metaData = fLvMetaData.getMetaData();
            RtmpData rtmpData = new RtmpData();
            rtmpData.buffer = metaData;
            rtmpData.type = FLV_RTMP_PACKET_TYPE_INFO;
            rtmpData.ts = 0;
            write(rtmpData);
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
        write(rtmpData);
    }

    public void sendVideoSPS(MediaFormat mediaFormat) {
        if (jniRtmpPointer.get() == -1) return;
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(mediaFormat);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH + AVCDecoderConfigurationRecord.length;
        final byte[] sendBytes = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(sendBytes, 0, true, true, AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0, sendBytes, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RtmpData rtmpData = new RtmpData();
        rtmpData.buffer = sendBytes;
        rtmpData.type = FLV_RTMP_PACKET_TYPE_VIDEO;
        rtmpData.ts = 0;
        write(rtmpData);
    }

    public void sendVideoFrame(ByteBuffer outputBuffer, MediaCodec.BufferInfo mBufferInfo, final int ts) {
        if (jniRtmpPointer.get() == -1) return;
        //获取帧类型
        outputBuffer.mark();
        byte type = outputBuffer.get(4);
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
        write(rtmpData);
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
        write(rtmpData);
    }


    synchronized private void write(RtmpData rtmpData) {
        if (jniRtmpPointer.get() == -1) return;
        RtmpClient.write(jniRtmpPointer.get(), rtmpData.buffer, rtmpData.buffer.length, rtmpData.type, rtmpData.ts);
        //FlyLog.d("send: %s",ByteUtil.bytes2String(rtmpData.buffer, 16));
    }


    public void close() {
        RtmpClient.close(jniRtmpPointer.get());
        jniRtmpPointer.set(-1);
        tHandler.removeCallbacksAndMessages(null);
    }

}
