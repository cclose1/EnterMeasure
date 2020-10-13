package com.cbc.android;

import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

public class DeviceDetails {
    private static boolean debugEnabled = true;

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    public static void setDebugEnabled(boolean debugEnabled) {
        DeviceDetails.debugEnabled = debugEnabled;
    }

    public static enum BuildProperty {Device, Manufacturer, Model, Product};

    private static DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();

    public static DisplayMetrics getMetrics() {
        return metrics;
    }
    public static String get(BuildProperty property) {
        switch (property) {
            case Device:
                return Build.DEVICE;
            case Manufacturer:
                return Build.MANUFACTURER;
            case Model:
                return Build.MODEL;
            case Product:
                return Build.PRODUCT;
        }
        return null;
    }
    /*
     * Attempts to deduce if in emulator from device details.
     */
    public static boolean isEmulator() {
        return get(BuildProperty.Model).startsWith("sdk");
    }
}
