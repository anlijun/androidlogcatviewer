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
package com.android.ddmuilib.logcat;

import com.android.ddmlib.Log.LogLevel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Dialog used to create or edit settings for a logcat filter.
 */
public final class LogCatFilterSettingsDialog extends TitleAreaDialog {
    private static final String TITLE = "Logcat Message Filter Settings";
    private static final String DEFAULT_MESSAGE =
            "Filter logcat messages by the source's tag, pid or minimum log level.\n"
            + "Empty fields will match all messages.";
    private static final int ITEM_SHOW_NUM = 20;
    
    private String mFilterName;
    private String mTag;
    private String mText;
    private String mPid;
//    private String mAppName;
    private String mLogLevel;
    private List<String> mPIDList;
    private List<String> mPIDShowList;
    private List<String> mPIDHideList;
    private List<String> mTagList;
    private List<String> mTagShowList;
    private List<String> mTagHideList;

    private Text mFilterNameText;
    private Text mTagFilterText;
    private Text mTextFilterText;
    private Text mPidFilterText;
    private TableViewer mTvPIDShow;
    private TableViewer mTvPIDHide;
    private TableViewer mTvTagShow;
    private TableViewer mTvTagHide;
//    private Text mAppNameFilterText;
    private Combo mLogLevelCombo;
    private Button mOkButton;
    
    private PIDSort mPIDSort;
    private TagSort mTagSort;

    /**
     * Construct the filter settings dialog with default values for all fields.
     * @param parentShell .
     */
    public LogCatFilterSettingsDialog(Shell parentShell) {
        super(parentShell);
        setDefaults("", "", "", "", LogLevel.VERBOSE, new ArrayList<String>(), new ArrayList<String>(),
        		new ArrayList<String>(), new ArrayList<String>());
    }

    /**
     * Set the default values to show when the dialog is opened.
     * @param filterName name for the filter.
     * @param tag value for filter by tag
     * @param text value for filter by text
     * @param pid value for filter by pid
     * @param appName value for filter by app name
     * @param level value for filter by log level
     */
    public void setDefaults(String filterName, String tag, String text, String pid,
            LogLevel level, List<String> PIDList, List<String> PIDHideList, List<String> tagList, List<String> tagHideList) {
        mFilterName = filterName;
        mTag = tag;
        mText = text;
        mPid = pid;
//        mAppName = appName;
        mLogLevel = level.getStringValue();
        mPIDList = PIDList;
        mPIDHideList = PIDHideList;
        mTagList = tagList;
        mTagHideList = tagHideList;
        mPIDSort = new PIDSort();
        mTagSort = new TagSort();
    }

    @Override
    protected Control createDialogArea(Composite shell) {
        setTitle(TITLE);
        setMessage(DEFAULT_MESSAGE);

        Composite parent = (Composite) super.createDialogArea(shell);
        Composite c = new Composite(parent, SWT.BORDER);
        c.setLayout(new GridLayout(2, false));
        c.setLayoutData(new GridData(GridData.FILL_BOTH));

        createLabel(c, "Filter Name:");
        mFilterNameText = new Text(c, SWT.BORDER);
        mFilterNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFilterNameText.setText(mFilterName);

        createSeparator(c);

        createLabel(c, "by Log Message:");
        mTextFilterText = new Text(c, SWT.BORDER);
        mTextFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTextFilterText.setText(mText);
        
        createLabel(c, "by Log Level:");
        mLogLevelCombo = new Combo(c, SWT.READ_ONLY | SWT.DROP_DOWN);
        mLogLevelCombo.setItems(getLogLevels().toArray(new String[0]));
        mLogLevelCombo.select(getLogLevels().indexOf(mLogLevel));
        
        createPTGroup(c);

//        createLabel(c, "by Application Name:");
//        mAppNameFilterText = new Text(c, SWT.BORDER);
//        mAppNameFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        mAppNameFilterText.setText(mAppName);

        /* call validateDialog() whenever user modifies any text field */
        ModifyListener m = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                DialogStatus status = validateDialog();
                mOkButton.setEnabled(status.valid);
                setErrorMessage(status.message);
            }
        };
        mFilterNameText.addModifyListener(m);
        mTagFilterText.addModifyListener(m);
        mTextFilterText.addModifyListener(m);
        mPidFilterText.addModifyListener(m);
