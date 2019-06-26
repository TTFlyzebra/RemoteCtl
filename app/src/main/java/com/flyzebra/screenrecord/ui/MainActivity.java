package com.flyzebra.screenrecord.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.flyzebra.screenrecord.R;
import com.flyzebra.screenrecord.task.ScreenRecorder;
import com.flyzebra.screenrecord.service.RecordService;

import java.util.ArrayList;
import java.util.List;

/**
 * Author FlyZebra
 * 2019/6/15 15:09
 * Describ:
 **/
public class MainActivity extends Activity {
    private String[] mPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private boolean isPermission = false;
    private final int REQUEST_CODE = 102;
    private MediaProjectionManager mMediaProjectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        verifyPermissions();

    }

    public void verifyPermissions() {
        List<String> applyPerms = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                applyPerms.add(permission);
            }
        }
        if (!applyPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this, applyPerms.toArray(new String[applyPerms.size()]), REQUEST_CODE);
        } else {
            //TODO::RUN
            isPermission = true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean authorized = true;
            for (int num : grantResults) {
                if (num != PackageManager.PERMISSION_GRANTED) {
                    authorized = false;
                    break;
                }
            }
            if (authorized) {
                //TODO::RUN
                isPermission = true;
            }
        }
    }

    public void captureAndStartRecord(View view) {
        if (isPermission) {
            if (mMediaProjectionManager != null && !ScreenRecorder.getInstance().isRunning()) {
                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (mMediaProjectionManager != null) {
                startRecord(mMediaProjectionManager, resultCode, data);
            }
        }
    }

    private void startRecord(MediaProjectionManager mMediaProjectionManager, int resultCode, Intent data) {
        moveTaskToBack(true);
        startService(new Intent(this, RecordService.class));
        ScreenRecorder.getInstance().start(mMediaProjectionManager.getMediaProjection(resultCode, data));
    }

    public void stopRecord(View view) {
        ScreenRecorder.getInstance().stop();
        sendBroadcast(new Intent(RecordService.MAIN_ACTION_BROADCAST_EXIT));
    }

    public void playRecord(View view) {
        startActivity(new Intent(this,PlayActivity.class));
    }
}
