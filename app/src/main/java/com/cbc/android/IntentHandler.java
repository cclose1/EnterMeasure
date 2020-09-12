package com.cbc.android;

import android.content.Intent;

public class IntentHandler {
    private Intent intent;

    public IntentHandler(Intent intent) {
        this.intent = intent;
    }
    public IntentHandler() {
        this(new Intent());
    }
    public Intent getIntent() {
        return intent;
    }

    public <T extends Enum<T>> T enumValueOf(Class<T> enumType, String value) {
        EnumUtils<T> en = new EnumUtils((enumType));

        if (value == null) return null;

        return (T) en.valueOf(value);
    }
    public String getStringExtra(String value) {
        return intent.getStringExtra(value);
    }
    public <T extends Enum<T>> T enumStringExtra(Class<T> enumType, String value) {
        return enumValueOf(enumType, intent.getStringExtra(value));
    }
    public String getAction() {
        return intent.getAction();
    }
    public <T extends Enum<T>> T getEnumAction(Class<T> enumType) {
        return enumValueOf(enumType, intent.getAction());
    }
}
