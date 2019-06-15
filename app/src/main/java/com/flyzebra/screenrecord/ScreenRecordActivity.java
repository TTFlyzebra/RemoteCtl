package com.flyzebra.screenrecord;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;


import android.view.KeyEvent;

import com.flyzebra.screenrecord.utils.CommonUtil;
import com.flyzebra.screenrecord.utils.ScreenUtil;


public class ScreenRecordActivity extends Activity {

    private static final String TAG = "ScreenRecordActivity";
    private static final int START_COUNTING = 1;
    private static final int COUNT_NUMBER = 3;

    public static ScreenRecordActivity instance;

    private TextView mTvTime;

    private int REQUEST_CODE = 1;
    private MyHandler mHandler = new MyHandler();
    public boolean CancleCount = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        collapseStatusBar();
        setContentView(R.layout.screen_record_activity);
        CommonUtil.init(this.getApplication());
        mTvTime = findViewById(R.id.count_text);

        startScreenRecordService();
        instance = this;
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent != null && intent.getBooleanExtra("srartRecordScreen",false)){
            Log.d(TAG, "onNewIntent startCountTime");
            startCountTime();
        }
    }

    public void startCountTime(){
        Log.d(TAG,"startCountTime");
        Message msg = mHandler.obtainMessage();
        msg.what = START_COUNTING;
        msg.obj = COUNT_NUMBER;
        mHandler.sendMessageDelayed(msg, 10);
    }

//    /**
//     * collapse status bar
//     *
//     */
//    public void collapseStatusBar() {
//        try {
//            @SuppressLint("WrongConstant") Object statusBarManager = this.getSystemService("statusbar");
//            Method collapse;
//            if (Build.VERSION.SDK_INT <= 16) {
//                collapse = statusBarManager.getClass().getMethod("collapse");
//            } else {
//                collapse = statusBarManager.getClass().getMethod("collapsePanels");
//            }
//            collapse.invoke(statusBarManager);
//        } catch (Exception localException) {
//            localException.printStackTrace();
//        }
//    }

    private ServiceConnection mServiceConnection;

    /**
     * satrt record Service
     */
    private void startScreenRecordService() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ScreenRecordService.RecordBinder recordBinder = (ScreenRecordService.RecordBinder) service;
                ScreenRecordService screenRecordService = recordBinder.getRecordService();
                ScreenUtil.setScreenService(screenRecordService);
                startCountTime();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, ScreenRecordService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                moveTaskToBack(true);
//                overridePendingTransition(R.anim.record_screen_anim_in, R.anim.record_screen_anim_out);
                ScreenUtil.setUpData(resultCode, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
//            Toast.makeText(ScreenRecordActivity.this, ScreenRecordActivity.this.getResources().getString(R.string.refuse_record_screen), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            CancleCount = true;
            moveTaskToBack(true);
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_HOME){
            CancleCount = true;
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        ScreenUtil.stopScreenRecord(this);
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
        instance = null;
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case START_COUNTING:
                    if(CancleCount){
                        CancleCount = false;
                        return;
                    }
                    int count = (int) msg.obj;
                    mTvTime.setText(count + "");
                    if (count > 0) {
                        Message msg1 = obtainMessage();
                        msg1.what = START_COUNTING;
                        msg1.obj = count - 1;
                        sendMessageDelayed(msg1, 1000);
                    } else {
                        ScreenUtil.startScreenRecord(ScreenRecordActivity.this, REQUEST_CODE);
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
