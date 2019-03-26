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

package com.moparisthebest.poi.ss.formula.ptg;

/**
 * Unary Plus operator
 * does not have any effect on the operand
 * @author Avik Sengupta
 */
public final class UnaryMinusPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x13;
    
    private final static String MINUS = "-";

    public static final ValueOperatorPtg instance = new UnaryMinusPtg();

    private UnaryMinusPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 1;
    }
    
   /** implementation of method from OperationsPtg*/  
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(MINUS);
        buffer.append(operands[ 0]);
        return buffer.toString();
    }
}
