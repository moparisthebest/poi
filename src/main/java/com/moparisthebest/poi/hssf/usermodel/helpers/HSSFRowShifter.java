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

package com.moparisthebest.poi.hssf.usermodel.helpers;

import com.moparisthebest.poi.hssf.usermodel.HSSFSheet;
import com.moparisthebest.poi.ss.formula.FormulaShifter;
import com.moparisthebest.poi.ss.formula.eval.NotImplementedException;
import com.moparisthebest.poi.ss.usermodel.Row;
import com.moparisthebest.poi.ss.usermodel.helpers.RowShifter;
import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.NotImplemented;
import com.moparisthebest.poi.util.POILogFactory;
import com.moparisthebest.poi.util.POILogger;

/**
 * Helper for shifting rows up or down
 * 
 * When possible, code should be implemented in the RowShifter abstract class to avoid duplication with {@link com.moparisthebest.poi.xssf.usermodel.helpers.XSSFRowShifter}
 */
public final class HSSFRowShifter extends RowShifter {
    private static final POILogger logger = POILogFactory.getLogger(HSSFRowShifter.class);

    public HSSFRowShifter(HSSFSheet sh) {
        super(sh);
    }

    @NotImplemented
    public void updateNamedRanges(FormulaShifter shifter) {
        throw new NotImplementedException("HSSFRowShifter.updateNamedRanges");
    }

    @NotImplemented
    public void updateFormulas(FormulaShifter shifter) {
        throw new NotImplementedException("updateFormulas");
    }

    @Internal
    @NotImplemented
    public void updateRowFormulas(Row row, FormulaShifter shifter) {
        throw new NotImplementedException("updateRowFormulas");
    }

    @NotImplemented
    public void updateConditionalFormatting(FormulaShifter shifter) {
        throw new NotImplementedException("updateConditionalFormatting");
    }
    
    @NotImplemented
    public void updateHyperlinks(FormulaShifter shifter) {
        throw new NotImplementedException("updateHyperlinks");
    }

}
