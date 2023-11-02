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
package org.apache.jackrabbit.spi.commons.conversion;

import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.XMLChar;

import javax.jcr.NamespaceException;

/**
 * <code>NameParser</code> parses a {@link String jcrName} using a
 * {@link NamespaceResolver} and a {@link NameFactory}.
 */
public class NameParser {

    // constants for parser
    private static final int STATE_PREFIX_START = 0;
    private static final int STATE_PREFIX = 1;
    private static final int STATE_NAME_START = 2;
    private static final int STATE_NAME = 3;
    private static final int STATE_URI_START = 4;
    private static final int STATE_URI = 5;

    /**
     * Parses the <code>jcrName</code> (either qualified or expanded) and
     * returns a new <code>Name</code>.
     *
     * @param jcrName the name to be parsed. The jcrName may either be in the
     * qualified or in the expanded form.
     * @param resolver <code>NamespaceResolver</code> use to retrieve the
     * namespace URI from the prefix contained in the given JCR name.
     * @return qName the new <code>Name</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     * @throws NamespaceException If the jcr name contains an unknown prefix.
     */
    public static Name parse(String jcrName, NamespaceResolver resolver, NameFactory factory)
            throws IllegalNameException, NamespaceException {

        if (jcrName == null) {
            complainAndThrow("name is null", "");
        }

        int len = jcrName.length();
        if (len == 0) {
            complainAndThrow("empty name", jcrName);
        }

        if (".".equals(jcrName) || "..".equals(jcrName)) {
            complainAndThrow("illegal name", jcrName);
        }

        // parse the name
        String prefix = "";
        String uri = null;
        int nameStart = 0;
        int state = STATE_PREFIX_START;
        boolean trailingSpaces = false;
        boolean checkFormat = (resolver == null);

        for (int i = 0; i < len; i++) {
            char c = jcrName.charAt(i);
            if (c == ':') {
                if (state == STATE_PREFIX_START) {
                    complainAndThrow("Prefix must not be empty", jcrName, i);
                } else if (state == STATE_PREFIX) {
                    if (trailingSpaces) {
                        complainAndThrow("Trailing spaces not allowed", jcrName, i);
                    }
                    prefix = jcrName.substring(0, i);
                    if (!XMLChar.isValidNCName(prefix)) {
                        complainAndThrow("Invalid name prefix: " + prefix, jcrName, i);
                    }
                    state = STATE_NAME_START;
                } else if (state == STATE_URI) {
                    // ignore -> validation of uri later on.
                } else {
                    complainAndThrow("'" + asDisplayableString(c) + "' not allowed in name", jcrName, i);
                }
                trailingSpaces = false;
            } else if (c == ' ') {
                if (state == STATE_PREFIX_START || state == STATE_NAME_START) {
                    complainAndThrow("'" + asDisplayableString(c) + "' not valid name start", jcrName, i);
                }
                trailingSpaces = true;
            } else if (c == '[' || c == ']' || c == '*' || c == '|') {
                complainAndThrow("'" + asDisplayableString(c) + "' not allowed in name", jcrName, i);
            } else if (Character.isWhitespace(c) && c < 128) {
                complainAndThrow("Whitespace character '" + asDisplayableString(c) + "' not allowed in name", jcrName, i);
            } else if (c == '/') {
                if (state == STATE_URI_START) {
                    state = STATE_URI;
                } else if (state != STATE_URI) {
                    complainAndThrow("'" + asDisplayableString(c) + "' not allowed in name", jcrName, i);
                }
                trailingSpaces = false;
            } else if (c == '{') {
                if (state == STATE_PREFIX_START) {
                    state = STATE_URI_START;
                } else if (state == STATE_URI_START || state == STATE_URI) {
                    // second '{' in the uri-part -> no valid expanded jcr-name.
                    // therefore reset the nameStart and change state.
                    state = STATE_NAME;
                    nameStart = 0;
                } else if (state == STATE_NAME_START) {
                    state = STATE_NAME;
                    nameStart = i;
                }
                trailingSpaces = false;
            } else if (c == '}') {
                if (state == STATE_URI_START || state == STATE_URI) {
                    String tmp = jcrName.substring(1, i);
                    if (tmp.length() == 0 || tmp.indexOf(':') != -1) {
                        // The leading "{...}" part is empty or contains
                        // a colon, so we treat it as a valid namespace URI.
                        // More detailed validity checks (is it well formed,
                        // registered, etc.) are not needed here.
                        uri = tmp;
                        state = STATE_NAME_START;
                    } else if (tmp.equals("internal")) {
                        // As a special Jackrabbit backwards compatibility
                        // feature, support {internal} as a valid URI prefix
                        uri = tmp;
                        state = STATE_NAME_START;
                    } else if (tmp.indexOf('/') == -1) {
                        // The leading "{...}" contains neither a colon nor
                        // a slash, so we can interpret it as a a part of a
                        // normal local name.
                        state = STATE_NAME;
                        nameStart = 0;
                    } else {
                        complainAndThrow("The URI prefix is neither a valid URI nor a valid part of a local name", jcrName);
                    }
                } else if (state == STATE_PREFIX_START) {
                    state = STATE_PREFIX; // prefix start -> validation later on will fail.
                } else if (state == STATE_NAME_START) {
                    state = STATE_NAME;
                    nameStart = i;
                }
                trailingSpaces = false;
            } else {
                if (state == STATE_PREFIX_START) {
                    state = STATE_PREFIX; // prefix start
                } else if (state == STATE_NAME_START) {
                    state = STATE_NAME;
                    nameStart = i;
                } else if (state == STATE_URI_START) {
                    state = STATE_URI;
                }
                trailingSpaces = false;
            }
        }

        // take care of qualified jcrNames starting with '{' that are not having
        // a terminating '}' -> make sure there are no illegal characters present.
        if (state == STATE_URI && (jcrName.indexOf(':') > -1 || jcrName.indexOf('/') > -1)) {
            complainAndThrow("Local name may not contain ':' nor '/'", jcrName);
        }

        if (nameStart == len || state == STATE_NAME_START) {
            complainAndThrow("Local name must not be empty", jcrName);
        }
        if (trailingSpaces) {
            complainAndThrow("Trailing spaces not allowed", jcrName);
        }

        // if namespace is null, this is just a check for format. this can only
        // happen if invoked internally
        if (checkFormat) {
            return null;
        }

        // resolve prefix to uri
        if (uri == null) {
            uri = resolver.getURI(prefix);
        }

        String localName = (nameStart == 0 ? jcrName : jcrName.substring(nameStart, len));
        return factory.create(uri, localName);
    }

