package com.cbc.android;

import android.content.res.Resources;
import android.graphics.Color;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

public class ScrollableTable {
    public  enum DisplayWidthMode {FillScreen, UseCalculated, OnOverflowFillScreen};
    public  enum ValuePixelSource {Value, Size, Measure}
    private TableLayout           header;
    private TableLayout           body;
    private View.OnClickListener  rowListener = null;
    private Logger                log         = new Logger(this.getClass().getSimpleName());
    private ScrollView            scrollView  = null;

    private class DisplayOptions {
        private TextSizer          sizer;
        private DisplayWidthMode   mode              = DisplayWidthMode.OnOverflowFillScreen;
        private int                screenPixelWidth  = Resources.getSystem().getDisplayMetrics().widthPixels;
        private float              fullScreenBorder  = 0;
        private float              columnGap         = 0;
        private int                totalWidths       = 0;
        private int                totalColumnPixels = 0;
        private int                maxColumnNameSize = 0;
        private ValuePixelSource   pixelSource       = ValuePixelSource.Value;
        private ArrayList<Integer> columns = new ArrayList<Integer>();

        public void clear() {
            totalWidths       = 0;
            totalColumnPixels = 0;
            columns.clear();
        }
        public DisplayOptions() {
            this.sizer = new TextSizer(header.getContext());
            setFullScreenBorder(3);
            setColumnGap(3);
        }
        public void setTextSize(float size) {
            sizer.setPixelSize(size);
        }
        public float getTextSize(){
            return sizer.getPixelSize();
        }
        public void setFullScreenBorder(int units, int size) {
            fullScreenBorder = sizer.convertToPx(units, size);
        }
        public void setFullScreenBorder(int size) {
            setFullScreenBorder(TypedValue.COMPLEX_UNIT_DIP, size);
        }
        public void setColumnGap(int units, int size) {
            columnGap = sizer.convertToPx(units, size);
        }
        public void setColumnGap(int size) {
            columnGap = sizer.convertToPx(TypedValue.COMPLEX_UNIT_DIP, size);
        }
        public float getRatioSize(Table.Cell cell) {
            return  (float)(screenPixelWidth - fullScreenBorder) / totalWidths * cell.getWidth();
        }
        public float getValueSize(String value) {
            return sizer.getTextMeasure(value);
        }
        public float getValueSize(int size) {
            String value = "";

            while (value.length() < size) value += 'a';

            return getValueSize(value);
        }
        public float getValueSize(Table.HeaderCell cell) {
            float pixels = 0;

            switch (displayOptions.pixelSource) {
                case Size:
                    pixels = getValueSize(cell.stats.getMaxSize());
                    break;
                case Value:
                    pixels = getValueSize(cell.stats.getMaxValue());
                    break;
                case Measure:
                    if (cell.stats.pixelStatsAvailable())
                        pixels = cell.stats.getMaxValuePixels();
                    else
                        pixels = getValueSize(cell.stats.getMaxValue());
            }
            return pixels;
        }
        public void addColumn(Table.HeaderCell col) {
            if (!col.getDisplay()) return;
            totalWidths       += col.getWidth();
            totalColumnPixels += getValueSize(col) + columnGap;

            if (col.getName().length() > maxColumnNameSize) maxColumnNameSize = col.getName().length();
        }
        public int getDisplayWidth(int colIndex) {
            return columns.get(colIndex).intValue();
        }
        public void setDisplayWidth(TextView cell, int colIndex) {
            int width = columns.get(colIndex).intValue();

            cell.setWidth(width);

            if (colIndex < columns.size()) cell.setPadding(0, 0, (int)columnGap, 0);
        }
        public void initialise(Table table) {
            sizer = table.getTextSizer();
            clear();
            table.rebuildColumnStatistics();

            for (int i = 0; i < table.getColumnCount(); i++) {
                addColumn(table.getColumn(i));
            }
            /*
             * Remove final columnGap as there is no following column.
             */
            totalColumnPixels -= columnGap;
            log.info("Table "              + table.getName()                 +
                    " mode "               + mode.toString()                 +
                    " pixel source "       + pixelSource.toString()          +
                    " row width "          + displayOptions.totalWidths      +
                    " screen pixel width " + displayOptions.screenPixelWidth +
                    " columns pixel size " + displayOptions.totalColumnPixels);

            for (int i = 0; i < table.getColumnCount(); i++) {
                Table.HeaderCell cell  = table.getColumn(i);
                float            width = 0;

                if (!cell.getDisplay()) {
                    /*
                     * Create an index entry for non displayed fields, so that the fields column index as
                     * index to columns.
                     */
                    columns.add(new Integer(-1));
                    continue;
                }
                switch (mode) {
                    case FillScreen:
                        width = getRatioSize(cell);
                        break;
                    case UseCalculated:
                        width = getValueSize(cell);
                        break;
                    case OnOverflowFillScreen:
                        if (totalColumnPixels > screenPixelWidth)
                            width = getRatioSize(cell);
                        else
                            width = getValueSize(cell);
                        break;
                }
                log.info("Column "            + Logger.rPad(cell.getName(), maxColumnNameSize) +
                        " width "             + Logger.lPad(cell.getWidth(), 3) +
                        " ratio pixels "      + Logger.lPad((int)getRatioSize(cell), 3) +
                        " value size pixels " + Logger.lPad((int)getValueSize(cell.stats.getMaxSize()), 3) +
                        " value pixels "      + Logger.lPad((int)getValueSize(cell.stats.maxValue), 3) +
                        " measure pixels "    + Logger.lPad((int)cell.stats.maxValuePixels, 3) +
                        " calculated pixels " + Logger.lPad((int)width, 3));

                columns.add(new Integer((int)width));
            }
        }
    }
    DisplayOptions displayOptions;

