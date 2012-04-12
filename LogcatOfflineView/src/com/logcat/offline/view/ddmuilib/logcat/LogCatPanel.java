/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logcat.offline.view.ddmuilib.logcat;

import com.android.ddmlib.DdmConstants;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.ITableFocusListener.IFocusedTableActivator;
import com.android.ddmuilib.actions.ToolItemAction;
import com.android.ddmuilib.logcat.ILogCatMessageSelectionListener;
import com.android.ddmuilib.logcat.LogCatFilterContentProvider;
import com.android.ddmuilib.logcat.LogCatFilterLabelProvider;
import com.android.ddmuilib.logcat.LogCatMessage;
import com.android.ddmuilib.logcat.LogCatViewerFilter;
import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.ImageLoader;
import com.android.ddmuilib.TableHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LogCatPanel displays a table listing the logcat messages.
 */
public final class LogCatPanel implements ILogCatMessageEventListener{
    private static final String RESET_ALL_FILTER = "Reset All Filter";

	private static final String RESET_PID_FILTER = "Reset PID Filter";

	private static final String RESET_TAG_FILTER = "Reset Tag Filter";

	/** Preference key to use for storing list of logcat filters. */
    public static final String LOGCAT_FILTERS_LIST = "logcat.view.filters.list";

    /** Preference key to use for storing font settings. */
    public static final String LOGCAT_VIEW_FONT_PREFKEY = "logcat.view.font";

    // Use a monospace font family
    private static final String FONT_FAMILY =
            DdmConstants.CURRENT_PLATFORM == DdmConstants.PLATFORM_DARWIN ? "Monaco":"Courier New";

    // Use the default system font size
    private static final FontData DEFAULT_LOGCAT_FONTDATA;
    static {
        int h = Display.getDefault().getSystemFont().getFontData()[0].getHeight();
        DEFAULT_LOGCAT_FONTDATA = new FontData(FONT_FAMILY, h, SWT.NORMAL);
    }

    public static final String LOGCAT_VIEW_COLSIZE_PREFKEY_PREFIX = "logcat.view.colsize.";
    public static final String DISPLAY_FILTERS_COLUMN_PREFKEY = "logcat.view.display.filters";

    /** Default message to show in the message search field. */
    private static final String DEFAULT_SEARCH_MESSAGE =
            "Search for messages. Accepts Java regexes. "
            + "Prefix with pid:, tag: or text: to limit scope.";

    /** Tooltip to show in the message search field. */
    private static final String DEFAULT_SEARCH_TOOLTIP =
            "Example search patterns:\n"
          + "    sqlite (search for sqlite in text field)";

    private static final String IMAGE_ADD_FILTER = "add.png"; //$NON-NLS-1$
    private static final String IMAGE_DELETE_FILTER = "delete.png"; //$NON-NLS-1$
    private static final String IMAGE_EDIT_FILTER = "edit.png"; //$NON-NLS-1$
    private static final String IMAGE_SAVE_LOG_TO_FILE = "save.png"; //$NON-NLS-1$
    private static final String IMAGE_DISPLAY_FILTERS = "displayfilters.png"; //$NON-NLS-1$
    
    private static final String ACTION_SHOW_TAG = "Show Selected Tag(s)";
    private static final String ACTION_HIDE_TAG = "Hide Selected Tag(s)";
    private static final String ACTION_HIGHLIGHT_TAG = "Highlight Selected Tag(s)";
    private static final String ACTION_SHOW_PID = "Show Selected PID(s)";
    private static final String ACTION_HIDE_PID = "Hide Selected PID(s)";
    private static final String ACTION_HIGHLIGHT_PID = "Highlight Selected PID(s)";
    
    private ToolItemAction[] mLogLevelActions;
    private String[] mLogLevelIcons = {
            "v.png", //$NON-NLS-1S
            "d.png", //$NON-NLS-1S
            "i.png", //$NON-NLS-1S
            "w.png", //$NON-NLS-1S
            "e.png", //$NON-NLS-1S
    };
    
    private String mCurrentFilterLogLevel = LogLevel.values()[0].getStringValue();

    private static final int[] WEIGHTS_SHOW_FILTERS = new int[] {15, 85};
    private static final int[] WEIGHTS_LOGCAT_ONLY = new int[] {0, 100};

    private PreferenceStore mPrefStore;

    private List<LogCatFilter> mLogCatFilters;
    private int mCurrentSelectedFilterIndex;

    private ToolItem mNewFilterToolItem;
    private ToolItem mDeleteFilterToolItem;
    private ToolItem mEditFilterToolItem;
    private TableViewer mFiltersTableViewer;

    private Text mLiveFilterText;
    private List<String> mSelectedPIDList;
    private List<String> mSelectedTagList;
    private List<String> mPIDList = new ArrayList<String>();
    private List<String> mTagList = new ArrayList<String>();

    private TableViewer mViewer;
    private Action mShowSelectedTag;
    private Action mHideSelectedTag;
    private Action mHighlightSelectedTag;
    private Action mResetTag;
    private Action mShowSelectedPID;
    private Action mHideSelectedPID;
    private Action mHighlightSelectedPID;
    private Action mResetPID;

    private String mLogFileExportFolder;
    
    private Color mYellow;
    private Color mGreen;
    
    private boolean mIsSynFromHere = false;
    

    private boolean mShouldScrollToLatestLog = true;
//    private ToolItem mPauseLogcatCheckBox;
    private boolean mLastItemPainted = false;

    private LogCatMessageLabelProvider mLogCatMessageLabelProvider;

    private SashForm mSash;
    
