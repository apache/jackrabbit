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

/**
 * This interface defines constants for data types and operation types
 * used in queries.
 */
public interface Constants {

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
     * equal operation: =
     */
    public static int OPERATION_EQ = 10;

    /**
     * not equal operation: <>
     */
    public static int OPERATION_NE = 11;

    /**
     * less than operation: <
     */
    public static int OPERATION_LT = 12;

    /**
     * greater than operation: >
     */
    public static int OPERATION_GT = 13;

    /**
     * greater or equal operation: >=
     */
    public static int OPERATION_GE = 14;

    /**
     * less than or equal operation: <=
     */
    public static int OPERATION_LE = 15;

    /**
     * like operation: identifier LIKE string_literal
     */
    public static int OPERATION_LIKE = 16;

    /**
     * between operation: identifier [ NOT ] BETWEEN literal AND literal
     */
    public static int OPERATION_BETWEEN = 17;

    /**
     * on operation: identifier [ NOT ] IN ( literal {, literal}* )
     */
    public static int OPERATION_IN = 18;

    /**
     * is null operation: identifier IS [ NOT ] NULL
     */
    public static int OPERATION_NULL = 19;
}
