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

import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.commons.collections.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;

/**
 * {@link PolicyTemplate}-Implementation for the resource-based {@link ACLImpl}.
 *
 * @see PolicyTemplate
 * @see ACLImpl
 */
class ACLTemplate implements PolicyTemplate {

    private static final Logger log = LoggerFactory.getLogger(ACLTemplate.class);

    private final String path;
    private final String name = ACLImpl.POLICY_NAME;
    private final String description;

    /**
     * Map containing the entries of this ACL Template using the principal
     * name as key. The value represents a List containing maximal one grant
     * and one deny ACE per principal.
     */
    private final Map entries = new HashMap();

    /**
     * Construct a new empty {@link PolicyTemplate}.
     */
    ACLTemplate(String path) {
        this.path = path;
        description = null;
    }

    /**
     * Create a {@link PolicyTemplate} that is used to edit an existing ACL
     * node.
     */
    ACLTemplate(NodeImpl aclNode) throws RepositoryException {
        this(aclNode, Collections.EMPTY_SET);
    }

    /**
     * Create a {@link PolicyTemplate} that is used to edit an existing ACL
     * policy but only lists those entries that match any of the principal
     * names present in the given filter. If the set is empty all entry will
     * be read as local entries. Otherwise only the entries matching any of
     * the principals in the set will be retrieved.
     */
    ACLTemplate(NodeImpl aclNode, Set principalNames) throws RepositoryException {
        if (aclNode == null || !aclNode.isNodeType(AccessControlConstants.NT_REP_ACL)) {
            throw new IllegalArgumentException("Node must be of type: " +
                    AccessControlConstants.NT_REP_ACL);
        }
        path = aclNode.getPath();
        description = null;
        loadEntries(aclNode, principalNames);
    }

    /**
     * Returns those {@link PolicyEntry entries} of this
     * <code>PolicyTemplate</code> that affect the permissions of the given
     * <code>principal</code>.
     *
     * @return the {@link PolicyEntry entries} present in this
     * <code>PolicyTemplate</code> that affect the permissions of the given
     * <code>principal</code>.
     */
    ACEImpl[] getEntries(Principal principal) {
        List l = internalGetEntries(principal);
        return (ACEImpl[]) l.toArray(new ACEImpl[l.size()]);
    }

    private void checkValidEntry(PolicyEntry entry) throws AccessControlException {
        if (!(entry instanceof ACEImpl)) {
            throw new AccessControlException("Invalid PolicyEntry " + entry + ". Expected instanceof ACEImpl.");
        }
        ACEImpl ace = (ACEImpl) entry;
        // TODO: ev. assert that the principal is known to the repository
        // make sure valid privileges are provided.
        PrivilegeRegistry.getBits(ace.getPrivileges());
    }

    private List internalGetEntries(Principal principal) {
        String principalName = principal.getName();
        if (entries.containsKey(principalName)) {
            return (List) entries.get(principalName);
        } else {
            return new ArrayList(2);
        }
    }

    private synchronized boolean internalAdd(ACEImpl entry) {
        Principal principal = entry.getPrincipal();
        List l = internalGetEntries(principal);
        if (l.isEmpty()) {
            l.add(entry);
            entries.put(principal.getName(), l);
            return true;
        } else {
            return adjustEntries(entry, l);
        }
    }

    private static boolean adjustEntries(ACEImpl entry, List l) {
        if (l.contains(entry)) {
            // the same entry is already contained -> no modification
            return false;
        }

        ACEImpl complementEntry = null;
        ACEImpl[] entries = (ACEImpl[]) l.toArray(new ACEImpl[l.size()]);
        for (int i = 0; i < entries.length; i++) {
            ACEImpl t = entries[i];
            if (entry.isAllow() == entries[i].isAllow()) {
                // replace the existing entry with the new one at the end.
                l.remove(i);
                l.add(entry);
            } else {
                complementEntry = t;
            }
        }

        // make sure, that the complement entry (if existing) does not
        // grant/deny the same privileges -> remove privs that are now
        // denied/granted.
        if (complementEntry != null) {
            int complPrivs = complementEntry.getPrivilegeBits();
            int resultPrivs = PrivilegeRegistry.diff(complPrivs, entry.getPrivilegeBits());
            if (resultPrivs == PrivilegeRegistry.NO_PRIVILEGE) {
                l.remove(complementEntry);
            } else if (resultPrivs != complPrivs) {
                l.remove(complementEntry);
                ACEImpl tmpl = new ACEImpl(entry.getPrincipal(), resultPrivs, !entry.isAllow());
                l.add(tmpl);
            } /* else: complement entry is null or does not need to be modified.*/
        }
        return true;
    }

