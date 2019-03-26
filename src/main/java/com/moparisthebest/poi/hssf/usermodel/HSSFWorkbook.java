/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package com.moparisthebest.poi.hssf.usermodel;

import static com.moparisthebest.poi.hssf.model.InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

import com.moparisthebest.poi.EncryptedDocumentException;
import com.moparisthebest.poi.POIDocument;
import com.moparisthebest.poi.ddf.EscherBSERecord;
import com.moparisthebest.poi.ddf.EscherBitmapBlip;
import com.moparisthebest.poi.ddf.EscherBlipRecord;
import com.moparisthebest.poi.ddf.EscherMetafileBlip;
import com.moparisthebest.poi.ddf.EscherRecord;
import com.moparisthebest.poi.hpsf.ClassID;
import com.moparisthebest.poi.hssf.OldExcelFormatException;
import com.moparisthebest.poi.hssf.model.DrawingManager2;
import com.moparisthebest.poi.hssf.model.HSSFFormulaParser;
import com.moparisthebest.poi.hssf.model.InternalSheet;
import com.moparisthebest.poi.hssf.model.InternalSheet.UnsupportedBOFType;
import com.moparisthebest.poi.hssf.model.InternalWorkbook;
import com.moparisthebest.poi.hssf.model.RecordStream;
import com.moparisthebest.poi.hssf.record.AbstractEscherHolderRecord;
import com.moparisthebest.poi.hssf.record.BackupRecord;
import com.moparisthebest.poi.hssf.record.BoundSheetRecord;
import com.moparisthebest.poi.hssf.record.DrawingGroupRecord;
import com.moparisthebest.poi.hssf.record.ExtendedFormatRecord;
import com.moparisthebest.poi.hssf.record.FilePassRecord;
import com.moparisthebest.poi.hssf.record.FontRecord;
import com.moparisthebest.poi.hssf.record.LabelRecord;
import com.moparisthebest.poi.hssf.record.LabelSSTRecord;
import com.moparisthebest.poi.hssf.record.NameRecord;
import com.moparisthebest.poi.hssf.record.RecalcIdRecord;
import com.moparisthebest.poi.hssf.record.Record;
import com.moparisthebest.poi.hssf.record.RecordFactory;
import com.moparisthebest.poi.hssf.record.SSTRecord;
import com.moparisthebest.poi.hssf.record.UnknownRecord;
import com.moparisthebest.poi.hssf.record.aggregates.RecordAggregate.RecordVisitor;
import com.moparisthebest.poi.hssf.record.common.UnicodeString;
import com.moparisthebest.poi.hssf.util.CellReference;
import com.moparisthebest.poi.ss.SpreadsheetVersion;
import com.moparisthebest.poi.ss.formula.FormulaShifter;
import com.moparisthebest.poi.ss.formula.FormulaType;
import com.moparisthebest.poi.ss.formula.SheetNameFormatter;
import com.moparisthebest.poi.ss.formula.udf.AggregatingUDFFinder;
import com.moparisthebest.poi.ss.formula.udf.IndexedUDFFinder;
import com.moparisthebest.poi.ss.formula.udf.UDFFinder;
import com.moparisthebest.poi.ss.usermodel.Name;
import com.moparisthebest.poi.ss.usermodel.Row.MissingCellPolicy;
import com.moparisthebest.poi.ss.usermodel.Sheet;
import com.moparisthebest.poi.ss.usermodel.SheetVisibility;
import com.moparisthebest.poi.ss.usermodel.Workbook;
import com.moparisthebest.poi.util.Configurator;
import com.moparisthebest.poi.util.HexDump;
import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.POILogFactory;
import com.moparisthebest.poi.util.POILogger;
import com.moparisthebest.poi.util.Removal;

/**
 * High level representation of a workbook.  This is the first object most users
 * will construct whether they are reading or writing a workbook.  It is also the
 * top level object for creating new sheets/etc.
 *
 * @see com.moparisthebest.poi.hssf.model.InternalWorkbook
 * @see com.moparisthebest.poi.hssf.usermodel.HSSFSheet
 */
public final class HSSFWorkbook implements com.moparisthebest.poi.ss.usermodel.Workbook {
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    /**
     * The maximum number of cell styles in a .xls workbook.
     * The 'official' limit is 4,000, but POI allows a slightly larger number.
     * This extra delta takes into account built-in styles that are automatically
     *
     * See http://office.microsoft.com/en-us/excel-help/excel-specifications-and-limits-HP005199291.aspx
     */
    private static final int MAX_STYLES = 4030;

    private static final int DEBUG = POILogger.DEBUG;

    /**
     * used for compile-time performance/memory optimization.  This determines the
     * initial capacity for the sheet collection.  Its currently set to 3.
     * Changing it in this release will decrease performance
     * since you're never allowed to have more or less than three sheets!
     */

    public final static int INITIAL_CAPACITY = Configurator.getIntValue("HSSFWorkbook.SheetInitialCapacity",3);

    /**
     * SpreadsheetVersion used by this workbook, EXCEL97 in every case where we need to read or write to disk
     */
    public final SpreadsheetVersion spreadsheetVersion;

    /**
     * this is the reference to the low level Workbook object
     */

    private InternalWorkbook workbook;

    /**
     * this holds the HSSFSheet objects attached to this workbook
     */

    protected List<HSSFSheet> _sheets;

    /**
     * this holds the HSSFName objects attached to this workbook
     */

    private ArrayList<HSSFName> names;

    /**
     * this holds the HSSFFont objects attached to this workbook.
     * We only create these from the low level records as required.
     */
    private Map<Short,HSSFFont> fonts;

    /**
     * holds whether or not to preserve other nodes in the POIFS.  Used
     * for macros and embedded objects.
     */
    private boolean   preserveNodes;

    /**
     * Used to keep track of the data formatter so that all
     * createDataFormatter calls return the same one for a given
     * book.  This ensures that updates from one places is visible
     * someplace else.
     */
    private HSSFDataFormat formatter;

    /**
     * The policy to apply in the event of missing or
     *  blank cells when fetching from a row.
     * See {@link MissingCellPolicy}
     */
    private MissingCellPolicy missingCellPolicy = MissingCellPolicy.RETURN_NULL_AND_BLANK;

    private static final POILogger log = POILogFactory.getLogger(HSSFWorkbook.class);

    /**
     * The locator of user-defined functions.
     * By default includes functions from the Excel Analysis Toolpack
     */
    private UDFFinder _udfFinder = new IndexedUDFFinder(AggregatingUDFFinder.DEFAULT);

    public static HSSFWorkbook create(InternalWorkbook book) {
    	return new HSSFWorkbook(book, SpreadsheetVersion.EXCEL2007);
    }
    /**
     * Creates new HSSFWorkbook from scratch (start here!)
     *
     */
    public HSSFWorkbook() {
        this(InternalWorkbook.createWorkbook(), SpreadsheetVersion.EXCEL2007);
    }

