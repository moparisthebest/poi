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

package com.moparisthebest.poi.poifs.crypt.xor;

import com.moparisthebest.poi.poifs.crypt.EncryptionHeader;
import com.moparisthebest.poi.poifs.crypt.standard.EncryptionRecord;
import com.moparisthebest.poi.util.LittleEndianByteArrayOutputStream;

public class XOREncryptionHeader extends EncryptionHeader implements EncryptionRecord, Cloneable {

    protected XOREncryptionHeader() {
    }

    @Override
    public void write(LittleEndianByteArrayOutputStream leos) {
    }

    @Override
    public XOREncryptionHeader clone() throws CloneNotSupportedException {
        return (XOREncryptionHeader)super.clone();
    }
}
