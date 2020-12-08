package com.flyzebra.remotectl;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.EditText;

import com.flyzebra.remotectl.model.FlvRtmpClient;
import com.flyzebra.remotectl.task.VideoStream;
import com.flyzebra.utils.SPUtil;
import com.flyzebra.utils.SystemPropTools;

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

    private EditText et_width, et_height, et_bitrate, et_fps, et_iframe,et_remoteip,et_remoteport,et_rtmpurl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_width = findViewById(R.id.et_width);
        et_height = findViewById(R.id.et_height);
        et_bitrate = findViewById(R.id.et_bitrate);
        et_fps = findViewById(R.id.et_fps);
        et_iframe = findViewById(R.id.et_iframe);
        et_remoteip = findViewById(R.id.et_remoteip);
        et_remoteport = findViewById(R.id.et_remoteport);
        et_rtmpurl = findViewById(R.id.et_rtmpurl);

        FlvRtmpClient.VIDEO_WIDTH = (int) SPUtil.get(this, "VIDEO_WIDTH", 400);
        FlvRtmpClient.VIDEO_HEIGHT = (int) SPUtil.get(this, "VIDEO_HEIGHT", 712);
        FlvRtmpClient.VIDEO_BITRATE = (int) SPUtil.get(this, "VIDEO_BITRATE", 1000000);
        FlvRtmpClient.VIDEO_IFRAME_INTERVAL = (int) SPUtil.get(this, "VIDEO_IFRAME_INTERVAL", 5);
        FlvRtmpClient.VIDEO_FPS = (int) SPUtil.get(this, "VIDEO_FPS", 24);

        et_width.setText(String.valueOf(FlvRtmpClient.VIDEO_WIDTH));
        et_height.setText(String.valueOf(FlvRtmpClient.VIDEO_HEIGHT));
        et_bitrate.setText(String.valueOf(FlvRtmpClient.VIDEO_BITRATE));
        et_fps.setText(String.valueOf(FlvRtmpClient.VIDEO_FPS));
        et_iframe.setText(String.valueOf(FlvRtmpClient.VIDEO_IFRAME_INTERVAL));
        String host = SystemPropTools.get("persist.sys.remotectl.ip", "192.168.8.140");
        String port = SystemPropTools.get("persist.sys.remotectl.port", "9008");
        String rtmpurl = SystemPropTools.get("persist.sys.rtmp.url","rtmp://192.168.8.244/live/screen");
        et_remoteip.setText(host);
        et_remoteport.setText(port);
        et_rtmpurl.setText(rtmpurl);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            verifyPermissions();
        } else {
            isPermission = true;
        }

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
            if (mMediaProjectionManager != null && !VideoStream.getInstance().isRunning()) {
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
        SPUtil.set(this, "VIDEO_WIDTH", Integer.valueOf(et_width.getText().toString()));
        SPUtil.set(this, "VIDEO_HEIGHT", Integer.valueOf(et_height.getText().toString()));
        SPUtil.set(this, "VIDEO_BITRATE", Integer.valueOf(et_bitrate.getText().toString()));
        SPUtil.set(this, "VIDEO_IFRAME_INTERVAL", Integer.valueOf(et_iframe.getText().toString()));
        SPUtil.set(this, "VIDEO_FPS", Integer.valueOf(et_fps.getText().toString()));
        SystemPropTools.set("persist.sys.remotectl.ip", et_remoteip.getText().toString());
        SystemPropTools.set("persist.sys.remotectl.port", et_remoteport.getText().toString());
        SystemPropTools.set("persist.sys.rtmp.url",et_rtmpurl.getText().toString());
        FlvRtmpClient.VIDEO_WIDTH = (int) SPUtil.get(this, "VIDEO_WIDTH", 400);
        FlvRtmpClient.VIDEO_HEIGHT = (int) SPUtil.get(this, "VIDEO_HEIGHT", 712);
        FlvRtmpClient.VIDEO_BITRATE = (int) SPUtil.get(this, "VIDEO_BITRATE", 1000000);
        FlvRtmpClient.VIDEO_IFRAME_INTERVAL = (int) SPUtil.get(this, "VIDEO_IFRAME_INTERVAL", 5);
        FlvRtmpClient.VIDEO_FPS = (int) SPUtil.get(this, "VIDEO_FPS", 24);
        moveTaskToBack(true);
        startService(new Intent(this, MainService.class));
//        VideoStream.getInstance().start(mMediaProjectionManager.getMediaProjection(resultCode, data));
    }

    public void stopRecord(View view) {
//        VideoStream.getInstance().stop();
        sendBroadcast(new Intent(MainService.MAIN_ACTION_BROADCAST_EXIT));
    }

    public void playRecord(View view) {
        sendBroadcast(new Intent(MainService.MAIN_ACTION_BROADCAST_EXIT));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startActivity(new Intent(this, PlayActivity.class));
    }
}
