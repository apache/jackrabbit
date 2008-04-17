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
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * <code>PolicyTemplateImpl</code>...
 */
class PolicyTemplateImpl implements PolicyTemplate {

    private static Logger log = LoggerFactory.getLogger(PolicyTemplateImpl.class);

    private final Principal principal;
    private final String acAbsPath;
    private final List entries = new ArrayList();

    PolicyTemplateImpl(List policyEntries, Principal principal, String acAbsPath) {
        this.principal = principal;
        this.entries.addAll(policyEntries);
        this.acAbsPath = acAbsPath;
    }

    Principal getPrincipal() {
        return principal;
    }

    //-----------------------------------------------------< PolicyTemplate >---
    public String getPath() {
        return acAbsPath;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public PolicyEntry[] getEntries() {
        return (PolicyEntry[]) entries.toArray(new PolicyEntry[entries.size()]);
    }

    public boolean setEntry(PolicyEntry entry) throws AccessControlException, RepositoryException {
        if (entry instanceof PolicyEntryImpl &&
            principal.equals(entry.getPrincipal())) {
            // make sure valid privileges are provided.
            PrivilegeRegistry.getBits(entry.getPrivileges());
            return internalAddEntry((PolicyEntryImpl) entry);
        } else {
            throw new AccessControlException("Invalid entry.");
        }
    }

    public boolean removeEntry(PolicyEntry entry) throws AccessControlException, RepositoryException {
        if (entry instanceof PolicyEntryImpl &&
            principal.equals(entry.getPrincipal())) {
            // make sure valid privileges are provided.
            PrivilegeRegistry.getBits(entry.getPrivileges());
            return entries.remove(entry);
        } else {
            throw new AccessControlException("Invalid entry.");
        }
    }

    //------------------------------------------------< AccessControlPolicy >---
    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getName()
     */
    public String getName() throws RepositoryException {
        return getClass().getName();
    }

    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getName()
     */
    public String getDescription() throws RepositoryException {
        return "Template for the user-based ACL: each ACL defining the access permissions for a single principal.";
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param entry
     * @return
     */
    private synchronized boolean internalAddEntry(PolicyEntryImpl entry) {
        if (entries.contains(entry)) {
            log.debug("Entry is already contained in policy -> no modification.");
            return false;
        }

        PolicyEntryImpl existing = null;
        for (Iterator it = entries.iterator(); it.hasNext() && existing == null;) {
            PolicyEntryImpl ex = (PolicyEntryImpl) it.next();
            if (ex.getNodePath().equals(entry.getNodePath()) &&
                ex.getGlob().equals(entry.getGlob()) &&
                ex.isAllow() == entry.isAllow()) {
                log.debug("Replacing existing policy entry: NodePath = " +
                        entry.getNodePath() +"; Glob = " +
                        entry.getGlob() + "; Changing privileges from " +
                        ex.getPrivilegeBits() + " to " + entry.getPrivilegeBits());
                existing = ex;
            }
        }

        if (existing != null) {
            int index = entries.indexOf(existing);
            return entries.set(index, entry) != null;
        } else {
            return entries.add(entry);
        }
    }
}