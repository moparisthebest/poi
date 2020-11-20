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

import com.moparisthebest.poi.common.usermodel.HyperlinkType;
import com.moparisthebest.poi.hssf.record.common.ExtendedColor;
import com.moparisthebest.poi.ss.usermodel.CreationHelper;
import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.Removal;

public class HSSFCreationHelper implements CreationHelper {
    private final HSSFWorkbook workbook;

    /**
     * Should only be called by {@link HSSFWorkbook#getCreationHelper()}
     *
     * @param wb the workbook to create objects for
     */
    @Internal(since="3.15 beta 3")
    /*package*/ HSSFCreationHelper(HSSFWorkbook wb) {
        workbook = wb;
    }

    @Override
    public HSSFRichTextString createRichTextString(String text) {
        return new HSSFRichTextString(text);
    }

    @Override
    public HSSFDataFormat createDataFormat() {
        return workbook.createDataFormat();
    }

    /**
     * {@inheritDoc}
     * @deprecated POI 3.15 beta 3. Use {@link #createHyperlink(HyperlinkType)} instead.
     */
    @Deprecated
    @Removal(version="3.17")
    @Override
    public HSSFHyperlink createHyperlink(int type) {
        return new HSSFHyperlink(type);
    }
    @Override
    public HSSFHyperlink createHyperlink(HyperlinkType type) {
        return new HSSFHyperlink(type);
    }

    @Override
    public HSSFExtendedColor createExtendedColor() {
        return new HSSFExtendedColor(new ExtendedColor());
    }

    /**
     * Creates a HSSFFormulaEvaluator, the object that evaluates formula cells.
     *
     * @return a HSSFFormulaEvaluator instance
     */
    @Override
    public HSSFFormulaEvaluator createFormulaEvaluator(){
        return new HSSFFormulaEvaluator(workbook);
    }

    /**
     * Creates a HSSFClientAnchor. Use this object to position drawing object in a sheet
     *
     * @return a HSSFClientAnchor instance
     * @see com.moparisthebest.poi.ss.usermodel.Drawing
     */
    @Override
    public HSSFClientAnchor createClientAnchor(){
        return new HSSFClientAnchor();
    }
}