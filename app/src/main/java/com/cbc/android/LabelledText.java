package com.cbc.android;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class LabelledText {
    private TextView  label = null;
    private EditText text  = null;
    private ViewGroup group = null;
    private TextSizer sizer = null;
    private Context   context = null;
    private ConstraintLayoutHandler layout = null;

    private View add(View view) {
        view.setId(View.generateViewId());
        view.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));

        if (group != null)
            group.addView(view);
        else
            layout.addView(view);

        return view;
    }
    private void initialise(String label) {
        this.sizer = new TextSizer(context);
        this.label = (TextView) add(new TextView(context));
        this.text  = (EditText) add(new EditText(context));
        this.label.setText(label);
        this.label.setGravity(Gravity.CENTER_HORIZONTAL);
        this.label.setTextSize(sizer.getPixelSize());
        this.text.setBackground(null);
        this.text.setInputType(InputType.TYPE_CLASS_TEXT);
        this.text.setTextSize(sizer.getPixelSize());
        this.text.setSingleLine(false);
        this.text.setMaxLines(1);
    }
    public LabelledText(ConstraintLayoutHandler layout, String label) {
        this.layout  = layout;
        this.context = layout.getLayout().getContext();
        initialise(label);
    }
    public LabelledText(ViewGroup group, String label) {
        this.group   = group;
        this.context = group.getContext();
        initialise(label);
    }
    /*
     * This method will fail if the parent group is not a ConstraintLayout. This is possible if the
     * constructor that takes a ViewGroup as a parameter was used and the ViewGroup is not a ConstraintLayout.
     */
    public void setConstraint(int previousId, int horizontalPos, int horizontalId) {
        ConstraintLayoutHandler layout = this.layout;

        if (layout == null) layout = new ConstraintLayoutHandler(group);

        layout.connect(label.getId(), horizontalPos,        horizontalId,  horizontalPos);
        layout.connect(label.getId(), ConstraintSet.TOP,    previousId,    ConstraintSet.BOTTOM);
        layout.connect(text,  ConstraintSet.LEFT,   label, ConstraintSet.RIGHT,  10);
        layout.connect(text,  ConstraintSet.TOP,    label, ConstraintSet.TOP);
        layout.connect(text,  ConstraintSet.BOTTOM, label, ConstraintSet.BOTTOM);
        layout.apply();
    }
    public void setReadOnly(boolean yes) {
        text.setFocusable(!yes);
        text.setFocusableInTouchMode(!yes) ;
        text.setClickable(!yes);
        text.setLongClickable(!yes);
        text.setCursorVisible(!yes) ;
    }
    public EditText getText() {
        return text;
    }
    public TextView getLabel() {
        return label;
    }
    public void setTextSize(int dp) {
        label.setTextSize(sizer.convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp));
        text.setTextSize(sizer.convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp));
    }
    public void setText(String value) {
        text.setText(value);
    }
    public void setVisible(boolean yes) {
        if (yes) {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
            label.setVisibility(View.GONE);
        }
    }
}