    public void setValuePixelSource(ValuePixelSource source) {
        displayOptions.pixelSource = source;
    }
    public void setFullScreenBorder(int size) {
        displayOptions.setFullScreenBorder(size);
    }
    public void setFullScreenBorder(int units, int size) {
        displayOptions.setFullScreenBorder(units, size);
    }
    public void setDisplayOptionsMode(DisplayWidthMode mode) {
        displayOptions.mode = mode;
    }

    public void setLogger(Logger logger) {
        this.log = logger;
    }
    private View createView(ViewGroup parent, String type, int height) {
        View view = null;

        switch (type) {
            case "TableLayout":
                view = new TableLayout(parent.getContext());
                view.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, height));
                break;
            case "ScrollView":
                view = new ScrollView(parent.getContext());
                view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height));
                break;
            case "TextView":
                view = new TextView(parent.getContext());
                break;
            default:
        }
        view.setId(View.generateViewId());
        parent.addView(view);

        return view;
    }
    /*
     * Creates a table immediately below previous. Height the size in DP of the scrollable table rows.
     */
    public ScrollableTable(View previous, int height) {
        ConstraintLayout layout = (ConstraintLayout) previous.getParent();
        ConstraintSet    cs         = new ConstraintSet();

        this.header = (TableLayout) createView(layout,     "TableLayout", ConstraintLayout.LayoutParams.WRAP_CONTENT);
        scrollView  = (ScrollView)  createView(layout,     "ScrollView",  height < 0? LinearLayout.LayoutParams.WRAP_CONTENT : height);
        this.body   = (TableLayout) createView(scrollView, "TableLayout", ConstraintLayout.LayoutParams.WRAP_CONTENT);

        displayOptions = new DisplayOptions();
        /*
         * The clone statement must be called after
         */

        cs.clone(layout);
        cs.connect(this.header.getId(), ConstraintSet.LEFT,  ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        cs.connect(this.header.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        cs.connect(this.header.getId(), ConstraintSet.TOP,   previous.getId(),        ConstraintSet.BOTTOM, 10);

        cs.connect(scrollView.getId(),  ConstraintSet.LEFT,  this.header.getId(),     ConstraintSet.LEFT);
        cs.connect(scrollView.getId(),  ConstraintSet.RIGHT, this.header.getId(),     ConstraintSet.RIGHT);
        cs.connect(scrollView.getId(),  ConstraintSet.TOP,   this.header.getId(),     ConstraintSet.BOTTOM);
        cs.applyTo(layout);
    }
    public ScrollableTable(TableLayout header, TableLayout body) {
        this.header = header;
        this.body   = body;
        displayOptions = new DisplayOptions();
    }
    private TableRow createRow(TableLayout table, Object tag, boolean addListener) {
        TableRow row = new TableRow(header.getContext());
        table.addView(row);

        if (tag != null) row.setTag(tag);
        if (rowListener != null && addListener) row.setOnClickListener(rowListener);

        return row;
    }
    private TextView createCell(TableRow row) {
        TextView cell = new TextView(header.getContext());
        row.addView(cell);

        return cell;
    }
    private TextView createCell(TableRow row, Table.Cell cell, int columnIndex) {
        TextView viewCell = createCell(row);
        viewCell.setText(cell.getValue());
        viewCell.setTextColor(Color.BLACK);

        viewCell.measure(0, 0);
        viewCell.setGravity(cell.getLeftAlign()? Gravity.LEFT : Gravity.RIGHT);
        displayOptions.setDisplayWidth(viewCell, columnIndex);

        if (!cell.getDisplay()) viewCell.setVisibility(View.GONE);

        if (cell.getMaxLength() > 0) {
            InputFilter[] filters  = viewCell.getFilters();
            InputFilter[] filtersn = new InputFilter[filters.length + 1];
            System.arraycopy(filters, 0, filtersn, 0, filters.length);
            filtersn[filters.length] = new InputFilter.LengthFilter(cell.getMaxLength());
            viewCell.setFilters(filtersn);
        }
        return viewCell;
    }
    public void setRowListener(View.OnClickListener listener) {
        rowListener = listener;
    }

    public void loadTable(Table table)  {
        int i;
        int j;
        int columnCount = table.getColumnCount();
        TableRow    columns;

        displayOptions.initialise(table);
        header.removeAllViews();
        body.removeAllViews();
        columns = createRow(header, null, false);

        for (i = 0; i < columnCount; i++) {
            Table.HeaderCell col = table.getColumn(i);

            createCell(columns, col, i);
        }
        for (i = 0; i < table.getRowCount(); i++) {
            Table.Row row = table.getRow(i);
            columns = createRow(body, row, true);

            for (j = 0; j < row.getColumnsCount(); j++) {
                Table.RowCell col = row.getCell(j);

                createCell(columns, col, j);
            }
        }
    }
    public int getScrollId() {
        return scrollView.getId();
    }
}
