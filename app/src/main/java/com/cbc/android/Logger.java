package com.cbc.android;

import android.util.Log;

public class Logger {
    private String tag = "Not Set";

    public static String lPad(String value, int length) {
        while (value.length() < length) value = ' ' + value;

        return value;
    }
    public static String rPad(String value, int length) {
        while (value.length() < length) value += ' ';

        return value;
    }
    public static String lPad(int value, int length) {
        return lPad(value + "", length);
    }

    public Logger(String tag) {
        this.tag = tag;
    }
    public void info(String message) {
        Log.i(tag, message);
    }
}
