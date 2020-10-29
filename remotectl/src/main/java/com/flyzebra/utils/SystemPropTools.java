package com.flyzebra.utils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ClassName: SystemPropTools
 * Description:
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 19-12-11 上午10:37
 */
public class SystemPropTools {
    private static Method propGet;
    private static Method propSet;

    private SystemPropTools() {
    }

    static {
        try {
            Class<?> S = Class.forName("android.os.SystemProperties");
            Method[] methods = S.getMethods();
            for (Method m : methods) {
                String n = m.getName();
                if (n.equals("get")) {
                    propGet = m;
                } else if (n.equals("set")) {
                    propSet = m;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String get(String name, String default_value) {
        try {
            return (String) propGet.invoke(null, name, default_value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return default_value;
    }

    public static void set(String name, String value) {
        try {
            propSet.invoke(null, name, value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static boolean getBoolen(String net_support_multi, boolean flag) {
        String str = get(net_support_multi,flag?"true":"fasle");
        return "true".equals(str);
    }
}
