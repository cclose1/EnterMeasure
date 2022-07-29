package com.cbc.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

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
    /*
     * Adds value to store value name, which must be a set.
     */
    public void addValue(String name, String value) {
        Set<String> current = store.getStringSet(name, null);
        Set<String> values  = new HashSet<String>();
        /*
         * Updating the current values does not work as current object has to change to force
         * an update to file storage. Adding the new value to current only affects the memory storage
         */
        if (current != null) {
            values.addAll(current);
        }
        values.add(value);
        editor.putStringSet(name, values);
        editor.commit();
    }
    public Set<String> getValues(String name) {
        Set<String> values = store.getStringSet(name, null);

        if (values == null) values = new HashSet<>();

        return values;
    }
    public void remove(String name) {
        editor.remove(name);
        editor.apply();
    }
}
