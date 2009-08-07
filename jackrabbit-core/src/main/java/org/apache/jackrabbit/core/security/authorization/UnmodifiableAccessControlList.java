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

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.Privilege;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.List;

/**
 * An implementation of the <code>AccessControlList</code> interface that only
 * allows for reading. The write methods
 * ({@link #addAccessControlEntry(Principal principal, Privilege[] privileges) addAccessControlEntry}
 * and {@link #removeAccessControlEntry(AccessControlEntry) removeAccessControlEntry})
 * throw an <code>AccessControlException</code>.
 */
public class UnmodifiableAccessControlList implements AccessControlList {

    private final AccessControlEntry[] accessControlEntries;

    /**
     * Construct a new <code>UnmodifiableAccessControlList</code>
     *
     * @param acl The AccessControlList to be wrapped in order to prevent
     * it's modification.
     * @throws RepositoryException The the entries cannot be retrieved from the
     * specified <code>AccessControlList</code>.
     */
    public UnmodifiableAccessControlList(AccessControlList acl) throws RepositoryException {
        accessControlEntries = acl.getAccessControlEntries();
    }

    /**
     * Construct a new <code>UnmodifiableAccessControlList</code>
     *
     * @param accessControlEntries A list of {@link AccessControlEntry access control entries}.
     */
    public UnmodifiableAccessControlList(List accessControlEntries) {
        this.accessControlEntries = (AccessControlEntry[]) accessControlEntries.toArray(new AccessControlEntry[accessControlEntries.size()]);
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
}