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

import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * Simple, immutable implementation of the
 * {@link org.apache.jackrabbit.api.jsr283.security.AccessControlEntry}
 * and the {@link PolicyEntry} interfaces.
 */
public abstract class AbstractPolicyEntry implements PolicyEntry {

    private static Logger log = LoggerFactory.getLogger(AbstractPolicyEntry.class);

    /**
     * Privileges contained in this entry
     */
    private final int privileges;

    /**
     * if the actions contained are allowed or denied
     */
    private final boolean allow;

    /**
     * the Principal of this entry
     */
    private final Principal principal;

    /**
     * Hash code being calculated on demand.
     */
    private int hashCode = -1;

    /**
     * Construct an access control entry for the given principal, privileges and
     * a polarity (deny or allow)
     *
     * @param principal
     * @param privileges
     * @param allow
     */
    protected AbstractPolicyEntry(Principal principal, int privileges, boolean allow) {
        this.principal = principal;
        this.privileges = privileges;
        this.allow = allow;
    }

    /**
     * Build the hash code.
     *
     * @return the hash code.
     */
    protected int buildHashCode() {
        int h = 17;
        h = 37 * h + principal.getName().hashCode();
        h = 37 * h + privileges;
        h = 37 * h + Boolean.valueOf(allow).hashCode();
        return h;
    }

    //-------------------------------------------------< AccessControlEntry >---
    /**
     * @see org.apache.jackrabbit.api.jsr283.security.AccessControlEntry#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.security.AccessControlEntry#getPrivileges()
     */
    public Privilege[] getPrivileges() {
        return PrivilegeRegistry.getPrivileges(privileges);
    }

    //--------------------------------------------------------< PolicyEntry >---
    /**
     * @see PolicyEntry#isAllow()
     */
    public boolean isAllow() {
        return allow;
    }

    /**
     * @see PolicyEntry#getPrivilegeBits()
     */
    public int getPrivilegeBits() {
        return privileges;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = buildHashCode();
        }
        return hashCode;
    }

    /**
     * Returns true if the principal, the allow-flag and all privileges are
     * equal / the same.
     *
     * @param obj
     * @return
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof AbstractPolicyEntry) {
            AbstractPolicyEntry tmpl = (AbstractPolicyEntry) obj;
            return principal.getName().equals(tmpl.principal.getName()) &&
                   allow == tmpl.allow &&
                   privileges == tmpl.privileges;
        }
        return false;
    }
}