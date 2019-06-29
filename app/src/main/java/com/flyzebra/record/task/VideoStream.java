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

import com.flyzebra.record.utils.FlyLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class VideoStream {
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

    private static final HandlerThread sWorkerThread = new HandlerThread("send-video");

    static {
        sWorkerThread.start();
    }

    private static final Handler tHandler = new Handler(sWorkerThread.getLooper());

    public static VideoStream getInstance() {
        return VideoStreamHolder.sInstance;
    }

    private static class VideoStreamHolder {
        public static final VideoStream sInstance = new VideoStream();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(MediaProjection mediaProjection) {
        isStop.set(false);
        AudioStream.getInstance().start();
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
        AudioStream.getInstance().stop();
        tHandler.removeCallbacksAndMessages(null);
        isStop.set(true);
    }

    private Runnable runTask = new Runnable() {
        @Override
        public void run() {
            FlyLog.d("send video task start!");
            while (isRunning.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isRunning.set(true);
            RtmpSendTask.getInstance().open(RtmpSendTask.RTMP_ADDR);
            while (!isStop.get()) {
                int eobIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        RtmpSendTask.getInstance().sendVideoSPS(mediaCodec.getOutputFormat());
                        SaveFileTask.getInstance().open(SaveFileTask.OPEN_VIDEO, mediaCodec.getOutputFormat());
                        break;
                    default:
                        if (startTime == 0) {
                            startTime = mBufferInfo.presentationTimeUs / 1000;
                        }
                        if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            ByteBuffer outputBuffer = outputBuffers[eobIndex];
                            outputBuffer.position(mBufferInfo.offset);
                            outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                            RtmpSendTask.getInstance().sendVideoFrame(outputBuffer, mBufferInfo, (int) ((mBufferInfo.presentationTimeUs / 1000) - startTime));
                            SaveFileTask.getInstance().writeVideoTrack(outputBuffer, mBufferInfo);
                        }
                        mediaCodec.releaseOutputBuffer(eobIndex, false);
                        break;
                }
            }
            SaveFileTask.getInstance().close();
            RtmpSendTask.getInstance().close();
            mVirtualDisplay.release();
            mVirtualDisplay = null;
            mMediaProjection.stop();
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
            isRunning.set(false);
            FlyLog.d("send video task end!");
        }
    };

}
