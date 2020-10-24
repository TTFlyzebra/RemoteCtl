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

import com.flyzebra.record.model.FileSaveTask;
import com.flyzebra.record.model.FlvRtmpClient;
import com.flyzebra.record.utils.FlyLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class VideoStream implements Runnable{
    private MediaProjection mMediaProjection;
    private MediaCodec mediaCodec;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int TIMEOUT_US = 10000;

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
        while (!isStop.get()) {
            int eobIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    FileSaveTask.getInstance().open(FileSaveTask.OPEN_VIDEO, mediaCodec.getOutputFormat());
                    FlvRtmpClient.getInstance().sendVideoSPS(mediaCodec.getOutputFormat());
                    break;
                default:
                    if (startTime == 0) {
                        startTime = mBufferInfo.presentationTimeUs / 1000;
                    }
                    if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(eobIndex);
                        outputBuffer.position(mBufferInfo.offset);
                        outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                        FlvRtmpClient.getInstance().sendVideoFrame(outputBuffer, mBufferInfo, (int) ((mBufferInfo.presentationTimeUs / 1000) - startTime));
                        FileSaveTask.getInstance().writeVideoTrack(outputBuffer,mBufferInfo);
                    }
                    mediaCodec.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mMediaProjection.stop();
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        isRunning.set(false);
        FlyLog.d("send video task end!");
    }

    private static class VideoStreamHolder {
        public static final VideoStream sInstance = new VideoStream();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start(MediaProjection mediaProjection) {
        FlvRtmpClient.getInstance().open(FlvRtmpClient.RTMP_ADDR);
        isStop.set(false);
        mMediaProjection = mediaProjection;
        initMediaCodec();
        createVirtualDisplay();
        tHandler.post(this);
    }

    private void initMediaCodec() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, FlvRtmpClient.VIDEO_WIDTH, FlvRtmpClient.VIDEO_HEIGHT);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, FlvRtmpClient.VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FlvRtmpClient.VIDEO_FPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FlvRtmpClient.VIDEO_IFRAME_INTERVAL);
            FlyLog.d("created video format: " + format.toString());
            //format.setFloat("max-fps-to-encoder", 24);
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mediaCodec.createInputSurface();
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createVirtualDisplay() {
        if (mVirtualDisplay == null) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("SCREEN", FlvRtmpClient.VIDEO_WIDTH, FlvRtmpClient.VIDEO_HEIGHT, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, null, null);
        }
    }

    public void stop() {
        tHandler.removeCallbacksAndMessages(null);
        FlvRtmpClient.getInstance().close();
        isStop.set(true);
    }

}
