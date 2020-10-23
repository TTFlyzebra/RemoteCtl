package com.flyzebra.scrcpy;

import android.app.Service;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.genymobile.scrcpy.Server;

import static com.genymobile.scrcpy.Server.main;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Server.main();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
