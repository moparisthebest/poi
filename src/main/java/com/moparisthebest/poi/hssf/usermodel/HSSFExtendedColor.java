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

import static com.moparisthebest.poi.hssf.record.common.ExtendedColor.TYPE_AUTO;
import static com.moparisthebest.poi.hssf.record.common.ExtendedColor.TYPE_INDEXED;
import static com.moparisthebest.poi.hssf.record.common.ExtendedColor.TYPE_RGB;
import static com.moparisthebest.poi.hssf.record.common.ExtendedColor.TYPE_THEMED;

import com.moparisthebest.poi.ss.usermodel.ExtendedColor;

/**
 * The HSSF file format normally stores Color information in the
 *  Palette (see PaletteRecord), but for a few cases (eg Conditional
 *  Formatting, Sheet Extensions), this XSSF-style color record 
 *  can be used.
 */
public class HSSFExtendedColor extends ExtendedColor {
    private com.moparisthebest.poi.hssf.record.common.ExtendedColor color;
    
    public HSSFExtendedColor(com.moparisthebest.poi.hssf.record.common.ExtendedColor color) {
        this.color = color;
    }
    
    protected com.moparisthebest.poi.hssf.record.common.ExtendedColor getExtendedColor() {
        return color;
    }

    public boolean isAuto() {
        return color.getType() == TYPE_AUTO;
    }
    public boolean isIndexed() {
        return color.getType() == TYPE_INDEXED;
    }
    public boolean isRGB() {
        return color.getType() == TYPE_RGB;
    }
    public boolean isThemed() {
        return color.getType() == TYPE_THEMED;
    }

    public short getIndex() {
        return (short)color.getColorIndex();
    }
    public int getTheme() {
        return color.getThemeIndex();
    }

    public byte[] getRGB() {
        // Trim trailing A
        byte[] rgb = new byte[3];
        byte[] rgba = color.getRGBA();
        if (rgba == null) return null;
        System.arraycopy(rgba, 0, rgb, 0, 3);
        return rgb;
    }
    public byte[] getARGB() {
        // Swap from RGBA to ARGB
        byte[] argb = new byte[4];
        byte[] rgba = color.getRGBA();
        if (rgba == null) return null;
        System.arraycopy(rgba, 0, argb, 1, 3);
        argb[0] = rgba[3];
        return argb;
    }

    protected byte[] getStoredRBG() {
        return getARGB();
    }

    public void setRGB(byte[] rgb) {
        if (rgb.length == 3) {
            byte[] rgba = new byte[4];
            System.arraycopy(rgb, 0, rgba, 0, 3);
            rgba[3] = -1;
        } else {
            // Shuffle from ARGB to RGBA
            byte a = rgb[0];
            rgb[0] = rgb[1];
            rgb[1] = rgb[2];
            rgb[2] = rgb[3];
            rgb[3] = a;
            color.setRGBA(rgb);
        }
        color.setType(TYPE_RGB);
    }

    public double getTint() {
        return color.getTint();
    }
    public void setTint(double tint) {
        color.setTint(tint);
    }
}
