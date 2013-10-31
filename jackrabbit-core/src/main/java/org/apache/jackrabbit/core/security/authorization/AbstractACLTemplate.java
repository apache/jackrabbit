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

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import javax.jcr.security.AccessControlEntry;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractACLTemplate</code>...
 */
public abstract class AbstractACLTemplate implements JackrabbitAccessControlList,
        AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(AbstractACLTemplate.class);

    /**
     * Path of the node this ACL template has been created for.
     */
    protected final String path;

    /**
     * The value factory
     */
    protected final ValueFactory valueFactory;

    protected AbstractACLTemplate(String path, ValueFactory valueFactory) {
        this.path = path;
        this.valueFactory = valueFactory;
    }

    /**
     * Validates the given parameters to create a new ACE and throws an
     * <code>AccessControlException</code> if any of them is invalid. Otherwise
     * this method returns silently.
     *
     * @param principal The principal to create the ACE for.
     * @param privileges The privileges to be granted/denied by the ACE.
     * @param isAllow Defines if the privileges are allowed or denied.
     * @param restrictions The additional restrictions.
     * @throws AccessControlException If any of the given parameters is invalid.
     */
    protected abstract void checkValidEntry(Principal principal,
                                            Privilege[] privileges,
                                            boolean isAllow,
                                            Map<String, Value> restrictions) throws AccessControlException;

    /**
     * Return the list of entries, if they are held in an orderable list.
     *
     * @return the list of entries.
     * @see #orderBefore(AccessControlEntry, AccessControlEntry)
     */
    protected abstract List<AccessControlEntry> getEntries();

    //--------------------------------------< JackrabbitAccessControlPolicy >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy#getPath()
     */
    public String getPath() {
        return path;
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, Collections.<String, Value>emptyMap());
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean, Map, Map)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions, Map<String, Value[]> mvRestrictions) throws AccessControlException, RepositoryException {
        if (mvRestrictions == null || mvRestrictions.isEmpty()) {
            return addEntry(principal, privileges, isAllow, restrictions);
        } else {
            throw new UnsupportedRepositoryOperationException("Not implemented. Please use Jackrabbit OAK to get support for multi-valued restrictions.");
        }
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#size()
     */
    public int size() {
        return getEntries().size();
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#isEmpty()
     */
    public boolean isEmpty() {
        return getEntries().isEmpty();
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#orderBefore(javax.jcr.security.AccessControlEntry, javax.jcr.security.AccessControlEntry)
     */
    public void orderBefore(AccessControlEntry srcEntry, AccessControlEntry destEntry) throws AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        if (srcEntry.equals(destEntry)) {
            log.debug("srcEntry equals destEntry -> no reordering required.");
            return;
        }

        List<AccessControlEntry> entries = getEntries();
        int index = (destEntry == null) ? entries.size()-1 : entries.indexOf(destEntry);
        if (index < 0) {
            throw new AccessControlException("destEntry not contained in this AccessControlList");
        } else {
            if (entries.remove(srcEntry)) {
                // re-insert the srcEntry at the new position.
                entries.add(index, srcEntry);
            } else {
                // src entry not contained in this list.
                throw new AccessControlException("srcEntry not contained in this AccessControlList");
            }
        }
    }

    //--------------------------------------------------< AccessControlList >---
    /**
     * @see javax.jcr.security.AccessControlList#getAccessControlEntries()
     */
    public AccessControlEntry[] getAccessControlEntries() throws RepositoryException {       
        List<? extends AccessControlEntry> l = getEntries();
        return l.toArray(new AccessControlEntry[l.size()]);
    }

    /**
     * @see javax.jcr.security.AccessControlList#addAccessControlEntry(java.security.Principal , javax.jcr.security.Privilege[])
     */
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.<String, Value>emptyMap());
    }
}
