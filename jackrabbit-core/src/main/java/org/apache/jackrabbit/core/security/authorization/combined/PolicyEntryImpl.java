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
package org.apache.jackrabbit.core.security.authorization.combined;

import org.apache.jackrabbit.core.security.authorization.AbstractPolicyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>PolicyEntryImpl</code>...
 */
class PolicyEntryImpl extends AbstractPolicyEntry {

    private static Logger log = LoggerFactory.getLogger(PolicyEntryImpl.class);

    private final String nodePath;
    private final String glob;

    /**
     * Globbing pattern
     */
    private final GlobPattern pattern;

    /**
     * Constructs an new entry.
     *
     * @param principal
     * @param privileges
     * @param allow
     */
    PolicyEntryImpl(Principal principal, int privileges, boolean allow,
                    String nodePath, String glob) {
        super(principal, privileges, allow);

        if (principal == null || nodePath == null) {
            throw new IllegalArgumentException("Neither principal nor nodePath must be null.");
        }
        this.nodePath = nodePath;
        this.glob = glob;

        // TODO: review again
        if (glob != null && glob.length() > 0) {
            StringBuffer b = new StringBuffer(nodePath);
            b.append(glob);
            pattern = GlobPattern.create(b.toString());
        } else {
            pattern = GlobPattern.create(nodePath);
        }
    }

    String getNodePath() {
        return nodePath;
    }

    String getGlob() {
        return glob;
    }

    boolean matches(String jcrPath) throws RepositoryException {
        return pattern.matches(jcrPath);
    }

    boolean matches(Item item) throws RepositoryException {
        return pattern.matches(item);
    }

    protected int buildHashCode() {
        int h = super.buildHashCode();
        h = 37 * h + nodePath.hashCode();
        h = 37 * h + glob.hashCode();
        return h;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns true if the principal, the allow-flag, all privileges and
     * the nodepath and the glob string are equal or the same, respectively.
     *
     * @param obj
     * @return
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof PolicyEntryImpl) {
            PolicyEntryImpl tmpl = (PolicyEntryImpl) obj;
            return super.equals(obj) &&
                   nodePath.equals(tmpl.nodePath) &&
                   glob.equals(tmpl.glob);
        }
        return false;
    }
}