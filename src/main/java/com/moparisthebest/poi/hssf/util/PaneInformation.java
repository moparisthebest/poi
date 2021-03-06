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

/**
 * Holds information regarding a split plane or freeze plane for a sheet.
 * @deprecated POI 3.15 beta 3. Use {@link com.moparisthebest.poi.ss.util.PaneInformation} instead.
 */
public class PaneInformation extends com.moparisthebest.poi.ss.util.PaneInformation
{
        public PaneInformation(short x, short y, short top, short left, byte active, boolean frozen) {
                super(x, y, top, left, active, frozen);
        }

}
