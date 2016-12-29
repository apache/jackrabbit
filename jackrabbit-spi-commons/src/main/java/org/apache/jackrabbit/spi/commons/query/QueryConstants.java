/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.commons.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This interface defines constants for data types and operation types
 * used in queries.
 */
public interface QueryConstants {

    /**
     * long data type
     */
    int TYPE_LONG = 1;

    /**
     * Name of long data type
     */
    final String TYPE_NAME_LONG = "LONG";

    /**
     * double data type
     */
    int TYPE_DOUBLE = 2;

    /**
     * Name of double data type
     */
    final String TYPE_NAME_DOUBLE = "DOUBLE";

    /**
     * string data type
     */
    int TYPE_STRING = 3;

    /**
     * Name of string data type
     */
    final String TYPE_NAME_STRING = "STRING";

    /**
     * date data type
     */
    int TYPE_DATE = 4;

    /**
     * Name of date data type
     */
    final String TYPE_NAME_DATE = "DATE";

    /**
     * timestamp data type
     */
    int TYPE_TIMESTAMP = 5;

    /**
     * Name of timestamp data type
     */
    final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";

    /**
     * position index type
     */
    int TYPE_POSITION = 6;

    /**
     * Name of position index
     */
    final String TYPE_NAME_POSITION = "POS";

    /**
     * Name for unknown data types
     */
    final String TYPE_NAME_UNKNOWN = "UNKNOWN TYPE";


    int OPERATIONS = 10;

    /**
     * equal operation: eq
     */
    int OPERATION_EQ_VALUE = OPERATIONS + 1;

    /**
     * Name of equal operation
     */
    final String OP_NAME_EQ_VALUE = "eq";

    /**
     * equal operation: =
     * general comparison
     */
    int OPERATION_EQ_GENERAL = OPERATION_EQ_VALUE + 1;

    /**
     * Name of equal operation (general comparison)
     */
    final String OP_NAME_EQ_GENERAL = "=";

    /**
     * not equal operation: ne
     */
    int OPERATION_NE_VALUE = OPERATION_EQ_GENERAL + 1;

    /**
     * Name of not equal operation
     */
    final String OP_NAME_NE_VALUE = "ne";

    /**
     * not equal operation: &lt;&gt;
     * general comparison
     */
    int OPERATION_NE_GENERAL = OPERATION_NE_VALUE + 1;

    /**
     * Name of not equal operation (general comparison)
     */
    final String OP_NAME_NE_GENERAL = "<>";

    /**
     * less than operation: lt
     */
    int OPERATION_LT_VALUE = OPERATION_NE_GENERAL + 1;

    /**
     * Name of less than operation
     */
    final String OP_NAME_LT_VALUE = "lt";

    /**
     * less than operation: &lt;
     * general comparison
     */
    int OPERATION_LT_GENERAL = OPERATION_LT_VALUE + 1;

    /**
     * Name of less than operation (general comparison)
     */
    final String OP_NAME_LT_GENERAL = "<";

    /**
     * greater than operation: gt
     */
    int OPERATION_GT_VALUE = OPERATION_LT_GENERAL + 1;

    /**
     * Name o^f greater than operation
     */
    final String OP_NAME_GT_VALUE = "gt";

    /**
     * greater than operation: &gt;
     * general comparison
     */
    int OPERATION_GT_GENERAL = OPERATION_GT_VALUE + 1;

    /**
     * Name of greater than operation (general comparison)
     */
    final String OP_NAME_GT_GENERAL = ">";

    /**
     * greater or equal operation: ge
     */
    int OPERATION_GE_VALUE = OPERATION_GT_GENERAL + 1;

    /**
     * Name of greater or equal operation
     */
    final String OP_NAME_GE_VALUE = "ge";

    /**
     * greater or equal operation: &gt;=
     * general comparison
     */
    int OPERATION_GE_GENERAL = OPERATION_GE_VALUE + 1;

    /**
     * Name of greater or equal operation (general comparison)
     */
    final String OP_NAME_GE_GENERAL = ">=";

    /**
     * less than or equal operation: le
     */
    int OPERATION_LE_VALUE = OPERATION_GE_GENERAL + 1;

    /**
     * Name of less than or equal operation
     */
    final String OP_NAME_LE_VALUE = "le";

    /**
     * less than or equal operation: &lt;=
     * general comparison
     */
    int OPERATION_LE_GENERAL = OPERATION_LE_VALUE + 1;

    /**
     * Name of less than or equal operation (general comparison)
     */
    final String OP_NAME_LE_GENERAL = "<=";

    /**
     * like operation: identifier LIKE string_literal
     */
    int OPERATION_LIKE = OPERATION_LE_GENERAL + 1;

    /**
     * Name of like operation
     */
    final String OP_NAME_LIKE = "LIKE";

    /**
     * between operation: identifier [ NOT ] BETWEEN literal AND literal
     */
    int OPERATION_BETWEEN = OPERATION_LIKE + 1;

