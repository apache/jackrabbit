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
 */
public class LongField {

    private static final int STRING_LONG_LEN
            = Long.toString(Long.MAX_VALUE, Character.MAX_RADIX).length() + 1;

    private LongField() {

    }

    public static String longToString(long value) {
        StringBuffer sb = new StringBuffer(STRING_LONG_LEN);
        if (value < 0) {
            // shift value
            value += Long.MAX_VALUE;
            value++;
            // after shift
            // Long.MIN_VALUE -> 0
            // 1 -> Long.MAX_VALUE

            // convert into string
            String s = Long.toString(value, Character.MAX_RADIX);
            // pad with leading zeros
            while ((sb.length() + s.length()) < STRING_LONG_LEN) {
                sb.append('0');
            }
            sb.append(s);
        } else {
            // convert into string
            String s = Long.toString(value, Character.MAX_RADIX);
            // mark as positive
            sb.append('1');
            // fill in zeros if needed
            while ((sb.length() + s.length()) < STRING_LONG_LEN) {
                sb.append('0');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static long stringToLong(String value) {
        if (value.charAt(0) == '1') {
            // positive
            return Long.parseLong(value.substring(1), Character.MAX_RADIX);
        }
        if (value.charAt(0) == '0') {
            // negative
            long longValue = Long.parseLong(value, Character.MAX_RADIX);
            // reverse shift
            longValue -= Long.MAX_VALUE;
            return --longValue;
        } else {
            throw new IllegalArgumentException(value);
        }
    }
}
