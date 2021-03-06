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
package com.moparisthebest.poi.hpsf;

import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.LittleEndian;
import com.moparisthebest.poi.util.POILogFactory;
import com.moparisthebest.poi.util.POILogger;

@Internal
class VariantBool {
    private final static POILogger logger = POILogFactory.getLogger( VariantBool.class );

    static final int SIZE = 2;

    private boolean _value;

    VariantBool( byte[] data, int offset ) {
        short value = LittleEndian.getShort( data, offset );
        switch (value) {
            case 0:
                _value = false;
                break;
            case -1:
                _value = true;
                break;
            default:
                logger.log( POILogger.WARN, "VARIANT_BOOL value '"+value+"' is incorrect" );
                _value = true;
                break;
        }
    }

    boolean getValue() {
        return _value;
    }

    void setValue( boolean value ) {
        this._value = value;
    }
}
