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
package org.apache.jackrabbit.util;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * <code>ChildrenCollectorFilter</code> is a utility class
 * which can be used to 'collect' child items of a
 * node whose names match a certain pattern. It implements the
 * <code>ItemVisitor</code> interface.
 */
public class ChildrenCollectorFilter extends TraversingItemVisitor.Default {
    static final char WILDCARD_CHAR = '*';
    static final String OR = "|";

    private final Collection children;
    private final boolean collectNodes;
    private final boolean collectProperties;
    private final String namePattern;

    /**
     * Constructs a <code>ChildrenCollectorFilter</code>
     *
     * @param namePattern       the pattern which should be applied to the names
     *                          of the children
     * @param children          where the matching children should be added
     * @param collectNodes      true, if child nodes should be collected; otherwise false
     * @param collectProperties true, if child properties should be collected; otherwise false
     * @param maxLevel          umber of hierarchy levels to traverse
     *                          (e.g. 1 for direct children only, 2 for children and their children, and so on)
     */
    public ChildrenCollectorFilter(
            String namePattern, Collection children,
            boolean collectNodes, boolean collectProperties, int maxLevel) {
        super(false, maxLevel);
        this.namePattern = namePattern;
        this.children = children;
        this.collectNodes = collectNodes;
        this.collectProperties = collectProperties;
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException {
        if (level > 0 && collectNodes) {
            if (matches(node.getName(), namePattern)) {
                children.add(node);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property property, int level)
            throws RepositoryException {
        if (level > 0 && collectProperties) {
            if (matches(property.getName(), namePattern)) {
                children.add(property);
            }
        }
    }

    /**
     * Matches the name pattern against the specified name.
     * <p/>
     * The pattern may be a full name or a partial name with one or more
     * wildcard characters ("*"), or a disjunction (using the "|" character
     * to represent logical <i>OR</i>) of these. For example,
     * <p/>
     * <code>"jcr:*|foo:bar"</code>
     * <p/>
     * would match
     * <p/>
     * <code>"foo:bar"</code>, but also <code>"jcr:whatever"</code>.
     * <p/>
     * <pre>
     * The EBNF for pattern is:
     *
     * namePattern ::= disjunct {'|' disjunct}
     * disjunct ::= name [':' name]
     * name ::= '*' |
     *          ['*'] fragment {'*' fragment}['*']
     * fragment ::= char {char}
     * char ::= nonspace | ' '
     * nonspace ::= (* Any Unicode character except:
     *               '/', ':', '[', ']', '*',
     *               ''', '"', '|' or any whitespace
     *               character *)
     * </pre>
     *
     * @param name the name to test the pattern with
     * @param pattern the pattern to be matched against the name
     * @return true if the specified name matches the pattern
     * @see javax.jcr.Node#getNodes(String)
     */
    public static boolean matches(String name, String pattern) {
        // split pattern
        StringTokenizer st = new StringTokenizer(pattern, OR, false);
        while (st.hasMoreTokens()) {
            // remove leading & trailing whitespace from token
            String token = st.nextToken().trim();
            if (internalMatches(name, token, 0, 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Internal helper used to recursively match the pattern
     *
     * @param s       The string to be tested
     * @param pattern The pattern
     * @param sOff    offset within <code>s</code>
     * @param pOff    offset within <code>pattern</code>.
     * @return true if <code>s</code> matched pattern, else false.
     */
    private static boolean internalMatches(String s, String pattern,
                                           int sOff, int pOff) {
        int pLen = pattern.length();
        int sLen = s.length();

        while (true) {
            if (pOff >= pLen) {
                if (sOff >= sLen) {
                    return true;
                } else if (s.charAt(sOff) == '[') {
                    // check for subscript notation (e.g. "whatever[1]")

                    // the entire pattern matched up to the subscript:
                    // -> ignore the subscript
                    return true;
                } else {
                    return false;
                }
            }
            if (sOff >= sLen && pattern.charAt(pOff) != WILDCARD_CHAR) {
                return false;
            }

            // check for a '*' as the next pattern char;
            // this is handled by a recursive call for
            // each postfix of the name.
            if (pattern.charAt(pOff) == WILDCARD_CHAR) {
                if (++pOff >= pLen) {
                    return true;
                }

                while (true) {
                    if (internalMatches(s, pattern, sOff, pOff)) {
                        return true;
                    }
                    if (sOff >= sLen) {
                        return false;
                    }
                    sOff++;
                }
            }

            if (pOff < pLen && sOff < sLen) {
                if (pattern.charAt(pOff) != s.charAt(sOff)) {
                    return false;
                }
            }
            pOff++;
            sOff++;
        }
    }
}
