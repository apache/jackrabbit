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
package org.apache.jackrabbit.name;

import org.apache.jackrabbit.util.XMLChar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>NameFormat</code> provides methods for formatting and parsing names.
 *
 * @deprecated Use the NameResolver interface from
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class NameFormat {

    /**
     * The regular expression pattern used to validate and parse
     * qualified names.
     * <p/>
     * The pattern contains the following groups:
     * <ul>
     * <li>group 1 is namespace prefix incl. delimiter (colon)
     * <li>group 2 is namespace prefix excl. delimiter (colon)
     * <li>group 3 is localName
     * </ul>
     */
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)");

    /**
     * Matcher instance as thread-local.
     */
    private static final ThreadLocal NAME_MATCHER = new ThreadLocal() {
        protected Object initialValue() {
            return NAME_PATTERN.matcher("dummy");
        }
    };

    /**
     * Checks if <code>jcrName</code> is a valid JCR-style name.
     *
     * @param jcrName the name to be checked
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        doParse(jcrName);
    }

    //-------------------------------------------------------< implementation >

    /**
     * Parses the <code>jcrName</code> and returns an array of two strings:
     * the first array element contains the prefix (or empty string),
     * the second the local name.
     *
     * @param jcrName the name to be parsed
     * @return An array holding two strings: the first array element contains
     *         the prefix (or empty string), the second the local name.
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    private static String[] doParse(String jcrName) throws IllegalNameException {
        if (jcrName == null || jcrName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        if (".".equals(jcrName) || "..".equals(jcrName)) {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        String prefix;
        String localName;

        Matcher matcher = (Matcher) NAME_MATCHER.get();
        matcher.reset(jcrName);
        if (matcher.matches()) {
            // check for prefix (group 1)
            if (matcher.group(1) != null) {
                // prefix specified
                // group 2 is namespace prefix excl. delimiter (colon)
                prefix = matcher.group(2);
                // check if the prefix is a valid XML prefix
                if (!XMLChar.isValidNCName(prefix)) {
                    // illegal syntax for prefix
                    throw new IllegalNameException("'" + jcrName
                            + "' is not a valid name: illegal prefix");
                }
            } else {
                // no prefix specified
                prefix = "";
            }

            // group 3 is localName
            localName = matcher.group(3);
        } else {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        return new String[]{prefix, localName};
    }
}