    private synchronized boolean internalRemove(ACEImpl entry) {
        List l = internalGetEntries(entry.getPrincipal());
        boolean success = l.remove(entry);
        if (l.isEmpty()) {
            entries.remove(entry.getPrincipal().getName());
        }
        return success;
    }

    /**
     * Read the child nodes of the given node and build {@link ACEImpl}
     * objects. If the filter set is not empty, the entries are
     * collected separately for each principal.
     *
     * @param aclNode
     * @param filter Set of principal names used to filter the entries present
     * within this ACL.
     */
    private void loadEntries(NodeImpl aclNode, Set filter)
            throws RepositoryException {
        PrincipalManager pMgr = ((SessionImpl) aclNode.getSession()).getPrincipalManager();
        // NOTE: don't simply add the individual matching entries, instead
        // collect entries separated for the principals first and later add
        // them in the order the need to be evaluated (order of principals).
        // therefore use ListOrderedMap in order to preserve the order of the
        // principalNames passed with the 'filter'.
        String noFilter = "";
        Map princToEntries = new ListOrderedMap();
        if (filter == null || filter.isEmpty()) {
            princToEntries.put(noFilter, new ArrayList());
        } else {
            for (Iterator it = filter.iterator(); it.hasNext();) {
                princToEntries.put(it.next().toString(), new ArrayList());
            }
        }

        NodeIterator itr = aclNode.getNodes();
        while (itr.hasNext()) {
            NodeImpl aceNode = (NodeImpl) itr.nextNode();
            String principalName = aceNode.getProperty(AccessControlConstants.P_PRINCIPAL_NAME).getString();
            // only process aceNode if no filter is present of if the filter
            // contains the principal-name defined with the ace-Node
            String key = (filter == null || filter.isEmpty()) ? noFilter : principalName;
            if (princToEntries.containsKey(key)) {
                Principal princ = pMgr.getPrincipal(principalName);
                Value[] privValues = aceNode.getProperty(AccessControlConstants.P_PRIVILEGES).getValues();
                String[] privNames = new String[privValues.length];
                for (int i = 0; i < privValues.length; i++) {
                    privNames[i] = privValues[i].getString();
                }
                // create a new ACEImpl
                ACEImpl ace = new ACEImpl(
                        princ,
                        PrivilegeRegistry.getBits(privNames),
                        aceNode.isNodeType(AccessControlConstants.NT_REP_GRANT_ACE));
                // add it to the proper list (e.g. separated by principals)
                ((List) princToEntries.get(key)).add(ace);
            }
        }

        // now retrieve the entries for each principal names and add them
        // to the single (complete) list of all entries that need to
        // be evaluated.
        for (Iterator it = princToEntries.keySet().iterator(); it.hasNext();) {
            String princName = it.next().toString();
            for (Iterator entries = ((List) princToEntries.get(princName)).iterator();
                 entries.hasNext();) {
                ACEImpl ace = (ACEImpl) entries.next();
                internalAdd(ace);
            }
        }
    }

    //------------------------------------------------< AccessControlPolicy >---
    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getName()
     */
    public String getName() throws RepositoryException {
        return name;
    }

    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getDescription()
     */
    public String getDescription() throws RepositoryException {
        return description;
    }

    //-----------------------------------------------------< PolicyTemplate >---
    /**
     * @see PolicyTemplate#getPath()
     */
    public String getPath() {
        return path;
    }

    /**
     * @see PolicyTemplate#isEmpty()
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see PolicyTemplate#size()
     */
    public int size() {
        return entries.size();
    }

    /**
     * @see PolicyTemplate#getEntries()
     */
    public PolicyEntry[] getEntries() {
        List l = new ArrayList();
        for (Iterator it = entries.values().iterator(); it.hasNext();) {
            l.addAll((List) it.next());
        }
        return (PolicyEntry[]) l.toArray(new PolicyEntry[l.size()]);
    }

    /**
     * @see PolicyTemplate#setEntry(PolicyEntry)
     */
    public boolean setEntry(PolicyEntry entry) throws AccessControlException, RepositoryException {
        checkValidEntry(entry);
        return internalAdd((ACEImpl) entry);
    }

    /**
     * @see PolicyTemplate#removeEntry(PolicyEntry)
     */
    public boolean removeEntry(PolicyEntry entry) throws AccessControlException, RepositoryException {
        checkValidEntry(entry);
        return internalRemove((ACEImpl) entry);
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
     * Returns true if the name and the entries are equal; false otherwise.
     *
     * @param obj
     * @return true if the name and the entries are equal; false otherwise.
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ACLTemplate) {
            ACLTemplate tmpl = (ACLTemplate) obj;
            boolean equalName = (name == null || tmpl.name == null || name.equals(tmpl.name));
            return equalName && entries.equals(tmpl.entries);
        }
        return false;
    }
}
