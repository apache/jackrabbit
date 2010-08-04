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

import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;

/**
 * An implementation of the <code>AccessControlList</code> interface that only
 * allows for reading. The write methods
 * ({@link #addAccessControlEntry(Principal principal, Privilege[] privileges) addAccessControlEntry}
 * and {@link #removeAccessControlEntry(AccessControlEntry) removeAccessControlEntry})
 * throw an <code>AccessControlException</code>.
 */
public class UnmodifiableAccessControlList implements JackrabbitAccessControlList {

    private final AccessControlEntry[] accessControlEntries;

    private final Map<String, Integer> restrictions;

    private final String path;

    /**
     * Construct a new <code>UnmodifiableAccessControlList</code>
     *
     * @param acl The AccessControlList to be wrapped in order to prevent
     * it's modification.
     * @throws RepositoryException The the entries cannot be retrieved from the
     * specified <code>AccessControlList</code>.
     */
    public UnmodifiableAccessControlList(AccessControlList acl) throws RepositoryException {
        if (acl instanceof JackrabbitAccessControlList) {
            JackrabbitAccessControlList jAcl = (JackrabbitAccessControlList) acl;
            accessControlEntries = acl.getAccessControlEntries();
            path = jAcl.getPath();
            Map<String, Integer> r = new HashMap<String, Integer>();
            for (String name: jAcl.getRestrictionNames()) {
                r.put(name, jAcl.getRestrictionType(name));
            }
            restrictions = Collections.unmodifiableMap(r);
        } else {
            accessControlEntries = acl.getAccessControlEntries();
            path = null;
            restrictions = Collections.emptyMap();
        }
    }

    /**
     * Construct a new <code>UnmodifiableAccessControlList</code>
     *
     * @param accessControlEntries A list of {@link AccessControlEntry access control entries}.
     */
    public UnmodifiableAccessControlList(List<AccessControlEntry> accessControlEntries) {
        this.accessControlEntries = accessControlEntries.toArray(new AccessControlEntry[accessControlEntries.size()]);
        path = null;
        restrictions = Collections.emptyMap();
    }

    //--------------------------------------------------< AccessControlList >---
    /**
     * @see AccessControlList#getAccessControlEntries()
     */
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        return accessControlEntries;
    }

    /**
     * @see AccessControlList#addAccessControlEntry(Principal, Privilege[])
     */
    public boolean addAccessControlEntry(Principal principal,
                                         Privilege[] privileges)
            throws AccessControlException, RepositoryException {
        throw new AccessControlException("Unmodifiable ACL. Use AccessControlManager#getApplicablePolicies in order to obtain an modifiable ACL.");
    }

    /**
     * @see AccessControlList#removeAccessControlEntry(AccessControlEntry)
     */
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        throw new AccessControlException("Unmodifiable ACL. Use AccessControlManager#getApplicablePolicies in order to obtain an modifiable ACL.");
    }

    public String[] getRestrictionNames() {
        return restrictions.keySet().toArray(new String[restrictions.size()]);
    }

    public int getRestrictionType(String restrictionName) {
        if (restrictions.containsKey(restrictionName)) {
            return restrictions.get(restrictionName);
        } else {
            return PropertyType.UNDEFINED;
        }
    }

    public boolean isEmpty() {
        return accessControlEntries.length == 0;
    }

    public int size() {
        return accessControlEntries.length;
    }

    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow) throws AccessControlException {
        throw new AccessControlException("Unmodifiable ACL. Use AccessControlManager#getPolicy or #getApplicablePolicies in order to obtain an modifiable ACL.");
    }

    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions) throws AccessControlException {
        throw new AccessControlException("Unmodifiable ACL. Use AccessControlManager#getPolicy or #getApplicablePolicies in order to obtain an modifiable ACL.");
    }

    public void orderBefore(AccessControlEntry srcEntry, AccessControlEntry destEntry) throws AccessControlException {
        throw new AccessControlException("Unmodifiable ACL. Use AccessControlManager#getPolicy or #getApplicablePolicy in order to obtain a modifiable ACL.");
    }

    public String getPath() {
        return path;
    }
}