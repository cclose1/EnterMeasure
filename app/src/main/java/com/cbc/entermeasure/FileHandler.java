package com.cbc.entermeasure;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableRow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;

import com.cbc.android.ConstraintLayoutHandler;
import com.cbc.android.EditTextHandler;
import com.cbc.android.IntentHandler;
import com.cbc.android.LabelledText;
import com.cbc.android.Logger;
import com.cbc.android.ScrollableTable;
import com.cbc.android.SpinnerHandler;
import com.cbc.android.Table;

import org.cbc.utils.system.DateFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

public class FileHandler extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    static public String REQUEST      = "com.cbc.entermeasure.request";
    static public String STORAGE_TYPE = "com.cbc.entermeasure.storagetype";

    public enum Request     {GetFile, GetPath}
    public enum StorageType {Local, External}

    private class Files {
        private File        root      = null;
        private File        directory = null;
        private File[]      files     = null;
        private StorageType source    = StorageType.Local;

        private Map<StorageType, ArrayList<File>> roots = new HashMap<>();

        private class DirectoryIterator implements Iterator<File> {
            int             count     = 0;
            boolean         canRemove = false;
            ArrayList<File> list      = null;

            public DirectoryIterator() {
                list      = null;
                canRemove = false;
            }
            public DirectoryIterator(StorageType type) {
                list      = roots.get(type);
                canRemove = true;
            }
            @Override
            public boolean hasNext() {
                if (list == null)
                    return files != null && count < files.length;
                else
                    return count < list.size();
            }
            @Override
            public File next() {
                if (hasNext()) {
                    canRemove = false;

                    if (list == null) return files[count++];

                    return list.get(count++);
                } else
                    throw new NoSuchElementException();
            }
            @Override
            public void remove() {
                if (!canRemove) throw new IllegalStateException();

                list.remove(--count);
            }
        }
        public Iterator<File> iterator() {
            return new DirectoryIterator();
        }
        public Iterator<File> iterator(StorageType type) {
            return new DirectoryIterator(type);
        }
        public Files() {
        }
        /*
         * Adds root to the collection of roots, if it is not already there.
         *
         * Note: root can be null, in which event no action is taken, other than to create a new ArrayList
         *       for type.
         */
        public void addRoot(StorageType type, File root) {
            ArrayList<File> dirs = roots.get(type);

            if (dirs == null) {
                dirs = new ArrayList<File>();
                roots.put(type, dirs);
            }
            if (root != null && !dirs.contains(root)) dirs.add(root);
        }
        /*
         * Adds roots to collection of roots.
         */
        public void addRoots(StorageType type, File[] roots) {
            if (roots  == null) return;

            for (int i = 0; i < roots.length; i++) addRoot(type, roots[i]);
        }
        public void loadFiles() throws IOException {
            files = directory.listFiles();
        }
        private void setDirectory(File directory) {
            this.directory = directory;
            files = directory.listFiles();
        }
        private boolean setRoot(StorageType type, File root) throws IOException {
            ArrayList<File> dirs = roots.get(type);

            if (dirs.contains(root)) {
                this.source = type;
                this.root   = root;
                setDirectory(root);
                return true;
            }
            return false;
        }
        public void setRoot(StorageType type) throws IOException {
            File root = roots.get(type).get(0);

            if (root != null) setRoot(type, root);
        }
        /*
         * Sets root directory to root. If root is not in the roots collection an IOException is thrown
         */
        public void setRoot(File root) throws IOException {
            for(StorageType type: StorageType.values()) {
                if (setRoot(type, root)) return;
            }
            throw new IOException("Directory " + directory.getAbsolutePath() + " is not a valid root");
        }
        public File getRoot() {
            return root;
        }
        public File getDirectory() {
            return directory;
        }
        public ArrayList<File> getDirectories(StorageType type) {
            return roots.get(type);
        }
        public String[] getFileNames() throws IOException {
            String[] files = new String[this.files.length];

            for (int i = 0; i < this.files.length; i++) {
                files[i] = this.files[i].getName();
            }
            return files;
        }
        public void expand(String name) throws IOException {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.getName().equals(name)) {
                    if (!file.isDirectory()) throw new IOException(file.getAbsolutePath() + " is not a directory");

                    setDirectory(file);
                    return;
                }
            }
            throw new IOException("Name " + name + " is not in directory " + directory.getAbsolutePath());
        }
        public void up() throws IOException {
            if (root.equals(directory)) throw new IOException("Cannot go up from root directory");

            setDirectory(directory.getParentFile());
        }
        public String getRelativeDirectory() {
            if (root == null || directory == null || root.getAbsolutePath().equals(directory.getAbsolutePath())) return "";

            return directory.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
        }
    }
    private IntentHandler           intent;
    private Request                 request;
    private EditTextHandler         etRequest     = null;
    private SpinnerHandler          spStorageType = null;
    private Logger                  logger        = new Logger("FileHandler");
    private Files                   files         = new Files();
    private DateFormatter           timeFormatter = new DateFormatter("yyyy-MM-dd HH:mm:ss", false);
    private ConstraintLayoutHandler layout        = null;

    private ScrollableTable  filesView      = null;
    private Table            filesTable     = null;
    private LabelledText     fldRoot        = null;
    private LabelledText     fldDirectory   = null;
    private Button           fldUp          = null;
    private ButtonListener   buttonListener = new ButtonListener();
    private RadioGroup       show           = null;

    private void setTable(boolean directory) {
        filesTable.removeRows();
        filesTable.setColumnVisible("Type",     directory);
        filesTable.setColumnVisible("Modified", directory);
        filesTable.setColumnVisible("Size",     directory);
    }
    private void addRow(Table table, File file, boolean isRoot) {
        Table.Row row  = table.createRow();

        table.addRow(row, false);
        row.setCell("Name",     isRoot? file.getAbsolutePath() : file.getName());
        row.setCell("Type",     file.isFile() ? "File" : "Dir");
        row.setCell("Write",    file.canWrite() ? "Y" : "N");
        row.setCell("Modified", timeFormatter.format(new Date(file.lastModified())));
        row.setCell("Size",     file.length());
        row.setCell("Count",    file.listFiles() == null? 0 : file.listFiles().length);
    }
    private void displayRootDirectory() throws Exception {
        filesTable.removeRows();

        setTable(false);

        for (Iterator<File> it = files.iterator((StorageType) spStorageType.getEnumSelected()); it.hasNext(); ) {
            addRow(filesTable, it.next(), true);
        }
        fldRoot.setText(files.getRoot().getAbsolutePath());
        filesView.loadTable(filesTable);
    }
    private void displayFileDirectory() {
        String directory = files.getRelativeDirectory();

        setTable(true);

        for (Iterator<File> it = files.iterator(); it.hasNext(); ) {
            addRow(filesTable, it.next(), false);
        }
        fldRoot.setText(files.getRoot().getAbsolutePath());
        filesView.loadTable(filesTable);
        fldDirectory.setText(directory);

        if (directory.length() == 0) {
            layout.clear(fldRoot.getLabel(), ConstraintSet.RIGHT);
            layout.connect(fldRoot.getLabel(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            layout.apply();
            fldDirectory.setVisible(false);
            fldUp.setVisibility(View.GONE);
        } else {
            layout.clear(fldRoot.getLabel(), ConstraintSet.LEFT);
            layout.connect(fldRoot.getLabel(), ConstraintSet.RIGHT, fldDirectory.getLabel(), ConstraintSet.RIGHT);
            layout.apply();
            fldDirectory.setVisible(true);
            fldUp.setVisibility(View.VISIBLE);
        }
    }
    private boolean isShowDirectory() {
        switch (show.getCheckedRadioButtonId()) {
            case R.id.showdirectory:
                return true;
            case R.id.showroots:
                return false;
            default:
                logger.warning("Show Radio Group " + show.getCheckedRadioButtonId() + " is not expected");
        }
        return true;
    }
    private void displayFilesTable() throws Exception {
        StorageType st = spStorageType.getEnumSelected();

        if (isShowDirectory()) {
            files.setRoot(st);
            displayFileDirectory();
        } else
            displayRootDirectory();
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.storageType:
                logger.info(
                        "StorageType selected position " + position     +
                        " view id "                      + view.getId() +
                        " value "                        + spStorageType.getValueAtPosition(position) +
                        " parent class "                 + parent.getClass().getName());
                try {
                    displayFilesTable();
                } catch (Exception e) {
                    logger.error("Position " + position + " not found in storage type spinner", e);
                }
                break;
            default:
                logger.warning("OnItemSelected for id " + parent.getId() + " ignored");
        }
    }
    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.getId() == fldUp.getId()) {
                try {
                    files.up();
                    displayFileDirectory();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private class TableRowListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Table.Row row  = (Table.Row) view.getTag();
            String    file = row.getCell("Name").getValue();

            if (isShowDirectory()) {
                if (row.getCell("Type").getValue().equals("Dir")) {
                    try {
                        files.expand(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    files.setRoot(new File(file));
                    show.check(R.id.showdirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            displayFileDirectory();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        logger.info("StorageType onNothingSelected-parent " + parent.getClass().getName());
    }
    public void onComplete(View view) throws Exception {
        Intent data = new Intent();

        StorageType st = spStorageType.getEnumSelected();
        data.putExtra(FileHandler.REQUEST, request.toString());
        setResult(RESULT_OK, data);
        finish();
    }
    public void storageTypeClicked(View view) {
    }
    private String getAbsolutePath(File file) {
        return file == null? "null" : file.getAbsolutePath();
    }
    private void logDir(String name, File directory) {
        logger.info("Directory " + Logger.rPad(name, 19) + " path " + getAbsolutePath(directory));
    }
    private void logDir(String name, File[] directories) {
        for (int i = 0; i < directories.length; i++) {
            logger.info("Directory " + Logger.rPad(name, 19) + " path " + getAbsolutePath(directories[i]));
        }
    }
    public void radioOptionClicked(View view) throws Exception {
        switch (((RadioGroup) view.getParent()).getId()) {
            case R.id.setfilesoption:
                displayFilesTable();
                break;
            default:
                logger.warning("Radio Group " + view.getId() + " not expected");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_handler);
        etRequest     = new EditTextHandler(findViewById(R.id.request));
        spStorageType = new SpinnerHandler(findViewById(R.id.storageType), StorageType.class);
        intent        = new IntentHandler(getIntent());
        request       = intent.getEnumAction(Request.class);
        layout        = new ConstraintLayoutHandler(findViewById(R.id.fhLayout));
        show          = (RadioGroup) findViewById(R.id.setfilesoption);

        fldRoot       = new LabelledText(layout, "Root");
        fldRoot.setConstraint(R.id.complete, ConstraintSet.LEFT, ConstraintSet.PARENT_ID);
        fldRoot.setReadOnly(true);
        fldDirectory  = new LabelledText((ViewGroup) layout.getLayout(), "Directory");
        fldDirectory.setConstraint(fldRoot.getText().getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID);
        fldDirectory.setReadOnly(true);
        fldUp         = (Button) layout.addView(new Button(this));
        fldUp.setText("Up");
        fldUp.setOnClickListener(buttonListener);
        filesView     = new ScrollableTable((View)fldDirectory.getText(), 300);
        filesTable    = new Table("Files", this);
        etRequest.setText(request.toString());

        layout.connect(fldUp, ConstraintSet.LEFT,  R.id.guideline,          ConstraintSet.LEFT);
        layout.connect(fldUp, ConstraintSet.RIGHT, R.id.guideline,          ConstraintSet.RIGHT);
        layout.connect(fldUp, ConstraintSet.TOP,   filesView.getScrollId(), ConstraintSet.BOTTOM, 10);
        layout.apply();

        filesTable.addColumnHeader("Name",     Table.ValueType.String, true, 30);
        filesTable.addColumnHeader("Type",     true);
        filesTable.addColumnHeader("Write",    true);
        filesTable.addColumnHeader("Modified", true);
        filesTable.addColumnHeader("Count",    Table.ValueType.Int);
        filesTable.addColumnHeader("Size",     Table.ValueType.Int);
        filesView.setValuePixelSource(ScrollableTable.ValuePixelSource.Measure);
        filesView.setRowListener(new TableRowListener());
        spStorageType.getSpinner().setOnItemSelectedListener(this);
        logDir("DataDir",           getDataDir());
        logDir("FilesDir",          getFilesDir());
        logDir("CacheDir",          getCacheDir());
        logDir("ExternalFilesDir",  getExternalFilesDir(null));
        logDir("ExternalCacheDir",  getExternalCacheDir());
        logDir("ExternalMediaDirs", getExternalMediaDirs());
        logDir("ExternalCacheDirs", getExternalCacheDirs());
        logDir("ExternalFilesDirs", getExternalFilesDirs(null));
        files.addRoot(StorageType.Local,     getFilesDir());
        files.addRoot(StorageType.Local,     getDataDir());
        files.addRoot(StorageType.Local,     getCacheDir());
        files.addRoot(StorageType.External,  getExternalFilesDir(null));
        files.addRoot(StorageType.External,  getExternalCacheDir());
        files.addRoots(StorageType.External, getExternalMediaDirs());
        files.addRoots(StorageType.External, getExternalCacheDirs());
        files.addRoots(StorageType.External, getExternalFilesDirs(null));

        try {
            files.setRoot(StorageType.Local);
            displayFileDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
