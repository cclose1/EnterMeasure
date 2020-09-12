package com.cbc.android;

import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public class EditTextHandler {
  private EditText text;
  private Alert    alert;
  private String   label;

  public EditTextHandler(View view) {
    text = (EditText) view;
  }
  public EditTextHandler(View view, Alert alerter, String label) {
    text       = (EditText) view;
    alert      = alerter;
    this.label = label;
  }
  public String getText() {
    text.getPaint().measureText("sss");
    return text.getText().toString().trim();
  }
  public void setMaxLength(int maxLength) {
    if (maxLength > 0) {
      InputFilter[] filters  = text.getFilters();
      InputFilter[] filtersn = new InputFilter[filters.length + 1];
      System.arraycopy(filters, 0, filtersn, 0, filters.length);
      filtersn[filters.length] = new InputFilter.LengthFilter(maxLength);
      text.setFilters(filtersn);
    }
  }
  public int getInt() {
    return Integer.parseInt(getText());
  }
  public void setText(String value) {
    text.setText(value);
  }
  public void setText(int value) {
    text.setText(Integer.toString(value));
  }
  public void clear() {
    setText("");
  }
  public void setFocus() {
    text.requestFocus();
  }
  public void setFocusable(boolean yes) {
    /*
     * Tried setFocusable, but it did not appear to work.
     */
    text.setEnabled(yes);
  }
  public boolean checkPresent() {
    if (getText().length() != 0) return true;

    alert.display("Validation Error", "Must provide a value" + (label != null? " for " + label : ""));
    setFocus();

    return false;
  }
  public void setLister(TextWatcher tw) {
    text.addTextChangedListener(tw);
  }
}
