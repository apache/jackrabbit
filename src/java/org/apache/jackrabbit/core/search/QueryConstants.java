/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;

/**
 * This interface defines constants for data types and operation types
 * used in queries.
 */
public interface QueryConstants {

    /**
     * QName for jcr:score
     */
    public static final QName JCR_SCORE = new QName(NamespaceRegistryImpl.NS_JCR_URI, "score");

    /**
     * QName for jcr:path
     */
    public static final QName JCR_PATH = new QName(NamespaceRegistryImpl.NS_JCR_URI, "path");

    /**
     * QName for jcr:statement
     */
    public static final QName JCR_STATEMENT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "statement");

    /**
     * QName for jcr:language
     */
    public static final QName JCR_LANGUAGE = new QName(NamespaceRegistryImpl.NS_JCR_URI, "language");

    /**
     * long data type
     */
    public static int TYPE_LONG = 1;

    /**
     * double data type
     */
    public static int TYPE_DOUBLE = 2;

    /**
     * string data type
     */
    public static int TYPE_STRING = 3;

    /**
     * date data type
     */
    public static int TYPE_DATE = 4;

    /**
     * timestamp data type
     */
    public static int TYPE_TIMESTAMP = 5;

    /**
     * position index type
     */
    public static int TYPE_POSITION = 6;

    public static int OPERATIONS = 10;

    /**
     * equal operation: =
     */
    public static int OPERATION_EQ_VALUE = OPERATIONS + 1;

    /**
     * equal operation: =
     * general comparison
     */
    public static int OPERATION_EQ_GENERAL = OPERATION_EQ_VALUE + 1;

    /**
     * not equal operation: <>
     */
    public static int OPERATION_NE_VALUE = OPERATION_EQ_GENERAL + 1;

    /**
     * not equal operation: <>
     * general comparision
     */
    public static int OPERATION_NE_GENERAL = OPERATION_NE_VALUE + 1;

    /**
     * less than operation: &lt;
     */
    public static int OPERATION_LT_VALUE = OPERATION_NE_GENERAL + 1;

    /**
     * less than operation: &lt;
     * general comparison
     */
    public static int OPERATION_LT_GENERAL = OPERATION_LT_VALUE + 1;

    /**
     * greater than operation: >
     */
    public static int OPERATION_GT_VALUE = OPERATION_LT_GENERAL + 1;

    /**
     * greater than operation: >
     * general comparision
     */
    public static int OPERATION_GT_GENERAL = OPERATION_GT_VALUE + 1;

    /**
     * greater or equal operation: >=
     */
    public static int OPERATION_GE_VALUE = OPERATION_GT_GENERAL + 1;

    /**
     * greater or equal operation: >=
     * general comparison
     */
    public static int OPERATION_GE_GENERAL = OPERATION_GE_VALUE + 1;

    /**
     * less than or equal operation: <=
     */
    public static int OPERATION_LE_VALUE = OPERATION_GE_GENERAL + 1;

    /**
     * less than or equal operation: <=
     * general comparison
     */
    public static int OPERATION_LE_GENERAL = OPERATION_LE_VALUE + 1;

    /**
     * like operation: identifier LIKE string_literal
     */
    public static int OPERATION_LIKE = OPERATION_LE_GENERAL + 1;

    /**
     * between operation: identifier [ NOT ] BETWEEN literal AND literal
     */
    public static int OPERATION_BETWEEN = OPERATION_LIKE + 1;

    /**
     * on operation: identifier [ NOT ] IN ( literal {, literal}* )
     */
    public static int OPERATION_IN = OPERATION_BETWEEN + 1;

    /**
     * is null operation: identifier IS NULL
     */
    public static int OPERATION_NULL = OPERATION_IN + 1;

    /**
     * is not null operation: identifier IS NOT NULL
     */
    public static int OPERATION_NOT_NULL = OPERATION_NULL + 1;
}
