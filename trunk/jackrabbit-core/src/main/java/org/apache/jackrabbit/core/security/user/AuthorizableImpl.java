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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * AuthorizableImpl
 */
abstract class AuthorizableImpl implements Authorizable, UserConstants {

    static final Logger log = LoggerFactory.getLogger(AuthorizableImpl.class);

    final UserManagerImpl userManager;
    private final NodeImpl node;
    private int hashCode;

    /**
     * @param node The node this Authorizable is persisted to.
     * @param userManager UserManager that created this Authorizable.
     * @throws IllegalArgumentException if the given node isn't of node type
     * {@link #NT_REP_AUTHORIZABLE}.
     */
    protected AuthorizableImpl(NodeImpl node, UserManagerImpl userManager) {
        this.node = node;
        this.userManager = userManager;
    }

    //-------------------------------------------------------< Authorizable >---
    /**
     * Returns the unescaped name of the node that defines this <code>Authorizable</code>.
     *
     * @return the unescaped name of the node that defines this <code>Authorizable</code>.
     * @see Authorizable#getID()
     */
    public String getID() throws RepositoryException {
        return Text.unescapeIllegalJcrChars(getNode().getName());
    }

    /**
     * @see Authorizable#declaredMemberOf()
     */
    public Iterator<Group> declaredMemberOf() throws RepositoryException {
        return collectMembership(false);
    }

    /**
     * @see Authorizable#memberOf()
     */
    public Iterator<Group> memberOf() throws RepositoryException {
        return collectMembership(true);
    }

    /**
     * @see Authorizable#getPropertyNames()
     */
    public Iterator<String> getPropertyNames() throws RepositoryException {
        List<String> l = new ArrayList<String>();
        for (PropertyIterator it = node.getProperties(); it.hasNext();) {
            Property prop = it.nextProperty();
            if (isAuthorizableProperty(prop, false)) {
                l.add(prop.getName());
            }
        }
        return l.iterator();
    }

