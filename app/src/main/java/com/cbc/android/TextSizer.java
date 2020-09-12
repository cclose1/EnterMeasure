package com.cbc.android;

import android.content.Context;
import android.util.TypedValue;
import android.widget.EditText;

public class TextSizer {
    private float    pixelSize = 16f;
    private Context  context   = null;
    private EditText sizer     = null;

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
}
