package com.flyzebra.utils;

import android.util.DisplayMetrics;
import android.content.Context;
import android.app.Application;
import android.view.WindowManager;

public class CommonUtil {

    private static int mScreenWidth;
    private static int mScreenHeight;

    private static int mScreenDpi;

    public static void init(Application activity){
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager mWindowManager  = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels; // 屏幕宽度（像素）
        mScreenHeight = metric.heightPixels; // 屏幕宽度（像素）
        mScreenDpi = metric.densityDpi;
    }

    public static int getScreenWidth(){
        return mScreenWidth;
    }

    public static int getScreenHeight(){
        return mScreenHeight;
    }

    public static int getScreenDpi(){
        return mScreenDpi;
    }
}
