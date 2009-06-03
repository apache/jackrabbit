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

import org.apache.jackrabbit.util.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>PathFormat</code> checks the formt .
 *
 * @deprecated Use the PathResolver interface from
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class PathFormat {

    /**
     * Pattern used to validate and parse path elements:<p>
     * <ul>
     * <li>group 1 is .
     * <li>group 2 is ..
     * <li>group 3 is namespace prefix incl. delimiter (colon)
     * <li>group 4 is namespace prefix excl. delimiter (colon)
     * <li>group 5 is localName
     * <li>group 6 is index incl. brackets
     * <li>group 7 is index excl. brackets
     * </ul>
     */
    private static final Pattern PATH_ELEMENT_PATTERN =
            Pattern.compile("(\\.)|"
            + "(\\.\\.)|"
            + "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)"
            + "(\\[([1-9]\\d*)\\])?");

    /**
     * Matcher instance as thread-local.
     */
    private static final ThreadLocal PATH_ELEMENT_MATCHER = new ThreadLocal() {
        protected Object initialValue() {
            return PATH_ELEMENT_PATTERN.matcher("dummy");
        }
    };

    /**
     * Checks if <code>jcrPath</code> is a valid JCR-style absolute or relative
     * path.
     *
     * @param jcrPath the path to be checked
     * @throws MalformedPathException If <code>jcrPath</code> is not a valid
     *                                JCR-style path.
     */
    public static void checkFormat(String jcrPath) throws MalformedPathException {
        // split path into path elements
        String[] elems = Text.explode(jcrPath, '/', true);
        if (elems.length == 0) {
            throw new MalformedPathException("empty path");
        }
        for (int i = 0; i < elems.length; i++) {
            // validate & parse path element
            String prefix;
            String localName;
            int index;

            String elem = elems[i];
            if (i == 0 && elem.length() == 0) {
                // path is absolute, i.e. the first element is the root element
                continue;
            }
            if (elem.length() == 0 && i == elems.length - 1) {
                // ignore trailing '/'
                break;
            }
            Matcher matcher = (Matcher) PATH_ELEMENT_MATCHER.get();
            matcher.reset(elem);
            
            if (!matcher.matches()) {
                // illegal syntax for path element
                throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '"
                        + elem + "' is not a legal path element");
            }
        }
    }
}
