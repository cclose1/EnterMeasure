package com.cbc.android;

import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public class EditTextHandler {
  private EditText text;
  private Alert    alert;
  private String   label;

  protected EditTextHandler(Alert alerter, String label) {
    alert      = alerter;
    this.label = label;
  }
  protected void setView(View view) {
    text = (EditText) view;
  }
  public EditTextHandler(View view) {
    this(null, null);
    setView(view);
  }
  public EditTextHandler(View view, Alert alerter, String label) {
    this(alerter, label);
    setView(view);
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
    text.setFocusable(yes);
  }
  public void setReadOnly(boolean yes) {
    text.setFocusable(!yes);
  }
  public int getId() {
    return  text.getId();
  }
  public void alert(String message) {
    alert.display("Validation Error", (label != null? "For " + label + "-" : "") + message);
    setFocus();
  }
  public boolean checkPresent() {
    if (getText().length() != 0) return true;

    alert("Must provide a value");

    return false;
  }
  public void setVisible(boolean yes) {
    if (yes) {
      text.setVisibility(View.VISIBLE);
    } else {
      text.setVisibility(View.GONE);
    }
  }
  public void setLines(int min, int max) {
    text.setSingleLine(false);
    text.setMinLines(min);
    text.setMaxLines(max);
  }
  public void setLines(int lines) {
    text.setSingleLine(false);
    text.setLines(lines);
  }
  public void setOnClickListener(View.OnClickListener listener) {
    text.setOnClickListener(listener);
  }
  public void setBackgroundColor(int color) {
    text.setBackgroundColor(color);
  }
  public void setLister(TextWatcher tw) {
    text.addTextChangedListener(tw);
  }
  public EditText getEditText() {
      return text;
  }
}
