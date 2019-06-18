package com.flyzebra.screenrecord.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.flyzebra.screenrecord.R;


public class ScreenRecordActivity extends Activity {

    private static final String TAG = "ScreenRecordActivity";
    private static final int START_COUNTING = 1;
    private static final int COUNT_NUMBER = 3;
    private TextView mTvTime;
    private int REQUEST_CODE = 1;
    private MyHandler mHandler = new MyHandler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.screen_record_activity);
        mTvTime = findViewById(R.id.count_text);
    }

    public void startCountTime(){
        Log.d(TAG,"startCountTime");
        Message msg = mHandler.obtainMessage();
        msg.what = START_COUNTING;
        msg.obj = COUNT_NUMBER;
        mHandler.sendMessageDelayed(msg, 10);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                moveTaskToBack(true);
//                overridePendingTransition(R.anim.record_screen_anim_in, R.anim.record_screen_anim_out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case START_COUNTING:
                    int count = (int) msg.obj;
                    mTvTime.setText(count + "");
                    if (count > 0) {
                        Message msg1 = obtainMessage();
                        msg1.what = START_COUNTING;
                        msg1.obj = count - 1;
                        sendMessageDelayed(msg1, 1000);
                    } else {
                        //TODO::
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
