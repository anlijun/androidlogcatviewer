package com.logcat.offline;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.ImageLoader;
import com.logcat.offline.view.ddmuilib.logcat.LogCatMessageParser;
import com.logcat.offline.view.ddmuilib.logcat.LogCatPanel;
import com.logcat.offline.view.ddmuilib.logcat.OfflinePreferenceStore;

public class UIThread {
	private static final String APP_NAME = "DDMS";
	private static UIThread uiThread;
	
	private static final String PREFERENCE_LOGSASH_H = "logSashLocation.h";
	private static final String PREFERENCE_LOGSASH_V = "logSashLocation.v";
	private static final String PREFERENCE_LAST_OPEN_FOLDER = "log.last.openfolder";
	
	public static final int PANEL_ID_MAIN = 0;
	public static final int PANEL_ID_EVENTS = 1;
	public static final int PANEL_ID_RADIO = 2;
	
	private Display mDisplay;
	private Label mStatusLine;
	
	private PreferenceStore mPreferenceStore;
	private LogCatPanel mLogCatPanel_main;
	private LogCatPanel mLogCatPanel_event;
	private LogCatPanel mLogCatPanel_radio;
	
	private Clipboard mClipboard;
    private MenuItem mCopyMenuItem;
    private MenuItem mSelectAllMenuItem;
    private TableFocusListener mTableListener;
	
	private UIThread(){
	}
	
	public static UIThread getInstance(){
		if (uiThread == null){
			uiThread = new UIThread();
		}
		return uiThread;
	}
	
	private class TableFocusListener implements ITableFocusListener {

        private IFocusedTableActivator mCurrentActivator;

        @Override
        public void focusGained(IFocusedTableActivator activator) {
            mCurrentActivator = activator;
            if (mCopyMenuItem.isDisposed() == false) {
                mCopyMenuItem.setEnabled(true);
                mSelectAllMenuItem.setEnabled(true);
            }
        }

        @Override
        public void focusLost(IFocusedTableActivator activator) {
            // if we move from one table to another, it's unclear
            // if the old table lose its focus before the new
            // one gets the focus, so we need to check.
            if (activator == mCurrentActivator) {
                activator = null;
                if (mCopyMenuItem.isDisposed() == false) {
                    mCopyMenuItem.setEnabled(false);
                    mSelectAllMenuItem.setEnabled(false);
                }
            }
        }

        public void copy(Clipboard clipboard) {
            if (mCurrentActivator != null) {
                mCurrentActivator.copy(clipboard);
            }
        }

        public void selectAll() {
            if (mCurrentActivator != null) {
                mCurrentActivator.selectAll();
            }
        }
    }
	
	public void runUI() {
        Display.setAppName(APP_NAME);
        mDisplay = Display.getDefault();
        Shell shell = new Shell(mDisplay, SWT.SHELL_TRIM);
        shell.setImage(ImageLoader.getDdmUiLibLoader().loadImage("ddms-128.png", mDisplay));
        shell.setText("LogcatOfflineView");
        mPreferenceStore = OfflinePreferenceStore.getPreferenceStore();
        createMenus(shell);
        createWidgets(shell);
        shell.pack();
        shell.setMaximized(true);
        shell.open();
        while (!shell.isDisposed()) {
            if (!mDisplay.readAndDispatch())
                mDisplay.sleep();
        }
        ImageLoader.dispose();
        mDisplay.dispose();
        OfflinePreferenceStore.save();
    }
	
