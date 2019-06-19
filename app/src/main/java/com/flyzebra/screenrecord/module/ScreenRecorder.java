package com.flyzebra.screenrecord.module;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.view.Surface;

import com.flyzebra.screenrecord.utils.FlyLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class ScreenRecorder extends Thread{
    private final MediaProjection mMediaProjection;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 2; // 2 seconds between I-frames
    private static final int TIMEOUT_US = 1000;
    private static final int BIT_RATE = 500000;
    private int mWidth = 1024;
    private int mHeight = 600;

    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private static AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private long startTime = 0;
    private int videoTrackIndex;


    public ScreenRecorder(MediaProjection mediaProjection) {
        this.mMediaProjection = mediaProjection;
        init();
        createVirtualDisplay();
        start();
    }

    private void init() {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mediaMuxer = new MediaMuxer("/sdcard/"+System.currentTimeMillis()+".mp4",MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = mediaMuxer.addTrack(mediaCodec.getOutputFormat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("SCREEN",
                mWidth, mHeight,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface,
                null,
                null);
    }

    @Override
    public void run() {
        if(mediaCodec!=null){
            mediaCodec.start();
        }
        if(mediaMuxer!=null){
            mediaMuxer.start();
        }
        while (!mQuit.get()) {
            int eobIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    FlyLog.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    FlyLog.d("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    FlyLog.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" + mediaCodec.getOutputFormat().toString());
//                    sendAVCDecoderConfigurationRecord(0, mediaCodec.getOutputFormat());
                    FlyLog.d("send format");
                    break;
                default:
                    FlyLog.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                    if (startTime == 0) {
                        startTime = mBufferInfo.presentationTimeUs / 1000;
                    }
                    /**
                     * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                     * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                     */
                    if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                        ByteBuffer realData = mediaCodec.getOutputBuffer(eobIndex);
//                        realData.position(mBufferInfo.offset + 4);
//                        realData.limit(mBufferInfo.offset + mBufferInfo.size);
//                        sendRealData((mBufferInfo.presentationTimeUs / 1000) - startTime, realData);
                        mediaMuxer.writeSampleData(videoTrackIndex, realData, mBufferInfo);
                        FlyLog.d("send buffer");
                    }
                    mediaCodec.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
        try{
            mediaMuxer.stop();
        }catch (Exception e){
            e.printStackTrace();
        }
        mediaMuxer.release();
    }


    public static void stopRun(){
        mQuit.set(true);
    }
}
