package com.cbc.android;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class Logger {
  public class LogViews {
    private class NamedView {
      private String name;
      private View view;

      private NamedView(String name, View view) {
        this.name = name;
        this.view = view;

        maxName  = getMax(maxName, name);
        maxClass = getMax(maxClass, view.getClass().getSimpleName());
      }
    }
    private int maxName  = 0;
    private int maxClass = 0;

    private int getMax(int size, String value) {
      return value.length() > size ? value.length() : size;
    }

    private ArrayList<NamedView> views = new ArrayList<>();

    public void add(String name, View view) {
      views.add(new NamedView(name, view));
    }
    public void addAll(String name, View view) {
      ScreenUtils.ViewParent viewParent = new ScreenUtils.ViewParent(view);

      if (viewParent.getView() != null) addAll("", viewParent.getView());

      add(name, view);
    }
    public void clear() {
      views.clear();
      maxName = 0;
      maxClass = 0;
    }

    public void log() {
      int[] lc = new int[2];
      StringBuffer ln = new StringBuffer();

      ln.append(rPad("Name", maxName + 1));
      ln.append("Id          ");
      ln.append(rPad("Class", maxClass + 1));
      ln.append("Width Height Screen Y  Top Bottom Parent Id   Class");
      Logger.log(Type.D, ln.toString());

      for (NamedView v : views) {
        ScreenUtils.ViewParent viewParent = new ScreenUtils.ViewParent(v.view);
        v.view.getLocationOnScreen(lc);
        ln.delete(0, ln.length());
        ln.append(rPad(v.name, maxName + 1));
        ln.append(rPad(v.view.getId(), 11) + ' ');
        ln.append(rPad(v.view.getClass().getSimpleName(), maxClass + 1));
        ln.append(lPad(v.view.getWidth(), 5));
        ln.append(lPad(v.view.getHeight(), 7));
        ln.append(lPad(lc[1], 9));
        ln.append(lPad(v.view.getTop(), 5));
        ln.append(lPad(v.view.getBottom(), 7));

        if (viewParent.exists()) {
          ln.append(' ' + rPad(viewParent.getId(), 11) + ' ');
          ln.append(viewParent.getClassName());
        }
        Logger.log(Type.D, ln.toString());
      }
    }
  }
  public LogViews createLogViews() {
    return new LogViews();
  }
  private static boolean displayDebug = true;
  private String tag = "Not Set";
  private enum Type {D, I, W, E};

  public static String lPad(String value, int length) {
    while (value.length() < length) value = ' ' + value;

    return value;
  }
  public static void setDebug(boolean on) {
    displayDebug = on;
  }
  public static String rPad(String value, int length) {
    while (value.length() < length) value += ' ';

    return value;
  }
  public static String lPad(int value, int length) {
    return lPad(value + "", length);
  }
  public static String rPad(int value, int length) {
    return rPad(value + "", length);
  }
  public Logger(String tag) {
    this.tag = tag;
  }
  public void info(String message) {
    Log.i(tag, message);
  }
  public void warning(String message) {
    Log.w(tag, message);
  }
  public void error(String message) {
    Log.e(tag, message);
  }
  public void error(String message, Exception exception) {
    Log.e(tag, message, exception);
  }

  static public void o(Type type, String message) {
    switch (type) {
      case I:
        Log.i("", message);
        break;
      case W:
        Log.w("", message);
        break;
      case D:
        if (!displayDebug) return;
        Log.d("", message);
        break;
      case E:
        Log.e("", message);
        break;
    }
  }
  static private void log(Type type, String message) {
    o(type, message);
  }
  static private void log(Type type, String id, String value) {
    log(type, id + (value == null? "" : " " + value));
  }
  static private void log(Type type, String id, int value) {
    log(type, id, "" + value);
  }
  static private void log(Type type, String id, float value) {
    log(type, id, "" + value);
  }
  static private void log(Type type, String id, int pad, String value) {
    log(type, rPad(id, pad), value);
  }
  static public void debug(String message) {
    o(Type.D, message);
  }

  static public void log(String description, View view) {
    Log.i("", description);
    Log.i("", "MeasuredHeight " + view.getMeasuredHeight());
    Log.i("", "PaddingTop     " + view.getPaddingTop());
    Log.i("", "PaddingBottom  " + view.getPaddingBottom());
    Log.i("", "PaddingLeft    " + view.getPaddingLeft());
    Log.i("", "PaddingRight   " + view.getPaddingRight());
    Log.i("", "LayoutParams   width " + view.getLayoutParams().width + " height " + view.getLayoutParams().height);
  }
  static public void logDisplayStats() {
    EnumUtils<?>   bp      = new EnumUtils(DeviceDetails.BuildProperty.class);
    DisplayMetrics metrics = DeviceDetails.getMetrics();
    int            pad     = bp.maxWidth() + 1;

    Log.i("", "Build");

    for (DeviceDetails.BuildProperty p : DeviceDetails.BuildProperty.values()) {
      Log.i("", rPad(p.name(), pad) + DeviceDetails.get(p));
    }
    Log.i("","Display Stats");
    Log.i("", "Height Pixels " + metrics.heightPixels);
    Log.i("", "Height Inches " + TextSizer.getValue(metrics.heightPixels, TextSizer.Units.IN));
    Log.i("", "Width  Pixels " + metrics.widthPixels);
    Log.i("", "Width  Inches " + TextSizer.getValue(metrics.widthPixels, TextSizer.Units.IN));
    Log.i("", "Density       " + metrics.density);
    Log.i("", "Density DPI   " + metrics.densityDpi);
    Log.i("", "Scale Density " + metrics.scaledDensity);
    Log.i("", "X DPI         " + metrics.xdpi);
    Log.i("", "Y DPI         " + metrics.ydpi);
  }
  static public void log(View view, String name) {
    if (view == null) {
      if (name != null) Logger.log(Type.D, "View " + name + " not found");

      return;
    }
    View         parent   = (View) view.getParent();
    StringBuffer msg      = new StringBuffer();
    int[]        location = new int[2];

    msg.append("View ");

    if (name != null) msg.append(name + " ");

    msg.append("id " + view.getId() + " ");
    msg.append("class " + view.getClass().getSimpleName());

    if (parent != null) {
      msg.append(" parent id " + parent.getId() + " ");
      msg.append("class " + parent.getClass().getSimpleName());
    }
    view.getLocationOnScreen(location);
    log(Type.D, msg.toString());
    log(Type.D, "Width   ", view.getWidth());
    log(Type.D, "Height  ", view.getHeight());
    log(Type.D, "Screen Y", location[1]);
    log(Type.D, "Left    ", view.getLeft());
    log(Type.D, "Top     ", view.getTop());
    log(Type.D, "Bottom  ", view.getBottom());
  }
  static public void log(View view) {
    log(view, null);
  }
}
