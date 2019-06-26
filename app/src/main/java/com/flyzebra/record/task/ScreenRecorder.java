package com.flyzebra.record.task;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.flyzebra.record.utils.ByteArrayTools;
import com.flyzebra.record.utils.FlyLog;
import com.flyzebra.rtmp.RtmpClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class ScreenRecorder {
    private MediaProjection mMediaProjection;
    private MediaCodec mediaCodec;

    // parameters for the encoder
    private int mWidth = 1024;
    private int mHeight = 600;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25; // 30 fps
    private static final int IFRAME_INTERVAL = 5; // 2 seconds between I-frames
    private static final int TIMEOUT_US = 10000;
    private final int BIT_RATE = (int) (mWidth * mHeight * 3.6);

    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private long startTime = 0;
    private int videoTrackIndex;

    private static final HandlerThread sWorkerThread = new HandlerThread("screen-recorder");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());
    private long jniRtmpPointer;
    private static final String RTMP_ADDR = "rtmp://192.168.1.87/live/test1";

    private long lastRecordTime = 0;
    private long one_record_time = 60000;

    public static ScreenRecorder getInstance() {
        return ScreenRecorderHolder.sInstance;
    }

    private static class ScreenRecorderHolder {
        public static final ScreenRecorder sInstance = new ScreenRecorder();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(MediaProjection mediaProjection) {
        isStop.set(false);
        mMediaProjection = mediaProjection;
        initMediaCodec();
        createVirtualDisplay();
        tHandler.post(runTask);
    }

    private void initMediaCodec() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        FlyLog.d("created video format: " + format);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mediaCodec.createInputSurface();
            FlyLog.d("created input surface: " + mSurface);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createVirtualDisplay() {
        if (mVirtualDisplay == null) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("SCREEN", mWidth, mHeight, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, null);
        }
    }

    public void stop() {
        tHandler.removeCallbacksAndMessages(null);
        isStop.set(true);
    }

    private Runnable runTask = new Runnable() {
        @Override
        public void run() {
            while (isRunning.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isRunning.set(true);
            jniRtmpPointer = RtmpClient.open(RTMP_ADDR, true);
            byte[] MetaData = new FLvMetaData().getMetaData();
            RtmpClient.write(jniRtmpPointer,
                    MetaData,
                    MetaData.length,
                    18, 0);
            while (!isStop.get()) {
                int eobIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                        FlyLog.v("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        FlyLog.v("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                        FlyLog.v("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" + mediaCodec.getOutputFormat().toString());
                        if (!isStop.get()) {
                            SaveRecordFile.getInstance().open(mediaCodec.getOutputFormat());
                            //TODO:send sps
                            //sendAVCDecoderConfigurationRecord(0, mediaCodec.getOutputFormat());
                        }
                        break;
                    default:
//                        FlyLog.v("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                        if (startTime == 0) {
                            startTime = mBufferInfo.presentationTimeUs / 1000;
                        }
                        /**
                         * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            ByteBuffer outputBuffer = outputBuffers[eobIndex];
                            outputBuffer.position(mBufferInfo.offset);
                            outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            //获取帧类型
                            outputBuffer.mark();
                            byte index[] = new byte[8];
                            outputBuffer.get(index, 0, index.length);
                            outputBuffer.reset();
                            int frameType = index[4] & 0x1F;
//                            FlyLog.d("H264 I:%s", ByteUtil.bytes2String(index));
//
                            if (!isStop.get()) {
                                //发送文件
//                                outputBuffer.mark();
//                                int lengh = 5 + 4 + outputBuffer.remaining() - 4;
//                                byte send[] = new byte[lengh];
//                                outputBuffer.get(send, 9, outputBuffer.remaining() - 4);
//                                FLVPackager.fillFlvVideoTag(send,
//                                        0,
//                                        false,
//                                        frameType == 5,
//                                        outputBuffer.remaining() - 4);
//                                final int res = RtmpClient.write(jniRtmpPointer,
//                                        send,
//                                        send.length,
//                                        9,
//                                        (int) ((mBufferInfo.presentationTimeUs / 1000) - startTime));
//                                if (res == 0) {
//                                    FlyLog.d("video frame sent = " + send.length);
//                                } else {
//                                    FlyLog.e("writeError = " + res);
//                                }
//                                outputBuffer.reset();
                                //保存文件
                                SaveRecordFile.getInstance().write(outputBuffer, mBufferInfo);
                            }
                        }
                        mediaCodec.releaseOutputBuffer(eobIndex, false);
                        break;
                }
            }
            SaveRecordFile.getInstance().close();
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
            mVirtualDisplay.release();
            mVirtualDisplay = null;
            mMediaProjection.stop();
            RtmpClient.close(jniRtmpPointer);
            isRunning.set(false);
        }
    };

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