    public HSSFWorkbook(final SpreadsheetVersion spreadsheetVersion) {
        this(InternalWorkbook.createWorkbook(), spreadsheetVersion);
        if(spreadsheetVersion == null)
            throw new IllegalArgumentException("SpreadsheetVersion must be non-null");
    }

    private HSSFWorkbook(InternalWorkbook book, final SpreadsheetVersion spreadsheetVersion) {
        this.spreadsheetVersion = spreadsheetVersion;
        workbook = book;
        _sheets = new ArrayList<HSSFSheet>(INITIAL_CAPACITY);
        names = new ArrayList<HSSFName>(INITIAL_CAPACITY);
    }

    /**
     * used internally to set the workbook properties.
     */

    private void setPropertiesFromWorkbook(InternalWorkbook book)
    {
        this.workbook = book;

        // none currently
    }

    /**
      * This is basically a kludge to deal with the now obsolete Label records.  If
      * you have to read in a sheet that contains Label records, be aware that the rest
      * of the API doesn't deal with them, the low level structure only provides read-only
      * semi-immutable structures (the sets are there for interface conformance with NO
      * Implementation).  In short, you need to call this function passing it a reference
      * to the Workbook object.  All labels will be converted to LabelSST records and their
      * contained strings will be written to the Shared String table (SSTRecord) within
      * the Workbook.
      *
      * @param records a collection of sheet's records.
      * @param offset the offset to search at
      * @see com.moparisthebest.poi.hssf.record.LabelRecord
      * @see com.moparisthebest.poi.hssf.record.LabelSSTRecord
      * @see com.moparisthebest.poi.hssf.record.SSTRecord
      */

     private void convertLabelRecords(List<Record> records, int offset)
     {
         if (log.check( POILogger.DEBUG )) {
            log.log(POILogger.DEBUG, "convertLabelRecords called");
        }
         for (int k = offset; k < records.size(); k++)
         {
             Record rec = records.get(k);

             if (rec.getSid() == LabelRecord.sid)
             {
                 LabelRecord oldrec = ( LabelRecord ) rec;

                 records.remove(k);
                 LabelSSTRecord newrec   = new LabelSSTRecord();
                 int            stringid =
                     workbook.addSSTString(new UnicodeString(oldrec.getValue()));

                 newrec.setRow(oldrec.getRow());
                 newrec.setColumn(oldrec.getColumn());
                 newrec.setXFIndex(oldrec.getXFIndex());
                 newrec.setSSTIndex(stringid);
                       records.add(k, newrec);
             }
         }
         if (log.check( POILogger.DEBUG )) {
            log.log(POILogger.DEBUG, "convertLabelRecords exit");
        }
     }

    /**
     * Retrieves the current policy on what to do when
     *  getting missing or blank cells from a row.
     * The default is to return blank and null cells.
     *  {@link MissingCellPolicy}
     */
    @Override
    public MissingCellPolicy getMissingCellPolicy() {
        return missingCellPolicy;
    }

    /**
     * Sets the policy on what to do when
     *  getting missing or blank cells from a row.
     * This will then apply to all calls to
     *  {@link HSSFRow#getCell(int)}}. See
     *  {@link MissingCellPolicy}.
     * Note that this has no effect on any
     *  iterators, only on when fetching Cells
     *  by their column index.
     */
    @Override
    public void setMissingCellPolicy(MissingCellPolicy missingCellPolicy) {
        this.missingCellPolicy = missingCellPolicy;
    }

    /**
     * sets the order of appearance for a given sheet.
     *
     * @param sheetname the name of the sheet to reorder
     * @param pos the position that we want to insert the sheet into (0 based)
     */

    @Override
    public void setSheetOrder(String sheetname, int pos ) {
        int oldSheetIndex = getSheetIndex(sheetname);
        _sheets.add(pos,_sheets.remove(oldSheetIndex));
        workbook.setSheetOrder(sheetname, pos);

        FormulaShifter shifter = FormulaShifter.createForSheetShift(oldSheetIndex, pos);
        for (HSSFSheet sheet : _sheets) {
            sheet.getSheet().updateFormulasAfterCellShift(shifter, /* not used */ -1 );
        }

        workbook.updateNamesAfterCellShift(shifter);
        updateNamedRangesAfterSheetReorder(oldSheetIndex, pos);
        
        updateActiveSheetAfterSheetReorder(oldSheetIndex, pos);
    }
    
    /**
     * copy-pasted from XSSFWorkbook#updateNamedRangesAfterSheetReorder(int, int)
     * 
     * update sheet-scoped named ranges in this workbook after changing the sheet order
     * of a sheet at oldIndex to newIndex.
     * Sheets between these indices will move left or right by 1.
     *
     * @param oldIndex the original index of the re-ordered sheet
     * @param newIndex the new index of the re-ordered sheet
     */
    private void updateNamedRangesAfterSheetReorder(int oldIndex, int newIndex) {
        // update sheet index of sheet-scoped named ranges
        for (final HSSFName name : names) {
            final int i = name.getSheetIndex();
            // name has sheet-level scope
            if (i != -1) {
                // name refers to this sheet
                if (i == oldIndex) {
                    name.setSheetIndex(newIndex);
                }
                // if oldIndex > newIndex then this sheet moved left and sheets between newIndex and oldIndex moved right
                else if (newIndex <= i && i < oldIndex) {
                    name.setSheetIndex(i+1);
                }
                // if oldIndex < newIndex then this sheet moved right and sheets between oldIndex and newIndex moved left
                else if (oldIndex < i && i <= newIndex) {
                    name.setSheetIndex(i-1);
                }
            }
        }
    }

    
    private void updateActiveSheetAfterSheetReorder(int oldIndex, int newIndex) {
        // adjust active sheet if necessary
        int active = getActiveSheetIndex();
        if(active == oldIndex) {
            // moved sheet was the active one
            setActiveSheet(newIndex);
        } else if ((active < oldIndex && active < newIndex) ||
                (active > oldIndex && active > newIndex)) {
            // not affected
        } else if (newIndex > oldIndex) {
            // moved sheet was below before and is above now => active is one less
            setActiveSheet(active-1);
        } else {
            // remaining case: moved sheet was higher than active before and is lower now => active is one more
            setActiveSheet(active+1);
        }
    }

    private void validateSheetIndex(int index) {
        int lastSheetIx = _sheets.size() - 1;
        if (index < 0 || index > lastSheetIx) {
            String range = "(0.." +    lastSheetIx + ")";
            if (lastSheetIx == -1) {
                range = "(no sheets)";
            }
            throw new IllegalArgumentException("Sheet index ("
                    + index +") is out of range " + range);
        }
    }

    /**
     * Selects a single sheet. This may be different to
     * the 'active' sheet (which is the sheet with focus).
     */
    @Override
    public void setSelectedTab(int index) {

        validateSheetIndex(index);
        int nSheets = _sheets.size();
        for (int i=0; i<nSheets; i++) {
               getSheetAt(i).setSelected(i == index);
        }
        workbook.getWindowOne().setNumSelectedTabs((short)1);
    }

