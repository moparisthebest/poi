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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.moparisthebest.poi.ddf.EscherComplexProperty;
import com.moparisthebest.poi.ddf.EscherContainerRecord;
import com.moparisthebest.poi.ddf.EscherDgRecord;
import com.moparisthebest.poi.ddf.EscherOptRecord;
import com.moparisthebest.poi.ddf.EscherProperty;
import com.moparisthebest.poi.ddf.EscherSpRecord;
import com.moparisthebest.poi.ddf.EscherSpgrRecord;
import com.moparisthebest.poi.hssf.model.DrawingManager2;
import com.moparisthebest.poi.hssf.record.CommonObjectDataSubRecord;
import com.moparisthebest.poi.hssf.record.EmbeddedObjectRefSubRecord;
import com.moparisthebest.poi.hssf.record.EndSubRecord;
import com.moparisthebest.poi.hssf.record.EscherAggregate;
import com.moparisthebest.poi.hssf.record.FtCfSubRecord;
import com.moparisthebest.poi.hssf.record.FtPioGrbitSubRecord;
import com.moparisthebest.poi.hssf.record.NoteRecord;
import com.moparisthebest.poi.hssf.record.ObjRecord;
import com.moparisthebest.poi.hssf.util.CellReference;
import com.moparisthebest.poi.poifs.filesystem.DirectoryEntry;
import com.moparisthebest.poi.poifs.filesystem.DirectoryNode;
import com.moparisthebest.poi.ss.usermodel.Chart;
import com.moparisthebest.poi.ss.usermodel.ClientAnchor;
import com.moparisthebest.poi.ss.usermodel.Drawing;
import com.moparisthebest.poi.ss.usermodel.Workbook;
import com.moparisthebest.poi.util.HexDump;
import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.NotImplemented;
import com.moparisthebest.poi.util.StringUtil;

/**
 * The patriarch is the toplevel container for shapes in a sheet.  It does
 * little other than act as a container for other shapes and groups.
 */
public final class HSSFPatriarch implements HSSFShapeContainer, Drawing<HSSFShape> {
    // private static POILogger log = POILogFactory.getLogger(HSSFPatriarch.class);
    private final List<HSSFShape> _shapes = new ArrayList<HSSFShape>();

    private final EscherSpgrRecord _spgrRecord;
    private final EscherContainerRecord _mainSpgrContainer;

    /**
     * The EscherAggregate we have been bound to.
     * (This will handle writing us out into records,
     * and building up our shapes from the records)
     */
    private EscherAggregate _boundAggregate;
    private final HSSFSheet _sheet;

    /**
     * Creates the patriarch.
     *
     * @param sheet the sheet this patriarch is stored in.
     * @param boundAggregate -low level representation of all binary data inside sheet
     */
    HSSFPatriarch(HSSFSheet sheet, EscherAggregate boundAggregate) {
        throw new IllegalStateException("support for this has been removed in poi-fast-calc");
    }

    /**
     * used to clone patriarch
     *
     * create patriarch from existing one
     * @param patriarch - copy all the shapes from this patriarch to new one
     * @param sheet where must be located new patriarch
     * @return new patriarch with copies of all shapes from the existing patriarch
     */
    static HSSFPatriarch createPatriarch(HSSFPatriarch patriarch, HSSFSheet sheet){
        HSSFPatriarch newPatriarch = new HSSFPatriarch(sheet, new EscherAggregate(true));
        newPatriarch.afterCreate();
        for (HSSFShape shape: patriarch.getChildren()){
            HSSFShape newShape;
            if (shape instanceof HSSFShapeGroup){
                newShape = ((HSSFShapeGroup)shape).cloneShape(newPatriarch);
            } else {
                newShape = shape.cloneShape();
            }
            newPatriarch.onCreate(newShape);
            newPatriarch.addShape(newShape);
        }
        return newPatriarch;
    }

    /**
     * check if any shapes contain wrong data
     * At now(13.08.2010) check if patriarch contains 2 or more comments with same coordinates
     */
    protected void preSerialize(){
        Map<Integer, NoteRecord> tailRecords = _boundAggregate.getTailRecords();
        /**
         * contains coordinates of comments we iterate over
         */
        Set<String> coordinates = new HashSet<String>(tailRecords.size());
        for(NoteRecord rec : tailRecords.values()){
            String noteRef = new CellReference(rec.getRow(),
                    rec.getColumn()).formatAsString(); // A1-style notation
            if(coordinates.contains(noteRef )){
                throw new IllegalStateException("found multiple cell comments for cell " + noteRef );
            } else {
                coordinates.add(noteRef);
            }
        }
    }

    /**
     * @param shape to be removed
     * @return true of shape is removed
     */
    @Override
    public boolean removeShape(HSSFShape shape) {
        boolean  isRemoved = _mainSpgrContainer.removeChildRecord(shape.getEscherContainer());
        if (isRemoved){
            shape.afterRemove(this);
            _shapes.remove(shape);
        }
        return isRemoved;
    }

