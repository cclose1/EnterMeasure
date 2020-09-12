package com.cbc.android;

import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class ConstraintLayoutHandler {
    private ConstraintLayout layout = null;
    private ConstraintSet    cs     = new ConstraintSet();

    public ConstraintLayoutHandler(View layout) {
        this.layout = (ConstraintLayout) layout;

        cs.clone(this.layout);
    }
    public View addView(View view) {
        view.setId(View.generateViewId());
        layout.addView(view);
        cs.clone(layout);
        return view;
    }
    public void clear(int id, int anchor) {
        cs.clear(id, anchor);
    }
    public void clear(View view, int anchor) {
        cs.clear(view.getId(), anchor);
    }
    public void connect(int startID, int startSide, int endID, int endSide) {
        cs.connect(startID, startSide, endID, endSide);
    }
    public void connect(int startID, int startSide, int endID, int endSide, int margin) {
        cs.connect(startID, startSide, endID, endSide, margin);
    }
    public void connect(View start, int startSide, View end, int endSide) {
        connect(start.getId(), startSide, end.getId(), endSide);
    }
    public void connect(View start, int startSide, View end, int endSide, int margin) {
        connect(start.getId(), startSide, end.getId(), endSide, margin);
    }
    public void connect(View start, int startSide, int endId, int endSide) {
        connect(start.getId(), startSide, endId, endSide);
    }
    public void connect(View start, int startSide, int endId, int endSide, int margin) {
        connect(start.getId(), startSide, endId, endSide, margin);
    }
    public void apply() {
        cs.applyTo(layout);
    }
    public ConstraintLayout getLayout() {
        return layout;
    }
}
