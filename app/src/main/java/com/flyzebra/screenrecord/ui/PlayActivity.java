package com.flyzebra.screenrecord.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.flyzebra.screenrecord.R;
import com.flyzebra.screenrecord.view.ExoPlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Author FlyZebra
 * 2019/6/26 11:17
 * Describ:
 **/
public class PlayActivity extends Activity {
    public ExoPlayerView exoPlayerView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        exoPlayerView = findViewById(R.id.exo_play);

        List<String> urls = new ArrayList<>();

        File file = new File("/sdcard/flyrecord");
        if(file.exists()&&file.isDirectory()){
            File files[] = file.listFiles();
            for(File f:files){
                if(!f.isDirectory()){
                    urls.add(f.getAbsolutePath());
                }
            }
        }
        exoPlayerView.playUris(urls);

    }
}
