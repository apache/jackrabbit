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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.principal.NoSuchPrincipalException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * AuthorizableImpl
 */
abstract class AuthorizableImpl implements Authorizable, UserConstants {

    static final Logger log = LoggerFactory.getLogger(AuthorizableImpl.class);

    final UserManagerImpl userManager;
    private final NodeImpl node;

    /**
     * @param node    the Authorizable is persisted to.
     * @param userManager UserManager that created this Authorizable.
     * @throws IllegalArgumentException if the given node isn't of node type
     * {@link #NT_REP_AUTHORIZABLE}.
     * @throws RepositoryException If an error occurs.
     */
    protected AuthorizableImpl(NodeImpl node, UserManagerImpl userManager)
            throws RepositoryException {
        if (!node.isNodeType(NT_REP_AUTHORIZABLE)) {
            throw new IllegalArgumentException("Node argument of NodeType " + NT_REP_AUTHORIZABLE + " required");
        }
        this.node = node;
        this.userManager = userManager;
    }

    //-------------------------------------------------------< Authorizable >---
    /**
     * @see Authorizable#getPrincipals()
     */
    public PrincipalIterator getPrincipals() throws RepositoryException {
        Collection coll = new ArrayList();
        // the first element is the main principal of this user.
        coll.add(getPrincipal());
        // in addition add all referees.
        PrincipalManager prMgr = getSession().getPrincipalManager();
        for (Iterator it = getRefereeValues().iterator(); it.hasNext();) {
            String refName = ((Value) it.next()).getString();
            Principal princ = null;
            if (prMgr.hasPrincipal(refName)) {
                try {
                    princ = prMgr.getPrincipal(refName);
                } catch (NoSuchPrincipalException e) {
                    // should not get here
                }
            }
            if (princ == null) {
                log.warn("Principal "+ refName +" unknown to PrincipalManager.");
                princ = new PrincipalImpl(refName);
            }
            coll.add(princ);
        }
        return new PrincipalIteratorAdapter(coll);
    }

    /**
     * @see Authorizable#addReferee(Principal)
     */
    public synchronized boolean addReferee(Principal principal) throws RepositoryException {
        String principalName = principal.getName();
        Value princValue = getSession().getValueFactory().createValue(principalName);

        List refereeValues = getRefereeValues();
        if (refereeValues.contains(princValue) || getPrincipal().getName().equals(principalName)) {
            return false;
        }
        if (userManager.hasAuthorizableOrReferee(principal)) {
            throw new AuthorizableExistsException("Another authorizable already represented by or refeering to " +  principalName);
        }
        refereeValues.add(princValue);

        userManager.setProtectedProperty(node, P_REFEREES, (Value[]) refereeValues.toArray(new Value[refereeValues.size()]));
        return true;
    }

    /**
     * @see Authorizable#removeReferee(Principal)
     */
    public synchronized boolean removeReferee(Principal principal) throws RepositoryException {
        Value princValue = getSession().getValueFactory().createValue(principal.getName());
        List existingValues = getRefereeValues();

        if (existingValues.remove(princValue))  {
            PropertyImpl prop = node.getProperty(P_REFEREES);
            if (existingValues.isEmpty()) {
                userManager.removeProtectedItem(prop, node);
            } else {
                userManager.setProtectedProperty(node, P_REFEREES, (Value[]) existingValues.toArray(new Value[existingValues.size()]));
            }
            return true;
        }

        // specified principal was not referee of this authorizable.
        return false;
    }

    /**
     * @see Authorizable#declaredMemberOf()
     */
    public Iterator declaredMemberOf() throws RepositoryException {
        List memberShip = new ArrayList();
        collectMembership(memberShip, false);
        return memberShip.iterator();
    }

    /**
     * @see Authorizable#memberOf()
     */
    public Iterator memberOf() throws RepositoryException {
        List memberShip = new ArrayList();
        collectMembership(memberShip, true);
        return memberShip.iterator();
    }

