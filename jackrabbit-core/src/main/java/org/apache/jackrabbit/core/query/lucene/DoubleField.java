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
package org.apache.jackrabbit.core.query.lucene;

/**
 * The <code>DoubleField</code> class is a utility to convert double
 * values into <code>String</code> values that are lexicographically ordered
 * according to the double value.
 */
public class DoubleField {

    private static final long SIGN_MASK = 0x8000000000000000L;

    private static final int STRING_DOUBLE_LEN
            = Long.toString(Long.MAX_VALUE, Character.MAX_RADIX).length() + 1;

    private DoubleField() {
    }

    public static String doubleToString(double value) {
        long longValue = Double.doubleToLongBits(value);
        StringBuffer sb = new StringBuffer(STRING_DOUBLE_LEN);

        if ((longValue & SIGN_MASK) == 0) {
            // positive
            String s = Long.toString(longValue, Character.MAX_RADIX);
            // add sign character
            sb.append('1');
            while ((sb.length() + s.length()) < STRING_DOUBLE_LEN) {
                sb.append('0');
            }
            sb.append(s);
        } else {
            // negative
            // fold value by reversing sign
            longValue = -longValue;
            String s = Long.toString(longValue, Character.MAX_RADIX);
            // pad with leading zeros
            while ((sb.length() + s.length()) < STRING_DOUBLE_LEN) {
                sb.append('0');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static double stringToDouble(String value) {
        if (value.charAt(0) == '1') {
            // positive
            long longValue = Long.parseLong(value.substring(1), Character.MAX_RADIX);
            return Double.longBitsToDouble(longValue);
        } else if (value.charAt(0) == '0') {
            // negative
            long longValue = Long.parseLong(value, Character.MAX_RADIX);
            // switch sign
            longValue = -longValue;
            return Double.longBitsToDouble(longValue);
        } else {
            throw new IllegalArgumentException("not a valid string representation of a double: " + value);
        }
    }
}
