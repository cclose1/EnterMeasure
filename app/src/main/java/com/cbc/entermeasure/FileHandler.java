package com.cbc.entermeasure;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;

import com.cbc.android.Alert;
import com.cbc.android.ConstraintLayoutHandler;
import com.cbc.android.DeviceDetails;
import com.cbc.android.EditTextHandler;
import com.cbc.android.IntentHandler;
import com.cbc.android.KeyValueStore;
import com.cbc.android.LabelledText;
import com.cbc.android.Logger;
import com.cbc.android.ScrollableTable;
import com.cbc.android.SpinnerHandler;
import com.cbc.android.Table;

import org.cbc.filehandler.FileReader;
import org.cbc.utils.system.DateFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class FileHandler extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    static public String REQUEST      = "com.cbc.entermeasure.request";
    static public String STORAGE_TYPE = "com.cbc.entermeasure.storagetype";
    static public String SELECTED     = "com.cbc.entermeasure.selected";

    public enum Request     {SelectFile, ManageFiles}
    public enum StorageType {Local, External, Dynamic}
    public enum FileType    {File, Directory}

    private class Files {
        private File        root         = null;
        private File        directory    = null;
        private File[]      files        = null;

        private StorageType source    = StorageType.Local;

        private Map<StorageType, ArrayList<File>> roots = new HashMap<>();

        private boolean same(File f1, File f2) {
            return f1.getAbsolutePath().equals(f2.getAbsolutePath());
        }
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
            if (root == null) return;

            if (dirs.contains(root))
                Logger.debug(type.toString() + " directory " + root.getAbsolutePath() + " already added");
            else {
                dirs.add(root);
                /*
                 * If the current type matches type and the root is null set it to the newly added root. This
                 * can only happen on the first root added for type.
                 */
                if (type == source && this.root == null) this.root = root;

                Logger.debug(type.toString() + " directory " + root.getAbsolutePath() + " added");
            }
        }
        /*
         * Adds roots to collection of roots.
         */
        public void addRoots(StorageType type, File[] roots) {
            if (roots  == null) return;

            for (int i = 0; i < roots.length; i++) addRoot(type, roots[i]);
        }
        public void loadFiles() {
            files = directory.listFiles();
        }
        private void setDirectory(File directory) {
            this.directory = directory;

            if (directory != null) {
                FileReader.DirectoryStats ds = FileReader.getDirectoryStats(directory);
                files = directory.listFiles();
            } else {
                files = null;
            }
        }
        private boolean setRoot(StorageType type, File root) throws IOException {
            ArrayList<File> dirs = roots.get(type);

            if (root == null || dirs != null && dirs.contains(root)) {
                this.source = type;
                this.root   = root;
                setDirectory(root);
                return true;
            }
            return false;
        }
        public void setRoot(StorageType type) throws IOException {
            File root =  roots.get(type) == null? null : roots.get(type).get(0);

            setRoot(type, root);
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
        public String checkCanRemove(StorageType type, File file) {
            if (file.isFile() || roots.get(type) == null) return "";

            for (File f: roots.get(type)) {
                if (same(f, file)) return "File " + file.getAbsolutePath() + " is a root directory";

                if (f.getAbsolutePath().startsWith(file.getAbsolutePath() + "\\")) return "File " + file.getAbsolutePath() + " is parent of root " + f.getAbsolutePath();
            }
            return "";
        }
        public String checkCanRemove(File file) {
            StorageType types[] = StorageType.values();
            String      result  = "";

            for(StorageType type: types) {
                result = checkCanRemove(type, file);

                if (result.length() != 0) return result;
            }
            return "";
        }
        public boolean canRemove(File file) {
            return checkCanRemove(file).length() == 0;
        }
        public boolean isRoot(File file) {
            return !canRemove(file);
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
        public File getFullPath(String name, boolean mustExist) throws IOException {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.getName().equals(name)) return file;
            }
            if (mustExist) throw new IOException("File " + name + " is not a directory " + directory.getAbsolutePath());

            return null;
        }
        public File getFullPath(String name) throws IOException {
            return getFullPath(name, true);
        }
        public void expand(String name) throws IOException {
            File file = getFullPath(name);

            if (!file.isDirectory()) throw new IOException(file.getAbsolutePath() + " is not a directory");

            setDirectory(file);
        }
        public void up() throws IOException {
            if (root.equals(directory)) throw new IOException("Cannot go up from root directory");

            setDirectory(directory.getParentFile());
        }
        public String getRelativeDirectory() {
            if (root == null || directory == null || same(root, directory)) return "";

            return directory.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
        }
        public void remove(File file) throws IOException {
            if (same(file, directory)) up();

            file.delete();
            loadFiles();
            logger.info("Deleted file " + file.getAbsolutePath());
        }
        public void create(FileType type, String name) throws IOException {
            boolean created = false;
            File    file    = new File(directory, name);

            if (file.exists()) throw new IOException(type.toString() + " " + name + " already exists in " + directory.toString());

            switch (type) {
                case File:
                    created = file.createNewFile();
                    break;
                case Directory:
                    created = file.mkdir();
                    break;
            }
            if (!created) throw new IOException("Create of " + name + "-ignored. No error reported");

            loadFiles();
        }
    }
    private class FileDeleter implements DialogInterface.OnClickListener {
        private Alert                     alert = null;
        private File                      file  = null;
        private FileReader.DirectoryStats stats = null;

        private String tabs(int n) {
            String t = "";

            while (n-- > 0) t += '\t';

            return t;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    files.remove(file);
                    displayFileDirectory();
                } catch (Exception e) {
                    logger.error("Dialogue failed on file deletion", e);
                }
            }
            file = null;
        }
        public FileDeleter(Context context) {
            alert = new Alert(context, this, true);
        }
        public void delete(File file) {
            StringBuilder message = new StringBuilder();
            String        title   = null;

            this.file = file;

            stats = FileReader.getDirectoryStats(file);

            if (file.isDirectory()) {
                title = "Delete Directory";
                message.append("Name"        + tabs(6) + file.getName()         + '\n');
                message.append("Depth"       + tabs(6) + stats.getDepth()       + '\n');
                message.append("Files"       + tabs(7) + stats.getFiles()       + '\n');
                message.append("Directories" + tabs(1) + stats.getDirectories() + '\n');
                message.append("Files Size"  + tabs(3) + stats.getTotalFileSize());
            } else {
                title = "Delete File";
                message.append("Name" + tabs(1) + file.getName() + '\n');
                message.append("Size" + tabs(3) + stats.getTotalFileSize());
            }
            alert.display(title, message.toString());
        }
        public void delete(String file) {
            delete(new File(file));
        }
    }
    private Alert                   alert;
    private IntentHandler           intent;
    private Request                 request;
    private EditTextHandler         etRequest     = null;
    private EditTextHandler         createName    = null;
    private SpinnerHandler          spStorageType = null;
    private SpinnerHandler          spFileType    = null;
    private SpinnerHandler          spShow        = null;
    private Logger                  logger        = new Logger("FileHandler");
    private Files                   files         = new Files();
    private DateFormatter           timeFormatter = new DateFormatter("yyyy-MM-dd HH:mm:ss", false);
    private ConstraintLayoutHandler layout        = null;
    private ViewGroup               rootLayout    = null;

    private ScrollableTable filesView      = null;
    private Table           filesTable     = null;
    private LabelledText    fldRoot        = null;
    private LabelledText    fldRootPath    = null;
    private LabelledText    fldDirectory   = null;
    private KeyValueStore   rootStore      = null;
    private Button          fldUp          = null;
    private CheckBox        persist        = null;
    private ButtonListener  buttonListener = new ButtonListener();
    private FileDeleter     deleter        = null;
    private int             rootNameSize   = 39;
    private int             dirNameSize    = 14;

    private void setTableFields(boolean directory) {
        findViewById(R.id.manageFields).setVisibility(directory? View.VISIBLE : View.GONE);
        findViewById(R.id.rootFields).setVisibility(directory? View.GONE : View.VISIBLE);
        /*
         * Changing the visibility of rootFields does not work, do it for each field.
         */
        findViewById(R.id.addRoot).setVisibility(directory? View.GONE : View.VISIBLE);
        persist.setVisibility(directory? View.GONE : View.VISIBLE);
        fldRootPath.setVisible(!directory);
    }
    private void setTable(boolean directory) {
        filesTable.removeRows();
        filesTable.setMaxLength("Name", directory? dirNameSize : rootNameSize);
        filesTable.setColumnVisible("Type",     directory);
        filesTable.setColumnVisible("Modified", directory);
        filesTable.setColumnVisible("Size",     directory);
        setTableFields(directory);
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
        fldRoot.setText(files.getRoot() == null? "" : files.getRoot().getAbsolutePath());
        filesView.loadTable(filesTable, true);
    }
    private void displayFileDirectory() {
        String directory = files.getRelativeDirectory();

        setTable(true);
        setScreen();

        for (Iterator<File> it = files.iterator(); it.hasNext(); ) {
            addRow(filesTable, it.next(), false);
        }
        fldRoot.setText(files.getRoot().getAbsolutePath());
        filesView.loadTable(filesTable, true);
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
        return spShow.getSelected().equals("Directory");
    }
    private void displayFilesTable() throws Exception {
        StorageType st = spStorageType.getEnumSelected();

        if (isShowDirectory()) {
            displayFileDirectory();
        } else
            displayRootDirectory();
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        try {
            switch (parent.getId()) {
                case R.id.storageType:
                    logger.info(
                        "StorageType selected position " + position     +
                        " view id "                      + view.getId() +
                        " value "                        + spStorageType.getValueAtPosition(position) +
                        " parent class "                 + parent.getClass().getName());
                    files.setRoot((StorageType) spStorageType.getEnumSelected());
                    displayFilesTable();
                    break;
                case R.id.show:
                    displayFilesTable();
                break;
            default:
                logger.warning("OnItemSelected for id " + parent.getId() + " ignored");
        }
    } catch (Exception e) {
        logger.error("Position " + position + " not found in storage type spinner", e);
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
    private void debugLog() {
        Logger log            = new Logger(null);
        Logger.LogViews views = log.createLogViews();
        Logger.logDisplayStats();
        views.addAll("Content",    findViewById(android.R.id.content));
        views.add("Main Layout",   findViewById(R.id.fhLayout));
        views.add("Manage Fields", findViewById(R.id.manageFields));
        views.add("Log",           findViewById(R.id.log));
        views.log();
    }
    public void onClick(View view) {
        int id = view.getId();

        try {
            if (id ==  R.id.log) {
                debugLog();
            } else if (id == fldDirectory.getId()) {
                String name  = fldDirectory.getText();
                File   file  = files.getDirectory();
                String check = files.checkCanRemove(file);

                if (check.length() != 0) {
                    alert.display("Validation Failure", check);
                    return;
                }
                if (!files.getRelativeDirectory().equals(name)) {
                    /*
                     * This an internal error.
                     */
                    logger.error("For delete directory, displayed directory " + name + " does not equal files directory " + files.getRelativeDirectory());
                    return;
                }
                deleter.delete(file);
            } else if (id == R.id.create) {
                FileType ft = spFileType.getEnumSelected();

                if (!createName.checkPresent()) return;

                try {
                    files.create(ft, createName.getText());
                    displayFileDirectory();
                } catch (IOException e) {
                    alert.display("Error", e.toString());
                }
            } else if (id == R.id.addRoot) {
                if (!fldRootPath.checkPresent()) return;

                File root = new File(fldRootPath.getText());

                if (!root.isDirectory()) {
                    fldRootPath.alert("Must be a directory");
                    return;
                }
                if (files.isRoot(root)) {
                    fldRootPath.alert("Already a root directory");
                    return;
                }
                files.addRoot(StorageType.Dynamic, root);

                if (persist.isChecked()) rootStore.addValue("Roots", fldRootPath.getText());

                fldRootPath.setText("");
                displayRootDirectory();
            } else
                logger.warning("OnClick id " + view.getId() + " not expected");
        } catch (Exception e) {
            logger.error("On click for Id " + id, e);
        }
    }
    private class TableRowListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Table.Row row  = (Table.Row) view.getTag();
            String    file = row.getCell("Name").getValue();

            try {
                if (isShowDirectory()) {
                    if (row.getCell("Type").getValue().equals("Dir")) {
                        files.expand(file);
                    } else {
                        if (request == Request.SelectFile) {
                            reply(files.getFullPath(file).getAbsolutePath());
                        } else {
                            deleter.delete(files.getFullPath(file));
                        }
                    }
                } else {
                    files.setRoot(new File(file));
                    spShow.setSelected("Directory");
                }
                displayFileDirectory();
            } catch (IOException e) {
                logger.error("Table row click ", e);
            }
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        logger.info("StorageType onNothingSelected-parent " + parent.getClass().getName());
    }
    private void reply(String file) {
        Intent data = new Intent();

        data.putExtra(FileHandler.REQUEST, request.toString());

        if (file != null) data.putExtra(FileHandler.SELECTED, file);

        setResult(RESULT_OK, data);
        finish();
    }
    public void onComplete(View view) {
        reply(null);
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
    private void setScreen() {
        int visible = etRequest.getText().equals("ManageFiles")? View.VISIBLE : View.GONE;

        findViewById(R.id.create).setVisibility(visible);
        findViewById(R.id.fileType).setVisibility(visible);
        findViewById(R.id.name).setVisibility(visible);
        findViewById(R.id.complete).setVisibility(visible);
    }
    @Override
    public void onBackPressed() {
        onComplete(null);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
             * Make menu back arrow behave as Complete button;
             */
            case android.R.id.home:
                onComplete(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_handler);
        alert         = new Alert(this);
        etRequest     = new EditTextHandler(findViewById(R.id.request));
        createName    = new EditTextHandler(findViewById(R.id.name), alert, "Create File");
        spStorageType = new SpinnerHandler(findViewById(R.id.storageType), StorageType.class);
        spFileType    = new SpinnerHandler(findViewById(R.id.fileType),    FileType.class);
        spShow        = new SpinnerHandler(findViewById(R.id.show),        "Roots|Directory", "\\|");
        intent        = new IntentHandler(getIntent());
        request       = intent.getEnumAction(Request.class);
        layout        = new ConstraintLayoutHandler(findViewById(R.id.fhLayout));
        rootLayout    = (ViewGroup) findViewById(R.id.rootFields);
        deleter       = new FileDeleter(this);
        fldRoot       = new LabelledText(layout, "Root");
        persist       = new CheckBox(this);
        persist.setText("Persist");
        fldRoot.setConstraint(R.id.show, ConstraintSet.LEFT, ConstraintSet.PARENT_ID);
        fldRoot.setReadOnly(true);
        fldRoot.setLines(1, 2);
        fldRootPath   = new LabelledText(rootLayout, "Path", "40%", alert);
        fldRootPath.setBackgroundColor(Color.rgb(230,255,255));
        rootLayout.addView(persist);
        fldDirectory  = new LabelledText((ViewGroup) layout.getLayout(), "Directory");
        fldDirectory.setConstraint(fldRoot.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID);
        fldDirectory.setReadOnly(true);
        fldDirectory.setOnClickListener(this);
        fldUp         = (Button) layout.addView(new Button(this));
        fldUp.setText("Up");
        fldUp.setOnClickListener(buttonListener);
        filesView     = new ScrollableTable((View) fldDirectory.getEditText(), 300);
        filesTable    = new Table("Files", this);
        etRequest.setText(request.toString());
        rootStore     = new KeyValueStore(this, "roots");

        layout.connect(fldUp,             ConstraintSet.LEFT,  R.id.guideline,          ConstraintSet.LEFT);
        layout.connect(fldUp,             ConstraintSet.RIGHT, R.id.guideline,          ConstraintSet.RIGHT);
        layout.connect(fldUp,             ConstraintSet.TOP,   filesView.getScrollId(), ConstraintSet.BOTTOM, 10);
        layout.connect(R.id.manageFields, ConstraintSet.TOP,   fldUp.getId(),           ConstraintSet.BOTTOM);

        layout.apply();

        filesTable.addColumnHeader("Name",     Table.ValueType.String, true, dirNameSize);
        filesTable.addColumnHeader("Type",     true);
        filesTable.addColumnHeader("Write",    true);
        filesTable.addColumnHeader("Modified", true);
        filesTable.addColumnHeader("Count",    Table.ValueType.Int);
        filesTable.addColumnHeader("Size",     Table.ValueType.Int);
        filesView.setValuePixelSource(ScrollableTable.ValuePixelSource.Measure);
        filesView.setDisplayOptionsMode(ScrollableTable.DisplayWidthMode.OnOverflowFillScreen);
        filesView.setFullScreenBorder(0);
        filesView.setColumnGap(1);
        filesView.setRowListener(new TableRowListener());
        spStorageType.getSpinner().setOnItemSelectedListener(this);
        spShow.getSpinner().setOnItemSelectedListener(this);
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
//        files.addRoot(StorageType.Dynamic,   new File("/sdcard"));

        for(String root : rootStore.getValues("Roots")) {
            files.addRoot(StorageType.Dynamic, new File(root));
        }
        setScreen();
        /*
         * Following is to stop the soft keyboard popping up and behave like a numeric keyboard when
         * using the emulator, i.e. uses development device keyboard.
         *
         * Setting input type in xml to textNosuggestions is not sufficient.
         */
        if (DeviceDetails.isEmulator()) {
            createName.getEditText().setShowSoftInputOnFocus(false);
            createName.getEditText().setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        try {
            files.setRoot(StorageType.Local);
            displayFileDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
