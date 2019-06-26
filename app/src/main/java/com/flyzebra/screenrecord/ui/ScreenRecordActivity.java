package com.flyzebra.screenrecord.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.flyzebra.screenrecord.R;
import com.flyzebra.screenrecord.task.ScreenRecorder;
import com.flyzebra.screenrecord.service.RecordService;


public class ScreenRecordActivity extends Activity {
    private ScreenRecorder mScreenRecorder;
    private TextView mTvTime;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mediaProjection;
    private int REQUEST_CODE = 1;

    private int count = 3;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable countTask = new Runnable() {
        @Override
        public void run() {
            mTvTime.setText(String.valueOf(count));
            count--;
            if (count < 0) {
                if (mediaProjection != null) {
                    //TODO::start record;
//
//                    mScreenRecorder = new ScreenRecorder(mediaProjection);
//                    finish();
                    moveTaskToBack(true);
                    startService(new Intent(ScreenRecordActivity.this,RecordService.class));
                }
                count = 3;
            }else{
                mHandler.postDelayed(this,1000);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_record_activity);
        mTvTime = findViewById(R.id.count_text);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (mMediaProjectionManager != null) {
            Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (mMediaProjectionManager != null) {
                mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                count = 3;
                mHandler.post(countTask);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

}