    /**
     * Selects multiple sheets as a group. This is distinct from
     * the 'active' sheet (which is the sheet with focus).
     * Unselects sheets that are not in <code>indexes</code>.
     *
     * @param indexes Array of sheets to select, the index is 0-based.
     */
    public void setSelectedTabs(int[] indexes) {
        Collection<Integer> list = new ArrayList<Integer>(indexes.length);
        for (int index : indexes) {
            list.add(index);
        }
        setSelectedTabs(list);
    }
    
    /**
     * Selects multiple sheets as a group. This is distinct from
     * the 'active' sheet (which is the sheet with focus).
     * Unselects sheets that are not in <code>indexes</code>.
     *
     * @param indexes Collection of sheets to select, the index is 0-based.
     */
    public void setSelectedTabs(Collection<Integer> indexes) {

        for (int index : indexes) {
            validateSheetIndex(index);
        }
        // ignore duplicates
        Set<Integer> set = new HashSet<Integer>(indexes);
        int nSheets = _sheets.size();
        for (int i=0; i<nSheets; i++) {
            boolean bSelect = set.contains(i);
            getSheetAt(i).setSelected(bSelect);
        }
        // this is true only if all values in set were valid sheet indexes (between 0 and nSheets-1, inclusive)
        short nSelected = (short) set.size();
        workbook.getWindowOne().setNumSelectedTabs(nSelected);
    }
    
    /**
     * Gets the selected sheets (if more than one, Excel calls these a [Group]). 
     *
     * @return indices of selected sheets
     */
    public Collection<Integer> getSelectedTabs() {
        Collection<Integer> indexes = new ArrayList<Integer>();
        int nSheets = _sheets.size();
        for (int i=0; i<nSheets; i++) {
            HSSFSheet sheet = getSheetAt(i);
            if (sheet.isSelected()) {
                indexes.add(i);
            }
        }
        return Collections.unmodifiableCollection(indexes);
    }
    
    /**
     * Convenience method to set the active sheet.  The active sheet is is the sheet
     * which is currently displayed when the workbook is viewed in Excel.
     * 'Selected' sheet(s) is a distinct concept.
     */
    @Override
    public void setActiveSheet(int index) {

        validateSheetIndex(index);
        int nSheets = _sheets.size();
        for (int i=0; i<nSheets; i++) {
             getSheetAt(i).setActive(i == index);
        }
        workbook.getWindowOne().setActiveSheetIndex(index);
    }

    /**
     * gets the tab whose data is actually seen when the sheet is opened.
     * This may be different from the "selected sheet" since excel seems to
     * allow you to show the data of one sheet when another is seen "selected"
     * in the tabs (at the bottom).
     * @see com.moparisthebest.poi.hssf.usermodel.HSSFSheet#setSelected(boolean)
     */
    @Override
    public int getActiveSheetIndex() {
        return workbook.getWindowOne().getActiveSheetIndex();
    }


    /**
     * sets the first tab that is displayed in the list of tabs
     * in excel. This method does <b>not</b> hide, select or focus sheets.
     * It just sets the scroll position in the tab-bar.
     *
     * @param index the sheet index of the tab that will become the first in the tab-bar
     */
    @Override
    public void setFirstVisibleTab(int index) {
        workbook.getWindowOne().setFirstVisibleTab(index);
    }

    /**
     * sets the first tab that is displayed in the list of tabs in excel.
     */
    @Override
    public int getFirstVisibleTab() {
        return workbook.getWindowOne().getFirstVisibleTab();
    }

    /**
     * Set the sheet name.
     *
     * @param sheetIx number (0 based)
     * @throws IllegalArgumentException if the name is null or invalid
     *  or workbook already contains a sheet with this name
     * @see #createSheet(String)
     * @see com.moparisthebest.poi.ss.util.WorkbookUtil#createSafeSheetName(String nameProposal)
     */
    @Override
    public void setSheetName(int sheetIx, String name) {
        if (name == null) {
            throw new IllegalArgumentException("sheetName must not be null");
        }

        if (workbook.doesContainsSheetName(name, sheetIx)) {
            throw new IllegalArgumentException("The workbook already contains a sheet named '" + name + "'");
        }
        validateSheetIndex(sheetIx);
        workbook.setSheetName(sheetIx, name);
    }

    /**
     * @return Sheet name for the specified index
     */
    @Override
    public String getSheetName(int sheetIndex) {
        validateSheetIndex(sheetIndex);
        return workbook.getSheetName(sheetIndex);
    }

    @Override
    public boolean isHidden() {
        return workbook.getWindowOne().getHidden();
    }

    @Override
    public void setHidden(boolean hiddenFlag) {
        workbook.getWindowOne().setHidden(hiddenFlag);
    }

    @Override
    public boolean isSheetHidden(int sheetIx) {
        validateSheetIndex(sheetIx);
        return workbook.isSheetHidden(sheetIx);
    }

    @Override
    public boolean isSheetVeryHidden(int sheetIx) {
        validateSheetIndex(sheetIx);
        return workbook.isSheetVeryHidden(sheetIx);
    }
    
    @Override
    public SheetVisibility getSheetVisibility(int sheetIx) {
        return workbook.getSheetVisibility(sheetIx);
    }

    @Override
    public void setSheetHidden(int sheetIx, boolean hidden) {
        setSheetVisibility(sheetIx, hidden ? SheetVisibility.HIDDEN : SheetVisibility.VISIBLE);
    }

    @Removal(version="3.18")
    @Deprecated
    @Override
    public void setSheetHidden(int sheetIx, int hidden) {
        switch (hidden) {
            case Workbook.SHEET_STATE_VISIBLE:
                setSheetVisibility(sheetIx, SheetVisibility.VISIBLE);
                break;
            case Workbook.SHEET_STATE_HIDDEN:
                setSheetVisibility(sheetIx, SheetVisibility.HIDDEN);
                break;
            case Workbook.SHEET_STATE_VERY_HIDDEN:
                setSheetVisibility(sheetIx, SheetVisibility.VERY_HIDDEN);
                break;
            default:
                throw new IllegalArgumentException("Invalid sheet state : " + hidden + "\n" +
                        "Sheet state must beone of the Workbook.SHEET_STATE_* constants");
        }
    }
    
    @Override
    public void setSheetVisibility(int sheetIx, SheetVisibility visibility) {
        validateSheetIndex(sheetIx);
        workbook.setSheetHidden(sheetIx, visibility);
    }

    /** Returns the index of the sheet by his name
     * @param name the sheet name
     * @return index of the sheet (0 based)
     */
    @Override
    public int getSheetIndex(String name){
        return workbook.getSheetIndex(name);
    }

    /** Returns the index of the given sheet
     * @param sheet the sheet to look up
     * @return index of the sheet (0 based). <tt>-1</tt> if not found
     */
    @Override
    public int getSheetIndex(com.moparisthebest.poi.ss.usermodel.Sheet sheet) {
        return _sheets.indexOf(sheet);
    }

