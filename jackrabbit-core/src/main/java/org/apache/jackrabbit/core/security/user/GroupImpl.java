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

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * GroupImpl...
 */
class GroupImpl extends AuthorizableImpl implements Group {

    private static final Logger log = LoggerFactory.getLogger(GroupImpl.class);

    private Principal principal = null;

    private GroupImpl(NodeImpl node, UserManagerImpl userManager) throws RepositoryException {
        super(node, userManager);
    }

    static Group create(NodeImpl node, UserManagerImpl userManager) throws RepositoryException {
        if (node == null || !node.isNodeType(NT_REP_GROUP)) {
            throw new IllegalArgumentException();
        }
        if(!Text.isDescendant(GROUPS_PATH, node.getPath())) {
            throw new IllegalArgumentException("Group has to be within the Group Path");
        }
        return new GroupImpl(node, userManager);
    }


    //-------------------------------------------------------< Authorizable >---
    /**
     * Returns the name of the node that defines this <code>Group</code>, that
     * has been used taking the principal name as hint.
     *
     * @return name of the node that defines this <code>Group</code>.
     * @see Authorizable#getID()
     */
    public String getID() throws RepositoryException {
        return getNode().getName();
    }

    /**
     * @see Authorizable#isGroup()
     */
    public boolean isGroup() {
        return true;
    }

    /**
     * @see Authorizable#getPrincipal()
     */
    public Principal getPrincipal() throws RepositoryException {
        if (principal == null) {
            principal = new NodeBasedGroup(getPrincipalName());
        }
        return principal;
    }

    //--------------------------------------------------------------< Group >---
    /**
     * @see Group#getMembers()
     */
    public Iterator getMembers() throws RepositoryException {
        return new MemberIterator(memberUUIDs().iterator());
    }

    /**
     * @see Group#isMember(Authorizable)
     */
    public boolean isMember(Authorizable authorizable) throws RepositoryException {
        if (authorizable == null || !(authorizable instanceof AuthorizableImpl)) {
            return false;
        } else {
            AuthorizableImpl impl = (AuthorizableImpl) authorizable;
            String uuid = impl.getNode().getUUID();
            return memberUUIDs().contains(uuid);
        }
    }

    /**
     * @see Group#addMember(Authorizable)
     */
    public boolean addMember(Authorizable authorizable) throws RepositoryException {
        if (authorizable == null || !(authorizable instanceof AuthorizableImpl)
                || isMember(authorizable)) {
            return false;
        }
        if (isCyclicMembership(authorizable)) {
            log.warn("Attempt to create circular group membership.");
            return false;
        }

        Node memberNode = ((AuthorizableImpl) authorizable).getNode();
        if (memberNode.isSame(getNode())) {
            String msg = "Attempt to add a Group as member of itself (" + getID() + ").";
            log.warn(msg);
            return false;
        }

        Value[] values;
        // TODO: replace by weak-refs
        Value added = getSession().getValueFactory().createValue(memberNode);
        NodeImpl node = getNode();
        if (node.hasProperty(P_MEMBERS)) {
            Value[] old = node.getProperty(P_MEMBERS).getValues();
            values = new Value[old.length + 1];
            System.arraycopy(old, 0, values, 0, old.length);
        } else {
            values = new Value[1];
        }
        values[values.length - 1] = added;
        userManager.setProtectedProperty(node, P_MEMBERS, values);
        return true;
    }

    /**
     * @see Group#removeMember(Authorizable)
     */
    public boolean removeMember(Authorizable authorizable) throws RepositoryException {
        if (!isMember(authorizable) || !(authorizable instanceof AuthorizableImpl)) {
            return false;
        }
        NodeImpl node = getNode();
        if (!node.hasProperty(P_MEMBERS)) {
            log.debug("Group has no members -> cannot remove member " + authorizable.getID());
            return false;
        }

        Value toRemove = getSession().getValueFactory().createValue(((AuthorizableImpl)authorizable).getNode());

        PropertyImpl property = node.getProperty(P_MEMBERS);
        List valList = new ArrayList(Arrays.asList(property.getValues()));

        if (valList.remove(toRemove)) {
            try {
                if (valList.isEmpty()) {
                    userManager.removeProtectedItem(property, node);
                } else {
                    Value[] values = (Value[]) valList.toArray(new Value[valList.size()]);
                    userManager.setProtectedProperty(node, P_MEMBERS, values);
                }
                return true;
            } catch (RepositoryException e) {
                // modification failed -> revert all pending changes.
                node.refresh(false);
                throw e;
            }
        } else {
            // nothing changed
            log.debug("Authorizable " + authorizable.getID() + " was not member of " + getID());
            return false;
        }
    }

    //--------------------------------------------------------------------------

