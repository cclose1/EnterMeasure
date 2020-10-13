package com.cbc.android;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.EditText;

public class TextSizer {
    public enum Units {
        MM((float)0.03937), CM((float)0.393701), IN((float)1.0), DP(metrics.density / metrics.densityDpi);

        private float multiplier;

        Units(float toInches) {
            multiplier = toInches;
        }

        public float getInches(float quantity) {
            return multiplier * quantity;
        }
        public float getUnits(float inches) {
            return inches / multiplier;
        }
    }
    private static DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
    private float          pixelSize = 16f;
    private Context        context   = null;
    private EditText       sizer     = null;

    public float convertToPx(int units, int size) {
        return TypedValue.applyDimension(units, size, context.getResources().getDisplayMetrics());
    }
    public void setPixelSize(int size) {
        setPixelSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }
    public void setPixelSize(float size) {
        pixelSize = size;
        sizer.setTextSize(pixelSize);
    }
    public TextSizer(Context context) {
        this.context = context;
        this.sizer   = new EditText(context);
        setPixelSize(16f);
    }
    public TextSizer(Context context, int sizeInDP) {
        this(context);
        setPixelSize(sizeInDP);
    }
    public TextSizer(Context context, float pixelSize) {
        this(context);
        setPixelSize(pixelSize);
    }
    public void setPixelSize(int units, int size) {
        setPixelSize(convertToPx(units, size));
    }
    public float getPixelSize() {
        return pixelSize;
    }
    public float getTextMeasure(String text) {
        return sizer.getPaint().measureText(text);
    }
    public float getTextMeasure(int charCount) {
        return charCount * pixelSize;
    }

    public static float getInches(float value, Units units) {
        return (float) (units.getInches(value));
    }
    public static float getPixels(float value, Units units) {
        return getInches(value, units) * metrics.densityDpi;
    }
    public static float getValue(float pixels, Units units) {
        float inches = pixels / metrics.densityDpi;

        return units.getUnits(inches);
    }
}
