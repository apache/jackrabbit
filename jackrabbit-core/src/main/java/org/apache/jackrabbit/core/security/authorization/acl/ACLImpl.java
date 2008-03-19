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

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Item;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * ACL-like implementation of the <code>AccessControlPolicy</code> interface.
 * <br>
 * The list consists of entries, each representing a set of Privileges
 * which are either granted or denied to one principal and may or may not
 * contain additional restrictions (see below).<br>
 * The entries are evaluated in the following order:
 * <ul>
 * <li>local entries</li>
 * <li>inherited base entries (recursively)</li>
 * </ul>
 * In order to calculate the permissions the ACL combines privileges and
 * additional restrictions and applies them to the passed jcr path. Note the
 * following additional rules:
 * <ul>
 * <li>Permissions not explicitly granted are denied</li>
 * <li>Permissions granted/denied by a local entry will not be overruled by
 * an inherited permission.</li>
 * </ul>
 *
 * @see AccessControlPolicy
 */
class ACLImpl implements AccessControlPolicy {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(ACLImpl.class);

    static final String POLICY_NAME = ACLImpl.class.getName();

    /**
     * the entries of this ACL
     */
    private final List localEntries;

    /**
     * the id of the node this item is stored
     */
    private final NodeId id;

    /**
     * the base ACL, if existent
     */
    private final ACLImpl base;

    /**
     * Flag indicating, if this ACL is used to protect an item that builds this
     * acl, e.g. a node with type rep:ACL or rep:ACE.
     */
    private final boolean protectsACL;

    /**
     * The compiled privileges of this ACL. -1 indicates that they have not
     * yet been calculated.
     */
    private int privileges = -1;

    /**
     * Create a new ACL for the given local entries and the entries inherited
     * from the given base.
     *
     * @param id the id of node this ACL has been built for.
     * @param localEntries A list of {@link ACEImpl local entries}.
     * @param base The base ACL.
     * @param protectsACL <code>true</code> if the node identified by the
     * given id, stores itself the ACL entries.
     */
    ACLImpl(NodeId id, List localEntries, ACLImpl base, boolean protectsACL) {
        this.id = id;
        this.localEntries = Collections.unmodifiableList(localEntries);
        this.base = base;
        this.protectsACL = protectsACL;
    }

    /**
     * Creates a new default ACL for an node that is <i>not access controlled</i>.
     * If the item is part of the ACL itself <code>protectsACL</code> must be
     * <code>true</code>.
     * <p/>
     * Since the actual ACL is the same as the one of the base acl, the access
     * controll node does not need to be specified.
     *
     * @param id the id of node this ACL has been built for.
     * @param base the list to add the current aces to
     * @param protectsACL <code>true</code> if the node identified by the
     * given id, stores itself the ACL entries.
     */
    ACLImpl(NodeId id, ACLImpl base, boolean protectsACL) {
        this(id, Collections.EMPTY_LIST, base, protectsACL);
    }

    /**
     * Returns the ID of <code>Node</code> storing this <code>ACL</code>.
     *
     * @return the item id
     */
    ItemId getId() {
        return id;
    }

    /**
     * Returns all entries of this ACL including the inherited ones.
     *
     * @return Iterator over ACEs.
     */
    Iterator getEntries() {
        return new ACEIteratorImpl(this);
    }

    /**
     * Retrieve the privileges from all entries (including the
     * inherited onces).
     *
     * @return Privileges or PrivilegeRegistry.NO_PRIVILEGE if there are
     * not privileges at all.
     * @throws AccessControlException
     */
    int getPrivileges() throws AccessControlException {
        if (privileges == -1) {
            Iterator entries = getEntries();
            int allows = PrivilegeRegistry.NO_PRIVILEGE;
            int denies = PrivilegeRegistry.NO_PRIVILEGE;
            // TODO check again.
            while (entries.hasNext() && allows != PrivilegeRegistry.ALL) {
                ACEImpl ace = (ACEImpl) entries.next();
                int entryBits = ace.getPrivilegeBits();
                if (ace.isAllow()) {
                    allows |= PrivilegeRegistry.diff(entryBits, denies);
                } else {
                    denies |= PrivilegeRegistry.diff(entryBits, allows);
                }
            }
            privileges = allows;
        }
        return privileges;
    }

    /**
     * Since the ACEs only define privileges on a node and do not allow to
     * add additional restrictions, the permissions can be determined without
     * taking the given target item into account.
     *
     * @param target
     * @return
     * @throws AccessControlException
     */
    int getPermissions(Item target) throws AccessControlException {
        return internalGetPermissions(true);
    }

    /**
     * Since the ACEs only define privileges on a node and do not allow to
     * add additional restrictions, the permissions can be determined without
     * taking the given target name into account.
     *
     * @param targetName
     * @return
     * @throws AccessControlException
     */
    int getPermissions(String targetName) throws AccessControlException {
        return internalGetPermissions(false);
    }

    /**
     * Since except for READ-privileges the permissions must be determined from
     * privileges defined for the parent we have to respect both: the base-ACL
     * and the local entries defined on this ACL.
     *
     * @return
     * @throws AccessControlException
     */
    private int internalGetPermissions(boolean existingItem) throws AccessControlException {
        int privs = getPrivileges();
        int basePrivs;
        if (existingItem && !localEntries.isEmpty()) {
            basePrivs = (base == null) ? PrivilegeRegistry.NO_PRIVILEGE : base.getPrivileges();
        } else {
            // no privileges defined on the node this ACL has been built for.
            // therefore all privileges are inherited and 'basePrivileges'
            // is the same as getPrivileges(), which combines both local and
            // inherited privileges.
            basePrivs = privs;
        }
        return Permission.calculatePermissions(privs, basePrivs, protectsACL);
    }

    //------------------------------------------------< AccessControlPolicy >---
    /**
     * @return the name of the current ACL,
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getName()
     */
    public String getName() throws RepositoryException {
        return POLICY_NAME;
    }

    /**
     * @see org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy#getDescription()
     */
    public String getDescription() throws RepositoryException {
        // TODO
        return null;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Iterator over entries present in this ACL or in this ACL and all its
     * base ACLs. If the base ACLs are included in the iterator the order
     * of ACE entries starts from the bottom of the inheritance chain i.e. most
     * derived ACL is included in the iterator first. This allows proper evalution
     * of privileges.
     */
    private static class ACEIteratorImpl implements Iterator {

        private final List baseEntries = new ArrayList();
        private Iterator currentEntries;
        private Object next;

        /**
         *
         * @param acl
         */
        private ACEIteratorImpl(ACLImpl acl) {
            currentEntries = new ArrayList(acl.localEntries).iterator();
            ACLImpl a = acl.base;
            while (a != null) {
                if (!a.localEntries.isEmpty()) {
                    baseEntries.add(new ArrayList(a.localEntries).iterator());
                }
                a = a.base;
            }
            next = seekNext();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return next != null;
        }

        public Object next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Object ret = next;
            next = seekNext();
            return ret;
        }

        private Object seekNext() {
            while (currentEntries != null && !currentEntries.hasNext()) {
                if (baseEntries.isEmpty()) {
                    // reached last 'base' acl
                    currentEntries = null;
                } else {
                    currentEntries = (Iterator) baseEntries.remove(0);
                }
            }
            return (currentEntries == null) ? null : currentEntries.next();
        }
    }
}
