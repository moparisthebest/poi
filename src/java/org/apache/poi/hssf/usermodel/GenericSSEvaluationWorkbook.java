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

package org.apache.poi.hssf.usermodel;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.SpreadsheetVersion;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * This is a in-memory workbook only, with XSSF (xlsx) limits as defined by SpreadsheetVersion.EXCEL2007 which can be
 * used to perform formula evaluations much much faster than XSSFWorkbook.
 *
 * You cannot write this to the filesystem, all write functions throw an IllegalStateException.
 *
 * @see org.apache.poi.hssf.usermodel.HSSFWorkbook
 */
public class GenericSSEvaluationWorkbook extends HSSFWorkbook {

    public GenericSSEvaluationWorkbook() {
        super(SpreadsheetVersion.EXCEL2007);
    }

    @Override
    public void write() throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

    @Override
    public void write(File newFile) throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

    @Override
    protected void writeProperties() throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

    @Override
    protected void writeProperties(POIFSFileSystem outFS) throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

    @Override
    protected void writeProperties(POIFSFileSystem outFS, List<String> writtenEntries) throws IOException {
        throw new IllegalStateException("CalcEngineHSSFWorkbook cannot write, meant only for in-memory calculations");
    }

}
