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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;

import java.security.Principal;

/**
 * Simple, immutable implementation of the
 * {@link org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry}
 * and the {@link PolicyEntry} interfaces.
 * 
 * @see PolicyEntry
 */
class ACEImpl implements PolicyEntry {

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
    private final int hashCode;

    /**
     * Construct an access control entry for the given principal, privileges and
     * a polarity (deny or allow)
     *
     * @param principal
     * @param privileges
     * @param allow
     */
    ACEImpl(Principal principal, int privileges, boolean allow) {
        this.principal = principal;
        this.privileges = privileges;
        this.allow = allow;

        int h = 17;
        h = 37 * h + principal.getName().hashCode();
        h = 37 * h + privileges;
        h = 37 * h + Boolean.valueOf(allow).hashCode();
        hashCode = h;
    }

    /**
     * @return the int representation of the privileges defined for this entry.
     * @see #getPrivileges() 
     */
    int getPrivilegeBits() {
        return privileges;
    }

    //-------------------------------------------------< AccessControlEntry >---
    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry#getPrivileges()
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

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
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

        if (obj instanceof ACEImpl) {
            ACEImpl tmpl = (ACEImpl) obj;
            // TODO: check again if comparing principal-name is sufficient
            return principal.getName().equals(tmpl.principal.getName()) &&
                   allow == tmpl.allow &&
                   privileges == tmpl.privileges;
        }
        return false;
    }
}