    /**
     * create an HSSFSheet for this HSSFWorkbook, adds it to the sheets and returns
     * the high level representation.  Use this to create new sheets.
     *
     * @return HSSFSheet representing the new sheet.
     */

    @Override
    public HSSFSheet createSheet()
    {
        HSSFSheet sheet = new HSSFSheet(this);

        _sheets.add(sheet);
        workbook.setSheetName(_sheets.size() - 1, "Sheet" + (_sheets.size() - 1));
        boolean isOnlySheet = _sheets.size() == 1;
        sheet.setSelected(isOnlySheet);
        sheet.setActive(isOnlySheet);
        return sheet;
    }

    /**
     * create an HSSFSheet from an existing sheet in the HSSFWorkbook.
     *
     * @return HSSFSheet representing the cloned sheet.
     */

    @Override
    public HSSFSheet cloneSheet(int sheetIndex) {
        validateSheetIndex(sheetIndex);
        HSSFSheet srcSheet = _sheets.get(sheetIndex);
        String srcName = workbook.getSheetName(sheetIndex);
        HSSFSheet clonedSheet = srcSheet.cloneSheet(this);
        clonedSheet.setSelected(false);
        clonedSheet.setActive(false);

        String name = getUniqueSheetName(srcName);
        int newSheetIndex = _sheets.size();
        _sheets.add(clonedSheet);
        workbook.setSheetName(newSheetIndex, name);

        // Check this sheet has an autofilter, (which has a built-in NameRecord at workbook level)
        int filterDbNameIndex = findExistingBuiltinNameRecordIdx(sheetIndex, NameRecord.BUILTIN_FILTER_DB);
        if (filterDbNameIndex != -1) {
            NameRecord newNameRecord = workbook.cloneFilter(filterDbNameIndex, newSheetIndex);
            HSSFName newName = new HSSFName(this, newNameRecord);
            names.add(newName);
        }
        // TODO - maybe same logic required for other/all built-in name records
//        workbook.cloneDrawings(clonedSheet.getSheet());

        return clonedSheet;
    }

    private String getUniqueSheetName(String srcName) {
        int uniqueIndex = 2;
        String baseName = srcName;
        int bracketPos = srcName.lastIndexOf('(');
        if (bracketPos > 0 && srcName.endsWith(")")) {
            String suffix = srcName.substring(bracketPos + 1, srcName.length() - ")".length());
            try {
                uniqueIndex = Integer.parseInt(suffix.trim());
                uniqueIndex++;
                baseName=srcName.substring(0, bracketPos).trim();
            } catch (NumberFormatException e) {
                // contents of brackets not numeric
            }
        }
        while (true) {
            // Try and find the next sheet name that is unique
            String index = Integer.toString(uniqueIndex++);
            String name;
            if (baseName.length() + index.length() + 2 < 31) {
                name = baseName + " (" + index + ")";
            } else {
                name = baseName.substring(0, 31 - index.length() - 2) + "(" + index + ")";
            }

            //If the sheet name is unique, then set it otherwise move on to the next number.
            if (workbook.getSheetIndex(name) == -1) {
              return name;
            }
        }
    }

    /**
     * Create a new sheet for this Workbook and return the high level representation.
     * Use this to create new sheets.
     *
     * <p>
     *     Note that Excel allows sheet names up to 31 chars in length but other applications
     *     (such as OpenOffice) allow more. Some versions of Excel crash with names longer than 31 chars,
     *     others - truncate such names to 31 character.
     * </p>
     * <p>
     *     POI's SpreadsheetAPI silently truncates the input argument to 31 characters.
     *     Example:
     *
     *     <pre><code>
     *     Sheet sheet = workbook.createSheet("My very long sheet name which is longer than 31 chars"); // will be truncated
     *     assert 31 == sheet.getSheetName().length();
     *     assert "My very long sheet name which i" == sheet.getSheetName();
     *     </code></pre>
     * </p>
     *
     * Except the 31-character constraint, Excel applies some other rules:
     * <p>
     * Sheet name MUST be unique in the workbook and MUST NOT contain the any of the following characters:
     * <ul>
     * <li> 0x0000 </li>
     * <li> 0x0003 </li>
     * <li> colon (:) </li>
     * <li> backslash (\) </li>
     * <li> asterisk (*) </li>
     * <li> question mark (?) </li>
     * <li> forward slash (/) </li>
     * <li> opening square bracket ([) </li>
     * <li> closing square bracket (]) </li>
     * </ul>
     * The string MUST NOT begin or end with the single quote (') character.
     * </p>
     *
     * @param sheetname  sheetname to set for the sheet.
     * @return Sheet representing the new sheet.
     * @throws IllegalArgumentException if the name is null or invalid
     *  or workbook already contains a sheet with this name
     * @see com.moparisthebest.poi.ss.util.WorkbookUtil#createSafeSheetName(String nameProposal)
     */
    @Override
    public HSSFSheet createSheet(String sheetname)
    {
        if (sheetname == null) {
            throw new IllegalArgumentException("sheetName must not be null");
        }

        if (workbook.doesContainsSheetName( sheetname, _sheets.size() )) {
            throw new IllegalArgumentException("The workbook already contains a sheet named '" + sheetname + "'");
        }

        HSSFSheet sheet = new HSSFSheet(this);

        workbook.setSheetName(_sheets.size(), sheetname);
        _sheets.add(sheet);
        boolean isOnlySheet = _sheets.size() == 1;
        sheet.setSelected(isOnlySheet);
        sheet.setActive(isOnlySheet);
        return sheet;
    }

    /**
     *  Returns an iterator of the sheets in the workbook
     *  in sheet order. Includes hidden and very hidden sheets.
     *
     * @return an iterator of the sheets.
     */
    @Override
    public Iterator<Sheet> sheetIterator() {
        return new SheetIterator<Sheet>();
    }

    /**
     * Alias for {@link #sheetIterator()} to allow
     * foreach loops
     */
    @Override
    public Iterator<Sheet> iterator() {
        return sheetIterator();
    }
    
