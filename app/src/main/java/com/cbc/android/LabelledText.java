package com.cbc.android;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class LabelledText extends EditTextHandler {
    private TextView                label   = null;
    private ViewGroup               group   = null;
    private TextSizer               sizer   = null;
    private Context                 context = null;
    private ConstraintLayoutHandler layout  = null;

    private View add(View view) {
        view.setId(View.generateViewId());
        view.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));

        if (group != null)
            group.addView(view);
        else
            layout.addView(view);

        return view;
    }
    private void initialise(int labelGap, String displayWidth) {
        this.sizer = new TextSizer(context);
        this.label.setTextSize(sizer.getPixelSize());

        if (labelGap != -1) this.label.setPadding(0, 0, labelGap, 0);

        if (displayWidth.length() != 0) getEditText().setWidth(TextSizer.convertToPx(displayWidth));

        getEditText().setBackground(null);
        getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        getEditText().setTextSize(sizer.getPixelSize());
        getEditText().setPadding(0, 0, 0, 0);    //When created programmatically sets to non zero values
    }
    private void initialise(String label, int labelGap, String displayWidth) {
        this.label = (TextView) add(new TextView(context));
        setView(add(new EditText(context)));
        this.label.setText(label);

        initialise(labelGap, displayWidth);
    }
    public LabelledText(ConstraintLayoutHandler layout, String label, String width, Alert alerter) {
        super(alerter, label);
        this.layout  = layout;
        this.context = layout.getLayout().getContext();
        initialise(label, -1, width);
    }
    public LabelledText(ConstraintLayoutHandler layout, String label) {
        this(layout, label, "", null);
    }
    public LabelledText(ViewGroup group, String label, String width, Alert alerter) {
        super(alerter, label);
        this.group   = group;
        this.context = group.getContext();
        initialise(label, 10, width);
    }
    public LabelledText(View label, View text, String width, Alert alerter) {
        super(alerter, ((TextView) label).getText().toString());
        this.label   = (TextView) label;
        this.context = label.getContext();
        setView(text);
        initialise(-1, width);
    }
    public LabelledText(ViewGroup group, String label) {
        this(group, label,"", null);
    }
    /*
     * This method will fail if the parent group is not a ConstraintLayout. This is possible if the
     * constructor that takes a ViewGroup as a parameter was used and the ViewGroup is not a ConstraintLayout.
     */
    public void setConstraint(int previousId, int horizontalPos, int horizontalId) {
        ConstraintLayoutHandler layout = this.layout;

        if (layout == null) layout = new ConstraintLayoutHandler(group);

        layout.connect(label.getId(), horizontalPos,      horizontalId, horizontalPos);
        layout.connect(label.getId(), ConstraintSet.TOP,  previousId,   ConstraintSet.BOTTOM);
        layout.connect(getEditText(), ConstraintSet.LEFT, label,        ConstraintSet.RIGHT,  10);
        layout.connect(getEditText(), ConstraintSet.TOP,  label,        ConstraintSet.TOP);
        layout.apply();
    }
    public void setReadOnly(boolean yes) {
        getEditText().setFocusable(!yes);
        getEditText().setFocusableInTouchMode(!yes) ;
        getEditText().setClickable(!yes);
        getEditText().setLongClickable(!yes);
        getEditText().setCursorVisible(!yes) ;
    }
    public TextView getLabel() {
        return label;
    }
    public void setTextSize(int dp) {
        label.setTextSize(sizer.convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp));
        getEditText().setTextSize(sizer.convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp));
    }
    public void setVisible(boolean yes) {
        if (yes) {
            label.setVisibility(View.VISIBLE);
            super.setVisible(yes);
        } else {
            label.setVisibility(View.GONE);
            super.setVisible(yes);
        }
    }
}