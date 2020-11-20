/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package com.moparisthebest.poi.ss.usermodel;

import com.moparisthebest.poi.util.Beta;

/**
 * Enum mapping the values of STDataConsolidateFunction
 */
@Beta
public enum DataConsolidateFunction {
    AVERAGE(1,"Average"),
    COUNT(2, "Count"),
    COUNT_NUMS(3, "Count"),
    MAX(4, "Max"),
    MIN(5, "Min"),
    PRODUCT(6, "Product"),
    STD_DEV(7, "StdDev"),
    STD_DEVP(8, "StdDevp"),
    SUM(9, "Sum"),
    VAR(10, "Var"),
    VARP(11, "Varp");

    private final int value;
    private final String name;

    DataConsolidateFunction(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getValue() {
        return this.value;
    }
}