    /**
     * Name of between operation
     */
    final String OP_NAME_BETWEEN = "BETWEEN";

    /**
     * in operation: identifier [ NOT ] IN ( literal {, literal}* )
     */
    int OPERATION_IN = OPERATION_BETWEEN + 1;

    /**
     * Name of in operation
     */
    final String OP_NAME_IN = "IN";

    /**
     * is null operation: identifier IS NULL
     */
    int OPERATION_NULL = OPERATION_IN + 1;

    /**
     * Name of is null operation
     */
    final String OP_NAME_NULL = "IS NULL";

    /**
     * is not null operation: identifier IS NOT NULL
     */
    int OPERATION_NOT_NULL = OPERATION_NULL + 1;

    /**
     * Name of is not null operation
     */
    final String OP_NAME_NOT_NULL = "NOT NULL";

    /**
     * similar operation:
     * XPath: rep:similar(path_string)
     * SQL: SIMILAR(path_string)
     */
    int OPERATION_SIMILAR = OPERATION_NOT_NULL + 1;

    /**
     * Name of similar operation
     */
    final String OP_NAME_SIMILAR = "similarity";

    /**
     * spellcheck operation:
     * XPath: rep:spellcheck(string_literal)
     * SQL: SPELLCHECK(string_literal)
     */
    int OPERATION_SPELLCHECK = OPERATION_SIMILAR + 1;

    /**
     * Name of spellcheck operation
     */
    final String OP_NAME_SPELLCHECK = "spellcheck";

    /**
     * Name of unknown operations
     */
    final String OP_NAME_UNKNOW = "UNKNOWN OPERATION";

    /**
     * Operation names
     */
    final ConstantNameProvider OPERATION_NAMES = new ConstantNameProvider() {
        private final Map operationNames;

        {
            Map map = new HashMap();
            map.put(new Integer(OPERATION_BETWEEN), OP_NAME_BETWEEN);
            map.put(new Integer(OPERATION_EQ_VALUE), OP_NAME_EQ_VALUE);
            map.put(new Integer(OPERATION_EQ_GENERAL), OP_NAME_EQ_GENERAL);
            map.put(new Integer(OPERATION_GE_GENERAL), OP_NAME_GE_GENERAL);
            map.put(new Integer(OPERATION_GE_VALUE), OP_NAME_GE_VALUE);
            map.put(new Integer(OPERATION_GT_GENERAL), OP_NAME_GT_GENERAL);
            map.put(new Integer(OPERATION_GT_VALUE), OP_NAME_GT_VALUE);
            map.put(new Integer(OPERATION_IN), OP_NAME_IN);
            map.put(new Integer(OPERATION_LE_GENERAL), OP_NAME_LE_GENERAL);
            map.put(new Integer(OPERATION_LE_VALUE), OP_NAME_LE_VALUE);
            map.put(new Integer(OPERATION_LIKE), OP_NAME_LIKE);
            map.put(new Integer(OPERATION_LT_GENERAL), OP_NAME_LT_GENERAL);
            map.put(new Integer(OPERATION_LT_VALUE), OP_NAME_LT_VALUE);
            map.put(new Integer(OPERATION_NE_GENERAL), OP_NAME_NE_GENERAL);
            map.put(new Integer(OPERATION_NE_VALUE), OP_NAME_NE_VALUE);
            map.put(new Integer(OPERATION_NOT_NULL), OP_NAME_NOT_NULL);
            map.put(new Integer(OPERATION_NULL), OP_NAME_NULL);
            map.put(new Integer(OPERATION_SIMILAR), OP_NAME_SIMILAR);
            map.put(new Integer(OPERATION_SPELLCHECK), OP_NAME_SPELLCHECK);
            operationNames = Collections.unmodifiableMap(map);
        }

        public String getName(int constant) {
            String name = (String) operationNames.get(new Integer(constant));
            return name == null? OP_NAME_UNKNOW : name;
        }
    };

    /**
     * Type names
     */
    final ConstantNameProvider TYPE_NAMES = new ConstantNameProvider() {
        private final Map typeNames;

        {
            Map map = new HashMap();
            map.put(new Integer(TYPE_DATE), TYPE_NAME_DATE);
            map.put(new Integer(TYPE_DOUBLE), TYPE_NAME_DOUBLE);
            map.put(new Integer(TYPE_LONG), TYPE_NAME_LONG);
            map.put(new Integer(TYPE_POSITION), TYPE_NAME_POSITION);
            map.put(new Integer(TYPE_STRING), TYPE_NAME_STRING);
            map.put(new Integer(TYPE_TIMESTAMP), TYPE_NAME_TIMESTAMP);
            typeNames = Collections.unmodifiableMap(map);
        }

        public String getName(int constant) {
            String name = (String) typeNames.get(new Integer(constant));
            return name == null? TYPE_NAME_UNKNOWN : name;
        }
    };

}
