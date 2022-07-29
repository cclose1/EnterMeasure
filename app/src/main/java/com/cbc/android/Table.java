package com.cbc.android;

import android.content.Context;

import org.cbc.json.JSONArray;
import org.cbc.json.JSONException;
import org.cbc.json.JSONFormat;
import org.cbc.json.JSONObject;
import org.cbc.json.JSONReader;
import org.cbc.json.JSONType;
import org.cbc.json.JSONValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Table {
    public enum ValueType {Int, String};
    private String       name;
    private boolean      statsUpToDate = true;
    private TextSizer    measurer = null;
    private Logger log = new Logger(this.getClass().getSimpleName());
    public void setLogger(Logger logger) {
        log = logger;
    }
    /*
     * Contains summary data for all the values contain in the column, including the header.
     *
     * The member attributes are:
     *
     * - count         Total number of values in the column.
     * - totValueWidth Sum of all the cell value widths.
     * - minValueWidth Smallest value width.
     * - maxValueWidth Largest value width.
     * - maxValue      Value of the cell have maxValueWidth.
     */
    public class ColumnStatistics {
        int    count;
        float  totValuePixels;
        float  maxValuePixels;
        int    totValueWidth;
        int    minValueWidth;
        int    maxValueWidth;
        String maxValue;

        public void clear() {
            count          = 0;
            totValuePixels = 0;
            maxValuePixels = 0;
            totValueWidth  = 0;
            minValueWidth  = 0;
            maxValueWidth  = 0;
            maxValue       = "";
        }
        public void update(String text, int maxColumnLength) {
            if (text == null) return;
            /*
             * If text size is larger than maxColumnLength, truncate it to maxColumnLength.
             */
            if (maxColumnLength != 0 && text.length() > maxColumnLength) text = text.substring(0, maxColumnLength - 1);

            int size = text.length();

            totValueWidth += size;
            count         += 1;

            if (minValueWidth == 0 || size < minValueWidth) minValueWidth = size;
            if (size > maxValueWidth) {
                maxValue      = text;
                maxValueWidth = size;
            }
            if (measurer != null) {
                float pixels = measurer.getTextMeasure(text);

                if (pixels > maxValuePixels) maxValuePixels = pixels;

                totValuePixels += pixels;
            }
        }
        public int getMinSize() {
            return minValueWidth;
        }
        public int getAvgSize() {
            return count == 0? 0 : totValueWidth / count;
        }
        public int getMaxSize() {
            return maxValueWidth;
        }
        public String getMaxValue() {
            return maxValue;
        }
        public boolean pixelStatsAvailable() {
            return measurer != null;
        }
        public float getMaxValuePixels() {
            return maxValuePixels;
        }
    }
    /*
     * Contains the fields that are common to header and row cells.
     */
    public abstract class Cell {
        protected String    value;
        protected ValueType type;
        protected int       length;
        protected int       maxLength;
        protected boolean   leftAlign;
        protected boolean   display;

        public void setValue(String value) {
            if (this.value != null && this.value.length() != 0 && !this.value.equals(value)) statsUpToDate = false;

            this.value = value;
        }
        public String getValue() {
            return value;
        }
        public boolean getLeftAlign() {
            return leftAlign;
        }
        public int getLength() {
            return length;
        }
        protected void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
        public int getMaxLength() {
            return maxLength;
        }
        public abstract String getName();
        public boolean getDisplay() {
            return display;
        }
        public ValueType getValueType() {
            return type;
        }
    }
    /*
     * Contains the common fields and those specific to header cells.
     *
     * Note: The width, due to the implementation, is the maximum of the widths of the values in the rows
     *       including the header row. This is same as the stats maxValueWidth.
     */
    public class HeaderCell extends Cell {
        boolean display        = false;
        ColumnStatistics stats = new ColumnStatistics();

        private HeaderCell(String name, ValueType type, boolean display, int maxLength) {
            setValue(name);
            this.display  = display;
            this.length = name.length();
            this.type     = type;
            this.maxLength = maxLength;
            stats.update(name, maxLength);
        }
        public String getName() {
            return value;
        }
        public boolean getDisplay() {
            return display;
        }
        public void setDisplay(boolean on) {
            display = on;
        }
        public void setMaxLength(int maxLength) {
            super.setMaxLength(maxLength);
        }
        public int getMaxLength() {
            return super.getMaxLength();
        }
    }
    /*
     * Define a cell in a table row. The member attribute header is the column header and all member methods, apart
     * from setValue, call the corresponding header method.
     *
     * It may make sense to allow row cells to be aligned differently to the header.
     */
    public class RowCell extends Cell {
        private HeaderCell header;

        private RowCell(int index) {
            header = headers.get(index);
        }
        private void setValue(String value, boolean leftAlign) {
            super.setValue(value);

            if (value == null) return;

            header.leftAlign = leftAlign;
            header.stats.update(value, header.getMaxLength());

            if (value.length() > header.length) header.length = value.length();
        }
        public void setValue(String value) {
            setValue(value, true);
        }
        public void setValue(int value) {
            setValue(Integer.toString(value), false);
        }
        public void setValue(long value) {
            setValue(Long.toString(value), false);
        }
        public boolean getLeftAlign() {
            return header.leftAlign;
        }
        public boolean getDisplay() {
            return header.display;
        }
        public int getLength() {
            return header.length;
        }
        public ValueType getValueType() {
            return header.type;
        }
        public String getName() {
            return header.getName();
        }
    }
    private ArrayList<HeaderCell> headers = new ArrayList<HeaderCell>();

    /*
     * Returns the index in headers of the cell with name, or -1 if there is none.
     */
    private int getColumnIndex(String name, boolean mustExist) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getName().equals(name)) return i;
        }
        if (mustExist) throw new RuntimeException("Column " + name + " does not exists in table " + this.name);
        return -1;
    }
    /*
     * Returns the index in headers of the cell with name, or -1 if there is none.
     */
    private int getColumnIndex(String name) {
        return getColumnIndex(name, false);
    }
    /*
     * Defines a row in the table.
     */
    public class Row {
        ArrayList<RowCell> columns = new ArrayList<RowCell>(headers.size());
        /*
         * Returns a new Row with columns set to an array of empty row cells.
         */
        private Row() {
            for (int i = 0; i < headers.size(); i++) columns.add(new RowCell(i));
        }
        public int getColumnsCount() {
            return columns.size();
        }
        protected void setCell(int column, int value) {
            columns.get(column).setValue(value);
        }
        protected void setCell(int column, long value) {
            columns.get(column).setValue(value);
        }
        protected void setCell(int column, String value) {
            columns.get(column).setValue(value);
        }
        public void setCell(String column, String value) {
            setCell(getColumnIndex(column, true), value);
        }
        public void setCell(String column, int value) {
            setCell(getColumnIndex(column, true), value);
        }
        public void setCell(String column, long value) {
            setCell(getColumnIndex(column, true), value);
        }
        public RowCell getCell(int index) {
            return columns.get(index);
        }
        public RowCell getCell(String name) {
            return columns.get(getColumnIndex(name));
        }
    }
    private ArrayList<Row> rows = new ArrayList<Row>();

    private void resetHeaders() {
        for (int i = 0; i < getColumnCount(); i++) {
            HeaderCell cell = (HeaderCell) headers.get(i);

            cell.length = cell.value.length();
            cell.stats.clear();
            cell.stats.update(cell.value, cell.getMaxLength());
        }

    }
    public void rebuildColumnStatistics() {
        if (statsUpToDate) return;

        resetHeaders();

        for (int i = 0; i < getRowCount(); i++) {
            Row row = rows.get(i);

            for (int j = 0; j < row.getColumnsCount(); j++) {
                RowCell cell = row.getCell(j);
                /*
                 * Set to same value to trigger the stats update
                 */
                cell.setValue(cell.getValue(), cell.header.leftAlign);
            }
        }
        log.info("Table " + name + " column statistics rebuilt");
        statsUpToDate = true;
    }
    public Table(String name) {
        this.name    = name;
    }
    public Table(String name, Context context) {
        this(name);
        setTextSizer(new TextSizer(context));
    }
    public String getName() {
        return name;
    }
    public void setTextSizer(TextSizer sizer) {
        this.measurer = sizer;
    }
    public TextSizer getTextSizer() {
        return this.measurer;
    }
    public boolean pixelStatsAvailable() {
        return measurer != null;
    }
    public void addColumnHeader(String name, ValueType type, boolean display, int maxLength) {
        if (getColumnIndex(name) != -1) throw new RuntimeException("Column " + name + " already exists in table " + name);

        headers.add(new HeaderCell(name, type, display, maxLength));
    }
    public void addColumnHeader(String name, ValueType type, boolean display) {
        addColumnHeader(name, ValueType.String, display, 0);
    }
    public void addColumnHeader(String name, boolean display) {
        addColumnHeader(name, ValueType.String, display, 0);
    }
    public void addColumnHeader(String name, ValueType type) {
        addColumnHeader(name, type, true, 0);
    }
    public void addColumnHeader(String name) {
        addColumnHeader(name, ValueType.String, true, 0);
    }

    public void setColumnVisible(int index, boolean yes) {
        headers.get(index).setDisplay(yes);
    }
    public void setColumnVisible(String name, boolean yes) {
        setColumnVisible(getColumnIndex(name, true), yes);
    }
    public void setMaxLength(int index, int maxLength) {
        headers.get(index).setMaxLength(maxLength);
    }
    public void setMaxLength(String name, int maxLength) {
        setMaxLength(getColumnIndex(name, true), maxLength);
    }
    public int getColumnCount() {
        return headers.size();
    }
    public HeaderCell getColumn(int index) {
        return headers.get(index);
    }
    public Row createRow() {
        return new Row();
    }

    class SortByName implements Comparator<Row>
    {
        public int compare(Row a, Row b)
        {
            return a.getCell("Time").getValue().compareTo(b.getCell("Time").getValue());
        }
    }
    public void removeRow(Table.Row row) {
        rows.remove(row);
        statsUpToDate = false;
    }
    public void addRow(Row row, boolean atStart) {
        if (atStart) rows.add(0, row); else rows.add(row);
    }
    public void sort() {
        Collections.sort(rows, new SortByName().reversed());
    }
    public int getRowCount() {
        return rows.size();
    }
    public Row getRow(int index) {
        return rows.get(index);
    }
    public void removeRows() {
        rows.clear();
        resetHeaders();
    }
    public void loadJSON(JSONReader reader) throws JSONException {
        JSONValue root = JSONValue.load(reader);
        JSONValue value;
        JSONArray row;
        JSONArray rows;
        Table.Row tRow;

        headers.clear();
        this.rows.clear();

        name = root.getObject().get("Table",  true).getString();
        row  = root.getObject().get("Header", true).getArray();

        for (int i = 0; i < row.size(); i++) {
            JSONObject col = row.get(i).getObject();

            addColumnHeader(
                    col.get("Name").getString(),
                    ValueType.valueOf(col.get("Type").getString()),
                    col.get("Display").getBoolean(),
                    0);
        }
        rows = root.getObject().get("Data", true).getArray();

        for (int i = 0; i < rows.size(); i++) {
            tRow = createRow();
            row  = rows.get(i).getArray();

            for (int j = 0; j < row.size(); j++) {
                value = row.get(j);

                switch (value.getType()) {
                    case Number:
                        tRow.setCell(j, value.getInt());
                        break;
                    case String:
                        tRow.setCell(j, value.getString());
                        break;
                    case Null:
                        tRow.setCell(j, "");
                        break;
                    default:
                        throw new JSONException(
                                        "Value type " +
                                                JSONType.valueOf(value.getType().toString()) +
                                                " for column " + j +
                                                " in table "   + name + " is not supported");
                }
            }
            addRow(tRow, false);
        }
    }
    public JSONObject getJSON() throws JSONException {
        JSONObject data = new JSONObject();
        JSONArray  row;

        data.add("Table", new JSONValue(name));
        row = data.add("Header", (JSONArray)null);

        for (int i = 0; i < getColumnCount(); i++) {
            JSONObject col;
            HeaderCell cell = getColumn(i);
            col = row.addObject();
            col.add("Name",      new JSONValue(cell.getName()));
            col.add("Type",      new JSONValue(cell.type.toString()));
            col.add("Precision", new JSONValue(cell.length));
            col.add("Display",   new JSONValue(cell.display));
        }
        row = data.add("Data", (JSONArray)null);

        for (int i = 0; i < getRowCount(); i++) {
            JSONArray col = row.addArray();
            Row      tRow = getRow(i);

            for (int j = 0; j < tRow.getColumnsCount(); j++) {
                RowCell cell = tRow.getCell(j);

                if (cell.getValueType() == Table.ValueType.Int)
                    col.add(new JSONValue(Integer.parseInt(cell.getValue())));
                else
                    col.add(new JSONValue(cell.getValue()));
            }
        }
        return data;
    }
    public void save(File file, boolean format) throws IOException, JSONException {
        FileOutputStream str = new FileOutputStream(file);

        str.write(getJSON().toString(new JSONFormat(format)).getBytes());
        str.close();
    }
    public void save(File path, String fileName, boolean format) throws IOException, JSONException {
        save(new File(path, fileName), format);
    }
    public void save(File path, String fileName) throws IOException, JSONException {
        save(new File(path, fileName), false);
    }
    public void restore(File file) throws FileNotFoundException, JSONException {
        loadJSON(new JSONReader(file));
    }
    public void restore(File path, String fileName) throws FileNotFoundException, JSONException {
        restore(new File(path, fileName));
    }
}