	private void createMenus(final Shell shell){
		// create menu bar
		Menu menuBar = new Menu(shell, SWT.BAR);

        // create top-level items
        MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
        fileItem.setText("&File");
        MenuItem editItem = new MenuItem(menuBar, SWT.CASCADE);
        editItem.setText("&Edit");
        Menu fileMenu = new Menu(menuBar);
        fileItem.setMenu(fileMenu);
        Menu editMenu = new Menu(menuBar);
        editItem.setMenu(editMenu);

        MenuItem item;
        // create File menu items
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Open File\tCtrl-O");
        item.setAccelerator('O' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String filePath = new FileDialog(shell).open();
                LogCatMessageParser.getInstance().parseLogFile(filePath, PANEL_ID_MAIN);
            }
        });
        
        // create Open bugreport menu items
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Open bugreport(dumpstate) file\tCtrl-B");
        item.setAccelerator('B' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String filePath = new FileDialog(shell).open();
                LogCatMessageParser.getInstance().parseDumpstateFile(filePath);
            }
        });
        
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("Open Log &Folder\tCtrl-F");
        item.setAccelerator('F' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(shell);
				String lastFolder = mPreferenceStore
						.getString(PREFERENCE_LAST_OPEN_FOLDER);
				if (lastFolder != null) {
					directoryDialog.setFilterPath(lastFolder);
				}
				String folderPath = directoryDialog.open();
				if (folderPath != null) {
					LogCatMessageParser.getInstance()
							.parseLogFolder(folderPath);
					if (lastFolder != null
							&& folderPath.compareTo(lastFolder) != 0) {
						mPreferenceStore.setValue(PREFERENCE_LAST_OPEN_FOLDER,
								folderPath);
					}
				}
			}
        });
        
        new MenuItem(fileMenu, SWT.SEPARATOR);
        
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("E&xit\tCtrl-Q");
        item.setAccelerator('Q' | SWT.MOD1);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });
        
     // create edit menu items
        mCopyMenuItem = new MenuItem(editMenu, SWT.NONE);
        mCopyMenuItem.setText("&Copy\tCtrl-C");
        mCopyMenuItem.setAccelerator('C' | SWT.MOD1);
        mCopyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.copy(mClipboard);
            }
        });

        new MenuItem(editMenu, SWT.SEPARATOR);

        mSelectAllMenuItem = new MenuItem(editMenu, SWT.NONE);
        mSelectAllMenuItem.setText("Select &All\tCtrl-A");
        mSelectAllMenuItem.setAccelerator('A' | SWT.MOD1);
        mSelectAllMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.selectAll();
            }
        });
        // tell the shell to use this menu
        shell.setMenuBar(menuBar);
	}
	
	private void createWidgets(final Shell shell) {
        Color darkGray = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        shell.setLayout(new GridLayout(1, false));
        final Composite panelArea = new Composite(shell, SWT.BORDER);
        panelArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        mStatusLine = new Label(shell, SWT.NONE);
        mStatusLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLine.setText("Initializing...");

        Composite mainPanel = new Composite(panelArea, SWT.NONE);
        final Sash sash_h = new Sash(panelArea, SWT.HORIZONTAL);
        sash_h.setBackground(darkGray);
        Composite eventPanel = new Composite(panelArea, SWT.NONE);
        final Sash sash_v = new Sash(panelArea, SWT.VERTICAL);
        sash_v.setBackground(darkGray);
        Composite radioPanel = new Composite(panelArea, SWT.NONE);

        panelArea.setLayout(new FormLayout());
        createMainPanel(mainPanel);
        createEventPanel(eventPanel);
        createRadioPanel(radioPanel);
        
        mClipboard = new Clipboard(panelArea.getDisplay());

        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(sash_h, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mainPanel.setLayoutData(data);

        final FormData sashData_h = new FormData();
        if (mPreferenceStore != null && mPreferenceStore.contains(PREFERENCE_LOGSASH_H)) {
        	sashData_h.top = new FormAttachment(0, mPreferenceStore.getInt(
                    PREFERENCE_LOGSASH_H));
        } else {
        	sashData_h.top = new FormAttachment(50,0); // 50% across
        }
        sashData_h.left = new FormAttachment(0, 0);
        sashData_h.right = new FormAttachment(100, 0);
        sash_h.setLayoutData(sashData_h);

        data = new FormData();
        data.top = new FormAttachment(sash_h, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(sash_v, 0);
        eventPanel.setLayoutData(data);
        
        final FormData sashData_v = new FormData();
        sashData_v.top = new FormAttachment(sash_h, 0);
        sashData_v.bottom = new FormAttachment(100, 0);
        if (mPreferenceStore != null && mPreferenceStore.contains(PREFERENCE_LOGSASH_V)) {
        	sashData_v.left = new FormAttachment(0, mPreferenceStore.getInt(
                    PREFERENCE_LOGSASH_V));
        } else {
        	sashData_v.left = new FormAttachment(50,0); // 50% across
        }
        sash_v.setLayoutData(sashData_v);

        data = new FormData();
        data.top = new FormAttachment(sash_h, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(sash_v, 0);
        data.right = new FormAttachment(100, 0);
        radioPanel.setLayoutData(data);

        sash_h.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Rectangle sashRect = sash_h.getBounds();
                Rectangle panelRect = panelArea.getClientArea();
                int bottom = panelRect.height - sashRect.height - 100;
                e.y = Math.max(Math.min(e.y, bottom), 100);
                if (e.y != sashRect.y) {
                	sashData_h.top = new FormAttachment(0, e.y);
                    if (mPreferenceStore != null) {
                    	mPreferenceStore.setValue(PREFERENCE_LOGSASH_H, e.y);
                    }
                    panelArea.layout();
                }
            }
        });
        
        sash_v.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Rectangle sashRect = sash_v.getBounds();
                Rectangle panelRect = panelArea.getClientArea();
                int right = panelRect.width - sashRect.width - 100;
                e.x = Math.max(Math.min(e.x, right), 100);
                if (e.x != sashRect.x) {
                	sashData_v.left = new FormAttachment(0, e.x);
                    if (mPreferenceStore != null) {
                    	mPreferenceStore.setValue(PREFERENCE_LOGSASH_V, e.x);
                    }
                    panelArea.layout();
                }
            }
        });
        
     // add a global focus listener for all the tables
        mTableListener = new TableFocusListener();

        mLogCatPanel_main.setTableFocusListener(mTableListener);
        mLogCatPanel_event.setTableFocusListener(mTableListener);
        mLogCatPanel_radio.setTableFocusListener(mTableListener);

        mStatusLine.setText("");
    }
	
	private void createMainPanel(Composite parent) {
        mLogCatPanel_main = new LogCatPanel(mPreferenceStore, PANEL_ID_MAIN, "main buffer");
        mLogCatPanel_main.createControl(parent);
	}
	
	private void createEventPanel(Composite parent) {
        mLogCatPanel_event = new LogCatPanel(mPreferenceStore, PANEL_ID_EVENTS, "events buffer");
        mLogCatPanel_event.createControl(parent);
	}
	
	private void createRadioPanel(Composite parent) {
        mLogCatPanel_radio = new LogCatPanel(mPreferenceStore, PANEL_ID_RADIO, "radio buffer");
        mLogCatPanel_radio.createControl(parent);
	}
}