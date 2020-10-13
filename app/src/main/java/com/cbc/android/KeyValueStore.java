package com.cbc.android;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyValueStore {
    private String                   storeName = null;
    private SharedPreferences        store     = null;
    private SharedPreferences.Editor editor    = null;

    public KeyValueStore(Context context, String storeName, int mode) {
        this.storeName = storeName;
        this.store     = context.getSharedPreferences(storeName, mode);
        this.editor    = store.edit();
    }
    public KeyValueStore(Context context, String storeName) {
        this(context, storeName, Context.MODE_PRIVATE);
    }
    public void setValue(String name, String value) {
        editor.putString(name, value);
        editor.apply();
        editor.commit();
    }
    public String getValue(String name, String defValue) {
        return store.getString(name, defValue);
    }
    public void remove(String name) {
        editor.remove(name);
        editor.apply();
    }
}
