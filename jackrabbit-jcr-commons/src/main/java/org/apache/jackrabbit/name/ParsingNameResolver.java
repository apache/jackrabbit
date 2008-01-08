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

import javax.jcr.NamespaceException;
import javax.jcr.Session;

/**
 * Name resolver that parsers and formats prefixed JCR names.
 * A {@link NamespaceResolver} is used for resolving the namespace prefixes.
 *
 * @deprecated Use the ParsingNameResolver class from 
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class ParsingNameResolver implements NameResolver {

    /**
     * Set of invalid or potentially invalid name characters.
     * Organized as a boolean array indexed by the ASCII character
     * code for maximum lookup performance.
     */
    private static final boolean[] MAYBE_INVALID = new boolean[128];

    /**
     * Initializes the set of invalid or potentially invalid name characters.
     */
    static {
        for (char ch = 0; ch < MAYBE_INVALID.length; ch++) {
            MAYBE_INVALID[ch] = Character.isSpaceChar(ch);
        }
        MAYBE_INVALID['.'] = true;
        MAYBE_INVALID[':'] = true;
        MAYBE_INVALID['/'] = true;
        MAYBE_INVALID['['] = true;
        MAYBE_INVALID[']'] = true;
        MAYBE_INVALID['*'] = true;
        MAYBE_INVALID['|'] = true;
        MAYBE_INVALID['"'] = true;
        MAYBE_INVALID['\''] = true;
    }

    /**
     * Checks whether the given character is an invalid or potentially an
     * invalid name character.
     *
     * @param ch character
     * @return <code>true</code> if the character maybe is invalid in a name,
     *         <code>false</code> if it is valid in any place within a name
     */
    private static boolean maybeInvalid(char ch) {
        if (ch < MAYBE_INVALID.length) {
            return MAYBE_INVALID[ch];
        } else {
            return Character.isSpaceChar(ch);
        }
    }

    /**
     * Namespace resolver.
     */
    private final NamespaceResolver resolver;

    /**
     * Creates a parsing name resolver.
     *
     * @param resolver namespace resolver
     */
    public ParsingNameResolver(NamespaceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Creates a parsing name resolver for a JCR session.
     *
     * @param session JCR session
     */
    public ParsingNameResolver(Session session) {
        this(new SessionNamespaceResolver(session));
    }

    //--------------------------------------------------------< NameResolver >

    /**
     * Parses the prefixed JCR name and returns the resolved qualified name.
     *
     * @param name prefixed JCR name
     * @return qualified name
     * @throws NameException if the JCR name format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    public QName getQName(String name)
            throws NameException, NamespaceException {
        int length = name.length();
        int colon = -1;

        for (int i = 0; i < length; i++) {
            char ch = name.charAt(i);
            if (maybeInvalid(ch)) {
                if (ch == ':' && colon == -1 && 0 < i && i + 1 < length) {
                    colon = i; // Detect the position of the first colon
                } else if (ch == ' ' && colon + 1 < i && i + 1 < length) {
                    // Allow spaces in the middle of the local name
                } else if (ch == '.'
                           && (i > 0 || length > 2
                               || (length == 2 && name.charAt(1) != '.'))) {
                    // Allow dots when the name is not "." or ".."
                } else {
                    throw new IllegalNameException("Invalid name: " + name);
                }
            }
        }

        if (length == 0) {
            throw new IllegalNameException("Empty name");
        } else if (colon == -1) {
            return new QName(QName.NS_DEFAULT_URI, name);
        } else {
            // Resolve the prefix (and validate the prefix format!)
            String uri = resolver.getURI(name.substring(0, colon));
            return new QName(uri, name.substring(colon + 1));
        }
    }

    /**
     * Returns the prefixed JCR name for the given qualified name.
     * If the name is in the default namespace, then the local name
     * is returned without a prefix. Otherwise the prefix for the
     * namespace is resolved and used to construct returned the JCR name.
     *
     * @param name qualified name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    public String getJCRName(QName name) throws NamespaceException {
        String uri = name.getNamespaceURI();
        if (uri.length() == 0) {
            return name.getLocalName();
        } else {
            return resolver.getPrefix(uri) + ":" + name.getLocalName();
        }
    }

}
