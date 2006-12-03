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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.XMLChar;

/**
 * Class providing some character escape methods. The escape schemata are
 * defined in chapter 6.4.3 (for JCR names) and (6.4.4 for JCR values) in the
 * JCR specification.
 */
public class EscapeJCRUtil {

    private static final String utf16esc = "_x[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]_";

    private static char[] Utf16Padding = {'0', '0', '0'};

    private static int utf16length = 4;

    public static String escapeJCRValues(String str) {
        return escapeValues(str);
    }

    public static String escapeJCRNames(String str) {
        return escapeNames(str);
    }

    //----------------------- private methods ----------------------------------

    private static String escapeNames(String str) {
        String[] split = str.split(":");
        if (split.length == 2) {
            // prefix should yet be a valid xml name
            String localname = escape(split[1]);
            String name = split[0] + ":" + localname;
            return name;
        } else {
            String localname = escape(split[0]);
            return localname;
        }
    }

    private static String escapeValues(String str) {
        char[] chars = str.toCharArray();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\u0020'
                    || c == '\u0009'
                    || c == '\n'
                    || c == '\r') {
                buf.append(escapeChar(c));
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Check if a substring can be misinterpreted as an escape sequence.
     *
     * @param str
     * @return
     */
    private static boolean canMisinterpret(String str) {
        boolean misinterprete = false;
        // check if is like "_xXXXX_"
        if (str.length() >= 7) {
            String sub16 = str.substring(0, 7);
            if (sub16.matches(utf16esc)) {
                misinterprete = true;
            }
        }
        return misinterprete;
    }

    /**
     * Escapes a single (invalid xml) character.
     *
     * @param c
     * @return
     */
    private static String escapeChar(char c) {
        String unicodeRepr = Integer.toHexString(c);
        StringBuffer escaped = new StringBuffer();
        escaped.append("_x");
        escaped.append(Utf16Padding, 0, utf16length - unicodeRepr.length());
        escaped.append(unicodeRepr);
        escaped.append("_");
        return escaped.toString();
    }

    /**
     * Escapes a string containing invalid xml character(s).
     *
     * @param str
     * @return
     */
    private static String escape(String str) {
        char[] chars = str.toCharArray();
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            // handle start character
            if (i == 0) {
                if (!XMLChar.isNameStart(c)) {
                    String escaped = escapeChar(c);
                    buf.append(escaped);
                } else {
                    String substr = str.substring(i, str.length());
                    if (canMisinterpret(substr)) {
                        buf.append(escapeChar(c));
                    } else {
                        buf.append(c);
                    }
                }
            } else {
                if (!XMLChar.isName(c)) {
                    buf.append(escapeChar(c));
                } else {
                    String substr = str.substring(i, str.length());
                    if (canMisinterpret(substr)) {
                        buf.append(escapeChar(c));
                    } else {
                        buf.append(c);
                    }
                }
            }
        }
        return buf.toString();
    }
}