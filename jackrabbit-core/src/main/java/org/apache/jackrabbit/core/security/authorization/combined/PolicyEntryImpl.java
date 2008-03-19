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

import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.GlobPattern;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Item;
import java.security.Principal;

/**
 * <code>PolicyEntryImpl</code>...
 */
class PolicyEntryImpl implements PolicyEntry {

    private static Logger log = LoggerFactory.getLogger(PolicyEntryImpl.class);

    /**
     * Privileges defined for this entry.
     */
    private final int privileges;

    /**
     * If the actions contained are allowed or denied
     */
    private final boolean allow;

    /**
     * The Principal of this entry
     */
    private final Principal principal;

    private final String nodePath;
    private final String glob;

    /**
     * Globbing pattern
     */
    private final GlobPattern pattern;

    /**
     * Hash code being calculated on demand.
     */
    private int hashCode = -1;

    /**
     * Constructs an new entry.
     *
     * @param principal
     * @param privileges
     * @param allow
     */
    PolicyEntryImpl(Principal principal, int privileges, boolean allow,
                    String nodePath, String glob) {
        if (principal == null || nodePath == null) {
            throw new IllegalArgumentException("Neither principal nor nodePath must be null.");
        }
        this.principal = principal;
        this.privileges = privileges;
        this.allow = allow;
        this.nodePath = nodePath;
        this.glob = (glob == null) ? GlobPattern.WILDCARD_ALL : glob;

        pattern = GlobPattern.create(nodePath + "/" +glob);
    }

    int getPrivilegeBits() {
        return privileges;
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

    //-------------------------------------------------< AccessControlEntry >---
    /**
     * @see AccessControlEntry#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @see AccessControlEntry#getPrivileges()
     */
    public Privilege[] getPrivileges() {
        return PrivilegeRegistry.getPrivileges(privileges);
    }

    //--------------------------------------------------------< PolicyEntry >---
    /**
     * @return true if all actions contained in this Entry are allowed
     * @see PolicyEntry#isAllow()
     */
    public boolean isAllow() {
        return allow;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        if (hashCode == -1) {
            int h = 17;
            h = 37 * h + principal.getName().hashCode();
            h = 37 * h + privileges;
            h = 37 * h + Boolean.valueOf(allow).hashCode();
            h = 37 * h + nodePath.hashCode();
            h = 37 * h + glob.hashCode();
            hashCode = h;
        }
        return hashCode;
    }

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
            // TODO: check again if comparing principal-name is sufficient
            return principal.getName().equals(tmpl.principal.getName()) &&
                   allow == tmpl.allow &&
                   privileges == tmpl.privileges &&
                   glob.equals(tmpl.glob);
        }
        return false;
    }

}