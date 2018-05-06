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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The <code>DecimalField</code> class is a utility to convert
 * <code>java.math.BigDecimal</code> values to <code>String</code> 
 * values that are lexicographically sortable according to the decimal value.
 * <p>
 * The string format uses the characters '0' to '9' and consists of:
 * <pre>
 * { value signum +2 }
 * { exponent signum +2 }
 * { exponent length -1 }
 * { exponent value }
 * { value (-1 if inverted) }
 * </pre>
 * Only the signum is encoded if the value is zero. The exponent is not
 * encoded if zero. Negative values are "inverted" character by character
 * ('0' -&gt; 9, '1' -&gt; '8', and so on). The same applies to the exponent.
 * <p>
 * Examples: 
 * 0 =&gt; "2"
 * 2 =&gt; "322" (signum 1; exponent 0; value 2)
 * 120 =&gt; "330212" (signum 1; exponent signum 1, length 1, value 2; value 12).
 * -1 =&gt; "179" (signum -1, rest inverted; exponent 0; value 1 (-1, inverted).
 * <p>
 * Values between BigDecimal(BigInteger.ONE, Integer.MIN_VALUE) and 
 * BigDecimal(BigInteger.ONE, Integer.MAX_VALUE) are supported.
 */
public class DecimalField {
    
    /**
     * Convert a BigDecimal to a String.
     * 
     * @param value the BigDecimal
     * @return the String
     */
    public static String decimalToString(BigDecimal value) {
        switch (value.signum()) {
        case -1:
            return "1" + invert(positiveDecimalToString(value.negate()), 1);
        case 0:
            return "2";
        default:
            return "3" + positiveDecimalToString(value);
        }
    }
    
    /**
     * Convert a String to a BigDecimal.
     * 
     * @param value the String
     * @return the BigDecimal
     */
    public static BigDecimal stringToDecimal(String value) {
        int sig = value.charAt(0) - '2';
        if (sig == 0) {
            return BigDecimal.ZERO;
        } else if (sig < 0) {
            value = invert(value, 1);
        }
        long expSig = value.charAt(1) - '2', exp;
        if (expSig == 0) {
            exp = 0;
            value = value.substring(2);
        } else {
            int expSize = value.charAt(2) - '0' + 1;
            if (expSig < 0) {
                expSize = 11 - expSize;
            }
            String e = value.substring(3, 3 + expSize);
            exp = expSig * Long.parseLong(expSig < 0 ? invert(e, 0) : e);
            value = value.substring(3 + expSize);
        }
        BigInteger x = new BigInteger(value);
        int scale = (int) (value.length() - exp - 1);
        return new BigDecimal(sig < 0 ? x.negate() : x, scale);
    }
    
    private static String positiveDecimalToString(BigDecimal value) {
        StringBuilder buff = new StringBuilder();
        long exp = value.precision() - value.scale() - 1;
        // exponent signum and size
        if (exp == 0) {
            buff.append('2');
        } else {
            String e = String.valueOf(Math.abs(exp));
            // exponent size is prepended
            e = String.valueOf(e.length() - 1) + e;
            // exponent signum
            if (exp > 0) {
                buff.append('3').append(e);
            } else {
                buff.append('1').append(invert(e, 0));
            }
        }
        String s = value.unscaledValue().toString();
        // remove trailing 0s
        int max = s.length() - 1;
        while (s.charAt(max) == '0') {
            max--;
        }
        return buff.append(s.substring(0, max + 1)).toString();
    }

    /**
     * "Invert" a number digit by digit (0 becomes 9, 9 becomes 0, and so on).
     * 
     * @param s the original string
     * @param incLast how much to increment the last character
     * @return the negated string
     */
    private static String invert(String s, int incLast) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('9' - chars[i] + '0');
        }
        chars[chars.length - 1] += incLast;
        return String.valueOf(chars);
    }

}
