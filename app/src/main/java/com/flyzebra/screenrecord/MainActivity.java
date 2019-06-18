package com.flyzebra.screenrecord;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void startRecord(View view) {
        if (isPermission) {
            moveTaskToBack(true);
        }
    }
}
