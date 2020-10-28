package com.flyzebra.utils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by FlyZebra on 2016/6/20.
 */
public class TimeUtil {
    public static String ymdhms="yyyyMMdd_HHmmss";


    public static String getCurrentTime(String strFormat) {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat(strFormat, Locale.getDefault());
        return format.format(date);
    }

}