    void afterCreate() {
        DrawingManager2 drawingManager = _sheet.getWorkbook().getWorkbook().getDrawingManager();
        short dgId = drawingManager.findNewDrawingGroupId();
        _boundAggregate.setDgId(dgId);
        _boundAggregate.setMainSpRecordId(newShapeId());
        drawingManager.incrementDrawingsSaved();
    }

    /**
     * Creates a new group record stored under this patriarch.
     *
     * @param anchor the client anchor describes how this group is attached
     *               to the sheet.
     * @return the newly created group.
     */
    public HSSFShapeGroup createGroup(HSSFClientAnchor anchor) {
        HSSFShapeGroup group = new HSSFShapeGroup(null, anchor);
        addShape(group);
        onCreate(group);
        return group;
    }

    /**
     * Creates a simple shape.  This includes such shapes as lines, rectangles,
     * and ovals.
     * 
     * Note: Microsoft Excel seems to sometimes disallow 
     * higher y1 than y2 or higher x1 than x2 in the anchor, you might need to 
     * reverse them and draw shapes vertically or horizontally flipped! 
     *
     * @param anchor the client anchor describes how this group is attached
     *               to the sheet.
     * @return the newly created shape.
     */
    public HSSFSimpleShape createSimpleShape(HSSFClientAnchor anchor) {
        HSSFSimpleShape shape = new HSSFSimpleShape(null, anchor);
        addShape(shape);
        //open existing file
        onCreate(shape);
        return shape;
    }

    /**
     * Creates a picture.
     *
     * @param anchor the client anchor describes how this group is attached
     *               to the sheet.
     * @param pictureIndex - pointer to the byte array saved inside workbook in escher bse record
     * @return the newly created shape.
     */
    public HSSFPicture createPicture(HSSFClientAnchor anchor, int pictureIndex) {
        HSSFPicture shape = new HSSFPicture(null, anchor);
        shape.setPictureIndex(pictureIndex);
        addShape(shape);
        //open existing file
        onCreate(shape);
        return shape;
    }

    /**
     *
     * @param anchor       the client anchor describes how this picture is
     *                     attached to the sheet.
     * @param pictureIndex the index of the picture in the workbook collection
     *                     of pictures.
     *
     * @return newly created shape
     */
    @Override
    public HSSFPicture createPicture(ClientAnchor anchor, int pictureIndex) {
        return createPicture((HSSFClientAnchor) anchor, pictureIndex);
    }

    @Override
    public HSSFObjectData createObjectData(ClientAnchor anchor, int storageId, int pictureIndex) {
        throw new IllegalStateException("support for this has been removed in poi-fast-calc");
    }
    
    /**
     * Creates a polygon
     *
     * @param anchor the client anchor describes how this group is attached
     *               to the sheet.
     * @return the newly created shape.
     */
    public HSSFPolygon createPolygon(HSSFClientAnchor anchor) {
        HSSFPolygon shape = new HSSFPolygon(null, anchor);
        addShape(shape);
        onCreate(shape);
        return shape;
    }

    /**
     * Constructs a textbox under the patriarch.
     *
     * @param anchor the client anchor describes how this group is attached
     *               to the sheet.
     * @return the newly created textbox.
     */
    public HSSFTextbox createTextbox(HSSFClientAnchor anchor) {
        HSSFTextbox shape = new HSSFTextbox(null, anchor);
        addShape(shape);
        onCreate(shape);
        return shape;
    }

    /**
     * Constructs a cell comment.
     *
     * @param anchor the client anchor describes how this comment is attached
     *               to the sheet.
     * @return the newly created comment.
     */
    public HSSFComment createComment(HSSFAnchor anchor) {
        HSSFComment shape = new HSSFComment(null, anchor);
        addShape(shape);
        onCreate(shape);
        return shape;
    }

    /**
     * YK: used to create autofilters
     *
     * @see com.moparisthebest.poi.hssf.usermodel.HSSFSheet#setAutoFilter(com.moparisthebest.poi.ss.util.CellRangeAddress)
     */
    HSSFSimpleShape createComboBox(HSSFAnchor anchor) {
        HSSFCombobox shape = new HSSFCombobox(null, anchor);
        addShape(shape);
        onCreate(shape);
        return shape;
    }

    @Override
    public HSSFComment createCellComment(ClientAnchor anchor) {
        return createComment((HSSFAnchor) anchor);
    }

    /**
     * Returns a unmodifiable list of all shapes contained by the patriarch.
     */
    @Override
    public List<HSSFShape> getChildren() {
        return Collections.unmodifiableList(_shapes);
    }

    /**
     * add a shape to this drawing
     */
    @Override
    @Internal
    public void addShape(HSSFShape shape) {
        shape.setPatriarch(this);
        _shapes.add(shape);
    }

    private void onCreate(HSSFShape shape) {
        EscherContainerRecord spgrContainer =
                _boundAggregate.getEscherContainer().getChildContainers().get(0);

        EscherContainerRecord spContainer = shape.getEscherContainer();
        int shapeId = newShapeId();
        shape.setShapeId(shapeId);

        spgrContainer.addChildRecord(spContainer);
        shape.afterInsert(this);
        setFlipFlags(shape);
    }

