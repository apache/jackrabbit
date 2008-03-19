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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;

/**
 * <code>GlobPattern</code>... TODO IMPROVE
 */
public class GlobPattern {

    private static Logger log = LoggerFactory.getLogger(GlobPattern.class);

    public static final String WILDCARD_ALL = "*";

    private final String pattern;

    private GlobPattern(String pattern)  {
        this.pattern = pattern;
    }

    public static GlobPattern create(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException();
        }
        return new GlobPattern(pattern);
    }

    public boolean matches(String toMatch) {
        // shortcut
        if (WILDCARD_ALL.equals(pattern) || pattern.equals(toMatch)) {
            return true;
        }

        // TODO
        return false;
    }

    public boolean matches(Item itemToMatch) {
        // TODO
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