package com.cbc.android;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SpinnerHandler {
    Spinner   spinner;
    EnumUtils<? extends Enum<?>> enumUtils = null;

    private void checkSourceEnum() throws Exception {
        if (enumUtils == null) throw new Exception("Spinner (" + spinner.getId() + " is not sourced from an enumertion");
    }
    private void setValues(CharSequence[] values){
        spinner.setAdapter(new ArrayAdapter<CharSequence>(spinner.getContext(), android.R.layout.simple_spinner_item, values));
    }
    public SpinnerHandler(View view) {
        spinner = (Spinner) view;
    }
    public SpinnerHandler(View view, int arrayResId) {
        this(view);
        spinner.setAdapter(
                ArrayAdapter.createFromResource(
                        spinner.getContext(),
                        arrayResId,
                        android.R.layout.simple_spinner_item));
    }
    public SpinnerHandler(View view, CharSequence[] values) {
        this(view);
        setValues(values);
    }
    public <T extends Enum<T>> SpinnerHandler(View view, Class<T> enumType) {
        this(view);
        enumUtils = new EnumUtils(enumType);
        setValues(enumUtils.getValues());
    }
    public SpinnerHandler(View view, String values, String splitter) {
        this(view, values.split(splitter));
    }
    public Spinner getSpinner() {
        return spinner;
    }
    public String getSelected() {
        return spinner.getSelectedItem().toString();
    }
    public <T extends Enum<?>> T getEnumSelected() throws Exception {
        checkSourceEnum();

        return (T) enumUtils.valueOf(getSelected().toString());
    }
    public String getValueAtPosition(int position) {
        return spinner.getItemAtPosition(position).toString();
    }
    public <T extends Enum<?>> T getEnumValueAtPosition(int position) throws Exception {
        checkSourceEnum();

        return (T) enumUtils.valueOf(getValueAtPosition(position));
    }
    public int getIndex(String value) {
        return ((ArrayAdapter<CharSequence>)spinner.getAdapter()).getPosition(value);
    }
    public void setSelected(int index) {
        spinner.setSelection(index);
    }
    public void setSelected(String value) {
        spinner.setSelection(getIndex(value));
    }
    public <T extends Enum<T>> void setSelected(T value) throws Exception {
        spinner.setSelection(getIndex(value.toString()));
    }

}
