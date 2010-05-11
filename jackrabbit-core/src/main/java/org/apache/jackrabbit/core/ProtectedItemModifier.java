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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.acl.ACLEditor;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.retention.RetentionManagerImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;

/**
 * <code>SecurityItemModifier</code>: An abstract helper class to allow classes
 * of the security API residing outside of the core package to modify and remove
 * protected items for security. The protected item definitions are required in
 * order not to have security relevant content being changed through common
 * item operations but forcing the usage of the security API. The latter asserts
 * that implementation specific constraints are not violated.
 */
public abstract class ProtectedItemModifier {

    private static final int DEFAULT_PERM_CHECK = -1;
    private final int permission;

    protected ProtectedItemModifier() {
        this(DEFAULT_PERM_CHECK);
    }

    protected ProtectedItemModifier(int permission) {
        Class cl = getClass();
        if (!(cl.equals(UserManagerImpl.class) ||
              cl.equals(RetentionManagerImpl.class) ||
              cl.equals(ACLEditor.class) ||
              cl.equals(org.apache.jackrabbit.core.security.authorization.principalbased.ACLEditor.class))) {
            throw new IllegalArgumentException("Only UserManagerImpl, RetentionManagerImpl and ACLEditor may extend from the ProtectedItemModifier");
        }
        this.permission = permission;
    }

    protected NodeImpl addNode(NodeImpl parentImpl, Name name, Name ntName) throws RepositoryException {
        checkPermission(parentImpl, name, getPermission(true, false));
        // validation: make sure Node is not locked or checked-in.
        parentImpl.checkSetProperty();

        NodeTypeImpl nodeType = parentImpl.session.getNodeTypeManager().getNodeType(ntName);
        NodeDefinitionImpl def = parentImpl.getApplicableChildNodeDefinition(name, ntName);

        // check for name collisions
        // TODO: improve. copied from NodeImpl
        NodeState thisState = (NodeState) parentImpl.getItemState();
        ChildNodeEntry cne = thisState.getChildNodeEntry(name, 1);
        if (cne != null) {
            // there's already a child node entry with that name;
            // check same-name sibling setting of new node
            if (!def.allowsSameNameSiblings()) {
                throw new ItemExistsException();
            }
            // check same-name sibling setting of existing node
            NodeId newId = cne.getId();
            NodeImpl n = (NodeImpl) parentImpl.session.getItemManager().getItem(newId);
            if (!n.getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException();
            }
        }

        return parentImpl.createChildNode(name, nodeType, null);
    }

    protected Property setProperty(NodeImpl parentImpl, Name name, Value value) throws RepositoryException {
        return setProperty(parentImpl, name, value, false);
    }

    protected Property setProperty(NodeImpl parentImpl, Name name, Value value, boolean ignorePermissions) throws RepositoryException {
        if (!ignorePermissions) {
            checkPermission(parentImpl, name, getPermission(false, false));
        }
        // validation: make sure Node is not locked or checked-in.
        parentImpl.checkSetProperty();
        InternalValue intVs = InternalValue.create(value, parentImpl.session);
        return parentImpl.internalSetProperty(name, intVs);
    }

    protected Property setProperty(NodeImpl parentImpl, Name name, Value[] values) throws RepositoryException {
        checkPermission(parentImpl, name, getPermission(false, false));
        // validation: make sure Node is not locked or checked-in.
        parentImpl.checkSetProperty();
        InternalValue[] intVs = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            intVs[i] = InternalValue.create(values[i], parentImpl.session);
        }
        return parentImpl.internalSetProperty(name, intVs);
    }

    protected void removeItem(ItemImpl itemImpl) throws RepositoryException {
        NodeImpl n;
        if (itemImpl.isNode()) {
            n = (NodeImpl) itemImpl;
        } else {
            n = (NodeImpl) itemImpl.getParent();
        }
        checkPermission(itemImpl, getPermission(itemImpl.isNode(), true));
        // validation: make sure Node is not locked or checked-in.
        n.checkSetProperty();
        itemImpl.internalRemove(true);
    }

    private void checkPermission(ItemImpl item, int perm) throws RepositoryException {
        if (perm > Permission.NONE) {
            SessionImpl sImpl = (SessionImpl) item.getSession();
            AccessManager acMgr = sImpl.getAccessManager();

            Path path = item.getPrimaryPath();
            acMgr.checkPermission(path, perm);
        }
    }

    private void checkPermission(NodeImpl node, Name childName, int perm) throws RepositoryException {
        if (perm > Permission.NONE) {
            SessionImpl sImpl = (SessionImpl) node.getSession();
            AccessManager acMgr = sImpl.getAccessManager();

            boolean isGranted = acMgr.isGranted(node.getPrimaryPath(), childName, perm);
            if (!isGranted) {
                throw new AccessDeniedException("Permission denied.");
            }
        }
    }

    private int getPermission(boolean isNode, boolean isRemove) {
        if (permission < Permission.NONE) {
            if (isNode) {
                return (isRemove) ? Permission.REMOVE_NODE : Permission.ADD_NODE;
            } else {
                return (isRemove) ? Permission.REMOVE_PROPERTY : Permission.SET_PROPERTY;
            }
        } else {
            return permission;
        }
    }
}