    private int mPanelID;
    private String mPannelName;
    /**
     * Construct a logcat panel.
     * @param prefStore preference store where UI preferences will be saved
     */
    public LogCatPanel(PreferenceStore prefStore, int panelID, String pannelName) {
        mPrefStore = prefStore;
        mPanelID = panelID;
        mPannelName = pannelName;
        
        LogCatMessageParser.getInstance().addMessageReceivedEventListener(this);
        LogCatSynSelectedListener.getInstance().addMessageReceivedEventListener(this);
        initializeFilters();
        setupDefaultPreferences();
        initializePreferenceUpdateListeners();
    }
    
    private void initializeFilters() {
        mLogCatFilters = new ArrayList<LogCatFilter>();

        /* add default filter matching all messages */
        String tag = "";
        String text = "";
        String pid = "";
//        String app = "";
        mLogCatFilters.add(new LogCatFilter("All messages (no filters)",
                tag, text, pid, "no tid", LogLevel.VERBOSE, new ArrayList<String>(), new ArrayList<String>()));

        /* restore saved filters from prefStore */
        List<LogCatFilter> savedFilters = getSavedFilters();
        mLogCatFilters.addAll(savedFilters);
    }

    private void setupDefaultPreferences() {
        PreferenceConverter.setDefault(mPrefStore, LogCatPanel.LOGCAT_VIEW_FONT_PREFKEY + mPanelID,
                DEFAULT_LOGCAT_FONTDATA);
        mPrefStore.setDefault(DISPLAY_FILTERS_COLUMN_PREFKEY + mPanelID, true);
    }

