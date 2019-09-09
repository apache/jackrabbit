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
package org.apache.jackrabbit.commons.json;

/**
 * JSON utilities.
 */
public class JsonUtil {

    /**
     * Generate a valid JSON string from the given <code>str</code>.
     *
     * @param str A String
     * @return JSON string surrounded by double quotes.
     * @see <a href="http://tools.ietf.org/html/rfc4627">RFC 4627</a>
     */
    public static String getJsonString(String str) {
        if (str == null || str.length() == 0) {
            return "\"\"";
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 2);
        // leading quote
        sb.append('"');
        // append passed string escaping characters as required
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch (c) {
                // reverse solidus and double quote
                case '\\':
                case '"':
                    sb.append('\\').append(c);
                    break;
                // tab, line breaking chars and backspace
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                // other control characters and 'unescaped'
                default:
                    if (c < 32) {
                        // control characters except those already covered above.
                        String uc = Integer.toHexString(c);
                        sb.append("\\u");
                        int uLen = uc.length();
                        while (uLen++ < 4) {
                            sb.append('0');
                        }
                        sb.append(uc);
                    } else {
                        // unescaped = %x20-21 / %x23-5B / %x5D-10FFFF
                        sb.append(c);
                    }
            }
        }
        // trailing quote
        sb.append('"');
        return sb.toString();
    }
}