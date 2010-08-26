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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.util.Text;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * <code>GlobPattern</code>...
 */
class GlobPattern {

    //TODO: add proper impl.

    private static Logger log = LoggerFactory.getLogger(GlobPattern.class);

    private static final char ALL = '*';
    public static final String WILDCARD_ALL = "*";

    private final String pattern;
    private final char[] patternChars;

    private final boolean matchesAll;
    private final boolean containsWildcard;

    private GlobPattern(String pattern)  {
        this.pattern = pattern;

        matchesAll = WILDCARD_ALL.equals(pattern);
        containsWildcard = pattern.indexOf(ALL) > -1;

        patternChars = pattern.toCharArray();

    }

    static GlobPattern create(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException();
        }
        return new GlobPattern(pattern);
    }

    boolean matches(String toMatch) {
        // shortcut
        if (matchesAll) {
            return true;
        }
        if (toMatch == null) {
            return false;
        }

        if (containsWildcard) {
            return matches(patternChars, toMatch.toCharArray());
        } else {
            return Text.isDescendantOrEqual(pattern, toMatch);
        }
    }

    boolean matches(Item itemToMatch) {
        try {
            // TODO: missing proper impl
            return matches(itemToMatch.getPath());
        } catch (RepositoryException e) {
            log.error("Unable to determine match.", e.getMessage());
            return false;
        }
    }

    private static boolean matches(char[] patternChars, char[] toMatch) {
        // TODO: add proper impl
        for (int i = 0; i < patternChars.length; i++) {
            if (patternChars[i] == ALL) {
                return true;
            }
            if (i >= toMatch.length || patternChars[i] != toMatch[i]) {
                return false;
            }
        }

        return false;
    }

    //-------------------------------------------------------------< Object >---

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return pattern;
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
            return pattern.equals(((GlobPattern)obj).pattern);
        }
        return false;
    }
}