    /**
     * Total count of all children and their children's children.
     * @return count of shapes including shapes inside shape groups
     */
    public int countOfAllChildren() {
        int count = _shapes.size();
        for (Iterator<HSSFShape> iterator = _shapes.iterator(); iterator.hasNext(); ) {
            HSSFShape shape = iterator.next();
            count += shape.countOfAllChildren();
        }
        return count;
    }

    /**
     * Sets the coordinate space of this group.  All children are constrained
     * to these coordinates.
     */
    @Override
    public void setCoordinates(int x1, int y1, int x2, int y2) {
        _spgrRecord.setRectY1(y1);
        _spgrRecord.setRectY2(y2);
        _spgrRecord.setRectX1(x1);
        _spgrRecord.setRectX2(x2);
    }

    /**
     * remove all shapes inside patriarch
     */
    @Override
    public void clear() {
        ArrayList <HSSFShape> copy = new ArrayList<HSSFShape>(_shapes);
        for (HSSFShape shape: copy){
            removeShape(shape);
        }
    }

    /**
     * @return new unique shapeId
     */
    int newShapeId() {
        DrawingManager2 dm = _sheet.getWorkbook().getWorkbook().getDrawingManager();
        EscherDgRecord dg =
                _boundAggregate.getEscherContainer().getChildById(EscherDgRecord.RECORD_ID);
        short drawingGroupId = dg.getDrawingGroupId();
        return dm.allocateShapeId(drawingGroupId, dg);
    }

    /**
     * Does this HSSFPatriarch contain a chart?
     * (Technically a reference to a chart, since they
     * get stored in a different block of records)
     * FIXME - detect chart in all cases (only seems
     * to work on some charts so far)
     */
    public boolean containsChart() {
        // TODO - support charts properly in usermodel

        // We're looking for a EscherOptRecord
        EscherOptRecord optRecord = (EscherOptRecord)
                _boundAggregate.findFirstWithId(EscherOptRecord.RECORD_ID);
        if (optRecord == null) {
            // No opt record, can't have chart
            return false;
        }

        for (Iterator<EscherProperty> it = optRecord.getEscherProperties().iterator(); it.hasNext(); ) {
            EscherProperty prop = it.next();
            if (prop.getPropertyNumber() == 896 && prop.isComplex()) {
                EscherComplexProperty cp = (EscherComplexProperty) prop;
                String str = StringUtil.getFromUnicodeLE(cp.getComplexData());

                if (str.equals("Chart 1\0")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return x coordinate of the left up corner
     */
    @Override
    public int getX1() {
        return _spgrRecord.getRectX1();
    }

    /**
     * @return y coordinate of the left up corner
     */
    @Override
    public int getY1() {
        return _spgrRecord.getRectY1();
    }

    /**
     * @return x coordinate of the right down corner
     */
    @Override
    public int getX2() {
        return _spgrRecord.getRectX2();
    }

    /**
     * @return y coordinate of the right down corner
     */
    @Override
    public int getY2() {
        return _spgrRecord.getRectY2();
    }

    /**
     * Returns the aggregate escher record we're bound to
     * @return - low level representation of sheet drawing data
     */
    @Internal
    public EscherAggregate getBoundAggregate() {
        return _boundAggregate;
    }

    /**
     * Creates a new client anchor and sets the top-left and bottom-right
     * coordinates of the anchor.
     *
     * @param dx1  the x coordinate in EMU within the first cell.
     * @param dy1  the y coordinate in EMU within the first cell.
     * @param dx2  the x coordinate in EMU within the second cell.
     * @param dy2  the y coordinate in EMU within the second cell.
     * @param col1 the column (0 based) of the first cell.
     * @param row1 the row (0 based) of the first cell.
     * @param col2 the column (0 based) of the second cell.
     * @param row2 the row (0 based) of the second cell.
     * @return the newly created client anchor
     */
    @Override
    public HSSFClientAnchor createAnchor(int dx1, int dy1, int dx2, int dy2, int col1, int row1, int col2, int row2) {
        return new HSSFClientAnchor(dx1, dy1, dx2, dy2, (short) col1, row1, (short) col2, row2);
    }

    @Override
    @NotImplemented
    public Chart createChart(ClientAnchor anchor) {
        throw new RuntimeException("NotImplemented");
    }

    private void setFlipFlags(HSSFShape shape){
        EscherSpRecord sp = shape.getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
        if (shape.getAnchor().isHorizontallyFlipped()) {
            sp.setFlags(sp.getFlags() | EscherSpRecord.FLAG_FLIPHORIZ);
        }
        if (shape.getAnchor().isVerticallyFlipped()) {
            sp.setFlags(sp.getFlags() | EscherSpRecord.FLAG_FLIPVERT);
        }
    }

    @Override
    public Iterator<HSSFShape> iterator() {
        return _shapes.iterator();
    }

    protected HSSFSheet getSheet() {
        return _sheet;
    }
}