    /**
     * @see Authorizable#getPropertyNames(String)
     */
    public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
        Node n = node.getNode(relPath);
        if (n.isSame(node)) {
            // same as #getPropertyNames()
            return getPropertyNames();
        } else if (Text.isDescendant(node.getPath(), n.getPath())) {
            List<String> l = new ArrayList<String>();
            for (PropertyIterator it = n.getProperties(); it.hasNext();) {
                Property prop = it.nextProperty();
                if (isAuthorizableProperty(prop, false)) {
                    l.add(prop.getName());
                }
            }
            return l.iterator();
        } else {
            throw new IllegalArgumentException("Relative path " + relPath + " refers to items outside of scope of authorizable " + getID());
        }
    }

    /**
     * @see #getProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        return node.hasProperty(relPath) && isAuthorizableProperty(node.getProperty(relPath), true);
    }

    /**
     * @see #hasProperty(String)
     * @see Authorizable#getProperty(String)
     */
    public Value[] getProperty(String relPath) throws RepositoryException {
        if (node.hasProperty(relPath)) {
            Property prop = node.getProperty(relPath);
            if (isAuthorizableProperty(prop, true)) {
                if (prop.isMultiple()) {
                    return prop.getValues();
                } else {
                    return new Value[]{prop.getValue()};
                }
            }
        }
        return null;
    }

    /**
     * Sets the Value for the given name. If a value existed, it is replaced,
     * if not it is created.
     *
     * @param relPath The relative path to the property or the property name.
     * @param value The property value.
     * @throws RepositoryException If the specified name defines a property
     * that needs to be modified by this user API or setting the corresponding
     * JCR property fails.
     * @see Authorizable#setProperty(String, Value)
     */
    public synchronized void setProperty(String relPath, Value value) throws RepositoryException {
        String name = Text.getName(relPath);
        String intermediate = (relPath.equals(name)) ? null : Text.getRelativeParent(relPath, 1);
        checkProtectedProperty(name);
        try {
            Node n = getOrCreateTargetNode(intermediate);
            // check if the property has already been created as multi valued
            // property before -> in this case remove in order to avoid
            // ValueFormatException.
            if (n.hasProperty(name)) {
                Property p = n.getProperty(name);
                if (p.isMultiple()) {
                    p.remove();
                }
            }
            n.setProperty(name, value);
            if (userManager.isAutoSave()) {
                node.save();
            }
        } catch (RepositoryException e) {
            log.debug("Failed to set Property " + name + " for " + this, e);
            node.refresh(false);
            throw e;
        }
    }

    /**
     * Sets the Value[] for the given name. If a value existed, it is replaced,
     * if not it is created.
     *
     * @param relPath The relative path to the property or the property name.
     * @param values The property values.
     * @throws RepositoryException If the specified name defines a property
     * that needs to be modified by this user API or setting the corresponding
     * JCR property fails.
     * @see Authorizable#setProperty(String, Value[])
     */
    public synchronized void setProperty(String relPath, Value[] values) throws RepositoryException {
        String name = Text.getName(relPath);
        String intermediate = (relPath.equals(name)) ? null : Text.getRelativeParent(relPath, 1);
        checkProtectedProperty(name);
        try {
            Node n = getOrCreateTargetNode(intermediate);
            // check if the property has already been created as single valued
            // property before -> in this case remove in order to avoid
            // ValueFormatException.
            if (n.hasProperty(name)) {
                Property p = n.getProperty(name);
                if (!p.isMultiple()) {
                    p.remove();
                }
            }
            n.setProperty(name, values);
            if (userManager.isAutoSave()) {
                node.save();
            }
        } catch (RepositoryException e) {
            log.debug("Failed to set Property " + name + " for " + this, e);
            node.refresh(false);
            throw e;
        }
    }

    /**
     * @see Authorizable#removeProperty(String)
     */
    public synchronized boolean removeProperty(String relPath) throws RepositoryException {
        String name = Text.getName(relPath);        
        checkProtectedProperty(name);
        try {
            if (node.hasProperty(relPath)) {
                Property p = node.getProperty(relPath);
                if (isAuthorizableProperty(p, true)) {
                    p.remove();
                    if (userManager.isAutoSave()) {
                        node.save();
                    }
                    return true;
                }
            }
            // no such property or wasn't a property of this authorizable.
            return false;
        } catch (RepositoryException e) {
            log.debug("Failed to remove Property " + relPath + " from " + this, e);
            node.refresh(false);
            throw e;
        }
    }

    /**
     * @see Authorizable#remove()
     */
    public synchronized void remove() throws RepositoryException {
        // don't allow for removal of the administrator even if the executing
        // session has all permissions.
        if (!isGroup() && ((User) this).isAdmin()) {
            throw new RepositoryException("The administrator cannot be removed.");
        }
        Session s = getSession();
        userManager.onRemove(this);
        node.remove();
        if (userManager.isAutoSave()) {
            s.save();
        }
    }
       
    /**
     * @see Authorizable#getPath()
     */
    public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
        return userManager.getPath(node);
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(isGroup() ? "group:" : "user:");
                sb.append(getSession().getWorkspace().getName());
                sb.append(":");
                sb.append(node.getIdentifier());
                hashCode = sb.toString().hashCode();
            } catch (RepositoryException e) {
            }
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthorizableImpl) {
            AuthorizableImpl otherAuth = (AuthorizableImpl) obj;
            try {
                return isGroup() == otherAuth.isGroup() && node.isSame(otherAuth.node);
            } catch (RepositoryException e) {
                // should not occur -> return false in this case.
            }
        }
        return false;
    }

    @Override
    public String toString() {
        try {
            String typeStr = (isGroup()) ? "Group '" : "User '";
            return new StringBuilder().append(typeStr).append(getID()).append("'").toString();
        } catch (RepositoryException e) {
            return super.toString();
        }
    }

    //--------------------------------------------------------------------------
    /**
     * @return node The underlying <code>Node</code> object.
     */
    NodeImpl getNode() {
        return node;
    }

    SessionImpl getSession() throws RepositoryException {
        return (SessionImpl) node.getSession();
    }

    String getPrincipalName() throws RepositoryException {
        // principal name is mandatory property -> no check required.
        return node.getProperty(P_PRINCIPAL_NAME).getString();
    }

    boolean isEveryone() throws RepositoryException {
        return isGroup() && EveryonePrincipal.NAME.equals(getPrincipalName());
    }

    private Iterator<Group> collectMembership(boolean includeIndirect) throws RepositoryException {
        Collection<String> groupNodeIds;
        MembershipCache cache = userManager.getMembershipCache();
        String nid = node.getIdentifier();

        final long t0 = System.nanoTime();
        boolean collect = false;
        if (node.getSession().hasPendingChanges()) {
            collect = true;
            // avoid retrieving outdated cache entries or filling the cache with
            // invalid data due to group-membership changes pending on the
            // current session.
            // this is mainly for backwards compatibility reasons (no cache present)
            // where transient changes (in non-autosave mode) were reflected to the
            // editing session (see JCR-2713)
            Session session = node.getSession();
            groupNodeIds = (includeIndirect) ? cache.collectMembership(nid, session) : cache.collectDeclaredMembership(nid, session);
        } else {
            //  retrieve cached membership. there are no pending changes.
            groupNodeIds = (includeIndirect) ? cache.getMemberOf(nid) : cache.getDeclaredMemberOf(nid);
        }
        final long t1 = System.nanoTime();
        Set<Group> groups = new HashSet<Group>(groupNodeIds.size());
        for (String identifier : groupNodeIds) {
            try {
                NodeImpl n = (NodeImpl) getSession().getNodeByIdentifier(identifier);
                Group group = userManager.createGroup(n);
                groups.add(group);
            } catch (RepositoryException e) {
                // group node doesn't exist or cannot be read -> ignore.
            }
        }
        final long t2 = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug("Collected {} {} group ids for [{}] in {}us, loaded {} groups in {}us (collect={}, cachesize={})", new Object[]{
                    groupNodeIds.size(),
                    includeIndirect ? "all" : "declared",
                    getID(),
                    (t1-t0) / 1000,
                    groups.size(),
                    (t2-t1) / 1000,
                    collect,
                    cache.getSize()
            });
        }
        return new RangeIteratorAdapter(groups.iterator(), groups.size());
    }

    /**
     * Returns true if the given property of the authorizable node is one of the
     * non-protected properties defined by the rep:Authorizable node type or a
     * some other descendant of the authorizable node.
     *
     * @param prop Property to be tested.
     * @param verifyAncestor If true the property is tested to be a descendant
     * of the node of this authorizable; otherwise it is expected that this
     * test has been executed by the caller.
     * @return <code>true</code> if the given property is defined
     * by the rep:authorizable node type or one of it's sub-node types;
     * <code>false</code> otherwise.
     * @throws RepositoryException If the property definition cannot be retrieved.
     */
    private boolean isAuthorizableProperty(Property prop, boolean verifyAncestor) throws RepositoryException {
        if (verifyAncestor && !Text.isDescendant(node.getPath(), prop.getPath())) {
            log.debug("Attempt to access property outside of authorizable scope.");
            return false;
        }

        PropertyDefinition def = prop.getDefinition();
        if (def.isProtected()) {
            return false;
        } else if (node.isSame(prop.getParent())) {
            NodeTypeImpl declaringNt = (NodeTypeImpl) prop.getDefinition().getDeclaringNodeType();
            return declaringNt.isNodeType(UserConstants.NT_REP_AUTHORIZABLE);
        } else {
            // another non-protected property somewhere in the subtree of this
            // authorizable node -> is a property that can be set using #setProperty.
            return true;
        }
    }

    /**
     * Test if the JCR property to be modified/removed is one of the
     * following that has a special meaning and must be altered using this
     * user API:
     * <ul>
     * <li>rep:principalName</li>
     * <li>rep:members</li>
     * <li>rep:impersonators</li>
     * <li>rep:password</li>
     * </ul>
     * Those properties are 'protected' in their property definition. This
     * method is a simple utility in order to save the extra effort to modify
     * the props just to find out later that they are in fact protected.
     *
     * @param propertyName Name of the property.
     * @return true if the property with the given name represents a protected
     * user/group property that needs to be changed through the API.
     * @throws RepositoryException If the specified name is not valid.
     */
    private boolean isProtectedProperty(String propertyName) throws RepositoryException {
        Name pName = getSession().getQName(propertyName);
        return P_PRINCIPAL_NAME.equals(pName)
                || P_MEMBERS.equals(pName)
                || P_IMPERSONATORS.equals(pName)
                || P_DISABLED.equals(pName)
                || P_PASSWORD.equals(pName);
    }

    /**
     * Throws ConstraintViolationException if {@link #isProtectedProperty(String)}
     * returns <code>true</code>.
     *
     * @param propertyName Name of the property.
     * @throws ConstraintViolationException If the property is protected according
     * to {@link #isProtectedProperty(String)}.
     * @throws RepositoryException If another error occurs.
     */
    private void checkProtectedProperty(String propertyName) throws ConstraintViolationException, RepositoryException {
        if (isProtectedProperty(propertyName)) {
            throw new ConstraintViolationException("Attempt to modify protected property " + propertyName + " of " + this);
        }
    }

    /**
     * 
     * @param relPath A relative path.
     * @return The corresponding node.
     * @throws RepositoryException If an error occurs.
     */
    private Node getOrCreateTargetNode(String relPath) throws RepositoryException {
        Node n;
        if (relPath != null) {
            if (node.hasNode(relPath)) {
                n = node.getNode(relPath);
            } else {
                n = node;
                for (String segment : Text.explode(relPath, '/')) {
                    if (n.hasNode(segment)) {
                        n = n.getNode(segment);
                    } else {
                        n = n.addNode(segment);
                    }
                }
            }
            if (!Text.isDescendantOrEqual(node.getPath(), n.getPath())) {
                node.refresh(false);
                throw new RepositoryException("Relative path " + relPath + " outside of scope of " + this);
            }
        } else {
            n = node;
        }
        return n;
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    class NodeBasedPrincipal extends PrincipalImpl implements ItemBasedPrincipal {

        /**
         * @param name for the principal
         */
        NodeBasedPrincipal(String name) {
            super(name);
        }

        NodeId getNodeId() {
            return node.getNodeId();
        }

        //---------------------------------------------< ItemBasedPrincipal >---
        /**
         * Method revealing the path to the Node that represents the
         * Authorizable this principal is created for.
         *
         * @return The path of the underlying node.
         * @see ItemBasedPrincipal#getPath()
         */
        public String getPath() throws RepositoryException {
            return node.getPath();
        }
    }
}
