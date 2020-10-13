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
        private DisplayWidthMode   mode                = DisplayWidthMode.FillScreen;
        private float              screenPixelWidth    = Resources.getSystem().getDisplayMetrics().widthPixels;
        private float              fullScreenBorder    = 0;
        private float              columnGap           = 0;
        private int                totalLengths        = 0;
        private float              totalColumnPixels   = 0;
        private float              updatedColumnPixels = 0;
        private float              totalMinPixels      = 0;
        private float              reducablePixels     = 0;
        private float              excessMinPixels     = 0;
        private int                maxColumnNameSize   = 0;
        private ValuePixelSource   pixelSource         = ValuePixelSource.Value;
        private ArrayList<Field>   columns             = new ArrayList<>();

        public class Field {
            private float            width = -1;
            private float            min   = -1;
            private Table.HeaderCell header;

            public Field(Table.HeaderCell cell) {
                header = cell;

                if (header.getDisplay()) {
                    min             = getSourceMinWidth(header);
                    totalMinPixels += min;
                }
            }
            public void setWidth(float width) {
                if (this.width < 0) {
                    if (width < min)
                        excessMinPixels += (min - width);
                    else
                        reducablePixels += (width - min);
                }
                this.width = width;
            }
            public float getWidth() {
                return width;
            }
            public void applyMinWidth() {
                if (width < min)
                    width = min;
                else
                    width -= (width - min) * excessMinPixels / reducablePixels;
                updatedColumnPixels += width;
            }
            public boolean isDisplayed() {
                return header.getDisplay();
            }
        }
        public void clear() {
            screenPixelWidth    = Resources.getSystem().getDisplayMetrics().widthPixels;
            totalLengths        = 0;
            totalColumnPixels   = 0;
            totalMinPixels      = 0;
            excessMinPixels     = 0;
            reducablePixels     = 0;
            updatedColumnPixels = 0;
            columns.clear();
        }
        public DisplayOptions() {
            this.sizer = new TextSizer(header.getContext());
            setFullScreenBorder(2);
            setColumnGap(2);
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
           setColumnGap(TypedValue.COMPLEX_UNIT_DIP, size);
        }
        public float getRatioSize(Table.Cell cell) {
            return  (float)(screenPixelWidth) / totalLengths * cell.getLength();
        }
        public float getWidth(String value) {
            return sizer.getTextMeasure(value);
        }
        public float getWidth(int size) {
            String value = "";

            while (value.length() < size) value += 'a';

            return getWidth(value);
        }
        public float getSourceMinWidth(Table.HeaderCell cell) {
            float pixels = 0;

            switch (displayOptions.pixelSource) {
                case Size:
                    pixels = getWidth(cell.getName().length());
                    break;
                case Value:
                    pixels = getWidth(cell.getName());
                    break;
                case Measure:
                    pixels = getWidth(cell.getName());
            }
            return pixels;
        }
        public float getSourceWidth(Table.HeaderCell cell) {
            float pixels = 0;

            switch (displayOptions.pixelSource) {
                case Size:
                    pixels = getWidth(cell.stats.getMaxSize());
                    break;
                case Value:
                    pixels = getWidth(cell.stats.getMaxValue());
                    break;
                case Measure:
                    if (cell.stats.pixelStatsAvailable())
                        pixels = cell.stats.getMaxValuePixels();
                    else
                        pixels = getWidth(cell.stats.getMaxValue());
            }
            return pixels;
        }
        public void addColumn(Table.HeaderCell col) {
            columns.add(new Field(col));

            if (!col.getDisplay()) return;

            totalLengths      += col.getLength();
            totalColumnPixels += getSourceWidth(col);
            screenPixelWidth  -= columnGap;

            if (col.getName().length() > maxColumnNameSize) maxColumnNameSize = col.getName().length();
        }
        public void setWidth(TextView cell, int colIndex) {
            Field f  = columns.get(colIndex);

            if (!f.isDisplayed()) return;

            cell.setWidth((int)f.getWidth());

            if (colIndex < columns.size()) cell.setPadding(0, 0, (int)columnGap, 0);
        }
        public void initialise(Table table, boolean applyMinWidth) {
            float width = 0;

            sizer = table.getTextSizer();
            clear();
            screenPixelWidth -= fullScreenBorder;
            table.rebuildColumnStatistics();

            for (int i = 0; i < table.getColumnCount(); i++) {
                addColumn(table.getColumn(i));
            }
            /*
             * Remove final columnGap as there is no following column. After this screenPixelWidth has
             * been reduced to space available for column value, i.e. the gaps and border have been taken off.
             */
            screenPixelWidth += columnGap;
            Logger.debug("Table "                    + table.getName()            +
                         " mode "                    + mode.toString()            +
                         " pixel source "            + pixelSource.toString()     + '\n'    +
                         "Screen border "            + fullScreenBorder           +
                         " row length "              + totalLengths               +
                         " column gap "              + columnGap                  +
                         " screen pixel width "      + Resources.getSystem().getDisplayMetrics().widthPixels + '\n' +
                         "Available values pixels "  + screenPixelWidth  +
                         " total min value pixels "  + totalMinPixels    +
                         " columns pixel size "      + totalColumnPixels);

            for (int i = 0; i < columns.size(); i++) {
                Field            fld  = columns.get(i);
                Table.HeaderCell cell = fld.header;

                if (!cell.getDisplay()) {
                    /*
                     * Create an index entry for non displayed fields, so that the fields column index as
                     * index to columns.
                     */
                    continue;
                }
                switch (mode) {
                    case FillScreen:
                        width = getRatioSize(cell);
                        break;
                    case UseCalculated:
                        width = getSourceWidth(cell);
                        break;
                    case OnOverflowFillScreen:
                        if (totalColumnPixels > screenPixelWidth)
                            width = getRatioSize(cell);
                        else
                            width = getSourceWidth(cell);
                        break;
                }
                Logger.debug("Column "             + Logger.rPad(cell.getName(), maxColumnNameSize) +
                             " length "            + Logger.lPad(cell.getLength(), 3) +
                             " min width "         + Logger.lPad((int)fld.min, 3) +
                             " ratio pixels "      + Logger.lPad((int)getRatioSize(cell), 3) +
                             " value size pixels " + Logger.lPad((int) getWidth(cell.stats.getMaxSize()), 3) +
                             " value pixels "      + Logger.lPad((int) getWidth(cell.stats.maxValue), 3) +
                             " measure pixels "    + Logger.lPad((int)cell.stats.maxValuePixels, 3) +
                             " calculated pixels " + Logger.lPad((int)width, 3));
                fld.setWidth(width);
            }
            Logger.debug("Excess min width pixels " + Logger.lPad((int)excessMinPixels, 3) +
                        " reducable pixels "        + Logger.lPad((int)reducablePixels, 3));
            if (applyMinWidth && excessMinPixels >= 0) {
                for (Field f : columns) {
                    f.applyMinWidth();
                }
                Logger.debug("Min width set. Field columns pixels " + updatedColumnPixels);
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
    public void setColumnGap(int size) {
        displayOptions.setColumnGap(size);
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
        this.header     = header;
        this.body       = body;
        this.scrollView = (ScrollView) body.getParent();
        displayOptions  = new DisplayOptions();
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
        displayOptions.setWidth(viewCell, columnIndex);

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

    public void loadTable(Table table, boolean setMinWidth)  {
        int i;
        int j;
        int columnCount = table.getColumnCount();
        TableRow    columns;

        displayOptions.initialise(table, setMinWidth);
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
    public void loadTable(Table table) {
        loadTable(table, false);
    }
    private int getMaxRowHeight(View row) {
        int height = row.getHeight();

        if (height > 0) return height;

        height = row.getMeasuredHeight();

        if (height > 0) return height;

        row.measure(0, 0);

        return row.getMeasuredHeight();
    }
    public int getMaxRowHeight() {
        int  height = 0;
        int  min    = -1;
        int  max    = 0;

        for (int i = 0; i < body.getChildCount(); i++) {
            height = getMaxRowHeight(body.getChildAt(i));

            if (min < 0 || height < min) min = height;
            if (height > max) max = height;
        }
        return max;
    }
    public int getScrollId() {
        return scrollView.getId();
    }
    /*
     * Calculates the maximum height that the scrollable table body can be set to and still have spaceAfter
     * pixels for following fields to be displayed within the screen without scrolling.
     *
     * Note: The calculation only works if the software keyboard does not cover the start of the scrollable
     *       table body
     */
    public int getMaxHeight(int spaceAfter, int maxRows) {
        ScreenUtils.Position   pBody         = ScreenUtils.getPosition(scrollView);
        int                    rowHeight     = getMaxRowHeight();
        int                    rowBodyHeight = maxRows > 0? maxRows * rowHeight : 0;
        int                    maxBodyHeight = 0;
        int                    bodyHeight    = 0;


        if (pBody.getY() == 0) return -1;

        maxBodyHeight = DeviceDetails.getMetrics().heightPixels - pBody.getY() - spaceAfter;
        bodyHeight    = rowBodyHeight > 0 && rowBodyHeight < maxBodyHeight? rowBodyHeight : maxBodyHeight;

        Logger.debug(
                   "Display height "   + DeviceDetails.getMetrics().heightPixels +
                   " root height "     + ScreenUtils.getRoot(body).getHeight()   +
                   " space after "     + spaceAfter    +
                   " body start "      + pBody.getY()  +
                   " rows "            + maxRows       +
                   " row height "      + rowHeight     +
                   " row body height " + rowBodyHeight +
                   " max body height " + maxBodyHeight +
                   " body height "     + bodyHeight + " pixels : " + TextSizer.getValue(bodyHeight, TextSizer.Units.DP) + " dp");
        return bodyHeight;
    }

    public boolean setMaxHeight(int spaceAfter, int maxRows) {
        int maxBodyHeight = getMaxHeight(spaceAfter, maxRows);

        if (maxBodyHeight < 0) return false;

        scrollView.getLayoutParams().height = maxBodyHeight;

        return true;
    }
}
