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
 * <code>java.math.BigDecimal</code> values into <code>String</code> 
 * values that are lexicographically sortable according to the decimal value.
 * <p>
 * The string format only uses the digits '0' to '9' (except the last character 
 * for negative values) and contains the following elements:
 * <pre>
 * { signum of value + 2 (1 character; decimal) } 
 * { signum of exponent + 2 (1 character; decimal) } 
 * { length of exponent - 1 (1 character; decimal) } 
 * { unsigned exponent (decimal) } 
 * { unsigned value (decimal) }
 * { 'n' if negated }
 * </pre>
 * If the signum is zero, then the value is zero and that's it. The same goes
 * for the exponent: it is only encoded if the signum of the exponent is not 0.
 * If the signum is -1, the rest of the string is "negated" character by
 * character as follows: '0' is converted to '9', '1' to '8', and so on. The
 * same applies to the exponent.
 * <p>
 * Examples: 
 * Decimal 0: String "2"
 * Decimal 2: String "322": Signum 1; exponent 0; value 2)
 * Decimal 123: String "3302123": Signum 1; exponent 2 which is
 *      encoded as signum 3, length 1, value 2; value 123).
 * Decimal -1: String "178n": Signum -1, the rest is negated;
 *      exponent 0 ("2" negated); value 1 ("1" negated).
 */
public class DecimalField {
    
    /**
     * Convert a BigDecimal to a String.
     * 
     * @param value the BigDecimal
     * @return the String
     */
    public static String decimalToString(BigDecimal value) {
        // sign (1: negative, 2: zero, 3: positive)
        switch (value.signum()) {
        case -1:
            // without the 'n', the string representation of -101
            // is larger than the string representation of -100
            return "1" + negate(positiveDecimalToString(value.negate())) + "n";
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
        int signum = value.charAt(0) - '2';
        if (signum == 0) {
            return BigDecimal.ZERO;
        } else if (signum < 0) {
            value = negate(value).substring(0, value.length() - 1);
        }
        int expSignum = value.charAt(1) - '2';
        long exp;
        if (expSignum == 0) {
            exp = 0;
            value = value.substring(2);
        } else {
            String e = value.substring(2, 3);
            if (expSignum < 0) {
                e = negate(e);
            }
            int expSize = e.charAt(0) - '0' + 1;
            e = value.substring(3, 3 + expSize);
            if (expSignum < 0) {
                e = negate(e);
            }
            exp = Long.parseLong(e);
            if (expSignum < 0) {
                exp = -exp;
            }
            value = value.substring(3 + expSize);
        }
        int scale = (int) (value.length() - exp - 1);
        BigInteger unscaled = new BigInteger(value.substring(0));
        if (signum < 0) {
            unscaled = unscaled.negate();
        }
        return new BigDecimal(unscaled, scale);
    }
    
    private static String positiveDecimalToString(BigDecimal value) {
        StringBuilder buff = new StringBuilder();
        int precision = value.precision();
        int scale = value.scale();
        long exp = precision - scale - 1;
        // exponent signum and size
        if (exp == 0) {
            buff.append('2');
        } else {
            // exponent
            String e = String.valueOf(Math.abs(exp));
            // exponent size is prepended
            e = String.valueOf(e.length() - 1) + e;
            // exponent signum
            if (exp > 0) {
                buff.append('3');
            } else {
                buff.append('1');
                // the exponent is negated
                e = negate(e);
            }
            buff.append(e);
        }
        // the unscaled value
        String s = value.unscaledValue().toString();
        int max = s.length() - 1;
        // remove trailing 0s
        while (s.charAt(max) == '0') {
            max--;
        }
        return buff.append(s.substring(0, max + 1)).toString();
    }

    /**
     * "Negate" a number digit by digit (0 becomes 9, 9 becomes 0, and so on).
     * 
     * @param s the original string
     * @return the negated string
     */
    private static String negate(String s) {
        // negate character by character
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('9' - chars[i] + '0');
        }
        return String.valueOf(chars);
    }

}
