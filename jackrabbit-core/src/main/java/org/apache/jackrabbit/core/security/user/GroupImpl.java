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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.flat.BTreeManager;
import org.apache.jackrabbit.commons.flat.ItemSequence;
import org.apache.jackrabbit.commons.flat.PropertySequence;
import org.apache.jackrabbit.commons.flat.Rank;
import org.apache.jackrabbit.commons.flat.TreeManager;
import org.apache.jackrabbit.commons.iterator.LazyIteratorChain;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GroupImpl...
 */
class GroupImpl extends AuthorizableImpl implements Group {

    private static final Logger log = LoggerFactory.getLogger(GroupImpl.class);

    private Principal principal;

    protected GroupImpl(NodeImpl node, UserManagerImpl userManager) {
        super(node, userManager);
    }

    //-------------------------------------------------------< Authorizable >---

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
     * @see Group#getDeclaredMembers()
     */
    public Iterator<Authorizable> getDeclaredMembers() throws RepositoryException {
        if (isEveryone()) {
            return userManager.findAuthorizables(getSession().getJCRName(P_PRINCIPAL_NAME), null, UserManager.SEARCH_TYPE_AUTHORIZABLE);
        } else {
            return getMembers(false, UserManager.SEARCH_TYPE_AUTHORIZABLE);
        }
    }

    /**
     * @see Group#getMembers()
     */
    public Iterator<Authorizable> getMembers() throws RepositoryException {
        if (isEveryone()) {
            return getDeclaredMembers();
        } else {
            return getMembers(true, UserManager.SEARCH_TYPE_AUTHORIZABLE);
        }
    }

    public boolean isDeclaredMember(Authorizable authorizable) throws RepositoryException {
        if (authorizable == null || !(authorizable instanceof AuthorizableImpl)
                || getNode().isSame(((AuthorizableImpl) authorizable).getNode())) {
            return false;
        } else if (isEveryone()) {
            return true;
        } else {
            return getMembershipProvider(getNode()).hasMember((AuthorizableImpl) authorizable);
        }
    }

