package com.flyzebra.screenrecord;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flyzebra.utils.CommonUtil;
import com.flyzebra.utils.FileUtil;
import com.flyzebra.utils.ScreenUtil;

import java.io.File;
import java.io.IOException;

/**
 * Author FlyZebra
 * 2019/6/15 9:40
 * Describ:
 **/

public class ScreenRecordService extends Service implements Handler.Callback {

    private final String TAG = "ScreenRecordService";
    private final String RecordScreenPath = "/RecordScreen/";
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    private boolean mIsRunning;
    private int mRecordWidth = CommonUtil.getScreenWidth();
    private int mRecordHeight = CommonUtil.getScreenHeight();
    private int mScreenDpi = CommonUtil.getScreenDpi();


    private int mResultCode;
    private Intent mResultData;

    //录屏文件的保存地址
    private String mRecordFilePath;

    private Handler mHandler;
    //已经录制多少秒了
    private int mRecordSeconds = 0;

    private static final int MSG_TYPE_COUNT_DOWN = 110;

    /**
     * 定义浮动窗口布局
     */
    LinearLayout mlayout;

    TextView recordTime;
    /**
     * 悬浮窗控件
     */
    ImageView recordHintButton;
    LinearLayout stopRecord;
    /**
     * 悬浮窗的布局
     */
    WindowManager.LayoutParams wmParams;
    LayoutInflater inflater;
    /**
     * 创建浮动窗口设置布局参数的对象
     */
    WindowManager mWindowManager;

    //触摸监听器
    GestureDetector mGestureDetector;

    FloatingListener mFloatingListener;

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIsRunning = false;
        mMediaRecorder = new MediaRecorder();
        mHandler = new Handler(Looper.getMainLooper(), this);
        ScreenUtil.addRecordListener(recordListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearRecordElement();
        try {
            if (mlayout != null) {
                // 移除悬浮窗口
                mWindowManager.removeView(mlayout);
            }
        } catch (Exception e) {
            Log.e(TAG, "not attached to window manager");
        }
    }

    public boolean isReady() {
        return mMediaProjection != null && mResultData != null;
    }

    public void clearRecordElement() {
        try {
            clearAll();
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (Exception e) {
            mMediaRecorder = null;
            Log.w(TAG, "clearRecordElement  exception e = " + e);
        } finally {
            mResultData = null;
            mIsRunning = false;
            mRecordSeconds = 0;
        }
    }

    public boolean ismIsRunning() {
        return mIsRunning;
    }

    public void setResultData(int resultCode, Intent resultData) {
        clearRecordElement();
        mResultCode = resultCode;
        mResultData = resultData;

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mProjectionManager != null) {
            mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean startRecord() {
        Log.d(TAG, "startRecord mIsRunning = " + mIsRunning);

        initWindow();//设置窗口的参数
        initFloating();//设置悬浮窗图标
        if (mMediaProjection == null) {
            mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
        }
        setUpMediaRecorder();
        createVirtualDisplay();
        mMediaRecorder.start();
        ScreenUtil.startRecord();
        //最多录制三分钟
        mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
        mIsRunning = true;
        return true;
    }

    public boolean stopRecord(String tip) {
        Log.d(TAG, "stopRecord mIsRunning = " + mIsRunning);
        if (mlayout != null) {
            // 移除悬浮窗口
            mWindowManager.removeView(mlayout);
        }

        mIsRunning = false;
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                mMediaRecorder = null;
                mMediaRecorder = new MediaRecorder();
                Log.w(TAG, "stopRecord  exception e = " + e);
            } finally {
                mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);
                ScreenUtil.stopRecord(tip);
                if (mRecordSeconds <= 2) {
                    FileUtil.deleteSDFile(mRecordFilePath);
                } else {
                    //通知系统图库更新
                    FileUtil.fileScanVideo(this, mRecordFilePath, mRecordWidth, mRecordHeight, mRecordSeconds);
                }
                mVirtualDisplay.release();
                mMediaProjection.stop();
                mRecordSeconds = 0;
                mMediaProjection = null;
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }
        return true;
    }

    public void pauseRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder.pause();
            }
        }
    }

    public void resumeRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder.resume();
            }
        }
    }

    private void createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private void setUpMediaRecorder() {
        mRecordFilePath = getSaveDirectory() + File.separator + System.currentTimeMillis() + ".mp4";
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mRecordFilePath);