//        mAppNameFilterText.addModifyListener(m);

        return c;
    }
    
    private void createPTGroup(Composite parent){
//    	Group group = new Group(parent, SWT.NONE);
    	TabFolder tabF = new TabFolder(parent, SWT.NONE);
    	tabF.setLayout(new GridLayout(2, false));
    	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = 2;
    	tabF.setLayoutData(gd);
    	
    	TabItem tabI1 = new TabItem(tabF, SWT.NONE);
    	tabI1.setText("Advanced");
    	tabI1.setControl(createTabItem1(tabF));
    	
    	TabItem tabI2 = new TabItem(tabF, SWT.NONE);
    	tabI2.setText("Original");
    	tabI2.setControl(createTabItem2(tabF));
    	
    }
    
    private Composite createTabItem1(Composite parent){
    	Composite comp = new Composite(parent, SWT.NONE);
    	comp.setLayout(new GridLayout(2, false));
    	
    	createPIDGroup(comp);
    	createTagGroup(comp);
    	return comp;
    }
    
    private Composite createTabItem2(Composite parent){
    	Composite comp = new Composite(parent, SWT.NONE);
    	comp.setLayout(new GridLayout(2, false));
    	createLabel(comp, "by PID:");
        mPidFilterText = new Text(comp, SWT.BORDER);
        mPidFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mPidFilterText.setText(mPid);
        
        createLabel(comp, "by Log Tag:");
        mTagFilterText = new Text(comp, SWT.BORDER);
        mTagFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTagFilterText.setText(mTag);
    	return comp;
    }
    
    private void createPIDGroup(Composite parnet){
    	Group pidG = new Group(parnet, SWT.NONE);
    	pidG.setLayout(new GridLayout(5, true));
    	GridData gd = new GridData(GridData.FILL_BOTH);
    	pidG.setLayoutData(gd);
    	pidG.setText("By PID");
    	
    	createLabel(pidG, "Show");
    	createLabel(pidG, "");
    	createLabel(pidG, "");
    	createLabel(pidG, "Hide");
    	createLabel(pidG, "");
    	
    	SashForm sash = new SashForm(pidG, SWT.HORIZONTAL);
    	gd = new GridData(GridData.FILL_BOTH);
    	gd.horizontalSpan = 2;
    	gd.verticalSpan = ITEM_SHOW_NUM;
    	gd.heightHint = pidG.getBounds().height;
        sash.setLayoutData(gd);
    	
    	Table tableS = new Table(sash, SWT.FULL_SELECTION | SWT.MULTI);
    	gd = new GridData(GridData.FILL_BOTH);
    	tableS.setLayoutData(gd);
    	mTvPIDShow = new TableViewer(tableS);
    	mTvPIDShow.setContentProvider(new ListContentProvider());
    	mTvPIDShow.setLabelProvider(new ListLabelProvider());
    	mTvPIDShow.setInput(getPIDShowList());
    	
    	Button showToHide_All = new Button(pidG, SWT.PUSH);
    	showToHide_All.setText(">>");
    	showToHide_All.setToolTipText("Move All");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	showToHide_All.setLayoutData(gd);
    	showToHide_All.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				mPIDHideList.addAll(0, mPIDShowList);
				mPIDShowList.clear();
		    	Collections.sort(mPIDHideList, mPIDSort);
		    	mTvPIDHide.refresh();
		    	mTvPIDShow.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	sash = new SashForm(pidG, SWT.HORIZONTAL);
    	gd = new GridData(GridData.FILL_BOTH);
    	gd.horizontalSpan = 2;
    	gd.verticalSpan = ITEM_SHOW_NUM;
    	gd.heightHint = pidG.getBounds().height;
        sash.setLayoutData(gd);
    	
    	Table tableH = new Table(sash, SWT.FULL_SELECTION | SWT.MULTI);
    	gd = new GridData(GridData.FILL_BOTH);
    	tableH.setLayoutData(gd);
    	mTvPIDHide = new TableViewer(tableH);
    	mTvPIDHide.setContentProvider(new ListContentProvider());
    	mTvPIDHide.setLabelProvider(new ListLabelProvider());
    	Collections.sort(mPIDHideList, mPIDSort);
    	mTvPIDHide.setInput(mPIDHideList);
    	
    	Button showToHide = new Button(pidG, SWT.PUSH);
    	showToHide.setText(">");
    	showToHide.setToolTipText("Move Selected");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	showToHide.setLayoutData(gd);
    	showToHide.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] selectIndexes = mTvPIDShow.getTable().getSelectionIndices();
				List<String> selectedList = new ArrayList<String>();
				for (int index : selectIndexes){
					selectedList.add(mPIDShowList.get(index));
				}
				mPIDHideList.addAll(selectedList);
				mPIDShowList.removeAll(selectedList);
		    	Collections.sort(mPIDHideList, mPIDSort);
		    	mTvPIDHide.refresh();
		    	mTvPIDShow.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	Label labelSpace = new Label(pidG, SWT.NONE);
    	gd = new GridData();
    	gd.verticalSpan = ITEM_SHOW_NUM - 4;
    	labelSpace.setLayoutData(gd);
    	
    	Button hideToShow = new Button(pidG, SWT.PUSH);
    	hideToShow.setText("<");
    	hideToShow.setToolTipText("Move Selected");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	hideToShow.setLayoutData(gd);
    	hideToShow.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] selectIndexes = mTvPIDHide.getTable().getSelectionIndices();
				List<String> selectedList = new ArrayList<String>();
				for (int index : selectIndexes){
					selectedList.add(mPIDHideList.get(index));
				}
				mPIDShowList.addAll(selectedList);
				mPIDHideList.removeAll(selectedList);
		    	Collections.sort(mPIDShowList, mPIDSort);
		    	mTvPIDShow.refresh();
		    	mTvPIDHide.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	Button hideToShow_All = new Button(pidG, SWT.PUSH);
    	hideToShow_All.setText("<<");
    	hideToShow_All.setToolTipText("Move All");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	hideToShow_All.setLayoutData(gd);
    	hideToShow_All.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				mPIDShowList.addAll(0, mPIDHideList);
				mPIDHideList.clear();
		    	Collections.sort(mPIDShowList, mPIDSort);
		    	mTvPIDShow.refresh();
		    	mTvPIDHide.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    		
    	});
    }
    
    private void createTagGroup(Composite parnet){
    	Group tagG = new Group(parnet, SWT.NONE);
    	tagG.setLayout(new GridLayout(5, true));
    	GridData gd = new GridData(GridData.FILL_BOTH);
    	tagG.setLayoutData(gd);
    	tagG.setText("By Tag");
    	
    	createLabel(tagG, "Show");
    	createLabel(tagG, "");
    	createLabel(tagG, "");
    	createLabel(tagG, "Hide");
    	createLabel(tagG, "");
    	
    	SashForm sash = new SashForm(tagG, SWT.HORIZONTAL);
    	gd = new GridData(GridData.FILL_BOTH);
    	gd.horizontalSpan = 2;
    	gd.verticalSpan = ITEM_SHOW_NUM;
    	gd.heightHint = tagG.getBounds().height;
        sash.setLayoutData(gd);
    	
    	Table tableS = new Table(sash, SWT.FULL_SELECTION | SWT.MULTI);
    	gd = new GridData(GridData.FILL_BOTH);
    	tableS.setLayoutData(gd);
    	mTvTagShow = new TableViewer(tableS);
    	mTvTagShow.setContentProvider(new ListContentProvider());
    	mTvTagShow.setLabelProvider(new ListLabelProvider());
    	mTvTagShow.setInput(getTagShowList());
    	
    	Button showToHide_All = new Button(tagG, SWT.PUSH);
    	showToHide_All.setText(">>");
    	showToHide_All.setToolTipText("Move All");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	showToHide_All.setLayoutData(gd);
    	showToHide_All.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				mTagHideList.addAll(0, mTagShowList);
				mTagShowList.clear();
		    	Collections.sort(mTagHideList, mTagSort);
		    	mTvTagHide.refresh();
		    	mTvTagShow.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	sash = new SashForm(tagG, SWT.HORIZONTAL);
    	gd = new GridData(GridData.FILL_BOTH);
    	gd.horizontalSpan = 2;
    	gd.verticalSpan = ITEM_SHOW_NUM;
    	gd.heightHint = tagG.getBounds().height;
        sash.setLayoutData(gd);
    	
    	Table tableH = new Table(sash, SWT.FULL_SELECTION | SWT.MULTI);
    	gd = new GridData(GridData.FILL_BOTH);
    	tableH.setLayoutData(gd);
    	mTvTagHide = new TableViewer(tableH);
    	mTvTagHide.setContentProvider(new ListContentProvider());
    	mTvTagHide.setLabelProvider(new ListLabelProvider());
    	Collections.sort(mTagHideList, mTagSort);
    	mTvTagHide.setInput(mTagHideList);
    	
    	Button showToHide = new Button(tagG, SWT.PUSH);
    	showToHide.setText(">");
    	showToHide.setToolTipText("Move Selected");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	showToHide.setLayoutData(gd);
    	showToHide.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] selectIndexes = mTvTagShow.getTable().getSelectionIndices();
				List<String> selectedList = new ArrayList<String>();
				for (int index : selectIndexes){
					selectedList.add(mTagShowList.get(index));
				}
				mTagHideList.addAll(selectedList);
				mTagShowList.removeAll(selectedList);
		    	Collections.sort(mTagHideList, mTagSort);
		    	mTvTagHide.refresh();
		    	mTvTagShow.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	Label labelSpace = new Label(tagG, SWT.NONE);
    	gd = new GridData();
    	gd.verticalSpan = ITEM_SHOW_NUM - 4;
    	labelSpace.setLayoutData(gd);
    	
    	Button hideToShow = new Button(tagG, SWT.PUSH);
    	hideToShow.setText("<");
    	hideToShow.setToolTipText("Move Selected");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	hideToShow.setLayoutData(gd);
    	hideToShow.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] selectIndexes = mTvTagHide.getTable().getSelectionIndices();
				List<String> selectedList = new ArrayList<String>();
				for (int index : selectIndexes){
					selectedList.add(mTagHideList.get(index));
				}
				mTagShowList.addAll(selectedList);
				mTagHideList.removeAll(selectedList);
		    	Collections.sort(mTagShowList, mTagSort);
		    	mTvTagShow.refresh();
		    	mTvTagHide.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    	});
    	
    	Button hideToShow_All = new Button(tagG, SWT.PUSH);
    	hideToShow_All.setText("<<");
    	hideToShow_All.setToolTipText("Move All");
    	gd = new GridData();
    	gd.widthHint = 40;
    	gd.horizontalAlignment = SWT.CENTER;
    	hideToShow_All.setLayoutData(gd);
    	hideToShow_All.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				mTagShowList.addAll(0, mTagHideList);
				mTagHideList.clear();
		    	Collections.sort(mTagShowList, mTagSort);
		    	mTvTagShow.refresh();
		    	mTvTagHide.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
    		
    	});
    }

    private List<String> getPIDShowList(){
    	mPIDShowList = new ArrayList<String>();
    	for (String PID : mPIDList){
    		if (PID.equals("?")){
    			continue;
    		}
    		boolean isHide = false;
    		for (String PIDHide : mPIDHideList){
    			if (PID.equals(PIDHide)){
    				isHide = true;
    				break;
    			}
    		}
    		if (!isHide && !mPIDShowList.contains(PID)){
    			mPIDShowList.add(PID);
    		}
    	}
    	Collections.sort(mPIDShowList, mPIDSort);
    	return mPIDShowList;
    }
    
    private List<String> getTagShowList(){
    	mTagShowList = new ArrayList<String>();
    	for (String tag : mTagList){
    		if (tag.equals("?")){
    			continue;
    		}
    		boolean isHide = false;
    		for (String tagHide : mTagHideList){
    			if (tag.equals(tagHide)){
    				isHide = true;
    				break;
    			}
    		}
    		if (!isHide && !mTagShowList.contains(tag)){
    			mTagShowList.add(tag);
    		}
    	}
    	Collections.sort(mTagShowList, mTagSort);
    	return mTagShowList;
    }
    
    class PIDSort implements Comparator<String>{
		@Override
		public int compare(String o1, String o2) {
			return Integer.parseInt(o1) - Integer.parseInt(o2);
		}
		public PIDSort(){
		}
    }
    
    class TagSort implements Comparator<String>{
		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
		public TagSort(){
		}
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        mOkButton = getButton(IDialogConstants.OK_ID);

        DialogStatus status = validateDialog();
        mOkButton.setEnabled(status.valid);
    }

    /**
     * A tuple that specifies whether the current state of the inputs
     * on the dialog is valid or not. If it is not valid, the message
     * field stores the reason why it isn't.
     */
    private final class DialogStatus {
        final boolean valid;
        final String message;

        private DialogStatus(boolean isValid, String errMessage) {
            valid = isValid;
            message = errMessage;
        }
    }

    private DialogStatus validateDialog() {
        /* check that there is some name for the filter */
        if (mFilterNameText.getText().trim().equals("")) {
            return new DialogStatus(false,
                    "Please provide a name for this filter.");
        }

        /* if a pid is provided, it should be a +ve integer */
        String pidText = mPidFilterText.getText().trim();
        if (pidText.trim().length() > 0) {
            int pid = 0;
            try {
                pid = Integer.parseInt(pidText);
            } catch (NumberFormatException e) {
                return new DialogStatus(false,
                        "PID should be a positive integer.");
            }

            if (pid < 0) {
                return new DialogStatus(false,
                        "PID should be a positive integer.");
            }
        }

        /* tag field must use a valid regex pattern */
        String tagText = mTagFilterText.getText().trim();
        if (tagText.trim().length() > 0) {
            try {
                Pattern.compile(tagText);
            } catch (PatternSyntaxException e) {
                return new DialogStatus(false,
                        "Invalid regex used in tag field: " + e.getMessage());
            }
        }

        /* text field must use a valid regex pattern */
        String messageText = mTextFilterText.getText().trim();
        if (messageText.trim().length() > 0) {
            try {
                Pattern.compile(messageText);
            } catch (PatternSyntaxException e) {
                return new DialogStatus(false,
                        "Invalid regex used in text field: " + e.getMessage());
            }
        }

        /* app name field must use a valid regex pattern */
//        String appNameText = mAppNameFilterText.getText().trim();
//        if (appNameText.trim().length() > 0) {
//            try {
//                Pattern.compile(appNameText);
//            } catch (PatternSyntaxException e) {
//                return new DialogStatus(false,
//                        "Invalid regex used in application name field: " + e.getMessage());
//            }
//        }

        return new DialogStatus(true, null);
    }

    private void createSeparator(Composite c) {
        Label l = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);
    }

    private void createLabel(Composite c, String text) {
        Label l = new Label(c, SWT.NONE);
        l.setText(text);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        l.setLayoutData(gd);
    }

    @Override
    protected void okPressed() {
        /* save values from the widgets before the shell is closed. */
        mFilterName = mFilterNameText.getText();
        mTag = mTagFilterText.getText();
        mText = mTextFilterText.getText();
        mLogLevel = mLogLevelCombo.getText();
        mPid = mPidFilterText.getText();
//        mAppName = mAppNameFilterText.getText();

        super.okPressed();
    }

    /**
     * Obtain the name for this filter.
     * @return user provided filter name, maybe empty.
     */
    public String getFilterName() {
        return mFilterName;
    }

    /**
     * Obtain the tag regex to filter by.
     * @return user provided tag regex, maybe empty.
     */
    public String getTag() {
        return mTag;
    }

    /**
     * Obtain the text regex to filter by.
     * @return user provided tag regex, maybe empty.
     */
    public String getText() {
        return mText;
    }

    /**
     * Obtain user provided PID to filter by.
     * @return user provided pid, maybe empty.
     */
    public String getPid() {
        return mPid;
    }