    /**
     * @see Group#isMember(Authorizable)
     */
    public boolean isMember(Authorizable authorizable) throws RepositoryException {
        if (authorizable == null || !(authorizable instanceof AuthorizableImpl)
                || getNode().isSame(((AuthorizableImpl) authorizable).getNode())) {
            return false;
        } else if (isEveryone()) {
            return true;
        } else {
            String thisID = getID();
            AuthorizableImpl impl = (AuthorizableImpl) authorizable;
            for (Iterator<Group> it = impl.memberOf(); it.hasNext(); ) {
                if (thisID.equals(it.next().getID())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @see Group#addMember(Authorizable)
     */
    public boolean addMember(Authorizable authorizable) throws RepositoryException {
        if (!(authorizable instanceof AuthorizableImpl)) {
            log.warn("Invalid Authorizable: {}", authorizable);
            return false;
        }
        if (isEveryone() || ((AuthorizableImpl) authorizable).isEveryone()) {
            return false;
        }

        AuthorizableImpl authImpl = ((AuthorizableImpl) authorizable);
        Node memberNode = authImpl.getNode();
        if (memberNode.isSame(getNode())) {
            String msg = "Attempt to add a group as member of itself (" + getID() + ").";
            log.warn(msg);
            return false;
        }

        if (isCyclicMembership(authImpl)) {
            log.warn("Attempt to create circular group membership.");
            return false;
        }

        return getMembershipProvider(getNode()).addMember(authImpl);
    }

    @Override
    public Set<String> addMembers(String... memberIds) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("not implemented");
    }


    /**
     * @see Group#removeMember(Authorizable)
     */
    public boolean removeMember(Authorizable authorizable) throws RepositoryException {
        if (!(authorizable instanceof AuthorizableImpl)) {
            log.warn("Invalid Authorizable: {}", authorizable);
            return false;
        }
        if (isEveryone()) {
            return false;
        }

        return getMembershipProvider(getNode()).removeMember((AuthorizableImpl) authorizable);
    }

    @Override
    public Set<String> removeMembers(String... memberIds) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("not implemented");
    }

    //--------------------------------------------------------------------------
    /**
     * Retrieve the membership provider for this group. This method deals with
     * members stored in the <code>P_MEMBERS</code> property and with those
     * repositories the store group members in a separate tree underneath the
     * <code>N_MEMBERS</code> node.
     *
     * @param node The node associated with this group.
     * @return an instance of <code>MembershipProvider</code>.
     * @throws RepositoryException If an error occurs.
     */
    private MembershipProvider getMembershipProvider(NodeImpl node) throws RepositoryException {
        MembershipProvider msp;
        if (userManager.hasMemberSplitSize()) {
            if (node.hasNode(N_MEMBERS) || !node.hasProperty(P_MEMBERS)) {
                msp = new NodeBasedMembershipProvider(node);
            } else {
                msp = new PropertyBasedMembershipProvider(node);
            }
        } else {
            msp = new PropertyBasedMembershipProvider(node);
        }

        if (node.hasProperty(P_MEMBERS) && node.hasNode(N_MEMBERS)) {
            log.warn("Found members node and members property on node {}. Ignoring {} members", node,
                    userManager.hasMemberSplitSize() ? "property" : "node");
        }

        return msp;
    }

    /**
     * @param includeIndirect If <code>true</code> all members of this group
     * will be return; otherwise only the declared members.
     * @param type Any of {@link UserManager#SEARCH_TYPE_AUTHORIZABLE},
     * {@link UserManager#SEARCH_TYPE_GROUP}, {@link UserManager#SEARCH_TYPE_USER}.
     * @return A collection of members of this group.
     * @throws RepositoryException If an error occurs while collecting the members.
     */
    private Iterator<Authorizable> getMembers(boolean includeIndirect, int type) throws RepositoryException {
        return getMembershipProvider(getNode()).getMembers(includeIndirect, type);
    }

    /**
     * Returns <code>true</code> if the given <code>newMember</code> is a Group
     * and contains <code>this</code> Group as declared or inherited member.
     *
     * @param newMember The new member to be tested for cyclic membership.
     * @return true if the 'newMember' is a group and 'this' is an declared or
     * inherited member of it.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    private boolean isCyclicMembership(AuthorizableImpl newMember) throws RepositoryException {
        if (newMember.isGroup()) {
            GroupImpl gr = (GroupImpl) newMember;
            for (Iterator<Authorizable> it = gr.getMembers(true, UserManager.SEARCH_TYPE_GROUP); it.hasNext(); ) {
                Authorizable member = it.next();
                GroupImpl grMemberImpl = (GroupImpl) member;
                if (getNode().getUUID().equals(grMemberImpl.getNode().getUUID())) {
                    // found cyclic group membership
                    return true;
                }

            }
        }
        return false;
    }

    private String safeGetID() {
        try {
            return getID();
        } catch (RepositoryException e) {
            return getNode().toString();
        }
    }

    static PropertySequence getPropertySequence(Node nMembers, UserManagerImpl userManager) throws RepositoryException {
        Comparator<String> order = Rank.comparableComparator();
        int maxChildren = userManager.getMemberSplitSize();
        int minChildren = maxChildren / 2;

        TreeManager treeManager = new BTreeManager(nMembers, minChildren, maxChildren, order,
                userManager.isAutoSave());

        return ItemSequence.createPropertySequence(treeManager);
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Principal Implementation
     */
    private class NodeBasedGroup extends NodeBasedPrincipal implements GroupPrincipal {

        private NodeBasedGroup(String name) {
            super(name);
        }

        //----------------------------------------------------------< Group >---

        /**
         * @return Always <code>false</code>. Group membership must be edited
         *         using the enclosing <code>GroupImpl</code> object.
         * @see java.security.acl.Group#addMember(Principal)
         */
        public boolean addMember(Principal user) {
            return false;
        }

        /**
         * @return Always <code>false</code>. Group membership must be edited
         *         using the enclosing <code>GroupImpl</code> object.
         * @see java.security.acl.Group#isMember(Principal)
         */
        public boolean removeMember(Principal user) {
            return false;
        }

        //----------------------------------------------------------< GroupPrincipal >---

        /**
         * Returns true, if the given <code>Principal</code> is represented by
         * a Authorizable, that is a member of the underlying UserGroup.
         *
         * @see org.apache.jackrabbit.api.security.principal.GroupPrincipal#isMember(Principal)
         */
        public boolean isMember(Principal member) {
            // shortcut for everyone group -> avoid collecting all members
            // as all users and groups are member of everyone.
            try {
                if (isEveryone()) {
                    return !getPrincipal().equals(member);
                }
            } catch (RepositoryException e) {
                // continue using regular membership evaluation
            }

            Collection<Principal> members = getMembers();
            return members.contains(member);
        }

        /**
         * Return all principals that refer to every member of the underlying
         * user group.
         *
         * @see org.apache.jackrabbit.api.security.principal.GroupPrincipal#members()
         */
        public Enumeration<? extends Principal> members() {
            return Collections.enumeration(getMembers());
        }

        //---------------------------------------------------< Serializable >---
        /**
         * implement the writeObject method to assert initialization of all members
         * before serialization.
         *
         * @param stream The object output stream.
         * @throws IOException If an error occurs.
         */
        private void writeObject(ObjectOutputStream stream) throws IOException {
            getMembers();
            stream.defaultWriteObject();
        }

        //----------------------------------------------------------------------
        /**
         * Collect the member of this group principal.
         *
         * @return the members of this group principal.
         */
        private Collection<Principal> getMembers() {
            Set<Principal> members = new HashSet<Principal>();
            try {
                for (Iterator<Authorizable> it = GroupImpl.this.getMembers(); it.hasNext(); ) {
                    members.add(it.next().getPrincipal());
                }
            } catch (RepositoryException e) {
                // should not occur.
                log.error("Unable to retrieve Group members.");
            }
            return members;
        }
    }

    /**
     * Inner MembershipProvider interface
     */
    private interface MembershipProvider {
        boolean addMember(AuthorizableImpl authorizable) throws RepositoryException;

        boolean removeMember(AuthorizableImpl authorizable) throws RepositoryException;

        Iterator<Authorizable> getMembers(boolean includeIndirect, int type) throws RepositoryException;

        boolean hasMember(AuthorizableImpl authorizable) throws RepositoryException;
    }

    /**
     * PropertyBasedMembershipProvider
     */
    private class PropertyBasedMembershipProvider implements MembershipProvider {
        private final NodeImpl node;

        private PropertyBasedMembershipProvider(NodeImpl node) {
            super();
            this.node = node;
        }

        /**
         * @see MembershipProvider#addMember(AuthorizableImpl)
         */
        public boolean addMember(AuthorizableImpl authorizable) throws RepositoryException {
            Node memberNode = authorizable.getNode();

            Value[] values;
            Value toAdd = getSession().getValueFactory().createValue(memberNode, true);
            if (node.hasProperty(P_MEMBERS)) {
                Value[] old = node.getProperty(P_MEMBERS).getValues();
                for (Value v : old) {
                    if (v.equals(toAdd)) {
                        log.debug("Authorizable {} is already member of {}", authorizable, this);
                        return false;
                    }
                }

                values = new Value[old.length + 1];
                System.arraycopy(old, 0, values, 0, old.length);
            } else {
                values = new Value[1];
            }
            values[values.length - 1] = toAdd;

            userManager.setProtectedProperty(node, P_MEMBERS, values, PropertyType.WEAKREFERENCE);
            return true;
        }

        /**
         * @see MembershipProvider#removeMember(AuthorizableImpl)
         */
        public boolean removeMember(AuthorizableImpl authorizable) throws RepositoryException {
            if (!node.hasProperty(P_MEMBERS)) {
                log.debug("Group has no members -> cannot remove member {}", authorizable.getID());
                return false;
            }

            Value toRemove = getSession().getValueFactory().createValue((authorizable).getNode(), true);

            PropertyImpl property = node.getProperty(P_MEMBERS);
            List<Value> valList = new ArrayList<Value>(Arrays.asList(property.getValues()));

            if (valList.remove(toRemove)) {
                try {
                    if (valList.isEmpty()) {
                        userManager.removeProtectedItem(property, node);
                    } else {
                        Value[] values = valList.toArray(new Value[valList.size()]);
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
                log.debug("Authorizable {} was not member of {}", authorizable.getID(), getID());
                return false;
            }
        }

        /**
         * @see MembershipProvider#getMembers(boolean, int)
         */
        public Iterator<Authorizable> getMembers(boolean includeIndirect, int type) throws RepositoryException {
            if (node.hasProperty(P_MEMBERS)) {
                Value[] members = node.getProperty(P_MEMBERS).getValues();

                if (includeIndirect) {
                    return includeIndirect(toAuthorizables(members, type), type);
                } else {
                    return new RangeIteratorAdapter(toAuthorizables(members, type), members.length);
                }
            } else {
                return Iterators.empty();
            }
        }

        /**
         * @see MembershipProvider#hasMember(AuthorizableImpl)
         */
        public boolean hasMember(AuthorizableImpl authorizable) throws RepositoryException {
            if (node.hasProperty(P_MEMBERS)) {
                Value[] members = node.getProperty(P_MEMBERS).getValues();
                for (Value v : members) {
                    if (authorizable.getNode().getIdentifier().equals(v.getString())) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }

    }

    /**
     * NodeBasedMembershipProvider
     */
    private class NodeBasedMembershipProvider implements MembershipProvider {
        private final NodeImpl node;

        private NodeBasedMembershipProvider(NodeImpl node) {
            super();
            this.node = node;
        }

        /**
         * @see MembershipProvider#addMember(AuthorizableImpl)
         */
        public boolean addMember(final AuthorizableImpl authorizable) throws RepositoryException {
            return userManager.performProtectedOperation(getSession(), new SessionWriteOperation<Boolean>() {
                public Boolean perform(SessionContext context) throws RepositoryException {
                    NodeImpl nMembers = (node.hasNode(N_MEMBERS)
                            ? node.getNode(N_MEMBERS)
                            : node.addNode(N_MEMBERS, NT_REP_MEMBERS, null));

                    try {
                        PropertySequence properties = getPropertySequence(nMembers, userManager);
                        String propName = Text.escapeIllegalJcrChars(authorizable.getID());
                        if (properties.hasItem(propName)) {
                            log.debug("Authorizable {} is already member of {}", authorizable, this);
                            return false;
                        } else {
                            Value newMember = getSession().getValueFactory().createValue(authorizable.getNode(), true);
                            properties.addProperty(propName, newMember);
                        }

                        if (userManager.isAutoSave()) {
                            node.save();
                        }
                        return true;
                    } catch (RepositoryException e) {
                        log.debug("addMember failed. Reverting changes", e);
                        if (nMembers.isNew()) {
                            node.refresh(false);
                        } else {
                            nMembers.refresh(false);
                        }
                        throw e;
                    }
                }
            });
        }

        /**
         * @see MembershipProvider#removeMember(AuthorizableImpl)
         */
        public boolean removeMember(final AuthorizableImpl authorizable) throws RepositoryException {
            if (!node.hasNode(N_MEMBERS)) {
                log.debug("Group has no members -> cannot remove member {}", authorizable.getID());
                return false;
            }

            return userManager.performProtectedOperation(getSession(), new SessionWriteOperation<Boolean>() {
                public Boolean perform(SessionContext context) throws RepositoryException {
                    NodeImpl nMembers = node.getNode(N_MEMBERS);
                    try {
                        PropertySequence properties = getPropertySequence(nMembers, userManager);
                        String propName = Text.escapeIllegalJcrChars(authorizable.getID());
                        if (properties.hasItem(propName)) {
                            properties.removeProperty(propName);
                            if (!properties.iterator().hasNext()) {
                                nMembers.remove();
                            }
                        } else {
                            log.debug("Authorizable {} was not member of {}", authorizable.getID(), getID());
                            return false;
                        }

                        if (userManager.isAutoSave()) {
                            node.save();
                        }
                        return true;
                    } catch (RepositoryException e) {
                        log.debug("removeMember failed. Reverting changes", e);
                        nMembers.refresh(false);
                        throw e;
                    }
                }
            });
        }

        /**
         * @see MembershipProvider#getMembers(boolean, int)
         */
        public Iterator<Authorizable> getMembers(boolean includeIndirect, int type) throws RepositoryException {
            if (node.hasNode(N_MEMBERS)) {
                PropertySequence members = getPropertySequence(node.getNode(N_MEMBERS), userManager);
                if (includeIndirect) {
                    return includeIndirect(toAuthorizables(members.iterator(), type), type);
                } else {
                    return toAuthorizables(members.iterator(), type);
                }
            } else {
                return Iterators.empty();
            }
        }

        /**
         * @see MembershipProvider#hasMember(AuthorizableImpl)
         */
        public boolean hasMember(AuthorizableImpl authorizable) throws RepositoryException {
            if (node.hasNode(N_MEMBERS)) {
                PropertySequence members = getPropertySequence(node.getNode(N_MEMBERS), userManager);
                return members.hasItem(authorizable.getID());
            } else {
                return false;
            }
        }

    }

    // -----------------------------------------------------------< utility >---
    /**
     * Returns an iterator of authorizables which includes all indirect members of the given iterator
     * of authorizables.
     *
     * @param authorizables
     * @param type
     * @return Iterator of Authorizable objects
     */
    private Iterator<Authorizable> includeIndirect(final Iterator<Authorizable> authorizables, final int type) {
        Iterator<Iterator<Authorizable>> indirectMembers = new Iterator<Iterator<Authorizable>>() {
            public boolean hasNext() {
                return authorizables.hasNext();
            }

            public Iterator<Authorizable> next() {
                Authorizable next = authorizables.next();
                return Iterators.iteratorChain(Iterators.singleton(next), indirect(next));
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            /**
             * Returns the transitive closure over the members of this authorizable.
             *
             * @param authorizable
             * @return Iterator of Authorizable objects
             */
            private Iterator<Authorizable> indirect(Authorizable authorizable) {
                if (authorizable.isGroup()) {
                    try {
                        return ((GroupImpl) authorizable).getMembers(true, type);
                    } catch (RepositoryException e) {
                        log.warn("Could not determine members of " + authorizable, e);
                    }
                }
                return Iterators.empty();
            }
        };

        return unique(new LazyIteratorChain<Authorizable>(indirectMembers));
    }

    /**
     * Filter the passed {@code authorizables} in order to ensure uniqueness.
     * @param authorizables
     * @return  all members of {@code authorizable} with duplicates removed
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3156">JCR-3156</a>
     */
    private Iterator<Authorizable> unique(Iterator<Authorizable> authorizables) {
        final HashSet<String> seenAuthorizables = new HashSet<String>();
        return Iterators.filterIterator(authorizables,
                new Predicate<Authorizable>() {

            public boolean test(Authorizable authorizable) {
                try {
                    return seenAuthorizables.add(authorizable.getID());
                }
                catch (RepositoryException e) {
                    log.warn("Could not determine id of " + authorizable, e);
                    return true;
                }
            }
        });
    }

    /**
     * Map an array of values to an iterator of authorizables.
     *
     * @param members
     * @param type
     * @return Iterator of Authorizable objects
     */
    private Iterator<Authorizable> toAuthorizables(final Value[] members, int type) {
        return new AuthorizableIterator(type) {
            private int pos;

            @Override
            protected String getNextMemberRef() throws RepositoryException {
                return pos < members.length
                        ? members[pos++].getString()
                        : null;
            }
        };
    }

    /**
     * Map an iterator of properties to an iterator of authorizables.
     *
     * @param members
     * @param type
     * @return Iterator of Authorizable objects
     */
    private Iterator<Authorizable> toAuthorizables(final Iterator<Property> members, int type) {
        return new AuthorizableIterator(type) {
            @Override
            protected String getNextMemberRef() throws RepositoryException {
                return members.hasNext()
                        ? members.next().getString()
                        : null;
            }
        };
    }

    /**
     * Iterator of authorizables of a specific type.
     */
    private abstract class AuthorizableIterator implements Iterator<Authorizable> {
        private Authorizable next;
        private final int type;

        public AuthorizableIterator(int type) {
            super();
            this.type = type;
        }

        public boolean hasNext() {
            prefetch();
            return next != null;
        }

        public Authorizable next() {
            prefetch();
            if (next == null) {
                throw new NoSuchElementException();
            }

            Authorizable element = next;
            next = null;
            return element;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the reference value of the next node representing the next authorizable or
         * <code>null</code> if there there are no more.
         *
         * @return reference value of the next node representing the next authorizable or
         *         <code>null</code> if there there are no more.
         * @throws javax.jcr.RepositoryException If an error occurs.
         */
        protected abstract String getNextMemberRef() throws RepositoryException;

        private void prefetch() {
            while (next == null) {
                try {
                    String memberRef = getNextMemberRef();
                    if (memberRef == null) {
                        return;
                    }

                    NodeImpl member = (NodeImpl) getSession().getNodeByIdentifier(memberRef);
                    if (type != UserManager.SEARCH_TYPE_USER && member.isNodeType(NT_REP_GROUP)) {
                        next = userManager.createGroup(member);
                    } else if (type != UserManager.SEARCH_TYPE_GROUP && member.isNodeType(NT_REP_USER)) {
                        next = userManager.createUser(member);
                    } else {
                        log.debug("Group member entry with invalid node type {} -> " +
                                "Not included in member set.", member.getPrimaryNodeType().getName());
                    }
                } catch (ItemNotFoundException e) {
                    log.debug("Authorizable node referenced by {} doesn't exist any more -> " +
                            "Ignored from member list.", safeGetID());
                } catch (RepositoryException e) {
                    log.debug("Error pre-fetching member for " + safeGetID(), e);
                }

            }
        }
    }
}