    /**
     * @see Authorizable#getPropertyNames()
     */
    public Iterator getPropertyNames() throws RepositoryException {
        List l = new ArrayList();
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
            PropertyImpl prop = (PropertyImpl) node.getProperty(name);
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
            node.setProperty(name, value);
            node.save();
        } catch (RepositoryException e) {
            log.warn("Failed to set Property " + name + " for Authorizable " + getID());
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
            node.setProperty(name, values);
            node.save();
        } catch (RepositoryException e) {
            log.warn("Failed to set Property " + name + " for Authorizable " + getID());
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
                PropertyImpl p = (PropertyImpl) node.getProperty(name);
                if (p.isMultiple()) {
                    p.setValue((Value[]) null);
                } else {
                    p.setValue((Value) null);
                }
                node.save();
                return true;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            log.warn("Failed to remove Property " + name + " from Authorizable " + getID());
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
        userManager.removeProtectedItem(node, node.getParent());
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

    boolean addToGroup(GroupImpl group) throws RepositoryException {
        try {
            Value[] values;
            // TODO: replace by weak-refs
            Value added = getSession().getValueFactory().createValue(group.getNode());
            NodeImpl node = getNode();
            if (node.hasProperty(P_GROUPS)) {
                Value[] old = node.getProperty(P_GROUPS).getValues();
                values = new Value[old.length + 1];
                System.arraycopy(old, 0, values, 0, old.length);
            } else {
                values = new Value[1];
            }
            values[values.length - 1] = added;
            userManager.setProtectedProperty(node, P_GROUPS, values);
            return true;
        } catch (RepositoryException e) {
            // revert all pending changes and rethrow.
            log.warn("Error while editing group membership:", e.getMessage());
            getSession().refresh(false);
            throw e;
        }
    }

    boolean removeFromGroup(GroupImpl group) throws RepositoryException {
        NodeImpl node = getNode();
        String message = "Authorizable " + getID() + " is not member of " + group.getID();
        if (!node.hasProperty(P_GROUPS)) {
            log.debug(message);
            return false;
        }

        Value toRemove = getSession().getValueFactory().createValue(group.getNode());
        PropertyImpl property = node.getProperty(P_GROUPS);
        List valList = new ArrayList(Arrays.asList(property.getValues()));
        if (valList.remove(toRemove)) {
            try {
                if (valList.isEmpty()) {
                    userManager.removeProtectedItem(property, node);
                } else {
                    Value[] values = (Value[]) valList.toArray(new Value[valList.size()]);
                    userManager.setProtectedProperty(node, P_GROUPS, values);
                }
                return true;
            } catch (RepositoryException e) {
                // modification failed -> revert all pending changes.
                node.refresh(false);
                throw e;
            }
        } else {
            // nothing changed
            log.debug(message);
            return false;
        }
    }

    private void collectMembership(List groups, boolean includedIndirect) throws RepositoryException {
        NodeImpl node = getNode();
        if (!node.hasProperty(P_GROUPS)) {
            return;
        }
        Value[] refs = node.getProperty(P_GROUPS).getValues();
        for (int i = 0; i < refs.length; i++) {
            NodeImpl groupNode = (NodeImpl) getSession().getNodeByUUID(refs[i].getString());
            Group group = GroupImpl.create(groupNode, userManager);
            if (groups.add(group) && includedIndirect) {
                ((AuthorizableImpl) group).collectMembership(groups, true);
            }
        }
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
     * <ul>
     * <li>rep:principalName</li>
     * <li>rep:userId</li>
     * <li>rep:referees</li>
     * <li>rep:groups</li>
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
        return P_PRINCIPAL_NAME.equals(pName) || P_USERID.equals(pName)
                || P_REFEREES.equals(pName) || P_GROUPS.equals(pName)
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
            throw new ConstraintViolationException("Attempt to modify protected property " + propertyName + " of an Authorizable.");
        }
    }

    private List getRefereeValues() throws RepositoryException {
        List principalNames = new ArrayList();
        if (node.hasProperty(P_REFEREES)) {
            try {
                Value[] refProp = node.getProperty(P_REFEREES).getValues();
                for (int i = 0; i < refProp.length; i++) {
                    principalNames.add(refProp[i]);
                }
            } catch (PathNotFoundException e) {
                // ignore. should never occur.
            }
        }
        return principalNames;
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