    /**
     *
     * @return
     * @throws RepositoryException
     */
    private Collection memberUUIDs() throws RepositoryException {
        Collection tmp = new HashSet();
        if (getNode().hasProperty(P_MEMBERS)) {
            Property prop = getNode().getProperty(P_MEMBERS);
            Value[] val = prop.getValues();
            for (int i = 0; i < val.length; i++) {
                tmp.add(val[i].getString());
            }
        }
        return tmp;
    }

    /**
     * Since {@link #isMember(Authorizable)} only detects the declared
     * members of this group, this methods is used to avoid cyclic membership
     * declarations.
     *
     * @param newMember
     * @return true if the 'newMember' is a group and 'this' is an declared or
     * inherited member of it.
     */
    private boolean isCyclicMembership(Authorizable newMember) throws RepositoryException {
        boolean cyclic = false;
        if (newMember.isGroup()) {
            Group gr = (Group) newMember;
            cyclic = gr.isMember(this);
            if (!cyclic) {
                PrincipalManager pmgr = getSession().getPrincipalManager();
                for (Iterator it = pmgr.getGroupMembership(getPrincipal());
                     it.hasNext() && !cyclic;) {
                    cyclic = newMember.getPrincipal().equals(it.next());
                }
            }
        }
        return cyclic;
    }

    //------------------------------------------------------< inner classes >---
    private class MemberIterator implements Iterator {

        private final Iterator ids;
        private Authorizable next;

        private MemberIterator(Iterator ids) {
            this.ids = ids;
            next = seekNext();
        }

        public boolean hasNext() {
            return next != null;
        }

        public Object next() {
            if (next == null) {
                throw new NoSuchElementException();
            }

            Authorizable n = next;
            next = seekNext();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Authorizable seekNext() {
            Authorizable auth = null;
            while (auth == null && ids.hasNext()) {
                String uuid = (String) ids.next();
                try {
                    NodeImpl mem = (NodeImpl) getSession().getNodeByUUID(uuid);
                    if (mem.isNodeType(NT_REP_GROUP)) {
                        auth = create(mem, userManager);
                    } else {
                        auth = UserImpl.create(mem, userManager);
                    }
                } catch (RepositoryException e) {
                    log.warn("Internal error while building next member.", e.getMessage());
                    // ignore and try next
                }
            }
            return auth;
        }
    }

    /**
     *
     */
    private class NodeBasedGroup extends NodeBasedPrincipal implements java.security.acl.Group {

        private Set members;

        private NodeBasedGroup(String name) {
            super(name);
        }

        //----------------------------------------------------------< Group >---
        /**
         * @return Always <code>false</code>. Group membership must be edited
         * using the enclosing <code>GroupImpl</code> object.
         * @see java.security.acl.Group#addMember(Principal)
         */
        public boolean addMember(Principal user) {
            return false;
        }

        /**
         * Returns true, if the given <code>Principal</code> is represented by
         * a Authorizable, that is a member of the underlying UserGroup.
         *
         * @see java.security.acl.Group#isMember(Principal)
         */
        public boolean isMember(Principal member) {
            Collection members = getMembers();
            if (members.contains(member)) {
                // shortcut.
                return true;
            }

            // test if member of a member-group
            for (Iterator it = members.iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                if (p instanceof java.security.acl.Group &&
                        ((java.security.acl.Group) p).isMember(member)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return Always <code>false</code>. Group membership must be edited
         * using the enclosing <code>GroupImpl</code> object.
         *
         * @see java.security.acl.Group#isMember(Principal)
         */
        public boolean removeMember(Principal user) {
            return false;
        }

        /**
         * Return all principals that refer to every member of the underlying
         * user group.
         *
         * @see java.security.acl.Group#members()
         */
        public Enumeration members() {
            return Collections.enumeration(getMembers());
        }

        //---------------------------------------------------< Serializable >---
        /**
         * implement the writeObject method to assert initalization of all members
         * before serialization.
         *
         * @param stream
         * @throws IOException
         */
        private void writeObject(ObjectOutputStream stream) throws IOException {
            getMembers();
            stream.defaultWriteObject();
        }

        //----------------------------------------------------------------------
        private Collection getMembers() {
            if (members == null) {
                members = new HashSet();
                try {
                    for (Iterator it = GroupImpl.this.getMembers(); it.hasNext();) {
                        Authorizable authrz = (Authorizable) it.next();
                        // TODO: check again. only add main principal, since
                        // 'referees' belong to a different provider and should
                        // not be exposed here
                        members.add(authrz.getPrincipal());
                    }
                } catch (RepositoryException e) {
                    // should not occur.
                    log.error("Unable to retrieve Group members.");
                }
            }
            return members;
        }
    }
}
