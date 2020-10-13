package com.cbc.android;

import android.view.View;

public class ScreenUtils {
    /*
     * Retrieves the parent object for a view.
     *
     * If the view has a parent, it is copied into object. The parent itself can be cast to a view
     * it is copied to view.
     */
    public static class ViewParent {
        private View   view   = null;
        private Object object = null;

        public ViewParent(View view) {
            object = view.getParent();

            if (object == null) return;

            try {
                this.view = (View) view.getParent();
            } catch (Exception ignored) {
            }
        }
        public boolean exists() {
            return object != null;
        }
        /*
         * If parent is a view returns getId(), otherwise, returns -1.
         */
        public int getId() {
            return view != null? view.getId(): -1;
        }
        /*
         * Returns the parents simple class name, or "", if exists() returns null.
         */
        public String getClassName() {
            return exists()? object.getClass().getSimpleName() : "";
        }
        /*
         * Returns the parent if it is a View, or null if not;
         */
        public View getView() {
            return view;
        }
        public Object getObject() {
            return object;
        }
    }
    public static class Position {
        int[] location = new int[2];

        public Position(@org.jetbrains.annotations.NotNull View view) {
            view.getLocationOnScreen(location);
        }
        public int getX() {
            return location[0];
        }
        public int getY() {
            return location[1];
        }
    }
    public static ViewParent getParent(View view) {
        return new ViewParent(view);
    }
    public static Position getPosition(View view) {
        return new Position(view);
    }
    public static View getRoot(View view) {
        View       root = view;
        ViewParent parent;

        while (view != null) {
            parent = getParent(view);
            view   = parent.getView();

            if (view == null) return root;

            root = view;
        }
        return null;
    }
}