//        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(MainActivity.cSocket);
//        mMediaRecorder.setOutputFile(pfd.getFileDescriptor());

        mMediaRecorder.setVideoSize(mRecordWidth, mRecordHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate((int) (mRecordWidth * mRecordHeight * 3.6));
        mMediaRecorder.setVideoFrameRate(20);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearAll() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public String getRecordFilePath() {
        return mRecordFilePath;
    }

    public String getSaveDirectory() {

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + RecordScreenPath;
            FileUtil.createSDFile(path);
            return path;
        } else {
            return null;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_TYPE_COUNT_DOWN: {
                String str = null;
                boolean enough = FileUtil.getSDFreeMemory() / (1024 * 1024) < 4;
                if (enough) {
                    //空间不足，停止录屏
                    str = "空间不足，停止录屏";
                    stopRecord(str);
                    Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
                    break;
                }

                mRecordSeconds++;
                int minute = 0, second = 0;
                if (mRecordSeconds >= 60) {
                    minute = mRecordSeconds / 60;
                    second = mRecordSeconds % 60;
                } else {
                    second = mRecordSeconds;
                }
                ScreenUtil.onRecording("0" + minute + ":" + (second < 10 ? "0" + second : second + ""));

                if (mRecordSeconds < 3 * 60) {
                    mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
                } else if (mRecordSeconds == 3 * 60) {
                    str = "record_time_end_tip";
                    stopRecord(str);
                    Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }
        return true;
    }

    public class RecordBinder extends Binder {
        public ScreenRecordService getRecordService() {
            return ScreenRecordService.this;
        }
    }

    /**
     * 初始化windowManager
     */
    private void initWindow() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        }
        wmParams = getParams(wmParams);//设置好悬浮窗的参数
        // 悬浮窗默认显示以左上角为起始坐标
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
        wmParams.x = 0;
        wmParams.y = 0;
        //得到容器，通过这个inflater来获得悬浮窗控件
        if (inflater == null) {
            inflater = LayoutInflater.from(getApplication());
        }
        // 获取浮动窗口视图所在布局
        if (mlayout == null) {
            mlayout = (LinearLayout) inflater.inflate(R.layout.record_screen_time_float, null);
        }
        // 添加悬浮窗的视图
        mWindowManager.addView(mlayout, wmParams);
    }

    /**
     * 对windowManager进行设置
     *
     * @param wmParams
     * @return
     */
    public WindowManager.LayoutParams getParams(WindowManager.LayoutParams wmParams) {
        if (wmParams == null) {
            wmParams = new WindowManager.LayoutParams();
        }

        //设置window type 下面变量2002是在屏幕区域显示，2003则可以显示在状态栏之上
        //wmParams.type = LayoutParams.TYPE_PHONE;
        //wmParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        //wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置可以显示在状态栏上
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        return wmParams;
    }

    /**
     * 找到悬浮窗的图标，并且设置事件
     * 设置悬浮窗的点击、滑动事件
     */
    private void initFloating() {
        recordTime = (TextView) mlayout.findViewById(R.id.record_time);
        recordHintButton = (ImageView) mlayout.findViewById(R.id.record_hint_button);
        setFlickerAnimation(recordHintButton);
        stopRecord = (LinearLayout) mlayout.findViewById(R.id.stop_record);
        mlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "OnClickListener");
                ScreenUtil.stopScreenRecord(ScreenRecordService.this);
            }
        });
        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(this, new MyOnGestureListener());
        }
        if(mFloatingListener == null){
            //设置监听器
            mFloatingListener = new FloatingListener();
        }
        mlayout.setOnTouchListener(mFloatingListener);
        stopRecord.setOnTouchListener(mFloatingListener);

    }

    private void setFlickerAnimation(ImageView iv_chat_head) {
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); //
        iv_chat_head.setAnimation(animation);
    }

    private ScreenUtil.RecordListener recordListener = new ScreenUtil.RecordListener() {
        @Override
        public void onStartRecord() {

        }

        @Override
        public void onPauseRecord() {

        }

        @Override
        public void onResumeRecord() {

        }

        @Override
        public void onStopRecord(String stopTip) {
            //Toast.makeText(ScreenRecordActivity.this,stopTip,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRecording(String timeTip) {
            if (recordTime != null) {
                recordTime.setText(timeTip);
            }
        }
    };

    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;
    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private int mStartX, mStartY, mStopX, mStopY;
    private boolean isMove;//判断悬浮窗是否移动

    private class FloatingListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View arg0, MotionEvent event) {

            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isMove = false;
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mStartX = (int) event.getX();
                    mStartY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mTouchCurrentX = (int) event.getRawX();
                    mTouchCurrentY = (int) event.getRawY();
                    wmParams.x += mTouchCurrentX - mTouchStartX;
                    wmParams.y += mTouchCurrentY - mTouchStartY;
                    if (mlayout != null) {
                        mWindowManager.updateViewLayout(mlayout, wmParams);
                    }

                    mTouchStartX = mTouchCurrentX;
                    mTouchStartY = mTouchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    mStopX = (int) event.getX();
                    mStopY = (int) event.getY();
                    if (Math.abs(mStartX - mStopX) >= 1 || Math.abs(mStartY - mStopY) >= 1) {
                        isMove = true;
                    }
                    break;
            }
            return mGestureDetector.onTouchEvent(event);  //此处必须返回false，否则OnClickListener获取不到监听
        }

    }

    class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!isMove) {
                System.out.println("onclick");
            }
            return super.onSingleTapConfirmed(e);
        }
    }

}