package com.flyzebra.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import com.flyzebra.screenrecord.ScreenRecordService;

public class ScreenUtil {

    private final static String TAG = "ScreenUtil";

    private static ScreenRecordService mScreenRecordService;

    private static List<RecordListener> mRecordListener = new ArrayList<>();

    private static List<OnPageRecordListener> mPageRecordListener = new ArrayList<>();

    public static boolean mIsRecordingTipShowing = false;

    /**
     * Only after the 5 version of the screen can be used
     * @return
     */
    public static boolean isScreenRecordEnable(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ;

    }


    public static void setScreenService(ScreenRecordService screenService){
        mScreenRecordService = screenService;
    }

    public static void clear(){
        if ( isScreenRecordEnable() && mScreenRecordService != null){
            mScreenRecordService.clearAll();
            mScreenRecordService = null;

        }

        if (mRecordListener != null && mRecordListener.size() > 0){
            mRecordListener.clear();
        }

        if (mPageRecordListener != null && mPageRecordListener.size() > 0 ){
            mPageRecordListener.clear();
        }
    }

    public static void startScreenRecord(Activity activity,int requestCode) {
        Log.d(TAG,"startScreenRecord isScreenRecordEnable() = "+isScreenRecordEnable()+" mScreenRecordService = "+mScreenRecordService);
        clearRecordElement();
        if (isScreenRecordEnable()){

            if (mScreenRecordService != null && !mScreenRecordService.ismIsRunning()){

                if (!mScreenRecordService.isReady()){

                    MediaProjectionManager mediaProjectionManager =
                            (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    if (mediaProjectionManager != null){
                        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY) != null){
                            Log.d(TAG,"startScreenRecord start");
                            activity.startActivityForResult(intent,requestCode);
                        }else {
//                            Toast.makeText(activity,R.string.can_not_record_tip,Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    mScreenRecordService.startRecord();

                }

            }
        }

    }

    /**
     * After obtaining the user permission screen, set the necessary data.
     * @param resultCode
     * @param resultData
     */
    public static void setUpData(int resultCode,Intent resultData) throws Exception{

        if (isScreenRecordEnable()){

            if (mScreenRecordService != null && !mScreenRecordService.ismIsRunning()){
                mScreenRecordService.setResultData(resultCode,resultData);
                mScreenRecordService.startRecord();
            }
        }
    }

    /**
     * stop record screen
     */
    public static void stopScreenRecord(Context context){
        Log.d(TAG,"stopScreenRecord mScreenRecordService = "+isScreenRecordEnable());
        if (isScreenRecordEnable()){
            if(mScreenRecordService != null)
                Log.d(TAG,"stopScreenRecord mScreenRecordService.ismIsRunning() = "+mScreenRecordService.ismIsRunning());
            if (mScreenRecordService != null && mScreenRecordService.ismIsRunning()){
//                String str = context.getString(R.string.record_video_tip);
                mScreenRecordService.stopRecord("record_video_tip");
            }
        }
    }

    public static void stopScreenRecord(){
        Log.d(TAG,"stopScreenRecord mScreenRecordService = "+isScreenRecordEnable(),new Throwable());
        if (isScreenRecordEnable()){
            if(mScreenRecordService != null)
                Log.d(TAG,"stopScreenRecord mScreenRecordService.ismIsRunning() = "+mScreenRecordService.ismIsRunning());
            if (mScreenRecordService != null && mScreenRecordService.ismIsRunning()){
                mScreenRecordService.stopRecord("stop");
            }
        }
    }

    public static boolean isRecordScreen(){
        if (mScreenRecordService != null && mScreenRecordService.ismIsRunning()){
            return true;
        }
        return false;
    }

    /**
     * record file save  path
     * @return
     */
    public static String getScreenRecordFilePath(){

        if (isScreenRecordEnable() && mScreenRecordService!= null) {
            return mScreenRecordService.getRecordFilePath();
        }
        return null;

    }

    /**
     * Determine whether or not it is currently being recorded
     * @return
     */
    public static boolean isCurrentRecording(){
        if (isScreenRecordEnable() && mScreenRecordService!= null) {
            return mScreenRecordService.ismIsRunning();
        }
        return false;
    }

    /**
     * true,The hint at the end of the recording is showing
     * @return
     */
    public static boolean isRecodingTipShow(){
        return mIsRecordingTipShowing;
    }

    public static void setRecordingStatus(boolean isShow){
        mIsRecordingTipShowing = isShow;
    }


    /**
     * The system is recording, the app screen will be in conflict, and some data will be cleaned up.
     */
    public static void clearRecordElement(){

        if (isScreenRecordEnable()){
            if (mScreenRecordService != null ){
                mScreenRecordService.clearRecordElement();
            }
        }
    }

    public static void addRecordListener(RecordListener listener){

        if (listener != null && !mRecordListener.contains(listener)){
            mRecordListener.add(listener);
        }

    }

    public static void removeRecordListener(RecordListener listener){
        if (listener != null && mRecordListener.contains(listener)){
            mRecordListener.remove(listener);
        }
    }

    public static void addPageRecordListener( OnPageRecordListener listener){

        if (listener != null && !mPageRecordListener.contains(listener)){
            mPageRecordListener.add(listener);
        }
    }

    public static void removePageRecordListener( OnPageRecordListener listener){

        if (listener != null && mPageRecordListener.contains(listener)){
            mPageRecordListener.remove(listener);
        }
    }

    public static void onPageRecordStart(){
        if (mPageRecordListener!= null && mPageRecordListener.size() > 0 ){
            for (OnPageRecordListener listener : mPageRecordListener){
                listener.onStartRecord();
            }
        }
    }


    public static void onPageRecordStop(){
        if (mPageRecordListener!= null && mPageRecordListener.size() > 0 ){
            for (OnPageRecordListener listener : mPageRecordListener){
                listener.onStopRecord();
            }
        }
    }

    public static void onPageBeforeShowAnim(){
        if (mPageRecordListener!= null && mPageRecordListener.size() > 0 ){
            for (OnPageRecordListener listener : mPageRecordListener){
                listener.onBeforeShowAnim();
            }
        }
    }

    public static void onPageAfterHideAnim(){
        if (mPageRecordListener!= null && mPageRecordListener.size() > 0 ){
            for (OnPageRecordListener listener : mPageRecordListener){
                listener.onAfterHideAnim();
            }
        }
    }

    public static void startRecord(){
        if (mRecordListener.size() > 0 ){
            for (RecordListener listener : mRecordListener){
                listener.onStartRecord();
            }
        }
    }

    public static void pauseRecord(){
        if (mRecordListener.size() > 0 ){
            for (RecordListener listener : mRecordListener){
                listener.onPauseRecord();
            }
        }
    }

    public static void resumeRecord(){
        if (mRecordListener.size() > 0 ){
            for (RecordListener listener : mRecordListener){
                listener.onResumeRecord();
            }
        }
    }

    public static void onRecording(String timeTip){
        if (mRecordListener.size() > 0 ){
            for (RecordListener listener : mRecordListener){
                listener.onRecording(timeTip);
            }
        }
    }

    public static void stopRecord(String stopTip){
        if (mRecordListener.size() > 0 ){
            for (RecordListener listener : mRecordListener){
                listener.onStopRecord( stopTip);
            }
        }
    }

    public interface RecordListener{

        void onStartRecord();
        void onPauseRecord();
        void onResumeRecord();
        void onStopRecord(String stopTip);
        void onRecording(String timeTip);
    }


    public interface OnPageRecordListener {

        void onStartRecord();
        void onStopRecord();

        void onBeforeShowAnim();
        void onAfterHideAnim();
    }
}
