package com.cbc.android;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;

public class ScrollableTable {
    public  enum DisplayWidthMode {FillScreen, UseCalculated, OnOverflowFillScreen};
    public  enum ValuePixelSource {Value, Size, Measure}
    private TableLayout           header;
    private TableLayout           body;
    private View.OnClickListener  rowListener = null;
    private Logger                log         = new Logger(this.getClass().getSimpleName());

    private class DisplayOptions {
        private float              textSize          = 16f;  //Think this is the font size
        private EditText           sizer;
        private DisplayWidthMode   mode              = DisplayWidthMode.OnOverflowFillScreen;
        private int                screenPixelWidth  = Resources.getSystem().getDisplayMetrics().widthPixels;
        private int                fullScreenBorder  = 0;
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
            this.sizer = new EditText(header.getContext());
            sizer.setTextSize(textSize);
        }
        public void setTextSize(float size) {
            textSize = size;
            sizer.setTextSize(size);
        }
        public float getTextSize(){
            return textSize;
        }
        public void setFullScreenBorder(String sizeText) {
            fullScreenBorder = (int)sizer.getPaint().measureText(sizeText);
        }
        public float getRatioSize(Table.Cell cell) {
            return  (float)(screenPixelWidth - fullScreenBorder) / totalWidths * cell.getWidth();
        }
        public float getValueSize(String value) {
            return sizer.getPaint().measureText(value + ' ');
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
            totalColumnPixels += getValueSize(col);

            if (col.getName().length() > maxColumnNameSize) maxColumnNameSize = col.getName().length();
        }
        public int getDisplayWidth(int colIndex) {
            return columns.get(colIndex).intValue();
        }
        public void initialise(Table table) {
            clear();

            for (int i = 0; i < table.getColumnCount(); i++) {
                addColumn(table.getColumn(i));
            }
            log.info("Table "              + table.getName() +
                    " row width "          + displayOptions.totalWidths +
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
                log.info("Column "             + Logger.rPad(cell.getName(), maxColumnNameSize) +
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
    public void setTextSize(float size) {
        displayOptions.setTextSize(size);
    }
    public void setFullScreenBorder(String sizeText) {
        displayOptions.setFullScreenBorder(sizeText);
    }
    public void setFullScreenBorder(int size) {
        String value = "a";

        while (value.length() < size) value += 'a';

        setFullScreenBorder(value);
    }
    public void setDisplayOptionsMode(DisplayWidthMode mode) {
        displayOptions.mode = mode;
    }

    public void setLogger(Logger logger) {
        this.log = logger;
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
        viewCell.setTextSize(displayOptions.getTextSize());

        viewCell.measure(0, 0);
        viewCell.setGravity(cell.getLeftAlign()? Gravity.LEFT : Gravity.RIGHT);
        viewCell.setWidth(displayOptions.getDisplayWidth(columnIndex));

        if (!cell.getDisplay()) viewCell.setVisibility(View.GONE);

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
}
