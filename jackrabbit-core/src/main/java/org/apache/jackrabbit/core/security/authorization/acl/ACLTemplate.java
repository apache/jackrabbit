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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractACLTemplate;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.UnknownPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the
 * {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList}
 * interface that is detached from the effective access control content.
 * Consequently, any modifications applied to this ACL only take effect, if
 * the policy gets
 * {@link javax.jcr.security.AccessControlManager#setPolicy(String, javax.jcr.security.AccessControlPolicy) reapplied}
 * to the <code>AccessControlManager</code> and the changes are saved.
 */
class ACLTemplate extends AbstractACLTemplate {

    private static final Logger log = LoggerFactory.getLogger(ACLTemplate.class);

    /**
     * List containing the entries of this ACL Template with maximal one
     * grant and one deny ACE per principal.
     */
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * The principal manager used for validation checks
     */
    private final PrincipalManager principalMgr;

    /**
     * The privilege registry
     */
    private final PrivilegeRegistry privilegeRegistry;

    /**
     * Construct a new empty {@link ACLTemplate}.
     *
     * @param path path
     * @param privilegeRegistry registry
     * @param valueFactory value factory
     * @param principalMgr manager
     */
    ACLTemplate(String path, PrincipalManager principalMgr, 
                PrivilegeRegistry privilegeRegistry, ValueFactory valueFactory) {
        super(path, valueFactory);
        this.principalMgr = principalMgr;
        this.privilegeRegistry = privilegeRegistry;
    }

    /**
     * Create a {@link ACLTemplate} that is used to edit an existing ACL
     * node.
     *
     * @param aclNode node
     * @param privilegeRegistry registry
     * @throws RepositoryException if an error occurs
     */
    ACLTemplate(NodeImpl aclNode, PrivilegeRegistry privilegeRegistry) throws RepositoryException {
        super((aclNode != null) ? aclNode.getParent().getPath() : null, (aclNode != null) ? aclNode.getSession().getValueFactory() : null);
        if (aclNode == null || !aclNode.isNodeType(AccessControlConstants.NT_REP_ACL)) {
            throw new IllegalArgumentException("Node must be of type 'rep:ACL'");
        }
        SessionImpl sImpl = (SessionImpl) aclNode.getSession();
        principalMgr = sImpl.getPrincipalManager();
        
        this.privilegeRegistry = privilegeRegistry;

        // load the entries:
        AccessControlManager acMgr = sImpl.getAccessControlManager();
        NodeIterator itr = aclNode.getNodes();
        while (itr.hasNext()) {
            NodeImpl aceNode = (NodeImpl) itr.nextNode();
            try {
                String principalName = aceNode.getProperty(AccessControlConstants.P_PRINCIPAL_NAME).getString();
                Principal princ = principalMgr.getPrincipal(principalName);
                if (princ == null) {
                    log.debug("Principal with name " + principalName + " unknown to PrincipalManager.");
                    princ = new PrincipalImpl(principalName);
                }

                Value[] privValues = aceNode.getProperty(AccessControlConstants.P_PRIVILEGES).getValues();
                Privilege[] privs = new Privilege[privValues.length];
                for (int i = 0; i < privValues.length; i++) {
                    privs[i] = acMgr.privilegeFromName(privValues[i].getString());
                }
                // create a new ACEImpl (omitting validation check)
                Entry ace = createEntry(
                        princ,
                        privs,
                        aceNode.isNodeType(AccessControlConstants.NT_REP_GRANT_ACE));
                // add the entry
                internalAdd(ace);
            } catch (RepositoryException e) {
                log.debug("Failed to build ACE from content.", e.getMessage());
            }
        }
    }

    /**
     * Create a new entry omitting any validation checks.
     * 
     * @param principal
     * @param privileges
     * @param isAllow
     * @return
     */
    Entry createEntry(Principal principal, Privilege[] privileges, boolean isAllow) throws AccessControlException {
        return new Entry(principal, privileges, isAllow);
    }

    private List<Entry> internalGetEntries(Principal principal) {
        String principalName = principal.getName();
        List entriesPerPrincipal = new ArrayList(2);
        for (Entry entry : entries) {
            if (principalName.equals(entry.getPrincipal().getName())) {
                entriesPerPrincipal.add(entry);
            }
        }
        return entriesPerPrincipal;
    }

    private synchronized boolean internalAdd(Entry entry) throws AccessControlException {
        Principal principal = entry.getPrincipal();
        List<Entry> entriesPerPrincipal = internalGetEntries(principal);
        if (entriesPerPrincipal.isEmpty()) {
            // simple case: just add the new entry at the end of the list.
            entries.add(entry);
            return true;
        } else {
            if (entriesPerPrincipal.contains(entry)) {
                // the same entry is already contained -> no modification
                return false;
            }
            // check if need to adjust existing entries
            int updateIndex = -1;
            Entry complementEntry = null;

            for (Entry e : entriesPerPrincipal) {
                if (entry.isAllow() == e.isAllow()) {
                    int existingPrivs = e.getPrivilegeBits();
                    if ((existingPrivs | ~entry.getPrivilegeBits()) == -1) {
                        // all privileges to be granted/denied are already present
                        // in the existing entry -> not modified
                        return false;
                    }

                    // remember the index of the existing entry to be updated later on.
                    updateIndex = entries.indexOf(e);

                    // remove the existing entry and create a new that includes
                    // both the new privileges and the existing ones.
                    entries.remove(e);
                    int mergedBits = e.getPrivilegeBits() | entry.getPrivilegeBits();
                    Privilege[] mergedPrivs = privilegeRegistry.getPrivileges(mergedBits);
                    // omit validation check.
                    entry = createEntry(entry.getPrincipal(), mergedPrivs, entry.isAllow());
                } else {
                    complementEntry = e;
                }
            }

            // make sure, that the complement entry (if existing) does not
            // grant/deny the same privileges -> remove privileges that are now
            // denied/granted.
            if (complementEntry != null) {

                int complPrivs = complementEntry.getPrivilegeBits();
                int resultPrivs = Permission.diff(complPrivs, entry.getPrivilegeBits());

                if (resultPrivs == PrivilegeRegistry.NO_PRIVILEGE) {
                    // remove the complement entry as the new entry covers
                    // all privileges granted by the existing entry.
                    entries.remove(complementEntry);
                    updateIndex--;
                    
                } else if (resultPrivs != complPrivs) {
                    // replace the existing entry having the privileges adjusted
                    int index = entries.indexOf(complementEntry);
                    entries.remove(complementEntry);
                    Entry tmpl = createEntry(entry.getPrincipal(),
                            privilegeRegistry.getPrivileges(resultPrivs),
                            !entry.isAllow());
                    entries.add(index, tmpl);
                } /* else: does not need to be modified.*/
            }

            // finally update the existing entry or add the new entry passed
            // to this method at the end.
            if (updateIndex < 0) {
                entries.add(entry);
            } else {
                entries.add(updateIndex, entry);
            }
            return true;
        }
    }

    //------------------------------------------------< AbstractACLTemplate >---
    /**
     * @see AbstractACLTemplate#checkValidEntry(java.security.Principal, javax.jcr.security.Privilege[], boolean, java.util.Map) 
     */
    @Override
    protected void checkValidEntry(Principal principal, Privilege[] privileges,
                                 boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException {
        if (restrictions != null && !restrictions.isEmpty()) {
            throw new AccessControlException("This AccessControlList does not allow for additional restrictions.");
        }
        // validate principal
        if (principal instanceof UnknownPrincipal) {
            log.debug("Consider fallback principal as valid: {}", principal.getName());
        } else if (!principalMgr.hasPrincipal(principal.getName())) {
            throw new AccessControlException("Principal " + principal.getName() + " does not exist.");
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.AbstractACLTemplate#getEntries()
     */
    @Override
    protected List<? extends AccessControlEntry> getEntries() {
        return entries;
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
     * @see javax.jcr.security.AccessControlList#removeAccessControlEntry(AccessControlEntry)
     */
    public synchronized void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (!(ace instanceof Entry)) {
            throw new AccessControlException("Invalid AccessControlEntry implementation " + ace.getClass().getName() + ".");
        }
        if (entries.contains(ace)) {
            entries.remove(ace);
        } else {
            throw new AccessControlException("AccessControlEntry " + ace + " cannot be removed from ACL defined at " + getPath());
        }
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    /**
     * Returns an empty String array.
     *
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionType(String)
     */
    public String[] getRestrictionNames() {
        return new String[0];
    }

    /**
     * Always returns {@link PropertyType#UNDEFINED} as no restrictions are
     * supported.
     *
     * @see JackrabbitAccessControlList#getRestrictionType(String)
     */
    public int getRestrictionType(String restrictionName) {
        return PropertyType.UNDEFINED;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#isEmpty()
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#size()
     */
    public int size() {
        return getEntries().size();
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean, Map)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        checkValidEntry(principal, privileges, isAllow, restrictions);
        Entry ace = createEntry(principal, privileges, isAllow);
        return internalAdd(ace);
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    /**
     * Returns true if the path and the entries are equal; false otherwise.
     *
     * @param obj Object to be tested.
     * @return true if the path and the entries are equal; false otherwise.
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ACLTemplate) {
            ACLTemplate acl = (ACLTemplate) obj;
            return path.equals(acl.path) && entries.equals(acl.entries);
        }
        return false;
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    class Entry extends AccessControlEntryImpl {

        private Entry(Principal principal, Privilege[] privileges, boolean allow)
                throws AccessControlException {
            super(principal, privileges, allow, Collections.<String, Value>emptyMap(), valueFactory);
        }

        /**
         * @param nodePath
         * @return <code>true</code> if this entry is defined on the node
         * at <code>nodePath</code>
         */
        boolean isLocal(String nodePath) {
            return path != null && path.equals(nodePath);
        }
    }
}
