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
package org.apache.jackrabbit.core.query;

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
     * double data type
     */
    int TYPE_DOUBLE = 2;

    /**
     * string data type
     */
    int TYPE_STRING = 3;

    /**
     * date data type
     */
    int TYPE_DATE = 4;

    /**
     * timestamp data type
     */
    int TYPE_TIMESTAMP = 5;

    /**
     * position index type
     */
    int TYPE_POSITION = 6;

    int OPERATIONS = 10;

    /**
     * equal operation: =
     */
    int OPERATION_EQ_VALUE = OPERATIONS + 1;

    /**
     * equal operation: =
     * general comparison
     */
    int OPERATION_EQ_GENERAL = OPERATION_EQ_VALUE + 1;

    /**
     * not equal operation: <>
     */
    int OPERATION_NE_VALUE = OPERATION_EQ_GENERAL + 1;

    /**
     * not equal operation: <>
     * general comparision
     */
    int OPERATION_NE_GENERAL = OPERATION_NE_VALUE + 1;

    /**
     * less than operation: &lt;
     */
    int OPERATION_LT_VALUE = OPERATION_NE_GENERAL + 1;

    /**
     * less than operation: &lt;
     * general comparison
     */
    int OPERATION_LT_GENERAL = OPERATION_LT_VALUE + 1;

    /**
     * greater than operation: >
     */
    int OPERATION_GT_VALUE = OPERATION_LT_GENERAL + 1;

    /**
     * greater than operation: >
     * general comparision
     */
    int OPERATION_GT_GENERAL = OPERATION_GT_VALUE + 1;

    /**
     * greater or equal operation: >=
     */
    int OPERATION_GE_VALUE = OPERATION_GT_GENERAL + 1;

    /**
     * greater or equal operation: >=
     * general comparison
     */
    int OPERATION_GE_GENERAL = OPERATION_GE_VALUE + 1;

    /**
     * less than or equal operation: <=
     */
    int OPERATION_LE_VALUE = OPERATION_GE_GENERAL + 1;

    /**
     * less than or equal operation: <=
     * general comparison
     */
    int OPERATION_LE_GENERAL = OPERATION_LE_VALUE + 1;

    /**
     * like operation: identifier LIKE string_literal
     */
    int OPERATION_LIKE = OPERATION_LE_GENERAL + 1;

    /**
     * between operation: identifier [ NOT ] BETWEEN literal AND literal
     */
    int OPERATION_BETWEEN = OPERATION_LIKE + 1;

    /**
     * on operation: identifier [ NOT ] IN ( literal {, literal}* )
     */
    int OPERATION_IN = OPERATION_BETWEEN + 1;

    /**
     * is null operation: identifier IS NULL
     */
    int OPERATION_NULL = OPERATION_IN + 1;

    /**
     * is not null operation: identifier IS NOT NULL
     */
    int OPERATION_NOT_NULL = OPERATION_NULL + 1;

    /**
     * similar operation:
     * XPath: rep:similar(path_string)
     * SQL: SIMILAR(path_string)
     */
    int OPERATION_SIMILAR = OPERATION_NOT_NULL + 1;

    /**
     * spellcheck operation:
     * XPath: rep:spellcheck(string_literal)
     * SQL: SPELLCHECK(string_literal)
     */
    int OPERATION_SPELLCHECK = OPERATION_SIMILAR + 1;
}
