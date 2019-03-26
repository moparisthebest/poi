//package org.apache.poi.hssf.usermodel.examples;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CalculationTest {
    public static void main(String[] args) {
        new CalculationTest().test();
    }
    
    @Test
    public void test() {
        runAssert(5000, 519.4700000000496);
        runAssert(20000, 2019.469999999259);
        runAssert(65536, 6573.070000006382);
        runAssert(65537, 6573.170000006382);
        runAssert(65538, 6573.270000006382);
        runAssert(65539, 6573.370000006383);
        runAssert(100000, 10019.470000018919);
        runAssert(SpreadsheetVersion.EXCEL2007.getLastRowIndex() - 5, 104876.47000161673);
        runAssert(SpreadsheetVersion.EXCEL2007.getLastRowIndex(), 104876.97000161676);
        runAssert(SpreadsheetVersion.EXCEL2007.getMaxRows(), 104877.07000161677);
        //runAssert(SpreadsheetVersion.EXCEL2007.getMaxRows() + 1, 104877.07000161677); // java.lang.IllegalArgumentException: Invalid row number (1048576) outside allowable range (0..1048575)
    }

    /*
    public static Workbook newXSSFWorkbookNoFormulaValidation() {
        final org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new XSSFWorkbook();
        wb.setCellFormulaValidation(false);
        return wb;
    }
    */

    public static void runAssert(final int maxRow, final double expected) {
        //runAssert(new org.apache.poi.hssf.usermodel.GenericSSEvaluationWorkbook(), maxRow, expected);
        runAssert(new org.apache.poi.hssf.usermodel.HSSFWorkbook(), maxRow, expected);
        if(maxRow <= SpreadsheetVersion.EXCEL97.getMaxRows())
            runAssert(new org.apache.poi.hssf.usermodel.HSSFWorkbook(SpreadsheetVersion.EXCEL97), maxRow, expected);
        //runAssert(newXSSFWorkbookNoFormulaValidation(), maxRow, expected);
    }

    public static void runAssert(final Workbook wb, final int maxRow, final double expected) {

        final FormulaEvaluator formulaEval = wb.getCreationHelper().createFormulaEvaluator();
        final Sheet sheet = wb.createSheet();
        int rowNum = -1, cellNum;
        Row row;
        Cell formulaCell;
        CellValue evaluatedCell = null;

        row = sheet.createRow(++rowNum);
        cellNum = -1;
        row.createCell(++cellNum).setCellValue("Product1");
        row.createCell(++cellNum).setCellValue(10.67);
        row.createCell(++cellNum).setCellValue(8);

        double price = 19.57;
        int quantity = 3;

        final int maxRows = maxRow - 1; // because we created 1 above

        final long start = System.currentTimeMillis();

        //rowNum = maxRow - 3;
        //for (int _x = maxRow - 3; _x < maxRows; ++_x) {
        for (int _x = 0; _x < maxRows; ++_x) {
            row = sheet.createRow(++rowNum);
            cellNum = -1;
            final int rowNumUse = rowNum + 1;
            row.createCell(++cellNum).setCellValue("Product" + rowNumUse);
            price += 0.1;
            ++quantity;
            row.createCell(++cellNum).setCellValue(price);
            row.createCell(++cellNum).setCellValue(quantity);

            formulaCell = row.createCell(++cellNum);
            //formulaCell.setCellFormula("VLOOKUP(\"Product" + rowNum + "\",$A:$C,2,0)");
            formulaCell.setCellFormula("VLOOKUP(\"Product" + rowNumUse + "\",A" + rowNumUse + ":C" + rowNumUse + ",2,0)");
            //formulaCell.setCellFormula("B" + rowNumUse);
            //System.out.println("formulaCell: " + formulaCell);
            
            evaluatedCell = formulaEval.evaluate(formulaCell);
            //System.out.println("evaluatedCell: " + evaluatedCell);
            if (evaluatedCell == null || evaluatedCell.getNumberValue() != price) {
                throw new AssertionError(String.format("Expected %f but got %s at rowNum: %d, rowNumUse: %d", price, evaluatedCell, rowNum, rowNumUse));
                //System.out.println(String.format("Expected %f but got %s at rowNum: %d, rowNumUse: %d", price, evaluatedCell, rowNum, rowNumUse));
                //break;
            }
        }

        //System.out.println("evaluatedCell: " + evaluatedCell);

        final long elapsed = System.currentTimeMillis() - start;
        System.out.printf("impl: %s, maxRow: %d, elapsed: %dms, %ds, %dmin%n", wb.getClass().getSimpleName(), maxRow, elapsed,
                elapsed / 1000, elapsed / 1000 / 60);

        if (evaluatedCell == null || evaluatedCell.getNumberValue() != expected) {
            throw new AssertionError("Expected " + expected + " but got " + evaluatedCell);
            //System.out.println("Expected " + expected + " but got " + evaluatedCell);
        }
        
        /*
        if(wb instanceof XSSFWorkbook) {
            try (FileOutputStream fos = new FileOutputStream("test.xlsx")) {
                wb.write(fos);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        */
    }
}