//    /**
//     * Obtain user provided application name to filter by.
//     * @return user provided app name regex, maybe empty
//     */
//    public String getAppName() {
//        return mAppName;
//    }

    /**
     * Obtain log level to filter by.
     * @return log level string.
     */
    public String getLogLevel() {
        return mLogLevel;
    }
    
    public List<String> getPIDHideList(){
    	return mPIDHideList;
    }
    
    public List<String> getTagHideList(){
    	return mTagHideList;
    }
    
    public void setPIDList(List<String> PIDList){
    	mPIDList = PIDList;
    }
    
    public void setTagList(List<String> tagList){
    	mTagList = tagList;
    }

    /**
     * Obtain the string representation of all supported log levels.
     * @return an array of strings, each representing a certain log level.
     */
    public static List<String> getLogLevels() {
        List<String> logLevels = new ArrayList<String>();

        for (LogLevel l : LogLevel.values()) {
            logLevels.add(l.getStringValue());
        }

        return logLevels;
    }
    
    class ListContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List<?>) {
	            Object[] e = ((List<?>) inputElement).toArray();
	            return e;
	        }
			return null;
		}
    }
    
    class ListLabelProvider extends LabelProvider implements ITableLabelProvider{
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof String){
				return (String)element;
			}
			return null;
		}
    	
    }
}