    private void initializePreferenceUpdateListeners() {
        mPrefStore.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                String changedProperty = event.getProperty();

                if (changedProperty.equals(LogCatPanel.LOGCAT_VIEW_FONT_PREFKEY + mPanelID)) {
                    mLogCatMessageLabelProvider.setFont(getFontFromPrefStore());
                    refreshLogCatTable();
                } else {
//                    mReceiver.resizeFifo(mPrefStore.getInt(
//                            LogCatMessageList.MAX_MESSAGES_PREFKEY));
                    refreshLogCatTable();
                }
            }
        });
    }

    private void saveFilterPreferences() {
        LogCatFilterSettingsSerializer serializer = new LogCatFilterSettingsSerializer();

        /* save all filter settings except the first one which is the default */
        String e = serializer.encodeToPreferenceString(
                mLogCatFilters.subList(1, mLogCatFilters.size()));
        mPrefStore.setValue(LOGCAT_FILTERS_LIST + mPanelID, e);
    }

    private List<LogCatFilter> getSavedFilters() {
        LogCatFilterSettingsSerializer serializer = new LogCatFilterSettingsSerializer();
        String e = mPrefStore.getString(LOGCAT_FILTERS_LIST + mPanelID);
        return serializer.decodeFromPreferenceString(e);
    }


    public Control createControl(Composite parent) {
        GridLayout layout = new GridLayout(1, false);
        parent.setLayout(layout);

        createViews(parent);
        setupDefaults();

        return null;
    }

    private void createViews(Composite parent) {
        mSash = createSash(parent);

        createListOfFilters(mSash);
        createLogTableView(mSash);

        boolean showFilters = mPrefStore.getBoolean(DISPLAY_FILTERS_COLUMN_PREFKEY + mPanelID);
        updateFiltersColumn(showFilters);
    }

    private SashForm createSash(Composite parent) {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        return sash;
    }

    private void createListOfFilters(SashForm sash) {
        Composite c = new Composite(sash, SWT.BORDER);
        GridLayout layout = new GridLayout(2, false);
        c.setLayout(layout);
        c.setLayoutData(new GridData(GridData.FILL_BOTH));

        createFiltersToolbar(c);
        createFiltersTable(c);
    }

    private void createFiltersToolbar(Composite parent) {
        Label l = new Label(parent, SWT.NONE);
        l.setText("Saved Filters");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        ToolBar t = new ToolBar(parent, SWT.FLAT);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        t.setLayoutData(gd);

        /* new filter */
        mNewFilterToolItem = new ToolItem(t, SWT.PUSH);
        mNewFilterToolItem.setImage(
                ImageLoader.getDdmUiLibLoader().loadImage(IMAGE_ADD_FILTER, t.getDisplay()));
        mNewFilterToolItem.setToolTipText("Add a new logcat filter");
        mNewFilterToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                addNewFilter();
            }
        });

        /* delete filter */
        mDeleteFilterToolItem = new ToolItem(t, SWT.PUSH);
        mDeleteFilterToolItem.setImage(
                ImageLoader.getDdmUiLibLoader().loadImage(IMAGE_DELETE_FILTER, t.getDisplay()));
        mDeleteFilterToolItem.setToolTipText("Delete selected logcat filter");
        mDeleteFilterToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                deleteSelectedFilter();
            }
        });

        /* edit filter */
        mEditFilterToolItem = new ToolItem(t, SWT.PUSH);
        mEditFilterToolItem.setImage(
                ImageLoader.getDdmUiLibLoader().loadImage(IMAGE_EDIT_FILTER, t.getDisplay()));
        mEditFilterToolItem.setToolTipText("Edit selected logcat filter");
        mEditFilterToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                editSelectedFilter();
            }
        });
    }

    private void addNewFilter() {
        LogCatFilterSettingsDialog d = new LogCatFilterSettingsDialog(
                Display.getCurrent().getActiveShell());
        d.setPIDList(mPIDList);
        d.setTagList(mTagList);
        if (d.open() != Window.OK) {
            return;
        }

        LogCatFilter f = new LogCatFilter(d.getFilterName().trim(),
                d.getTag().trim(),
                d.getText().trim(),
                d.getPid().trim(),
                "no tid",
//                d.getAppName().trim(),
                LogLevel.getByString(d.getLogLevel()), d.getPIDHideList(), d.getTagHideList());

        mLogCatFilters.add(f);
        mFiltersTableViewer.refresh();

        /* select the newly added entry */
        int idx = mLogCatFilters.size() - 1;
        mFiltersTableViewer.getTable().setSelection(idx);

        filterSelectionChanged();
        saveFilterPreferences();
    }

    private void deleteSelectedFilter() {
        int selectedIndex = mFiltersTableViewer.getTable().getSelectionIndex();
        if (selectedIndex <= 0) {
            /* return if no selected filter, or the default filter was selected (0th). */
            return;
        }

        mLogCatFilters.remove(selectedIndex);
        mFiltersTableViewer.refresh();
        mFiltersTableViewer.getTable().setSelection(selectedIndex - 1);

        filterSelectionChanged();
        saveFilterPreferences();
    }

    private void editSelectedFilter() {
        int selectedIndex = mFiltersTableViewer.getTable().getSelectionIndex();
        if (selectedIndex < 0) {
            return;
        }

        LogCatFilter curFilter = mLogCatFilters.get(selectedIndex);

        LogCatFilterSettingsDialog dialog = new LogCatFilterSettingsDialog(
                Display.getCurrent().getActiveShell());
        dialog.setDefaults(curFilter.getName(), curFilter.getTag(), curFilter.getText(),
                curFilter.getPid(), curFilter.getLogLevel(), mPIDList, curFilter.getPIDHideList(),
                mTagList, curFilter.getTagHideList());
        if (dialog.open() != Window.OK) {
            return;
        }

        LogCatFilter f = new LogCatFilter(dialog.getFilterName(),
                dialog.getTag(),
                dialog.getText(),
                dialog.getPid(),
                "no tid",
//                dialog.getAppName(),
                LogLevel.getByString(dialog.getLogLevel()), dialog.getPIDHideList(), dialog.getTagHideList());
        mLogCatFilters.set(selectedIndex, f);
        mFiltersTableViewer.refresh();

        mFiltersTableViewer.getTable().setSelection(selectedIndex);
        filterSelectionChanged();
        saveFilterPreferences();
    }

  /*  *//**
     * Select the transient filter for the specified application. If no such filter
     * exists, then create one and then select that. This method should be called from
     * the UI thread.
     * @param appName application name to filter by
     *//*
    public void selectTransientAppFilter(String appName) {
        assert mViewer.getTable().getDisplay().getThread() == Thread.currentThread();

        LogCatFilter f = findTransientAppFilter(appName);
        if (f == null) {
            f = createTransientAppFilter(appName);
            mLogCatFilters.add(f);
        }

        selectFilterAt(mLogCatFilters.indexOf(f));
    }

    private LogCatFilter findTransientAppFilter(String appName) {
        for (LogCatFilter f : mLogCatFilters) {
            if (f.isTransient() && f.getAppName().equals(appName)) {
                return f;
            }
        }
        return null;
    }

    private LogCatFilter createTransientAppFilter(String appName) {
        LogCatFilter f = new LogCatFilter(appName + " (Session Filter)",
                "",
                "",
                "",
                appName,
                LogLevel.VERBOSE);
        f.setTransient();
        return f;
    }

    private void selectFilterAt(final int index) {
        mFiltersTableViewer.refresh();
        mFiltersTableViewer.getTable().setSelection(index);
        filterSelectionChanged();
    }*/

    private void createFiltersTable(Composite parent) {
        final Table table = new Table(parent, SWT.FULL_SELECTION);

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        table.setLayoutData(gd);

        mFiltersTableViewer = new TableViewer(table);
        mFiltersTableViewer.setContentProvider(new LogCatFilterContentProvider());
        mFiltersTableViewer.setLabelProvider(new LogCatFilterLabelProvider());
        mFiltersTableViewer.setInput(mLogCatFilters);

        mFiltersTableViewer.getTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                filterSelectionChanged();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                editSelectedFilter();
            }
        });
    }

    private void createLogTableView(SashForm sash) {
        Composite c = new Composite(sash, SWT.NONE);
        c.setLayout(new GridLayout());
        c.setLayoutData(new GridData(GridData.FILL_BOTH));

        createLiveFilters(c);
        createLogcatViewTable(c);
    }

    /**
     * Create the search bar at the top of the logcat messages table.
     * FIXME: Currently, this feature is incomplete: The UI elements are created, but they
     * are all set to disabled state.
     */
    private void createLiveFilters(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        c.setLayout(new GridLayout(3, false));
        c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mLiveFilterText = new Text(c, SWT.BORDER | SWT.SEARCH);
        mLiveFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mLiveFilterText.setMessage("<" + mPannelName + "> " + DEFAULT_SEARCH_MESSAGE);
        mLiveFilterText.setToolTipText(DEFAULT_SEARCH_TOOLTIP);
        mLiveFilterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                updateAppliedFilters();
            }
        });

        ToolBar toolBar = new ToolBar(c, SWT.FLAT);
        
        final LogLevel[] levels = LogLevel.values();
        mLogLevelActions = new ToolItemAction[mLogLevelIcons.length];
        for (int i = 0 ; i < mLogLevelActions.length; i++) {
            String name = levels[i].getStringValue();
            final ToolItemAction newAction = new ToolItemAction(toolBar, SWT.CHECK);
            mLogLevelActions[i] = newAction;
            newAction.item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // disable the other actions and record current index
                    for (int k = 0 ; k < mLogLevelActions.length; k++) {
                        ToolItemAction a = mLogLevelActions[k];
                        if (a == newAction) {
                            a.setChecked(true);
                            // set the log level
                            mCurrentFilterLogLevel = levels[k].getStringValue();
                            updateAppliedFilters();
                        } else {
                            a.setChecked(false);
                        }
                    }
                }
            });
            mLogLevelActions[0].setChecked(true);

            newAction.item.setToolTipText(name);
            newAction.item.setImage(ImageLoader.getDdmUiLibLoader().loadImage(mLogLevelIcons[i],
                    toolBar.getDisplay()));
        }

        ToolItem saveToLog = new ToolItem(toolBar, SWT.PUSH);
        saveToLog.setImage(ImageLoader.getDdmUiLibLoader().loadImage(IMAGE_SAVE_LOG_TO_FILE,
                toolBar.getDisplay()));
        saveToLog.setToolTipText("Export Selected Items To Text File..");
        saveToLog.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                saveLogToFile();
            }
        });
        
        final ToolItem showFiltersColumn = new ToolItem(toolBar, SWT.CHECK);
        showFiltersColumn.setImage(
                ImageLoader.getDdmUiLibLoader().loadImage(IMAGE_DISPLAY_FILTERS,
                        toolBar.getDisplay()));
        showFiltersColumn.setSelection(mPrefStore.getBoolean(DISPLAY_FILTERS_COLUMN_PREFKEY + mPanelID));
        showFiltersColumn.setToolTipText("Display Saved Filters View");
        showFiltersColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean showFilters = showFiltersColumn.getSelection();
                mPrefStore.setValue(DISPLAY_FILTERS_COLUMN_PREFKEY + mPanelID, showFilters);
                updateFiltersColumn(showFilters);
            }
        });

    }
    
    /**
     * Save logcat messages selected in the table to a file.
     */
    private void saveLogToFile() {
        /* show dialog box and get target file name */
        final String fName = getLogFileTargetLocation();
        if (fName == null) {
            return;
        }

        /* obtain list of selected messages */
        final List<LogCatMessage> selectedMessages = getSelectedLogCatMessages();
        if (selectedMessages == null){
        	return;
        }

        /* save messages to file in a different (non UI) thread */
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter w = new BufferedWriter(new FileWriter(fName));
                    for (LogCatMessage m : selectedMessages) {
                        w.append(m.toString());
                        w.newLine();
                    }
                    w.close();
                } catch (final IOException e) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                                    "Unable to export selection to file.",
                                    "Unexpected error while saving selected messages to file: "
                                            + e.getMessage());
                        }
                    });
                }
            }
        });
        t.setName("Saving selected items to logfile..");
        t.start();
    }
    
    /**
     * Display a {@link FileDialog} to the user and obtain the location for the log file.
     * @return path to target file, null if user canceled the dialog
     */
    private String getLogFileTargetLocation() {
        FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);

        fd.setText("Save Log..");
        fd.setFileName("log.txt");

        if (mLogFileExportFolder == null) {
            mLogFileExportFolder = System.getProperty("user.home");
        }
        fd.setFilterPath(mLogFileExportFolder);

        fd.setFilterNames(new String[] {
                "Text Files (*.txt)"
        });
        fd.setFilterExtensions(new String[] {
                "*.txt"
        });

        String fName = fd.open();
        if (fName != null) {
            mLogFileExportFolder = fd.getFilterPath();  /* save path to restore on future calls */
        }

        return fName;
    }

    private void updateFiltersColumn(boolean showFilters) {
        if (showFilters) {
            mSash.setWeights(WEIGHTS_SHOW_FILTERS);
        } else {
            mSash.setWeights(WEIGHTS_LOGCAT_ONLY);
        }
    }
    
    private List<LogCatMessage> getSelectedLogCatMessages() {
    	Object input = mViewer.getInput();
    	if (input == null){
    		return null;
    	}
        Table table = mViewer.getTable();
        int[] indices = table.getSelectionIndices();
        Arrays.sort(indices);  //Table.getSelectionIndices() does not specify an order 

        // Get items from the table's input as opposed to getting each table item's data.
        // Retrieving table item's data can return NULL in case of a virtual table if the item
        // has not been displayed yet.
        List<LogCatMessage> filteredItems = applyCurrentFilters((List<?>) input);
        List<LogCatMessage> selectedMessages = new ArrayList<LogCatMessage>(indices.length);
        for (int i : indices) {
            if (i < filteredItems.size()) {
                LogCatMessage m = filteredItems.get(i);
                selectedMessages.add(m);
            }
        }

        return selectedMessages;
    }

    private List<LogCatMessage> applyCurrentFilters(List<?> msgList) {
        Object[] items = msgList.toArray();
        List<LogCatMessage> filteredItems = new ArrayList<LogCatMessage>(items.length);
        List<LogCatViewerFilter> filters = getFiltersToApply();

        for (Object item : items) {
            if (!(item instanceof LogCatMessage)) {
                continue;
            }

            LogCatMessage msg = (LogCatMessage) item;
            if (!isMessageFiltered(msg, filters)) {
                filteredItems.add(msg);
            }
        }

        return filteredItems;
    }

    private boolean isMessageFiltered(LogCatMessage msg, List<LogCatViewerFilter> filters) {
        for (LogCatViewerFilter f : filters) {
            if (!f.select(null, null, msg)) {
                // message does not make it through this filter
                return true;
            }
        }

        return false;
    }

    private void createLogcatViewTable(Composite parent) {
        // The SWT.VIRTUAL bit causes the table to be rendered faster. However it makes all rows
        // to be of the same height, thereby clipping any rows with multiple lines of text.
        // In such a case, users can view the full text by hovering over the item and looking at
        // the tooltip.
        final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);
        mViewer = new TableViewer(table);

        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.getHorizontalBar().setVisible(true);

        /** Columns to show in the table. */
        String[] properties = {
                "Level",
                "Time",
                "PID",
//                "Application",
                "TID",//add tid
                "Tag",
                "Text",
        };

        /** The sampleText for each column is used to determine the default widths
         * for each column. The contents do not matter, only their lengths are needed. */
        String[] sampleText = {
                "    ",
                "    00-00 00:00:00.0000 ",
                "  0000",
//                "    com.android.launcher",
                "  0000",//add tid
                "    SampleTagText++++",
                "    Log Message field should be pretty long by default. As long as possible for correct display on Mac.",
        };

        mLogCatMessageLabelProvider = new LogCatMessageLabelProvider(getFontFromPrefStore());
        for (int i = 0; i < properties.length; i++) {
            TableColumn tc = TableHelper.createTableColumn(mViewer.getTable(),
                    properties[i],                      /* Column title */
                    SWT.LEFT,                           /* Column Style */
                    sampleText[i],                      /* String to compute default col width */
                    getColPreferenceKey(properties[i]), /* Preference Store key for this column */
                    mPrefStore);
            TableViewerColumn tvc = new TableViewerColumn(mViewer, tc);
            tvc.setLabelProvider(mLogCatMessageLabelProvider);
        }

        mViewer.getTable().setLinesVisible(true); /* zebra stripe the table */
        mViewer.getTable().setHeaderVisible(true);
        mViewer.setContentProvider(new LogCatMessageContentProvider());
        WrappingToolTipSupport.enableFor(mViewer, ToolTip.NO_RECREATE);

        // Set the row height to be sufficient enough to display the current font.
        // This is not strictly necessary, except that on WinXP, the rows showed up clipped. So
        // we explicitly set it to be sure.
        mViewer.getTable().addListener(SWT.MeasureItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.height = event.gc.getFontMetrics().getHeight();
            }
        });

        // Update the label provider whenever the text column's width changes
        TableColumn textColumn = mViewer.getTable().getColumn(properties.length - 1);
        textColumn.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent event) {
                TableColumn tc = (TableColumn) event.getSource();
                int width = tc.getWidth();
                GC gc = new GC(tc.getParent());
                int avgCharWidth = gc.getFontMetrics().getAverageCharWidth();
                gc.dispose();

                if (mLogCatMessageLabelProvider != null) {
                    mLogCatMessageLabelProvider.setMinimumLengthForToolTips(width/avgCharWidth);
                }
            }
        });
		mViewer.getTable().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mIsSynFromHere = true;
				String time = getSelectedLogCatMessages().get(0).getTime();
				if (time != null && time.length() > 10) {//04-08 13:13:43.851
					LogCatSynSelectedListener.getInstance().synSelected(time);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
        createViewMenu();

        setupAutoScrollLockBehavior();
//        initDoubleClickListener();
    }
    
    private void createViewMenu(){
    	MenuManager mmg = new MenuManager();
    	Menu menu = mmg.createContextMenu(mViewer.getTable());

    	mmg.add(new Action(RESET_ALL_FILTER){
			@Override
			public void run() {
				mResetTag.run();
				mResetPID.run();
				updateAppliedFilters();
			}
    	});
    	mmg.add(new Separator());
    	
    	mShowSelectedTag = new Action(ACTION_SHOW_TAG){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				mSelectedTagList = new ArrayList<String>();
				mSelectedTagList.add(LogCatFilter.SHOW_KEYWORD);
				setText(getText() + " : ");
				for (LogCatMessage item : selectedItems){
					String tag = item.getTag();
					setText(getText() + tag + ", ");
					mSelectedTagList.add(tag);
				}
				updateAppliedFilters();
				mShowSelectedTag.setEnabled(false);
				mHideSelectedTag.setEnabled(false);
			}
    	};
    	mHideSelectedTag = new Action(ACTION_HIDE_TAG){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				mSelectedTagList = new ArrayList<String>();
				mSelectedTagList.add(LogCatFilter.HIDE_KEYWORD);
				setText(getText() + " : ");
				for (LogCatMessage item : selectedItems){
					String tag = item.getTag();
					setText(getText() + tag + ", ");
					mSelectedTagList.add(tag);
				}
				updateAppliedFilters();
				mShowSelectedTag.setEnabled(false);
				mHideSelectedTag.setEnabled(false);
			}
    	};
    	mHighlightSelectedTag = new Action(ACTION_HIGHLIGHT_TAG){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				Object input = mViewer.getInput();
		        List<LogCatMessage> filteredItems = applyCurrentFilters((List<?>) input);
		        setText(getText() + " : ");
		        List<String> selectedTags = new ArrayList<String>();
		        if (mYellow == null){
		        	mYellow = mViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_YELLOW);
		        }
		        for (LogCatMessage item : selectedItems){
					String selectTag = item.getTag();
					setText(getText() + selectTag + ", ");
					selectedTags.add(selectTag);
				}
		       TableItem[] items = mViewer.getTable().getItems();
		        for (int i = 0; i < filteredItems.size(); i++){
		        	String localTag = filteredItems.get(i).getTag();
		        	if (selectedTags.contains(localTag)){
//		        		mViewer.getTable().getItem(i).setBackground(mYellow);
		        		items[i].setBackground(mYellow);
		        	}
		        }
		        mViewer.getTable().redraw();
		        mHighlightSelectedTag.setEnabled(false);
			}
    	};
    	mResetTag = new Action(RESET_TAG_FILTER){
			@Override
			public void run() {
				mSelectedTagList = null;
				mShowSelectedTag.setText(ACTION_SHOW_TAG);
				mHideSelectedTag.setText(ACTION_HIDE_TAG);
				mHighlightSelectedTag.setText(ACTION_HIGHLIGHT_TAG);
				mShowSelectedTag.setEnabled(true);
				mHideSelectedTag.setEnabled(true);
				mHighlightSelectedTag.setEnabled(true);
				if (mYellow == null){
					mYellow = mViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_YELLOW);
		        }
				cleanBackground(mYellow.hashCode());
				updateAppliedFilters();
			}
    	};
    	    	
    	mmg.add(mResetTag);
    	mmg.add(mShowSelectedTag);
    	mmg.add(mHideSelectedTag);
    	mmg.add(mHighlightSelectedTag);
    	
    	mmg.add(new Separator());
    	mShowSelectedPID = new Action(ACTION_SHOW_PID){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				mShowSelectedPID.setEnabled(false);
				mHideSelectedPID.setEnabled(false);
				mSelectedPIDList = new ArrayList<String>();
				mSelectedPIDList.add(LogCatFilter.SHOW_KEYWORD);
				setText(getText() + " : ");
				for (LogCatMessage item : selectedItems){
					String PID = item.getPid();
					setText(getText() + PID + ", ");
					mSelectedPIDList.add(PID);
				}
				updateAppliedFilters();
			}
    	};
    	mHideSelectedPID = new Action(ACTION_HIDE_PID){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				mShowSelectedPID.setEnabled(false);
				mHideSelectedPID.setEnabled(false);
				mSelectedPIDList = new ArrayList<String>();
				mSelectedPIDList.add(LogCatFilter.HIDE_KEYWORD);
				setText(getText() + " : ");
				for (LogCatMessage item : selectedItems){
					String PID = item.getPid();
					setText(getText() + PID + ", ");
					mSelectedPIDList.add(PID);
				}
				updateAppliedFilters();
			}
    	};
    	mHighlightSelectedPID = new Action(ACTION_HIGHLIGHT_PID){
			@Override
			public void run() {
				List<LogCatMessage> selectedItems = getSelectedLogCatMessages();
				if (selectedItems == null || selectedItems.size() == 0){
					return;
				}
				Object input = mViewer.getInput();
		        List<LogCatMessage> filteredItems = applyCurrentFilters((List<?>) input);
		        setText(getText() + " : ");
		        List<String> selectedPIDs = new ArrayList<String>();
		        if (mGreen == null){
		        	mGreen = mViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_GREEN);
		        }
		        for (LogCatMessage item : selectedItems){
					String selectPID = item.getPid();
					setText(getText() + selectPID + ", ");
					selectedPIDs.add(selectPID);
				}
		        for (int i = 0; i < filteredItems.size(); i++){
		        	String localPID = filteredItems.get(i).getPid();
		        	if (selectedPIDs.contains(localPID)){
		        		mViewer.getTable().getItem(i).setBackground(mGreen);
		        	}
		        }
		        mViewer.getTable().redraw();
		        mHighlightSelectedPID.setEnabled(false);
			}
    	};
    	mResetPID = new Action(RESET_PID_FILTER){
			@Override
			public void run() {
				mSelectedPIDList = null;
				mShowSelectedPID.setText(ACTION_SHOW_PID);
				mHideSelectedPID.setText(ACTION_HIDE_PID);
				mHighlightSelectedPID.setText(ACTION_HIGHLIGHT_PID);
				mShowSelectedPID.setEnabled(true);
				mHideSelectedPID.setEnabled(true);
				mHighlightSelectedPID.setEnabled(true);
				if (mGreen == null){
					mGreen = mViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_GREEN);
		        }
				cleanBackground(mGreen.hashCode());
				updateAppliedFilters();
			}
    	};
    	mmg.add(mResetPID);
    	mmg.add(mShowSelectedPID);
    	mmg.add(mHideSelectedPID);
    	mmg.add(mHighlightSelectedPID);

    	mViewer.getTable().setMenu(menu);
    }
    
    private void cleanBackground(int color){
    	for (TableItem item : mViewer.getTable().getItems()){
    		if (item.getBackground().hashCode() == color){
    			item.setBackground(mViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_WHITE));
    		}
    	}
    	mViewer.getTable().redraw();
    }

    /**
     * Setup to automatically enable or disable scroll lock. From a user's perspective,
     * the logcat window will: <ul>
     * <li> Automatically scroll and reveal new entries if the scrollbar is at the bottom. </li>
     * <li> Not scroll even when new messages are received if the scrollbar is not at the bottom.
     * </li>
     * </ul>
     * This requires that we are able to detect where the scrollbar is and what direction
     * it is moving. Unfortunately, that proves to be very platform dependent. Here's the behavior
     * of the scroll events on different platforms: <ul>
     * <li> On Windows, scroll bar events specify which direction the scrollbar is moving, but
     * it is not possible to determine if the scrollbar is right at the end. </li>
     * <li> On Mac/Cocoa, scroll bar events do not specify the direction of movement (it is always
     * set to SWT.DRAG), and it is not possible to identify where the scrollbar is since
     * small movements of the scrollbar are not reflected in sb.getSelection(). </li>
     * <li> On Linux/gtk, we don't get the direction, but we can accurately locate the
     * scrollbar location using getSelection(), getThumb() and getMaximum().
     * </ul>
     */
   private void setupAutoScrollLockBehavior() {
        if (DdmConstants.CURRENT_PLATFORM == DdmConstants.PLATFORM_WINDOWS) {
            // On Windows, it is not possible to detect whether the scrollbar is at the
            // bottom using the values of ScrollBar.getThumb, getSelection and getMaximum.
            // Instead we resort to the following workaround: attach to the paint listener
            // and see if the last item has been painted since the previous scroll event.
            // If the last item has been painted, then we assume that we are at the bottom.
            mViewer.getTable().addListener(SWT.PaintItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    TableItem item = (TableItem) event.item;
                    TableItem[] items = mViewer.getTable().getItems();
                    if (items.length > 0 && items[items.length - 1] == item) {
                        mLastItemPainted = true;
                    }
                }
            });
            mViewer.getTable().getVerticalBar().addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    boolean scrollToLast;
                    if (event.detail == SWT.ARROW_UP || event.detail == SWT.PAGE_UP
                            || event.detail == SWT.HOME) {
                        // if we know that we are moving up, then do not scroll down
                        scrollToLast = false;
                    } else {
                        // otherwise, enable scrollToLast only if the last item was displayed
                        scrollToLast = mLastItemPainted;
                    }

                    setScrollToLatestLog(scrollToLast, true);
                    mLastItemPainted = false;
                }
            });
        } else if (DdmConstants.CURRENT_PLATFORM == DdmConstants.PLATFORM_LINUX) {
            // On Linux/gtk, we do not get any details regarding the scroll event (up/down/etc).
            // So we completely rely on whether the scrollbar is at the bottom or not.
            mViewer.getTable().getVerticalBar().addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    ScrollBar sb = (ScrollBar) event.getSource();
                    boolean scrollToLast = sb.getSelection() + sb.getThumb() == sb.getMaximum();
                    setScrollToLatestLog(scrollToLast, true);
                }
            });
        } else {
            // On Mac, we do not get any details regarding the (trackball) scroll event,
            // nor can we rely on getSelection() changing for small movements. As a result, we
            // do not setup any auto scroll lock behavior. Mac users have to manually pause and
            // unpause if they are looking at a particular item in a high volume stream of events.
        }
    }

    private void setScrollToLatestLog(boolean scroll, boolean updateCheckbox) {
        mShouldScrollToLatestLog = scroll;

        if (scroll) {
            mViewer.refresh();
            scrollToLatestLog();
        }
    }

    private static class WrappingToolTipSupport extends ColumnViewerToolTipSupport {
        protected WrappingToolTipSupport(ColumnViewer viewer, int style,
                boolean manualActivation) {
            super(viewer, style, manualActivation);
        }

        @Override
        protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell,
                Composite parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            GridLayout l = new GridLayout(1, false);
            l.horizontalSpacing = 0;
            l.marginWidth = 0;
            l.marginHeight = 0;
            l.verticalSpacing = 0;
            comp.setLayout(l);

            Text text = new Text(comp, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
            text.setEditable(false);
            text.setText(cell.getElement().toString());
            text.setLayoutData(new GridData(500, 150));

            return comp;
        }

        @Override
        public boolean isHideOnMouseDown() {
            return false;
        }

        public static final void enableFor(ColumnViewer viewer, int style) {
            new WrappingToolTipSupport(viewer, style, false);
        }
    }

    private String getColPreferenceKey(String field) {
        return LOGCAT_VIEW_COLSIZE_PREFKEY_PREFIX + mPanelID + field;
    }

    private Font getFontFromPrefStore() {
        FontData fd = PreferenceConverter.getFontData(mPrefStore,
                LogCatPanel.LOGCAT_VIEW_FONT_PREFKEY + mPanelID);
        return new Font(Display.getDefault(), fd);
    }

    private void setupDefaults() {
        int defaultFilterIndex = 0;
        mFiltersTableViewer.getTable().setSelection(defaultFilterIndex);

        filterSelectionChanged();
    }

    /**
     * Perform all necessary updates whenever a filter is selected (by user or programmatically).
     */
    private void filterSelectionChanged() {
        int idx = getSelectedSavedFilterIndex();
        if (idx == -1) {
            /* One of the filters should always be selected.
             * On Linux, there is no way to deselect an item.
             * On Mac, clicking inside the list view, but not an any item will result
             * in all items being deselected. In such a case, we simply reselect the
             * first entry. */
            idx = 0;
            mFiltersTableViewer.getTable().setSelection(idx);
        }

        mCurrentSelectedFilterIndex = idx;

        resetUnreadCountForSelectedFilter();
        updateFiltersToolBar();
        updateAppliedFilters();
    }

    private void resetUnreadCountForSelectedFilter() {
        int index = getSelectedSavedFilterIndex();
        mLogCatFilters.get(index).resetUnreadCount();

        refreshFiltersTable();
    }

    private int getSelectedSavedFilterIndex() {
        return mFiltersTableViewer.getTable().getSelectionIndex();
    }

    private void updateFiltersToolBar() {
        /* The default filter at index 0 can neither be edited, nor removed. */
        boolean en = getSelectedSavedFilterIndex() != 0;
        mEditFilterToolItem.setEnabled(en);
        mDeleteFilterToolItem.setEnabled(en);
    }

    private void updateAppliedFilters() {
        List<LogCatViewerFilter> filters = getFiltersToApply();
        mViewer.setFilters(filters.toArray(new LogCatViewerFilter[filters.size()]));

        /* whenever filters are changed, the number of displayed logs changes
         * drastically. Display the latest log in such a situation. */
        if (mViewer.getInput() == null){
        	return;
        }
        if (getSelectedLogCatMessages() == null || getSelectedLogCatMessages().size() == 0 )
        	scrollToLatestLog();
    }

    private List<LogCatViewerFilter> getFiltersToApply() {
        /* list of filters to apply = saved filter + live filters */
        List<LogCatViewerFilter> filters = new ArrayList<LogCatViewerFilter>();
        filters.add(getSelectedSavedFilter());
        filters.addAll(getCurrentLiveFilters());
        return filters;
    }

    private List<LogCatViewerFilter> getCurrentLiveFilters() {
        List<LogCatViewerFilter> liveFilters = new ArrayList<LogCatViewerFilter>();

        List<LogCatFilter> liveFilterSettings = LogCatFilter.fromString(
                mLiveFilterText.getText(),                                  /* current query */
                LogLevel.getByString(mCurrentFilterLogLevel), mSelectedPIDList, mSelectedTagList);     /* current log level */
        for (LogCatFilter s : liveFilterSettings) {
            liveFilters.add(new LogCatViewerFilter(s));
        }

        return liveFilters;
    }

    private LogCatViewerFilter getSelectedSavedFilter() {
        int index = getSelectedSavedFilterIndex();
        return new LogCatViewerFilter(mLogCatFilters.get(index));
    }


    /**
     * Update view whenever a message is received.
     * @param receivedMessages list of messages from logcat
     * Implements {@link ILogCatMessageEventListener#messageReceived()}.
     */
    public void messageReceived(List<LogCatMessage> receivedMessages, int panelID, File file) {
    	if (panelID != mPanelID){
    		return;
    	}
		// change file name
		mPannelName = file.getName();
		mLiveFilterText.setMessage("<" + mPannelName + "> " + DEFAULT_SEARCH_MESSAGE);
		mLiveFilterText.setToolTipText("File path: " + file.getAbsolutePath() + "\n"
				+ DEFAULT_SEARCH_TOOLTIP);

    	setPIDAndTagList(receivedMessages);
    	mViewer.setInput(receivedMessages);
        refreshLogCatTable();
        updateUnreadCount(receivedMessages);
        refreshFiltersTable();
    }
    
    public void synSelected(String synTime){
    	if (!mIsSynFromHere){
    		Object input = mViewer.getInput();
    		if (input == null){
    			return;
    		}
    		int low = 0;
    		int high = mViewer.getTable().getItemCount() - 1;
    		int mid = (low + high) / 2;
	        List<LogCatMessage> filteredItems = applyCurrentFilters((List<?>) input);
    		while (low <= high){
    			mid = (low + high) / 2;
    			/*if (mid == 0){
    				mid = 1;
    			}*/
    			String localTime = filteredItems.get(mid).getTime();
    			if (synTime.compareTo(localTime) < 0){
    				high = mid - 1;
    			} else if (synTime.compareTo(localTime) > 0){
    				low = mid + 1;
    			} else {
    				break;
    			}
    		}
    		mViewer.getTable().setSelection(mid);
    		mViewer.getTable().setTopIndex(mid - 6);
    	}
    	mIsSynFromHere = false;
    }
    
    private void setPIDAndTagList(List<LogCatMessage> receivedMessages){
    	mPIDList = new ArrayList<String>();
    	mTagList = new ArrayList<String>();
    	for (LogCatMessage msg : receivedMessages){
    		mPIDList.add(msg.getPid());
    		mTagList.add(msg.getTag());
    	}
    }
    
    /**
     * When new messages are received, and they match a saved filter, update
     * the unread count associated with that filter.
     * @param receivedMessages list of new messages received
     */
    private void updateUnreadCount(List<LogCatMessage> receivedMessages) {
        for (int i = 0; i < mLogCatFilters.size(); i++) {
            if (i == mCurrentSelectedFilterIndex) {
                /* no need to update unread count for currently selected filter */
                continue;
            }
            mLogCatFilters.get(i).updateUnreadCount(receivedMessages);
        }
    }

    private void refreshFiltersTable() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (mFiltersTableViewer.getTable().isDisposed()) {
                    return;
                }
                mFiltersTableViewer.refresh();
            }
        });
    }

    /** Task currently submitted to {@link Display#asyncExec} to be run in UI thread. */
    private LogCatTableRefresherTask mCurrentRefresher;

    /**
     * Refresh the logcat table asynchronously from the UI thread.
     * This method adds a new async refresh only if there are no pending refreshes for the table.
     * Doing so eliminates redundant refresh threads from being queued up to be run on the
     * display thread.
     */
    private void refreshLogCatTable() {
        synchronized (this) {
            if (mCurrentRefresher == null && mShouldScrollToLatestLog) {
                mCurrentRefresher = new LogCatTableRefresherTask();
                Display.getDefault().asyncExec(mCurrentRefresher);
            }
        }
    }

    private class LogCatTableRefresherTask implements Runnable {
        @Override
        public void run() {
            if (mViewer.getTable().isDisposed()) {
                return;
            }
            synchronized (LogCatPanel.this) {
                mCurrentRefresher = null;
            }

            if (mShouldScrollToLatestLog) {
                mViewer.refresh();
                scrollToLatestLog();
            }
        }
    }

    /** Scroll to the last line. */
    private void scrollToLatestLog() {
        mViewer.getTable().setTopIndex(mViewer.getTable().getItemCount() - 1);
    }

    private List<ILogCatMessageSelectionListener> mMessageSelectionListeners;

   /* private void initDoubleClickListener() {
        mMessageSelectionListeners = new ArrayList<ILogCatMessageSelectionListener>(1);

        mViewer.getTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                List<LogCatMessage> selectedMessages = getSelectedLogCatMessages();
                if (selectedMessages.size() == 0) {
                    return;
                }

                for (ILogCatMessageSelectionListener l : mMessageSelectionListeners) {
                    l.messageDoubleClicked(selectedMessages.get(0));
                }
            }
        });
    }*/

    public void addLogCatMessageSelectionListener(ILogCatMessageSelectionListener l) {
        mMessageSelectionListeners.add(l);
    }
    
    private ITableFocusListener mTableFocusListener;

    /**
     * Specify the listener to be called when the logcat view gets focus. This interface is
     * required by DDMS to hook up the menu items for Copy and Select All.
     * @param listener listener to be notified when logcat view is in focus
     */
    public void setTableFocusListener(ITableFocusListener listener) {
        mTableFocusListener = listener;

        final Table table = mViewer.getTable();
        final IFocusedTableActivator activator = new IFocusedTableActivator() {
            @Override
            public void copy(Clipboard clipboard) {
                copySelectionToClipboard(clipboard);
            }

            @Override
            public void selectAll() {
                table.selectAll();
            }
        };

        table.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                mTableFocusListener.focusGained(activator);
            }

            @Override
            public void focusLost(FocusEvent e) {
                mTableFocusListener.focusLost(activator);
            }
        });
    }

    /** Copy all selected messages to clipboard. */
    public void copySelectionToClipboard(Clipboard clipboard) {
        StringBuilder sb = new StringBuilder();

        List<LogCatMessage> selectedList = getSelectedLogCatMessages();
        if (selectedList == null){
        	return;
        }
        for (LogCatMessage m : selectedList) {
            sb.append(m.toString());
            sb.append('\n');
        }

        clipboard.setContents(
                new Object[] {sb.toString()},
                new Transfer[] {TextTransfer.getInstance()}
                );
    }

    /** Select all items in the logcat table. */
    public void selectAll() {
        mViewer.getTable().selectAll();
    }
}
