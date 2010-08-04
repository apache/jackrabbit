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

    private GlobPattern(String pattern)  {
        this.pattern = pattern;
    }

    static GlobPattern create(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException();
        }
        return new GlobPattern(pattern);
    }

    boolean matches(String toMatch) {
        // shortcut
        if (WILDCARD_ALL.equals(pattern)) {
            return true;
        }
        if (toMatch == null) {
            return false;
        }

        if (containsWildCard()) {
            return matches(pattern, toMatch);
        } else {
            return pattern.equals(toMatch) || Text.isDescendant(pattern, toMatch);
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

    private boolean containsWildCard() {
        // TODO: add proper impl
        return pattern.indexOf(ALL) > -1;
    }

    private static boolean matches(String pattern, String toMatch) {
        // TODO: add proper impl
        char[] c1 = pattern.toCharArray();
        char[] c2 = toMatch.toCharArray();

        for (int i = 0; i < c1.length; i++) {
            if (c1[i] == ALL) {
                return true;
            }
            if (i >= c2.length || c1[i] != c2[i]) {
                return false;
            }
        }

        return false;
    }

    //-------------------------------------------------------------< Object >---

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return pattern.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        return pattern;
    }

    /**
     * @see Object#equals(Object)
     */
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