    private final class SheetIterator<T extends Sheet> implements Iterator<T> {
        final private Iterator<T> it;
        private T cursor = null;
        @SuppressWarnings("unchecked")
        public SheetIterator() {
            it = (Iterator<T>) _sheets.iterator();
        }
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }
        @Override
        public T next() throws NoSuchElementException {
            cursor = it.next();
            return cursor;
        }
        /**
         * Unexpected behavior may occur if sheets are reordered after iterator
         * has been created. Support for the remove method may be added in the future
         * if someone can figure out a reliable implementation.
         */
        @Override
        public void remove() throws IllegalStateException {
            throw new UnsupportedOperationException("remove method not supported on HSSFWorkbook.iterator(). "+
                        "Use Sheet.removeSheetAt(int) instead.");
        }
    }

    /**
     * get the number of spreadsheets in the workbook (this will be three after serialization)
     * @return number of sheets
     */
    @Override
    public int getNumberOfSheets()
    {
        return _sheets.size();
    }

    private HSSFSheet[] getSheets() {
        HSSFSheet[] result = new HSSFSheet[_sheets.size()];
        _sheets.toArray(result);
        return result;
    }

    /**
     * Get the HSSFSheet object at the given index.
     * @param index of the sheet number (0-based physical & logical)
     * @return HSSFSheet at the provided index
     * @throws IllegalArgumentException if the index is out of range (index
     *            &lt; 0 || index &gt;= getNumberOfSheets()).
     */
    @Override
    public HSSFSheet getSheetAt(int index)
    {
        validateSheetIndex(index);
        return _sheets.get(index);
    }

    /**
     * Get sheet with the given name (case insensitive match)
     * @param name of the sheet
     * @return HSSFSheet with the name provided or <code>null</code> if it does not exist
     */

    @Override
    public HSSFSheet getSheet(String name)
    {
        HSSFSheet retval = null;

        for (int k = 0; k < _sheets.size(); k++)
        {
            String sheetname = workbook.getSheetName(k);

            if (sheetname.equalsIgnoreCase(name))
            {
                retval = _sheets.get(k);
            }
        }
        return retval;
    }

    /**
     * Removes sheet at the given index.<p/>
     *
     * Care must be taken if the removed sheet is the currently active or only selected sheet in
     * the workbook. There are a few situations when Excel must have a selection and/or active
     * sheet. (For example when printing - see Bug 40414).<br/>
     *
     * This method makes sure that if the removed sheet was active, another sheet will become
     * active in its place.  Furthermore, if the removed sheet was the only selected sheet, another
     * sheet will become selected.  The newly active/selected sheet will have the same index, or
     * one less if the removed sheet was the last in the workbook.
     *
     * @param index of the sheet  (0-based)
     */
    @Override
    public void removeSheetAt(int index) {
        validateSheetIndex(index);
        boolean wasSelected = getSheetAt(index).isSelected();

        _sheets.remove(index);
        workbook.removeSheet(index);

        // set the remaining active/selected sheet
        int nSheets = _sheets.size();
        if (nSheets < 1) {
            // nothing more to do if there are no sheets left
            return;
        }
        // the index of the closest remaining sheet to the one just deleted
        int newSheetIndex = index;
        if (newSheetIndex >= nSheets) {
            newSheetIndex = nSheets-1;
        }

        if (wasSelected) {
            boolean someOtherSheetIsStillSelected = false;
            for (int i =0; i < nSheets; i++) {
                if (getSheetAt(i).isSelected()) {
                    someOtherSheetIsStillSelected = true;
                    break;
                }
            }
            if (!someOtherSheetIsStillSelected) {
                setSelectedTab(newSheetIndex);
            }
        }

        // adjust active sheet
        int active = getActiveSheetIndex();
        if(active == index) {
            // removed sheet was the active one, reset active sheet if there is still one left now
            setActiveSheet(newSheetIndex);
        } else if (active > index) {
            // removed sheet was below the active one => active is one less now
            setActiveSheet(active-1);
        }
    }

    /**
     * determine whether the Excel GUI will backup the workbook when saving.
     *
     * @param backupValue   true to indicate a backup will be performed.
     */

    public void setBackupFlag(boolean backupValue)
    {
        BackupRecord backupRecord = workbook.getBackupRecord();

        backupRecord.setBackup(backupValue ? (short) 1
                : (short) 0);
    }

    /**
     * determine whether the Excel GUI will backup the workbook when saving.
     *
     * @return the current setting for backups.
     */

    public boolean getBackupFlag()
    {
        BackupRecord backupRecord = workbook.getBackupRecord();

        return backupRecord.getBackup() != 0;
    }

    int findExistingBuiltinNameRecordIdx(int sheetIndex, byte builtinCode) {
        for(int defNameIndex =0; defNameIndex<names.size(); defNameIndex++) {
            NameRecord r = workbook.getNameRecord(defNameIndex);
            if (r == null) {
                throw new RuntimeException("Unable to find all defined names to iterate over");
            }
            if (!r.isBuiltInName() || r.getBuiltInName() != builtinCode) {
                continue;
            }
            if (r.getSheetNumber() -1 == sheetIndex) {
                return defNameIndex;
            }
        }
        return -1;
    }


    HSSFName createBuiltInName(byte builtinCode, int sheetIndex) {
      NameRecord nameRecord =
        workbook.createBuiltInName(builtinCode, sheetIndex + 1);
      HSSFName newName = new HSSFName(this, nameRecord, null);
      names.add(newName);
      return newName;
    }


    HSSFName getBuiltInName(byte builtinCode, int sheetIndex) {
      int index = findExistingBuiltinNameRecordIdx(sheetIndex, builtinCode);
      if (index < 0) {
        return null;
      } else {
        return names.get(index);
      }
    }


    /**
     * create a new Font and add it to the workbook's font table
     * @return new font object
     */

    @Override
    public HSSFFont createFont()
    {
        /*FontRecord font =*/ workbook.createNewFont();
        short fontindex = (short) (getNumberOfFonts() - 1);

        if (fontindex > 3)
        {
            fontindex++;   // THERE IS NO FOUR!!
        }
        if(fontindex == Short.MAX_VALUE){
            throw new IllegalArgumentException("Maximum number of fonts was exceeded");
        }

        // Ask getFontAt() to build it for us,
        //  so it gets properly cached
        return getFontAt(fontindex);
    }

    /**
     * Finds a font that matches the one with the supplied attributes
     * @deprecated 3.15 beta 2. Use {@link #findFont(boolean, short, short, String, boolean, boolean, short, byte)} instead.
     */
    @Deprecated
    @Override
    public HSSFFont findFont(short boldWeight, short color, short fontHeight,
                             String name, boolean italic, boolean strikeout,
                             short typeOffset, byte underline)
    {
        short numberOfFonts = getNumberOfFonts();
        for (short i=0; i<=numberOfFonts; i++) {
            // Remember - there is no 4!
            if(i == 4) {
                continue;
            }

            HSSFFont hssfFont = getFontAt(i);
            if (hssfFont.getBoldweight() == boldWeight
                    && hssfFont.getColor() == color
                    && hssfFont.getFontHeight() == fontHeight
                    && hssfFont.getFontName().equals(name)
                    && hssfFont.getItalic() == italic
                    && hssfFont.getStrikeout() == strikeout
                    && hssfFont.getTypeOffset() == typeOffset
                    && hssfFont.getUnderline() == underline)
            {
                return hssfFont;
            }
        }

        return null;
    }
    /**
     * Finds a font that matches the one with the supplied attributes
     */
    @Override
    public HSSFFont findFont(boolean bold, short color, short fontHeight,
                             String name, boolean italic, boolean strikeout,
                             short typeOffset, byte underline)
    {
        short numberOfFonts = getNumberOfFonts();
        for (short i=0; i<=numberOfFonts; i++) {
            // Remember - there is no 4!
            if(i == 4) {
                continue;
            }

            HSSFFont hssfFont = getFontAt(i);
            if (hssfFont.getBold() == bold
                    && hssfFont.getColor() == color
                    && hssfFont.getFontHeight() == fontHeight
                    && hssfFont.getFontName().equals(name)
                    && hssfFont.getItalic() == italic
                    && hssfFont.getStrikeout() == strikeout
                    && hssfFont.getTypeOffset() == typeOffset
                    && hssfFont.getUnderline() == underline)
            {
                return hssfFont;
            }
        }

        return null;
    }

    /**
     * get the number of fonts in the font table
     * @return number of fonts
     */

    @Override
    public short getNumberOfFonts()
    {
        return (short) workbook.getNumberOfFontRecords();
    }

    /**
     * Get the font at the given index number
     * @param idx  index number
     * @return HSSFFont at the index
     */
    @Override
    public HSSFFont getFontAt(short idx) {
        if(fonts == null) {
            fonts = new HashMap<Short, HSSFFont>();
        }

        // So we don't confuse users, give them back
        //  the same object every time, but create
        //  them lazily
        Short sIdx = Short.valueOf(idx);
        if(fonts.containsKey(sIdx)) {
            return fonts.get(sIdx);
        }

        FontRecord font = workbook.getFontRecordAt(idx);
        HSSFFont retval = new HSSFFont(idx, font);
        fonts.put(sIdx, retval);

        return retval;
    }

    /**
     * Reset the fonts cache, causing all new calls
     *  to getFontAt() to create new objects.
     * Should only be called after deleting fonts,
     *  and that's not something you should normally do
     */
    protected void resetFontCache() {
        fonts = new HashMap<Short, HSSFFont>();
    }

    /**
     * Create a new Cell style and add it to the workbook's style table.
     * You can define up to 4000 unique styles in a .xls workbook.
     *
     * @return the new Cell Style object
     * @throws IllegalStateException if the number of cell styles exceeded the limit for this type of Workbook.
     */
    @Override
    public HSSFCellStyle createCellStyle()
    {
        if(workbook.getNumExFormats() == MAX_STYLES) {
            throw new IllegalStateException("The maximum number of cell styles was exceeded. " +
                    "You can define up to 4000 styles in a .xls workbook");
        }
        ExtendedFormatRecord xfr = workbook.createCellXF();
        short index = (short) (getNumCellStyles() - 1);
        return new HSSFCellStyle(index, xfr, this);
    }

    /**
     * get the number of styles the workbook contains
     * @return count of cell styles
     */
    @Override
    public int getNumCellStyles()
    {
        return workbook.getNumExFormats();
    }

    /**
     * get the cell style object at the given index
     * @param idx  index within the set of styles
     * @return HSSFCellStyle object at the index
     */
    @Override
    public HSSFCellStyle getCellStyleAt(int idx)
    {
        ExtendedFormatRecord xfr = workbook.getExFormatAt(idx);
        return new HSSFCellStyle((short)idx, xfr, this);
    }

    protected void validateSpreadsheetVersionWritePossible() throws IllegalStateException {
        if (this.spreadsheetVersion != SpreadsheetVersion.EXCEL97) {
            throw new IllegalStateException("SpreadsheetVersion not EXCEL97, cannot write file meant only for in-memory calculations");
        }
    }

    /**
     * Totals the sizes of all sheet records and eventually serializes them
     */
    private static final class SheetRecordCollector implements RecordVisitor {

        private List<Record> _list;
        private int _totalSize;

        public SheetRecordCollector() {
            _totalSize = 0;
            _list = new ArrayList<Record>(128);
        }
        public int getTotalSize() {
            return _totalSize;
        }
        @Override
        public void visitRecord(Record r) {
            _list.add(r);
            _totalSize+=r.getRecordSize();

        }
        public int serialize(int offset, byte[] data) {
            int result = 0;
            for (Record rec : _list) {
                result += rec.serialize(offset + result, data);
            }
            return result;
        }
    }
    
    /*package*/ InternalWorkbook getWorkbook() {
        return workbook;
    }

    @Override
    public int getNumberOfNames(){
        return names.size();
    }

    @Override
    public HSSFName getName(String name) {
        int nameIndex = getNameIndex(name);
        if (nameIndex < 0) {
            return null;
        }
        return names.get(nameIndex);
    }

    @Override
    public List<HSSFName> getNames(String name) {
        List<HSSFName> nameList = new ArrayList<HSSFName>();
        for(HSSFName nr : names) {
            if(nr.getNameName().equals(name)) {
                nameList.add(nr);
            }
        }

        return Collections.unmodifiableList(nameList);
    }

    @Override
    public HSSFName getNameAt(int nameIndex) {
        int nNames = names.size();
        if (nNames < 1) {
            throw new IllegalStateException("There are no defined names in this workbook");
        }
        if (nameIndex < 0 || nameIndex > nNames) {
            throw new IllegalArgumentException("Specified name index " + nameIndex
                    + " is outside the allowable range (0.." + (nNames-1) + ").");
        }
        return names.get(nameIndex);
    }

    @Override
    public List<HSSFName> getAllNames() {
        return Collections.unmodifiableList(names);
    }

    public NameRecord getNameRecord(int nameIndex) {
        return getWorkbook().getNameRecord(nameIndex);
    }

    /** gets the named range name
     * @param index the named range index (0 based)
     * @return named range name
     */
    public String getNameName(int index){
        return getNameAt(index).getNameName();
    }

    /**
     * Sets the printarea for the sheet provided
     * <p>
     * i.e. Reference = $A$1:$B$2
     * @param sheetIndex Zero-based sheet index (0 Represents the first sheet to keep consistent with java)
     * @param reference Valid name Reference for the Print Area
     */
    @Override
    public void setPrintArea(int sheetIndex, String reference)
    {
        NameRecord name = workbook.getSpecificBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, sheetIndex+1);


        if (name == null) {
            name = workbook.createBuiltInName(NameRecord.BUILTIN_PRINT_AREA, sheetIndex+1);
            // adding one here because 0 indicates a global named region; doesn't make sense for print areas
        }
        String[] parts = COMMA_PATTERN.split(reference);
        StringBuffer sb = new StringBuffer(32);
        for (int i = 0; i < parts.length; i++) {
            if(i>0) {
                sb.append(",");
            }
            SheetNameFormatter.appendFormat(sb, getSheetName(sheetIndex));
            sb.append("!");
            sb.append(parts[i]);
        }
        name.setNameDefinition(HSSFFormulaParser.parse(sb.toString(), this, FormulaType.NAMEDRANGE, sheetIndex));
    }

    /**
     * For the Convenience of Java Programmers maintaining pointers.
     * @see #setPrintArea(int, String)
     * @param sheetIndex Zero-based sheet index (0 = First Sheet)
     * @param startColumn Column to begin printarea
     * @param endColumn Column to end the printarea
     * @param startRow Row to begin the printarea
     * @param endRow Row to end the printarea
     */
    @Override
    public void setPrintArea(int sheetIndex, int startColumn, int endColumn,
                              int startRow, int endRow) {

        //using absolute references because they don't get copied and pasted anyway
        CellReference cell = new CellReference(startRow, startColumn, true, true);
        String reference = cell.formatAsString();

        cell = new CellReference(endRow, endColumn, true, true);
        reference = reference+":"+cell.formatAsString();

        setPrintArea(sheetIndex, reference);
    }


    /**
     * Retrieves the reference for the printarea of the specified sheet, the sheet name is appended to the reference even if it was not specified.
     * @param sheetIndex Zero-based sheet index (0 Represents the first sheet to keep consistent with java)
     * @return String Null if no print area has been defined
     */
    @Override
    public String getPrintArea(int sheetIndex) {
        NameRecord name = workbook.getSpecificBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, sheetIndex+1);
        //adding one here because 0 indicates a global named region; doesn't make sense for print areas
        if (name == null) {
            return null;
        }

        return HSSFFormulaParser.toFormulaString(this, name.getNameDefinition());
    }

    /**
     * Delete the printarea for the sheet specified
     * @param sheetIndex Zero-based sheet index (0 = First Sheet)
     */
    @Override
    public void removePrintArea(int sheetIndex) {
        getWorkbook().removeBuiltinRecord(NameRecord.BUILTIN_PRINT_AREA, sheetIndex+1);
    }

    /** creates a new named range and add it to the model
     * @return named range high level
     */
    @Override
    public HSSFName createName(){
        NameRecord nameRecord = workbook.createName();

        HSSFName newName = new HSSFName(this, nameRecord);

        names.add(newName);

        return newName;
    }

    @Override
    public int getNameIndex(String name) {

        for (int k = 0; k < names.size(); k++) {
            String nameName = getNameName(k);

            if (nameName.equalsIgnoreCase(name)) {
                return k;
            }
        }
        return -1;
    }


    /**
     * As {@link #getNameIndex(String)} is not necessarily unique
     * (name + sheet index is unique), this method is more accurate.
     *
     * @param name the name whose index in the list of names of this workbook
     *        should be looked up.
     * @return an index value >= 0 if the name was found; -1, if the name was
     *         not found
     */
    int getNameIndex(HSSFName name) {
      for (int k = 0; k < names.size(); k++) {
        if (name == names.get(k)) {
            return k;
        }
      }
      return -1;
    }


    @Override
    public void removeName(int index){
        names.remove(index);
        workbook.removeName(index);
    }

    /**
     * Returns the instance of HSSFDataFormat for this workbook.
     * @return the HSSFDataFormat object
     * @see com.moparisthebest.poi.hssf.record.FormatRecord
     * @see com.moparisthebest.poi.hssf.record.Record
     */
    @Override
    public HSSFDataFormat createDataFormat() {
    if (formatter == null) {
        formatter = new HSSFDataFormat(workbook);
    }
    return formatter;
    }


    @Override
    public void removeName(String name) {
        int index = getNameIndex(name);
        removeName(index);
    }


    /**
     * As {@link #removeName(String)} is not necessarily unique
     * (name + sheet index is unique), this method is more accurate.
     *
     * @param name the name to remove.
     */
    @Override
    public void removeName(Name name) {
      int index = getNameIndex((HSSFName) name);
      removeName(index);
    }

    public HSSFPalette getCustomPalette()
    {
        return new HSSFPalette(workbook.getCustomPalette());
    }

    /** Test only. Do not use */
    public void insertChartRecord()
    {
        int loc = workbook.findFirstRecordLocBySid(SSTRecord.sid);
        byte[] data = {
           (byte)0x0F, (byte)0x00, (byte)0x00, (byte)0xF0, (byte)0x52,
           (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
           (byte)0x06, (byte)0xF0, (byte)0x18, (byte)0x00, (byte)0x00,
           (byte)0x00, (byte)0x01, (byte)0x08, (byte)0x00, (byte)0x00,
           (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02,
           (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00,
           (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00,
           (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
           (byte)0x33, (byte)0x00, (byte)0x0B, (byte)0xF0, (byte)0x12,
           (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xBF, (byte)0x00,
           (byte)0x08, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x81,
           (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x00, (byte)0x08,
           (byte)0xC0, (byte)0x01, (byte)0x40, (byte)0x00, (byte)0x00,
           (byte)0x08, (byte)0x40, (byte)0x00, (byte)0x1E, (byte)0xF1,
           (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D,
           (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x0C, (byte)0x00,
           (byte)0x00, (byte)0x08, (byte)0x17, (byte)0x00, (byte)0x00,
           (byte)0x08, (byte)0xF7, (byte)0x00, (byte)0x00, (byte)0x10,
        };
        UnknownRecord r = new UnknownRecord((short)0x00EB, data);
        workbook.getRecords().add(loc, r);
    }

    /**
     * Spits out a list of all the drawing records in the workbook.
     */
    public void dumpDrawingGroupRecords(boolean fat)
    {
        DrawingGroupRecord r = (DrawingGroupRecord) workbook.findFirstRecordBySid( DrawingGroupRecord.sid );
        r.decode();
        List<EscherRecord> escherRecords = r.getEscherRecords();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
        for (EscherRecord escherRecord : escherRecords) {
            if (fat) {
                System.out.println(escherRecord.toString());
            } else {
                escherRecord.display(w, 0);
            }
        }
        w.flush();
    }

    void initDrawings(){
        DrawingManager2 mgr = workbook.findDrawingGroup();
        if(mgr != null) {
            for(HSSFSheet sh : _sheets)  {
                sh.getDrawingPatriarch();
            }
        } else {
            workbook.createDrawingGroup();
        }
    }

    /**
     * Gets all pictures from the Workbook.
     *
     * @return the list of pictures (a list of {@link HSSFPictureData} objects.)
     */
    @Override
    public List<HSSFPictureData> getAllPictures()
    {
        // The drawing group record always exists at the top level, so we won't need to do this recursively.
        List<HSSFPictureData> pictures = new ArrayList<HSSFPictureData>();
        for (Record r : workbook.getRecords()) {
            if (r instanceof AbstractEscherHolderRecord) {
                ((AbstractEscherHolderRecord) r).decode();
                List<EscherRecord> escherRecords = ((AbstractEscherHolderRecord) r).getEscherRecords();
                searchForPictures(escherRecords, pictures);
            }
        }
        return Collections.unmodifiableList(pictures);
    }

    /**
     * Performs a recursive search for pictures in the given list of escher records.
     *
     * @param escherRecords the escher records.
     * @param pictures the list to populate with the pictures.
     */
    private void searchForPictures(List<EscherRecord> escherRecords, List<HSSFPictureData> pictures)
    {
        for(EscherRecord escherRecord : escherRecords) {

            if (escherRecord instanceof EscherBSERecord)
            {
                EscherBlipRecord blip = ((EscherBSERecord) escherRecord).getBlipRecord();
                if (blip != null)
                {
                    // TODO: Some kind of structure.
                    HSSFPictureData picture = new HSSFPictureData(blip);
					pictures.add(picture);
                }


            }

            // Recursive call.
            searchForPictures(escherRecord.getChildRecords(), pictures);
        }

    }

    protected static Map<String,ClassID> getOleMap() {
    	Map<String,ClassID> olemap = new HashMap<String,ClassID>();
    	olemap.put("PowerPoint Document", ClassID.PPT_SHOW);
    	for (String str : WORKBOOK_DIR_ENTRY_NAMES) {
    		olemap.put(str, ClassID.XLS_WORKBOOK);
    	}
    	// ... to be continued
    	return olemap;
    }

    /**
     * Adds the LinkTable records required to allow formulas referencing
     *  the specified external workbook to be added to this one. Allows
     *  formulas such as "[MyOtherWorkbook]Sheet3!$A$5" to be added to the
     *  file, for workbooks not already referenced.
     *
     * @param name The name the workbook will be referenced as in formulas
     * @param workbook The open workbook to fetch the link required information from
     */
    @Override
    public int linkExternalWorkbook(String name, Workbook workbook) {
        return this.workbook.linkExternalWorkbook(name, workbook);
    }

    /**
     * Is the workbook protected with a password (not encrypted)?
     */
    public boolean isWriteProtected() {
        return this.workbook.isWriteProtected();
    }

    /**
     * protect a workbook with a password (not encypted, just sets writeprotect
     * flags and the password.
     * @param password to set
     */
    public void writeProtectWorkbook( String password, String username ) {
       this.workbook.writeProtectWorkbook(password, username);
    }

    /**
     * removes the write protect flag
     */
    public void unwriteProtectWorkbook() {
       this.workbook.unwriteProtectWorkbook();
    }

    /**
     * Gets all embedded OLE2 objects from the Workbook.
     *
     * @return the list of embedded objects (a list of {@link HSSFObjectData} objects.)
     */
    public List<HSSFObjectData> getAllEmbeddedObjects()
    {
        List<HSSFObjectData> objects = new ArrayList<HSSFObjectData>();
        for (HSSFSheet sheet : _sheets)
        {
            getAllEmbeddedObjects(sheet, objects);
        }
        return Collections.unmodifiableList(objects);
    }

    /**
     * Gets all embedded OLE2 objects from the Workbook.
     *
     * @param sheet embedded object attached to
     * @param objects the list of embedded objects to populate.
     */
    private void getAllEmbeddedObjects(HSSFSheet sheet, List<HSSFObjectData> objects)
    {
        HSSFPatriarch patriarch = sheet.getDrawingPatriarch();
        if (null == patriarch){
            return;
        }
        getAllEmbeddedObjects(patriarch, objects);
    }
    /**
     * Recursively iterates a shape container to get all embedded objects.
     *
     * @param parent the parent.
     * @param objects the list of embedded objects to populate.
     */
    private void getAllEmbeddedObjects(HSSFShapeContainer parent, List<HSSFObjectData> objects)
    {
        for (HSSFShape shape : parent.getChildren()) {
            if (shape instanceof HSSFObjectData) {
                objects.add((HSSFObjectData) shape);
            } else if (shape instanceof HSSFShapeContainer) {
                getAllEmbeddedObjects((HSSFShapeContainer) shape, objects);
            }
        }
    }
    @Override
    public HSSFCreationHelper getCreationHelper() {
        return new HSSFCreationHelper(this);
    }

    /**
     *
     * Returns the locator of user-defined functions.
     * The default instance extends the built-in functions with the Analysis Tool Pack
     *
     * @return the locator of user-defined functions
     */
    /*package*/ UDFFinder getUDFFinder(){
        return _udfFinder;
    }

    /**
     * Register a new toolpack in this workbook.
     *
     * @param toopack the toolpack to register
     */
    @Override
    public void addToolPack(UDFFinder toopack){
        AggregatingUDFFinder udfs = (AggregatingUDFFinder)_udfFinder;
        udfs.add(toopack);
    }

    /**
     * Whether the application shall perform a full recalculation when the workbook is opened.
     * <p>
     * Typically you want to force formula recalculation when you modify cell formulas or values
     * of a workbook previously created by Excel. When set to true, this flag will tell Excel
     * that it needs to recalculate all formulas in the workbook the next time the file is opened.
     * </p>
     * <p>
     * Note, that recalculation updates cached formula results and, thus, modifies the workbook.
     * Depending on the version, Excel may prompt you with "Do you want to save the changes in <em>filename</em>?"
     * on close.
     * </p>
     *
     * @param value true if the application will perform a full recalculation of
     * workbook values when the workbook is opened
     * @since 3.8
     */
    @Override
    public void setForceFormulaRecalculation(boolean value){
        InternalWorkbook iwb = getWorkbook();
        RecalcIdRecord recalc = iwb.getRecalcId();
        recalc.setEngineId(0);
    }

    /**
     * Whether Excel will be asked to recalculate all formulas when the  workbook is opened.
     *
     * @since 3.8
     */
    @Override
    public boolean getForceFormulaRecalculation(){
        InternalWorkbook iwb = getWorkbook();
        RecalcIdRecord recalc = (RecalcIdRecord)iwb.findFirstRecordBySid(RecalcIdRecord.sid);
        return recalc != null && recalc.getEngineId() != 0;
    }

    /**
     * Whether a call to {@link HSSFCell#setCellFormula(String)} will validate the formula or not.
     *
     * @param value true if the application will validate the formula is correct
     * @since 3.17
     */
    @Override
    public void setCellFormulaValidation(final boolean value) {
        // currently {@link HSSFCell#setCellFormula(String)} does no validation anyway, ignore
    }

    /**
     * Whether a call to {@link HSSFCell#setCellFormula(String)} will validate the formula or not.
     *
     * @since 3.17
     */
    @Override
    public boolean getCellFormulaValidation() {
        return false;
    }

	/**
	 * Changes an external referenced file to another file.
	 * A formula in Excel which references a cell in another file is saved in two parts:
	 * The referenced file is stored in an reference table. the row/cell information is saved separate.
	 * This method invocation will only change the reference in the lookup-table itself.
	 * @param oldUrl The old URL to search for and which is to be replaced
	 * @param newUrl The URL replacement
	 * @return true if the oldUrl was found and replaced with newUrl. Otherwise false
	 */
    public boolean changeExternalReference(String oldUrl, String newUrl) {
    	return workbook.changeExternalReference(oldUrl, newUrl);
    }
    
    @Internal
    public InternalWorkbook getInternalWorkbook() {
        return workbook;
    }
    
    /**
     * Returns the spreadsheet version (EXCLE97) of this workbook
     * 
     * @return EXCEL97 SpreadsheetVersion enum
     * @since 3.14 beta 2
     */
    @Override
    public SpreadsheetVersion getSpreadsheetVersion() {
        return this.spreadsheetVersion;
    }
}
