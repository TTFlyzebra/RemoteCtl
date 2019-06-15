package com.flyzebra.screenrecord.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.CopyOnWriteArrayList;
import android.util.Log;

import com.flyzebra.screenrecord.ScreenRecordActivity;

/** Platform implementation of the rotation lock controller. **/
public final class RecordScreenControllerImpl implements RecordScreenController {
    private final String TAG = "RecordScreenControllerImpl";
    private int REQUEST_CODE = 1;
    private final Context mContext;
    private final CopyOnWriteArrayList<RecordScreenListener> mListeners = new CopyOnWriteArrayList<RecordScreenListener>();
    private boolean mRecordScreenEnabled;
    private boolean mTorchAvailable = true;
    private boolean mRecordScreenControllerEnabled;

    public RecordScreenControllerImpl(Context context) {
        mContext = context;
    }

    public boolean getRecordScreenController(){
        return mRecordScreenControllerEnabled;
    }

    public void setRecordScreenController(boolean enabled) {
        synchronized (this) {
            mRecordScreenControllerEnabled = enabled;
            dispatchModeChanged();
        }
    }

    @SuppressLint("LongLogTag")
    public void dispatchModeChanged(){
        Log.d(TAG,"dispatchModeChanged ScreenRecordActivity.instance = "+ScreenRecordActivity.instance+" ScreenUtil.isRecordScreen() = "+ScreenUtil.isRecordScreen());
        if(ScreenRecordActivity.instance == null){
            Intent intent = new Intent(mContext,ScreenRecordActivity.class);
            mContext.startActivity(intent);
        }else if(!ScreenUtil.isRecordScreen()){
//            ScreenRecordActivity.instance.collapseStatusBar();
            Intent intent = new Intent(mContext,ScreenRecordActivity.class);
            intent.putExtra("srartRecordScreen",true);
            mContext.startActivity(intent);
        }else{
            ScreenUtil.stopScreenRecord();
        }

    }

    public boolean isEnabled(){
        return mRecordScreenEnabled;
    }

    public synchronized boolean isAvailable() {
        return mTorchAvailable;
    }

    public void setRecordScreen(boolean newState){
        mRecordScreenEnabled = newState;
    }

    public void addCallback(RecordScreenController.RecordScreenListener callback) {
        mListeners.add(callback);
        notifyChanged(callback);
    }

    public void removeCallback(RecordScreenListener callback) {
        mListeners.remove(callback);
    }


    private void notifyChanged() {
        for (RecordScreenListener listener : mListeners) {
            notifyChanged(listener);
        }
    }

    private void notifyChanged(RecordScreenListener listener) {
        listener.onRecordScreenChanged(false);
    }
}
