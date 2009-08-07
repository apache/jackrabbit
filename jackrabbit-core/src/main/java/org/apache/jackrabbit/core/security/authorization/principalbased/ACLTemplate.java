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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the {@link JackrabbitAccessControlList} interface that
 * is detached from the effective access control content. Consequently, any
 * modifications applied to this ACL only take effect, if the policy gets
 * {@link org.apache.jackrabbit.api.jsr283.security.AccessControlManager#setPolicy(String, org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy) reapplied}
 * to the <code>AccessControlManager</code> and the changes are saved.
 */
class ACLTemplate implements JackrabbitAccessControlList, AccessControlConstants {

    private static Logger log = LoggerFactory.getLogger(ACLTemplate.class);

    /**
     * rep:nodePath property name (optional if the ACL is stored with the
     * node itself).
     */
    static final Name P_NODE_PATH = NF.create(Name.NS_REP_URI, "nodePath");
    /**
     * rep:glob property name used to restrict the number of child nodes
     * or properties that are affected by the privileges applied at
     * rep:nodePath
     */
    static final Name P_GLOB = NF.create(Name.NS_REP_URI, "glob");

    private final Principal principal;
    private final String path;
    private final ValueFactory valueFactory;

    private final List entries = new ArrayList();

    private final String jcrNodePathName;
    private final String jcrGlobName;

    ACLTemplate(Principal principal, String path, NamePathResolver resolver, ValueFactory vf) throws RepositoryException {
        this(principal, path, null, resolver, vf);
    }

    ACLTemplate(Principal principal, NodeImpl acNode) throws RepositoryException {
        this(principal, acNode.getPath(), acNode, (SessionImpl) acNode.getSession(), acNode.getSession().getValueFactory());
    }

    private ACLTemplate(Principal principal, String path, NodeImpl acNode,
                        NamePathResolver resolver, ValueFactory vf)
            throws RepositoryException {
        this.principal = principal;
        this.path = path;
        this.valueFactory = vf;

        jcrNodePathName = resolver.getJCRName(P_NODE_PATH);
        jcrGlobName = resolver.getJCRName(P_GLOB);

        if (acNode != null && acNode.hasNode(N_POLICY)) {
            // build the list of policy entries;
            NodeImpl aclNode = acNode.getNode(N_POLICY);
            AccessControlManager acMgr = ((SessionImpl) aclNode.getSession()).getAccessControlManager();

            // loop over all entries in the aclNode for the princ-Principal
            for (NodeIterator aceNodes = aclNode.getNodes(); aceNodes.hasNext();) {
                NodeImpl aceNode = (NodeImpl) aceNodes.nextNode();
                if (aceNode.isNodeType(NT_REP_ACE)) {
                    // the isAllow flag:
                    boolean isAllow = aceNode.isNodeType(NT_REP_GRANT_ACE);
                    // the privileges
                    Value[] pValues = aceNode.getProperty(P_PRIVILEGES).getValues();
                    Privilege[] privileges = new Privilege[pValues.length];
                    for (int i = 0; i < pValues.length; i++) {
                        privileges[i] = acMgr.privilegeFromName(pValues[i].getString());
                    }
                    // the restrictions:
                    Map restrictions = new HashMap(2);
                    Property prop = aceNode.getProperty(P_NODE_PATH);
                    restrictions.put(prop.getName(), prop.getValue());

                    if (aceNode.hasProperty(P_GLOB)) {
                        prop = aceNode.getProperty(P_GLOB);
                        restrictions.put(prop.getName(), prop.getValue());
                    }
                    // finally add the entry
                    AccessControlEntry entry = createEntry(principal, privileges, isAllow, restrictions);
                    entries.add(entry);
                } else {
                    log.warn("ACE must be of nodetype rep:ACE -> ignored child-node " + aceNode.getPath());
                }
            }
        } // else: no-node at all or no acl-node present.
    }

    AccessControlEntry createEntry(Principal princ, Privilege[] privileges, boolean allow, Map restrictions) throws RepositoryException {
        if (!principal.equals(princ)) {
            throw new AccessControlException("Invalid principal. Expected: " + principal);
        }
        if (!allow && principal instanceof Group) {
            throw new AccessControlException("For group principals permissions can only be added but not denied.");
        }

        Set rNames = restrictions.keySet();
        if (!rNames.contains(jcrNodePathName)) {
            throw new AccessControlException("Missing mandatory restriction: " + jcrNodePathName);
        }

        // make sure the nodePath restriction is of type PATH
        Value v = (Value) restrictions.get(jcrNodePathName);
        if (v.getType() != PropertyType.PATH) {
            v = valueFactory.createValue(v.getString(), PropertyType.PATH);
            restrictions.put(jcrNodePathName, v);
        }
        // ... and glob is of type STRING.
        v = (Value) restrictions.get(jcrGlobName);
        if (v != null && v.getType() != PropertyType.STRING) {
            v = valueFactory.createValue(v.getString(), PropertyType.STRING);
            restrictions.put(jcrGlobName, v);
        }
        return new Entry(princ, privileges, allow, restrictions);
    }

    //-----------------------------------------------------< JackrabbitAccessControlList >---
    /**
     * @see JackrabbitAccessControlList#getPath()
     */
    public String getPath() {
        return path;
    }

    /**
     * @see JackrabbitAccessControlList#getRestrictionNames()
     */
    public String[] getRestrictionNames() {
        return new String[] {jcrNodePathName, jcrGlobName};
    }

    /**
     * @see JackrabbitAccessControlList#getRestrictionType(String)
     */
    public int getRestrictionType(String restrictionName) {
        if (jcrNodePathName.equals(restrictionName)) {
            return PropertyType.PATH;
        } else if (jcrGlobName.equals(restrictionName)) {
            return PropertyType.STRING;
        } else {
            return PropertyType.UNDEFINED;
        }
    }

    /**
     * @see JackrabbitAccessControlList#isEmpty()
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see JackrabbitAccessControlList#size()
     */
    public int size() {
        return entries.size();
    }

    /**
     * @see JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, null);
    }

    /**
     * Known restrictions are:
     * <pre>
     *   rep:nodePath  (mandatory) value-type: PATH
     *   rep:glob      (optional)  value-type: STRING
     * </pre>
     *
     * @see JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean, Map)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map restrictions)
            throws AccessControlException, RepositoryException {
        if (restrictions == null || restrictions.isEmpty()) {
            log.debug("Restrictions missing. Using default: rep:nodePath = " + getPath() + "; rep:glob = null.");
            // default restrictions:
            restrictions = Collections.singletonMap(jcrNodePathName,
                    valueFactory.createValue(getPath(), PropertyType.PATH));
        }
        AccessControlEntry entry = createEntry(principal, privileges, isAllow, restrictions);
        if (entries.contains(entry)) {
            log.debug("Entry is already contained in policy -> no modification.");
            return false;
        } else {
            // TODO: to be improved. clean redundant entries
            entries.add(0, entry);
            return true;
        }
    }

    //--------------------------------------------------< AccessControlList >---
    /**
     * @see AccessControlList#getAccessControlEntries()
     */
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        return (AccessControlEntry[]) entries.toArray(new AccessControlEntry[entries.size()]);
    }

    /**
     * @see AccessControlList#addAccessControlEntry(Principal, Privilege[])
     */
    public boolean addAccessControlEntry(Principal principal,
                                         Privilege[] privileges)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.EMPTY_MAP);
    }

    /**
     * @see AccessControlList#removeAccessControlEntry(AccessControlEntry)
     */
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (!(ace instanceof Entry)) {
            throw new AccessControlException("Invalid AccessControlEntry implementation " + ace.getClass().getName() + ".");
        }
        if (!entries.remove(ace)) {
            throw new AccessControlException("Cannot remove AccessControlEntry " + ace);
        }
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
     * @param obj Object to test.
     * @return true if the path and the entries are equal; false otherwise.
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ACLTemplate) {
            ACLTemplate acl = (ACLTemplate) obj;
            return principal.equals(acl.principal) &&
                   path.equals(acl.path) && entries.equals(acl.entries);
        }
        return false;
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    class Entry extends AccessControlEntryImpl {

        /**
         *
         */
        private final String nodePath;

        /**
         * Globbing pattern
         */
        private final GlobPattern pattern;

        private Entry(Principal principal, Privilege[] privileges, boolean allow, Map restrictions)
                throws AccessControlException, RepositoryException {
            super(principal, privileges, allow, restrictions, valueFactory);

            // TODO: review again
            Value np = getRestriction(jcrNodePathName);
            nodePath = getRestriction(jcrNodePathName).getString();
            Value glob = getRestriction(jcrGlobName);
            if (glob != null) {
                StringBuffer b = new StringBuffer(nodePath);
                b.append(glob.getString());
                pattern = GlobPattern.create(b.toString());
            } else {
                pattern = GlobPattern.create(nodePath);
            }
        }

        boolean matches(String jcrPath) throws RepositoryException {
            return pattern.matches(jcrPath);
        }

        boolean matches(Item item) throws RepositoryException {
            return pattern.matches(item);
        }

        boolean matchesNodePath(String jcrPath) {
            return nodePath.equals(jcrPath);
        }
    }
}