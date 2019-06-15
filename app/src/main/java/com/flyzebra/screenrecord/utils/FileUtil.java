package com.flyzebra.screenrecord.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;

public class FileUtil {

    /**
     * Delete files or directories in the SD card
     *
     * @param path
     * @return
     */
    public static boolean deleteSDFile(String path) {
        return deleteSDFile(path, false);
    }

    /**
     * Delete files or directories in the SD card
     *
     * @param path
     * @param deleteParent true为删除父目录
     * @return
     */
    public static boolean deleteSDFile(String path, boolean deleteParent) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            return true;
        }
        return deleteFile(file, deleteParent);
    }

    /**
     *Create files or directories in the SD card
     *
     * *@param path
     * @return
     */
    public static boolean createSDFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
            return true;
        }
        return true;
    }

    /**
     * @param file
     * @param deleteParent true is delete parent directories
     * @return
     */
    public static boolean deleteFile(File file, boolean deleteParent) {
        boolean flag = false;
        if (file == null) {
            return flag;
        }
        if (file.isDirectory()) {
            //directories
            File[] files = file.listFiles();
            if (files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    flag = deleteFile(files[i], true);
                    if (!flag) {
                        return flag;
                    }
                }
            }
            if (deleteParent) {
                flag = file.delete();
            }
        } else {
            flag = file.delete();
        }
        file = null;
        return flag;
    }

    /**
     * Add to the media database
     *
     * @param context
     */
    public static Uri fileScanVideo(Context context, String videoPath, int videoWidth, int videoHeight,
                                    int videoTime) {

        File file = new File(videoPath);
        if (file.exists()) {

            Uri uri = null;

            long size = file.length();
            String fileName = file.getName();
            long dateTaken = System.currentTimeMillis();

            ContentValues values = new ContentValues(11);
            values.put(MediaStore.Video.Media.DATA, videoPath); // path;
            values.put(MediaStore.Video.Media.TITLE, fileName); // title;
            values.put(MediaStore.Video.Media.DURATION, videoTime * 1000); // time
            values.put(MediaStore.Video.Media.WIDTH, videoWidth); // width
            values.put(MediaStore.Video.Media.HEIGHT, videoHeight); // height
            values.put(MediaStore.Video.Media.SIZE, size); // size;
            values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken); // date;
            values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);// filename;
            values.put(MediaStore.Video.Media.DATE_MODIFIED, dateTaken / 1000);// modify time;
            values.put(MediaStore.Video.Media.DATE_ADDED, dateTaken / 1000); // add time;
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

            ContentResolver resolver = context.getContentResolver();

            if (resolver != null) {
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                } catch (Exception e) {
                    e.printStackTrace();
                    uri = null;
                }
            }

            if (uri == null) {
                MediaScannerConnection.scanFile(context, new String[]{videoPath}, new String[]{"video/*"}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
            }

            return uri;
        }

        return null;
    }

    /**
     * The SD card exists and can be used
     */
    public static boolean isSDExists() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * The residual capacity of the SD card is obtained, the unit is Byte
     *
     * @return
     */
    public static long getSDFreeMemory() {
        try {
            if (isSDExists()) {
                File pathFile = Environment.getExternalStorageDirectory();
                // Retrieve overall information about the space on a filesystem.
                // This is a Wrapper for Unix statfs().
                StatFs statfs = new StatFs(pathFile.getPath());
                // Get the SIZE of every block on the SDCard
                long nBlockSize = statfs.getBlockSize();
                // Getting the number of Block used by the program
                // long nAvailBlock = statfs.getAvailableBlocksLong();
                long nAvailBlock = statfs.getAvailableBlocks();
                // Calculating the remaining size of SDCard Byte
                long nSDFreeSize = nAvailBlock * nBlockSize;
                return nSDFreeSize;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
