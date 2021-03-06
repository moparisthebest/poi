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

package com.moparisthebest.poi.hssf.util;

import com.moparisthebest.poi.hssf.usermodel.HSSFSheet;
import com.moparisthebest.poi.hssf.usermodel.HSSFWorkbook;
import com.moparisthebest.poi.ss.util.RegionUtil;
import com.moparisthebest.poi.ss.util.CellRangeAddress;

/**
 * Various utility functions that make working with a region of cells easier.
 */
public final class HSSFRegionUtil {

	private HSSFRegionUtil() {
		// no instances of this class
	}

	/**
	 * Sets the left border for a region of cells by manipulating the cell style
	 * of the individual cells on the left
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderLeft(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderLeft(border, region, sheet, workbook);
	}

	/**
	 * Sets the leftBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setLeftBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setLeftBorderColor(color, region, sheet, workbook);
	}

	/**
	 * Sets the borderRight attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderRight(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderRight(border, region, sheet, workbook);
	}

	/**
	 * Sets the rightBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setRightBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setRightBorderColor(color, region, sheet, workbook);
	}

	/**
	 * Sets the borderBottom attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderBottom(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderBottom(border, region, sheet, workbook);
	}

	/**
	 * Sets the bottomBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBottomBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBottomBorderColor(color, region, sheet, workbook);
	}

	/**
	 * Sets the borderBottom attribute of the HSSFRegionUtil object
	 *
	 * @param border The new border
	 * @param region The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setBorderTop(int border, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setBorderTop(border, region, sheet, workbook);
	}

	/**
	 * Sets the topBorderColor attribute of the HSSFRegionUtil object
	 *
	 * @param color The color of the border
	 * @param region  The region that should have the border
	 * @param workbook The workbook that the region is on.
	 * @param sheet The sheet that the region is on.
	 */
	public static void setTopBorderColor(int color, CellRangeAddress region, HSSFSheet sheet,
			HSSFWorkbook workbook) {
		RegionUtil.setTopBorderColor(color, region, sheet, workbook);
	}
}
