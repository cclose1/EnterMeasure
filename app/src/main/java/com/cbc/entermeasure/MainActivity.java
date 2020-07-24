package com.cbc.entermeasure;

import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cbc.android.Alert;
import com.cbc.android.EditTextHandler;
import com.cbc.android.Logger;
import com.cbc.android.ScrollableTable;
import com.cbc.android.SpinnerHandler;
import com.cbc.android.Table;
import com.cbc.android.Timer;

import org.cbc.json.JSONException;
import org.cbc.utils.system.DateFormatter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private enum ActionButtonName {Update, Save, Cancel}

    private class OnclickHandle implements View.OnClickListener {
        private class Tag {
            protected int              id;
            protected ActionButtonName name;

            protected Tag(int id, ActionButtonName name) {
                this.id   = id;
                this.name = name;
            }
        }
        private View      view;
        private Tag       action;
        private String    caption;
        private Table.Row row;

        private void clearFields() {
            orientation.setSelected(0);
            side.setSelected(0);
            systolic.clear();
            diastolic.clear();
            pulse.clear();
            time.clear();
            systolic.setFocus();
        }
        private void complete(boolean load) {
            clearFields();
            view    = null;
            action  = null;
            caption = null;
            row     = null;
            setActionKeys(false);

            if (load) measuresView.loadTable(measures);
        }
        private boolean mandatoryPresent() {
            if (!systolic.checkPresent())  return false;
            if (!diastolic.checkPresent()) return false;
            if (!pulse.checkPresent())     return false;

            return true;
        }
        private void updateRow() {
            row.setCell("Time",          time.getText());
            row.setCell("Session Index", gapUpdater.getSessionCount());
            row.setCell("Orientation",   orientation.getSelected());
            row.setCell("Side",          side.getSelected());
            row.setCell("Systolic",      systolic.getInt());
            row.setCell("Diastolic",     diastolic.getInt());
            row.setCell("Pulse",         pulse.getInt());
        }
        private boolean update(boolean newRow) {
            if (!mandatoryPresent()) return false;

            try {
                /*
                 * timeFormatter is implemented using SimpleDateFormat, which even when set in not lenient mode,
                 * is not guaranteed to correctly convert a string to the right date, i.e. not getting an exception
                 * does not guarantee that Date object matches the date string. The setText ensures that the date string
                 * matches the format. E.g. 2001-8-1 will be expanded to 2001-08-01.
                 *
                 * DateTimeFormatter provides for accurate parsing of dates, but requires the minimum API level 26,
                 * released 21 Aug 2017.
                 */
                Date ts = timeFormatter.parse(time.getText());
                time.setText(timeFormatter.format(ts));
            } catch (ParseException e) {
                alert.display("Validation Error", "Timestamp format must be YYYY-MM-DD HH:MM:SS");
                time.setFocus();
                return false;
            }
            if (newRow) {
                row = measures.createRow();
                measures.addRow(row, true);
            }
            updateRow();
            return true;
        }
        private void delete() {
            measures.removeRow(row);
        }
        @Override
        public void onClick(View clicked) {
            view    = clicked;
            action  = (Tag) view.getTag();
            caption = ((Button) view).getText().toString();

            switch (action.name) {
                case Update:
                    if (!update(false)) return;
                    measures.sort();
                    complete(true);
                    break;
                case Save:
                    if (caption.equals("Save")) {
                        if (!update(true)) return;
                    } else
                        delete();
                    complete(true);
                    break;
                case Cancel:
                    complete(false);
                    break;
            }
        }
        private ActionButtonName getName(View view) {
            String caption = ((Button) view).getText().toString();

            return ActionButtonName.valueOf(caption);
        }
        public void setOnClickListener(int id, ActionButtonName name) {
            View view = findViewById(id);
            view.setOnClickListener(this);
            view.setTag(new Tag(id, name == null? getName(view) : name));
        }
        public void setOnClickListener(int id) {
            setOnClickListener(id, null);
        }
        public void setTableRow(Table.Row row) {
            this.row = row;
        }
    }
    private class GapUpdater implements Timer.Action {
        private int  maxSession   = 1800;
        private int  sessionCount = 0;
        private Date sessionStart = null;
        @Override
        public void start() {
        }
        @Override
        public void update(long lapsedMilliSeconds) {
            Date now = new Date();

            if (sessionStart != null && (now.getTime() - sessionStart.getTime()) / 1000 > maxSession) {
                endSession();
            }
            if (sessionStart == null)
                gap.clear();
            else
                gap.setText((int) lapsedMilliSeconds / 1000);
        }
        @Override
        public void reset() {
            update(0);
        }
        @Override
        public void stop() {
            gap.clear();
        }
        public void setMaxSession(int maxSession) {
            this.maxSession = maxSession;
        }
        public void endSession() {
            gapTimer.stop();
            sessionStart = null;
            sessionCount = 0;
        }
        public void incrementSessionCount() {
            if (sessionStart == null)
                sessionStart = new Date();
            else
                sessionCount++;
        }
        public int getSessionCount() {
            return sessionCount;
        }
    }
    private class RowListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            TableRow viewRow = (TableRow) view;
            Table.Row row = (Table.Row) view.getTag();
            action.setTableRow(row);

            orientation.setSelected(row.getCell("Orientation").getValue());
            side.setSelected(row.getCell("Side").getValue());
            systolic.setText(row.getCell("Systolic").getValue());
            diastolic.setText(row.getCell("Diastolic").getValue());
            pulse.setText(row.getCell("Pulse").getValue());
            time.setText(row.getCell("Time").getValue());
            systolic.setFocus();
            setActionKeys(true);
        }
    }
    private class TextMeasurer implements Table.TextMeasurer {
        private EditText measurer;

        public TextMeasurer(float textSize) {
            measurer = new EditText(MainActivity.this);
            measurer.setTextSize(textSize);
        }
        @Override
        public float getTextMeaure(String text) {
            return measurer.getPaint().measureText(text);
        }
    }
    float textSize = 16f;

    public void setEnableButton(int id, boolean visible, String caption) {
        Button btn = (Button) findViewById(id);

        btn.setVisibility(visible? View.VISIBLE : View.GONE);

        if (caption != null) btn.setText(caption);
    }
    public void setActionKeys(boolean update) {
        if (update) {
            setEnableButton(R.id.update, true, null);
            setEnableButton(R.id.save,   true, "Delete");
            setEnableButton(R.id.cancel, true, null);
            time.setFocusable(true);
        } else {
            setEnableButton(R.id.update, false, null);
            setEnableButton(R.id.save,   true,  "Save");
            setEnableButton(R.id.cancel, false, null);
            time.setFocusable(false);
        }
    }
    public OnclickHandle action = new OnclickHandle();
    private void setTextSize(View view, float size) {
        if (view == null) view = findViewById(android.R.id.content).getRootView();

        if (view instanceof EditText)
            ((EditText) view).setTextSize(size);
        else if (view instanceof TextView)
            ((TextView) view).setTextSize(size);
        else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;

            for (int i = 0; i < vg.getChildCount(); i++) {
                setTextSize(vg.getChildAt(i), size);
            }
        }
    }
    private SpinnerHandler         orientation   = null;
    private SpinnerHandler         side          = null;
    private EditTextHandler        systolic      = null;
    private EditTextHandler        diastolic     = null;
    private EditTextHandler        pulse         = null;
    private EditTextHandler        time          = null;
    private EditTextHandler        gap           = null;
    private Alert                  alert         = null;
    private GapUpdater             gapUpdater    = new GapUpdater();
    private Timer                  gapTimer      = new Timer(gapUpdater);
    private RowListener            rowListener   = new RowListener();
    private DateFormatter          timeFormatter = new DateFormatter("yyyy-MM-dd HH:mm:ss", false);
    private Logger                 logger        = new Logger("EnterMeasure");
    private TextMeasurer           measurer;
    private Table                  measures;
    private ScrollableTable        measuresView;

    public void writeClicked(View view) {
        try {
            gapUpdater.endSession();
            measures.save(getFilesDir(), "measures.txt");
            /*
             * This seems to work but can't find the file in Device File Explorer.
             */
            measures.save(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "testext.txt");
        } catch (JSONException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadClicked(View view) {
        gapUpdater.endSession();
        try {
            measures.restore(getFilesDir(), "measures.txt");
            measuresView.loadTable(measures);
        } catch (JSONException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void displayTextSizeOptionClicked() {
        RadioGroup rg = (RadioGroup) findViewById(R.id.setdisplaysize);

        switch (rg.getCheckedRadioButtonId()) {
            case R.id.radiomaxvalue:
                measuresView.setValuePixelSource(ScrollableTable.ValuePixelSource.Value);
                break;
            case R.id.radiomaxsize:
                measuresView.setValuePixelSource(ScrollableTable.ValuePixelSource.Size);
                break;
            case R.id.radiomaxmeasure:
                measuresView.setValuePixelSource(ScrollableTable.ValuePixelSource.Measure);
                break;
        }
        measuresView.loadTable(measures);
    }
    public void displayTextSizeOptionClicked(View view) {
        displayTextSizeOptionClicked();
    }
    private void setScreenDisplay(boolean fillScreen) {
        View view = findViewById(R.id.setdisplaysize);

        if (fillScreen) {
            measuresView.setDisplayOptionsMode(ScrollableTable.DisplayWidthMode.FillScreen);
            measuresView.loadTable(measures);
            view.setVisibility(View.GONE);
        } else {
            measuresView.setDisplayOptionsMode(ScrollableTable.DisplayWidthMode.OnOverflowFillScreen);
            view.setVisibility(View.VISIBLE);
            displayTextSizeOptionClicked();
        }
    }
    public void fillScreenChecked(View view) {
        setScreenDisplay(((CheckBox) view).isChecked());
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (time.getText().length() == 0) {
                    gapUpdater.incrementSessionCount();
                    time.setText(timeFormatter.format(new Date()));
                    gapTimer.reset();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        action.setOnClickListener(R.id.update);
        action.setOnClickListener(R.id.save);
        action.setOnClickListener(R.id.cancel);

        measurer = new TextMeasurer(textSize);
        measuresView = new ScrollableTable(
                (TableLayout) findViewById(R.id.MeasurementsHeader), (TableLayout) findViewById(R.id.MeasurementsBody));
        measuresView.setRowListener(rowListener);
        measuresView.setLogger(logger);
        measures = new Table("Measures");
        measures.setLogger(logger);
        measures.setTextMeasurer(measurer);
        measuresView.setTextSize(textSize);
        measuresView.setFullScreenBorder(2);
        measures.addColumnHeader("Time");
        measures.addColumnHeader("Session Index", Table.ValueType.Int, false);;
        measures.addColumnHeader("Side",          false);
        measures.addColumnHeader("Orientation",   false);
        measures.addColumnHeader("Systolic",      Table.ValueType.Int);
        measures.addColumnHeader("Diastolic",     Table.ValueType.Int);
        measures.addColumnHeader("Pulse",         Table.ValueType.Int);
        alert       = new Alert(this);
        time        = new EditTextHandler(findViewById(R.id.time));
        gap         = new EditTextHandler(findViewById(R.id.gap));
        orientation = new SpinnerHandler(findViewById(R.id.orientation), "|Lying|Seated Horizontal|Seated Vertical|Standing Horizontal|Standing Vertical", "\\|");
        side        = new SpinnerHandler(findViewById(R.id.side),        "Left|Right", "\\|");
        systolic    = new EditTextHandler(findViewById(R.id.systolic),   alert,"Systolic");
        diastolic   = new EditTextHandler(findViewById(R.id.diastolic),  alert,"Diastolic");
        pulse       = new EditTextHandler(findViewById(R.id.pulse),      alert,"Pulse");

        setTextSize(null, textSize);
        setScreenDisplay(false);
        systolic.setLister(tw);
        diastolic.setLister(tw);
        pulse.setLister(tw);
        systolic.setFocus();
        gapTimer.start();
        setActionKeys(false);
    }
}