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
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Session;
import javax.jcr.ItemNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
     * @throws RepositoryException If an error occurs.
     */
    protected AuthorizableImpl(NodeImpl node, UserManagerImpl userManager)
            throws RepositoryException {
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
        Set<Group> memberShip = new HashSet<Group>();
        collectMembership(memberShip, false);
        return memberShip.iterator();
    }

    /**
     * @see Authorizable#memberOf()
     */
    public Iterator<Group> memberOf() throws RepositoryException {
        Set<Group> memberShip = new HashSet<Group>();
        collectMembership(memberShip, true);
        return memberShip.iterator();
    }

    /**
     * @see Authorizable#getPropertyNames()
     */
    public Iterator<String> getPropertyNames() throws RepositoryException {
        List<String> l = new ArrayList<String>();
        for (PropertyIterator it = node.getProperties(); it.hasNext();) {
            Property prop = it.nextProperty();
            if (isAuthorizableProperty(prop)) {
                l.add(prop.getName());
            }
        }
        return l.iterator();
    }

    /**
     * @see #getProperty(String)
     */
    public boolean hasProperty(String name) throws RepositoryException {
        return node.hasProperty(name) && isAuthorizableProperty(node.getProperty(name));
    }

    /**
     * @see #hasProperty(String)
     * @see Authorizable#getProperty(String)
     */
    public Value[] getProperty(String name) throws RepositoryException {
        if (hasProperty(name)) {
            Property prop = node.getProperty(name);
            if (isAuthorizableProperty(prop)) {
                if (prop.isMultiple()) {
                    return prop.getValues();
                } else {
                    return new Value[] {prop.getValue()};
                }
            }
        }
        return null;
    }

    /**
     * Sets the Value for the given name. If a value existed, it is replaced,
     * if not it is created.
     *
     * @param name The property name.
     * @param value The property value.
     * @throws RepositoryException If the specified name defines a property
     * that needs to be modified by this user API or setting the corresponding
     * JCR property fails.
     * @see Authorizable#setProperty(String, Value)
     */
    public synchronized void setProperty(String name, Value value) throws RepositoryException {
        checkProtectedProperty(name);
        try {
            // check if the property has already been created as multi valued
            // property before -> in this case remove in order to avoid valueformatex.
            if (node.hasProperty(name)) {
                Property p = node.getProperty(name);
                if (p.isMultiple()) {
                    p.remove();
                }
            }
            node.setProperty(name, value);
            if (userManager.isAutoSave()) {
                node.save();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to set Property " + name + " for " + this, e);
            node.refresh(false);
            throw e;
        }
    }

    /**
     * Sets the Value[] for the given name. If a value existed, it is replaced,
     * if not it is created.
     *
     * @param name The property name.
     * @param values The property values.
     * @throws RepositoryException If the specified name defines a property
     * that needs to be modified by this user API or setting the corresponding
     * JCR property fails.
     * @see Authorizable#setProperty(String, Value[])
     */
    public synchronized void setProperty(String name, Value[] values) throws RepositoryException {
        checkProtectedProperty(name);
        try {
            // check if the property has already been created as single valued
            // property before -> in this case remove in order to avoid valueformatex.
            if (node.hasProperty(name)) {
                Property p = node.getProperty(name);
                if (!p.isMultiple()) {
                    p.remove();
                }
            }
            node.setProperty(name, values);
            if (userManager.isAutoSave()) {
                node.save();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to set Property " + name + " for " + this, e);
            node.refresh(false);
            throw e;
        }
    }
    /**
     * @see Authorizable#removeProperty(String)
     */
    public synchronized boolean removeProperty(String name) throws RepositoryException {
        checkProtectedProperty(name);
        try {
            if (node.hasProperty(name)) {
                // 'node' is protected -> use setValue instead of Property.remove()
                Property p = node.getProperty(name);
                if (p.isMultiple()) {
                    p.setValue((Value[]) null);
                } else {
                    p.setValue((Value) null);
                }
                if (userManager.isAutoSave()) {
                    node.save();
                }
                return true;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            log.warn("Failed to remove Property " + name + " from " + this, e);
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
        node.remove();
        if (userManager.isAutoSave()) {
            s.save();
        }
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

    private void collectMembership(final Set<Group> groups, boolean includeIndirect) throws RepositoryException {
        PropertyIterator refs = getMembershipReferences();
        if (refs != null) {
            while (refs.hasNext()) {
                try {
                    NodeImpl n = (NodeImpl) refs.nextProperty().getParent();
                    if (n.isNodeType(NT_REP_GROUP)) {
                        Group group = userManager.createGroup(n);
                        // only retrieve indirect membership if the group is not
                        // yet present (detected eventual circular membership).
                        if (groups.add(group) && includeIndirect) {
                            ((AuthorizableImpl) group).collectMembership(groups, true);
                        }
                    } else {
                        // weak-ref property 'rep:members' that doesn't reside under an
                        // group node -> doesn't represent a valid group member.
                        log.debug("Invalid member reference to '" + this + "' -> Not included in membership set.");
                    }
                } catch (ItemNotFoundException e) {
                    // group node doesn't exist  -> -> ignore exception
                    // and skip this reference from membership list.
                } catch (AccessDeniedException e) {
                    // not allowed to see the group node -> ignore exception
                    // and skip this reference from membership list.
                }
            }
        } else {
            // workaround for failure of Node#getWeakReferences
            // traverse the tree below groups-path and collect membership manually.
            log.info("Traversing groups tree to collect membership.");
            ItemVisitor visitor = new TraversingItemVisitor.Default() {
                @Override
                protected void entering(Property property, int level) throws RepositoryException {
                    PropertyImpl pImpl = (PropertyImpl) property;
                    NodeImpl n = (NodeImpl) pImpl.getParent();
                    if (P_MEMBERS.equals(pImpl.getQName()) && n.isNodeType(NT_REP_GROUP)) {
                        for (Value value : property.getValues()) {
                            if (value.getString().equals(node.getIdentifier())) {
                                Group gr = (Group) userManager.getAuthorizable(n);
                                groups.add(gr);
                            }
                        }
                    }
                }
            };
            Node groupsNode = getSession().getNode(userManager.getGroupsPath());
            visitor.visit(groupsNode);
        }
    }

    /**
     * @return the iterator returned by {@link Node#getWeakReferences(String)}
     * or <code>null</code> if the method call fails with <code>RepositoryException</code>.
     * See fallback scenario above.
     */
    private PropertyIterator getMembershipReferences() {
        PropertyIterator refs = null;
        try {
            refs = node.getWeakReferences(getSession().getJCRName(P_MEMBERS));
        } catch (RepositoryException e) {
            log.error("Failed to retrieve membership references of " + this + ".", e);
        }
        return refs;
    }

    /**
     * Returns true if the given property of the authorizable node is one of the
     * non-protected properties defined by the rep:authorizable.
     *
     * @param prop Property to be tested.
     * @return <code>true</code> if the given property is defined
     * by the rep:authorizable node type or one of it's sub-node types;
     * <code>false</code> otherwise.
     * @throws RepositoryException If the property definition cannot be retrieved.
     */
    private static boolean isAuthorizableProperty(Property prop) throws RepositoryException {
        PropertyDefinition def = prop.getDefinition();
        if (def.isProtected()) {
            return false;
        } else  {
            NodeTypeImpl declaringNt = (NodeTypeImpl) prop.getDefinition().getDeclaringNodeType();
            return declaringNt.isNodeType(UserConstants.NT_REP_AUTHORIZABLE);
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
                || P_IMPERSONATORS.equals(pName) || P_PASSWORD.equals(pName);
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
