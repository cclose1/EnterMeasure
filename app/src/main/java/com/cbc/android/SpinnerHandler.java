package com.cbc.android;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SpinnerHandler {
    Spinner spinner;

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
        spinner.setAdapter(new ArrayAdapter<CharSequence>(spinner.getContext(), android.R.layout.simple_spinner_item, values));
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
    public int getIndex(String value) {
        return ((ArrayAdapter<CharSequence>)spinner.getAdapter()).getPosition(value);
    }
    public void setSelected(int index) {
        spinner.setSelection(index);
    }
    public void setSelected(String value) {
        spinner.setSelection(getIndex(value));
    }
}
