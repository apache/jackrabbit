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
package org.apache.jackrabbit.core.security.authorization;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>GlobPattern</code> defines a simplistic pattern matching. It consists
 * of a mandatory (leading) path and an optional "glob" that may contain one or
 * more wildcard characters ("<code>*</code>") according to the glob matching
 * defined by {@link javax.jcr.Node#getNodes(String[])}. In contrast to that
 * method the <code>GlobPattern</code> operates on path (not only names).
 * <p>
 * Please note the following special cases:
 * <pre>
 * NodePath     |   Restriction   |   Matches
 * -----------------------------------------------------------------------------
 * /foo         |   null          |   matches /foo and all children of /foo
 * /foo         |   ""            |   matches /foo only
 * </pre>
 * <p>
 * Examples including wildcard char:
 * <pre>
 * NodePath = "/foo"
 * Restriction   |   Matches
 * -----------------------------------------------------------------------------
 * &#42;         |   all siblings of foo and foo's and the siblings' descendants
 * /&#42;cat     |   all children of /foo whose path ends with "cat"
 * /&#42;/cat    |   all non-direct descendants of /foo named "cat"
 * /cat&#42;     |   all descendant path of /foo that have the direct foo-descendant segment starting with "cat"
 * &#42;cat      |   all siblings and descendants of foo that have a name ending with cat
 * &#42;/cat     |   all descendants of /foo and foo's siblings that have a name segment "cat"
 * cat/&#42;     |   all descendants of '/foocat'
 * /cat/&#42;    |   all descendants of '/foo/cat'
 * &#42;cat/&#42;    |   all descendants of /foo that have an intermediate segment ending with 'cat'
 * </pre>
 */
public final class GlobPattern {

    private static Logger log = LoggerFactory.getLogger(GlobPattern.class);

    private static final char WILDCARD_CHAR = '*';
    private static final int  MAX_WILDCARD = 20;

    private final String nodePath;
    private final String restriction;
    
    private final Pattern pattern;

    private GlobPattern(String nodePath, String restriction)  {
        if (nodePath == null) {
            throw new IllegalArgumentException();
        }

        this.nodePath = nodePath;
        this.restriction = restriction;
        
        if (restriction != null && restriction.length() > 0) {
            StringBuilder b = new StringBuilder(nodePath);
            b.append(restriction);

            int lastPos = restriction.lastIndexOf(WILDCARD_CHAR);
            if (lastPos >= 0) {
                String end;
                if (lastPos != restriction.length()-1) {
                    end = restriction.substring(lastPos + 1);
                } else {
                    end = null;
                }
                pattern = new WildcardPattern(b.toString(), end);
            } else {
                pattern = new PathPattern(b.toString());
            }
        } else {
            pattern = new PathPattern();
        }
    }

    public static GlobPattern create(String nodePath, String restrictions) {
        return new GlobPattern(nodePath, restrictions);
    }

    public static GlobPattern create(String nodePath) {
        return create(nodePath, null);
    }
    
    public boolean matches(String toMatch) {
        if (toMatch == null) {
            return false;
        } else {
            return pattern.matches(toMatch);
        }
    }

    public boolean matches(Item itemToMatch) {
        try {
            // TODO: missing proper impl
            return matches(itemToMatch.getPath());
        } catch (RepositoryException e) {
            log.error("Unable to determine match. {}", e.getMessage());
            return false;
        }
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int h = 37 * 17 + nodePath.hashCode();
        if (restriction  != null) {
            h = 37 * h + restriction.hashCode();
        }
        return h;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return nodePath + " : " + restriction;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GlobPattern) {
            GlobPattern other = (GlobPattern) obj;
            return nodePath.equals(other.nodePath) &&
                    ((restriction == null) ? other.restriction == null : restriction.equals(other.restriction));
        }
        return false;
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Base for PathPattern and WildcardPattern
     */
    private abstract class Pattern {
        abstract boolean matches(String toMatch);
    }

    /**
     * Path pattern: The restriction is missing or doesn't contain any wildcard character.
     */
    private final class PathPattern extends Pattern {

        private final String patternStr;

        private PathPattern() {
            this(null);
        }
        private PathPattern(String patternStr) {
            this.patternStr = patternStr;
        }
        
        @Override
        boolean matches(String toMatch) {
            if (restriction == null) {
                return Text.isDescendantOrEqual(nodePath, toMatch);
            } else if (restriction.length() == 0) {
                return nodePath.equals(toMatch);
            } else {
                // no wildcard contained in restriction: use path defined
                // by nodePath + restriction to calculate the match
                return Text.isDescendantOrEqual(patternStr, toMatch);
            }
        }
    }

    /**
     * Wildcard pattern: The specified restriction contains one or more wildcard character(s).
     */
    private final class WildcardPattern extends Pattern {

        private final String patternEnd;
        private final char[] patternChars;        

        private WildcardPattern(String patternStr, String patternEnd) {
            patternChars = patternStr.toCharArray();
            this.patternEnd = patternEnd;
        }

        @Override
        boolean matches(String toMatch) {
            if (patternEnd != null && !toMatch.endsWith(patternEnd)) {
                // shortcut: verify if end of pattern matches end of toMatch
                return false;
            }
            char[] tm = (toMatch.endsWith("/")) ? toMatch.substring(0, toMatch.length()-1).toCharArray() : toMatch.toCharArray();
            // shortcut didn't reveal mismatch -> need to process the internal match method.
            return matches(patternChars, 0, tm, 0, MAX_WILDCARD);
        }

        /**
         *
         * @param pattern The pattern
         * @param pOff
         * @param s
         * @param sOff
         * @return <code>true</code> if matches, <code>false</code> otherwise
         */
        private boolean matches(char[] pattern, int pOff,
                                char[] s, int sOff, int cnt) {

            if (cnt <= 0) {
                throw new IllegalArgumentException("Illegal glob pattern " + GlobPattern.this);
            }
            /*
            NOTE: code has been copied (and slightly modified) from
            ChildrenCollectorFilter#internalMatches.
            TODO: move them to a common utility.
            */

            int pLength = pattern.length;
            int sLength = s.length;

            while (true) {
                // end of pattern reached: matches only if sOff points at the end
                // of the string to match.
                if (pOff >= pLength) {
                    return sOff >= sLength;
                }

                // the end of the string to match has been reached but pattern
                // doesn't have '*' at patternIndex -> no match
                if (sOff >= sLength && pattern[pOff] != WILDCARD_CHAR) {
                    return false;
                }

                // the next character of the pattern is '*'
                // -> recursively test if the rest of the specified string matches
                if (pattern[pOff] == WILDCARD_CHAR) {
                    if (++pOff >= pLength) {
                        return true;
                    }

                    cnt--;
                    while (true) {
                        if (matches(pattern, pOff, s, sOff, cnt)) {
                            return true;
                        }
                        if (sOff >= sLength) {
                            return false;
                        }
                        sOff++;
                    }
                }

                // not yet reached end of patter nor string and not wildcard character.
                // the 2 strings don't match in case the characters at the current
                // position are not the same.
                if (pOff < pLength && sOff < sLength) {
                    if (pattern[pOff] != s[sOff]) {
                        return false;
                    }
                }
                pOff++;
                sOff++;
            }
        }
    }
}
