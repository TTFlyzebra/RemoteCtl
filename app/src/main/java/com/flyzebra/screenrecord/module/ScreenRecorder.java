package com.flyzebra.screenrecord.module;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import com.flyzebra.screenrecord.utils.FlyLog;

import java.io.IOException;

/**
 * Author FlyZebra
 * 2019/6/18 16:12
 * Describ:
 **/
public class ScreenRecorder {
    private MediaCodec mediaCodec;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25; // 30 fps
    private static final int IFRAME_INTERVAL = 2; // 2 seconds between I-frames
    private static final int TIMEOUT_US = 1000;
    private static final int BIT_RATE = 500000;
    private final int mWidth = 1280;
    private final int mHeight = 720;

    private Surface mSurface;


    private void prepareCodec() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        FlyLog.d( "created video format: " + format);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mediaCodec.createInputSurface();
            FlyLog.d( "created input surface: " + mSurface);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