    private static String asDisplayableString(char c) {
        if (c >= ' ' && c < 127) {
            return Character.toString(c);
        } else if (c == '\b') {
            return "\\b";
        } else if (c == '\f') {
            return "\\f";
        } else if (c == '\n') {
            return "\\n";
        } else if (c == '\r') {
            return "\\r";
        } else if (c == '\t') {
            return "\\t";
        } else if (c == '\'') {
            return "\\'";
        } else if (c == '"') {
            return "\\\"";
        } else {
            return String.format("\\u%04x", (int) c);
        }
    }

    private static String formatNameForDisplay(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            b.append(asDisplayableString(name.charAt(i)));
        }
        return b.toString();
    }

    private static void complainAndThrow(String reason, String name) throws IllegalNameException {
        complainAndThrow(reason, name, -1);
    }

    private static void complainAndThrow(String reason, String name, int index) throws IllegalNameException{
        String msg;
        if (index == -1) {
            msg = String.format("%s (name: \"%s\")", reason, formatNameForDisplay(name));
        } else {
            msg = String.format("%s (name: \"%s\", at position: %d)", reason, formatNameForDisplay(name), index);
        }
        throw new IllegalNameException(msg);
    }

    /**
     * Parses an array of <code>jcrName</code> and returns the respective
     * array of <code>Name</code>.
     *
     * @param jcrNames the array of names to be parsed
     * @param resolver <code>NamespaceResolver</code> use to retrieve the
     * namespace URI from the prefix contained in the given JCR name.
     * @param factory
     * @return the new array of <code>Name</code>
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     * JCR-style name.
     * @throws NamespaceException If the jcr name contains an unknown prefix.
     */
    public static Name[] parse(String jcrNames[], NamespaceResolver resolver, NameFactory factory)
            throws NameException, NamespaceException {

        Name[] ret = new Name[jcrNames.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = parse(jcrNames[i], resolver, factory);
        }
        return ret;
    }

    /**
     * Check the format of the jcr name. Note that the prefix is not resolved
     * and therefore namespace violations (unknown prefix) will not be detected.
     *
     * @param jcrName
     * @throws IllegalNameException If the jcrName contains an invalid format.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        try {
            parse(jcrName, null, null);
        } catch (NamespaceException e) {
            // will never occur since the resolver is not passed to the parser
        }
    }
}
