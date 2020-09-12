package com.cbc.android;

import java.util.ArrayList;

public class EnumUtils<T extends Enum<T>> {
    private Class enumType;

    public EnumUtils(Class<T> type) {
        this.enumType = type;
    }
    public String[] getValues() {
        ArrayList<String> values = new ArrayList<String> ();

        for (Object enumVal: enumType.getEnumConstants()) {
            values.add(enumVal.toString());
        }
        return values.toArray(new String[values.size()]);
    }
    public T valueOf(String value) {
        if (value == null) return null;

        return (T) Enum.valueOf(enumType, value);
    }
    public String toString(T value) {
        return value.toString();
    }
}
