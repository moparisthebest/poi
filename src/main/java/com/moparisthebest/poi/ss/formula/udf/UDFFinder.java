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

package com.moparisthebest.poi.ss.formula.udf;

import com.moparisthebest.poi.ss.formula.atp.AnalysisToolPak;
import com.moparisthebest.poi.ss.formula.functions.FreeRefFunction;

/**
 * Common interface for "Add-in" libraries and user defined function libraries.
 */
public interface UDFFinder {
    // FIXME: Findbugs error: IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION
    /**
     * Default UDFFinder implementation
     * 
     * @deprecated use AggregatingUDFFinder.DEFAULT instead, deprecated in POI 3.15,
     *     scheduled for removable in POI 3.17 
     */
    @Deprecated
    public static final UDFFinder DEFAULT = new AggregatingUDFFinder(AnalysisToolPak.instance);

    /**
     * Returns executor by specified name. Returns <code>null</code> if the function name is unknown.
     *
     * @param name Name of function.
     * @return Function executor.
     */
    FreeRefFunction findFunction(String name);
}