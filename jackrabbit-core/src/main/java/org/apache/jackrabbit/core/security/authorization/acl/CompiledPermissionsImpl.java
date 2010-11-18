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

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.security.authorization.*;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.util.Text;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import java.security.Principal;
import java.util.*;

/**
 * <code>CompiledPermissionsImpl</code>...
 */
class CompiledPermissionsImpl extends AbstractCompiledPermissions implements AccessControlListener {

    private final List<String> principalNames;
    private final SessionImpl session;
    private final EntryCollector entryCollector;
    private final AccessControlUtils util;

    /*
     * Start with initial map size of 1024 and grow up to 5000 before
     * removing LRU items.
     */
    @SuppressWarnings("unchecked")
    private final Map<ItemId, Boolean> readCache = new LRUMap(1024) {
        @Override
        protected boolean removeLRU(LinkEntry entry) {
            return size() > 5000;
        }
    };

    private final Object monitor = new Object();

    CompiledPermissionsImpl(Set<Principal> principals, SessionImpl session,
                            EntryCollector entryCollector, AccessControlUtils util,
                            boolean listenToEvents) throws RepositoryException {
        this.session = session;
        this.entryCollector = entryCollector;
        this.util = util;

        principalNames = new ArrayList<String>(principals.size());
        for (Principal princ : principals) {
            principalNames.add(princ.getName());
        }

        if (listenToEvents) {
            /*
            Make sure this AclPermission recalculates the permissions if
            any ACL concerning it is modified.
            */
            entryCollector.addListener(this);
        }
    }

    private Result buildResult(NodeImpl node, boolean isExistingNode, boolean isAcItem, EntryFilterImpl filter) throws RepositoryException {
        // retrieve all ACEs at path or at the direct ancestor of path that
        // apply for the principal names.
        NodeImpl n = ACLProvider.getNode(node, isAcItem);
        Iterator<AccessControlEntry> entries = entryCollector.collectEntries(n, filter).iterator();

        /*
        Calculate privileges and permissions:
        Since the ACEs only define privileges on a node and do not allow
        to add additional restrictions, the permissions can be determined
        without taking the given target name or target item into account.
        */
        int allows = Permission.NONE;
        int denies = Permission.NONE;

        int allowPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
        int denyPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
        int parentAllows = PrivilegeRegistry.NO_PRIVILEGE;
        int parentDenies = PrivilegeRegistry.NO_PRIVILEGE;

        String parentPath = Text.getRelativeParent(filter.getPath(), 1);

        while (entries.hasNext()) {
            ACLTemplate.Entry ace = (ACLTemplate.Entry) entries.next();
            /*
            Determine if the ACE also takes effect on the parent:
            Some permissions (e.g. add-node or removal) must be determined
            from privileges defined for the parent.
            A 'local' entry defined on the target node never effects the
            parent. For inherited ACEs determine if the ACE matches the
            parent path.
            */
            int entryBits = ace.getPrivilegeBits();
            boolean isLocal = isExistingNode && ace.isLocal(node.getNodeId());
            boolean matchesParent = (!isLocal && ace.matches(parentPath));
            if (matchesParent) {
                if (ace.isAllow()) {
                    parentAllows |= Permission.diff(entryBits, parentDenies);
                } else {
                    parentDenies |= Permission.diff(entryBits, parentAllows);
                }
            }
            if (ace.isAllow()) {
                allowPrivileges |= Permission.diff(entryBits, denyPrivileges);
                int permissions = PrivilegeRegistry.calculatePermissions(allowPrivileges, parentAllows, true, isAcItem);
                allows |= Permission.diff(permissions, denies);
            } else {
                denyPrivileges |= Permission.diff(entryBits, allowPrivileges);
                int permissions = PrivilegeRegistry.calculatePermissions(denyPrivileges, parentDenies, false, isAcItem);
                denies |= Permission.diff(permissions, allows);
            }
        }
        return new Result(allows, denies, allowPrivileges, denyPrivileges);
    }

    //------------------------------------< AbstractCompiledPermissions >---
    /**
     * @see AbstractCompiledPermissions#buildResult(org.apache.jackrabbit.spi.Path)
     */
    @Override
    protected Result buildResult(Path absPath) throws RepositoryException {
        boolean existingNode = false;
        NodeImpl node;

        ItemManager itemMgr = session.getItemManager();
        try {
            ItemImpl item = itemMgr.getItem(absPath);
            if (item.isNode()) {
                node = (NodeImpl) item;
                existingNode = true;
            } else {
                node = (NodeImpl) item.getParent();
            }
        } catch (RepositoryException e) {
            // path points to a non-persisted item.
            // -> find the nearest persisted node starting from the root.
            Path.Element[] elems = absPath.getElements();
            NodeImpl parent = (NodeImpl) session.getRootNode();
            for (int i = 1; i < elems.length - 1; i++) {
                Name name = elems[i].getName();
                int index = elems[i].getIndex();
                if (!parent.hasNode(name, index)) {
                    // last persisted node reached
                    break;
                }
                parent = parent.getNode(name, index);

            }
            node = parent;
        }

        if (node == null) {
            // should never get here
            throw new ItemNotFoundException("Item out of hierarchy.");
        }

        boolean isAcItem = util.isAcItem(absPath);
        return buildResult(node, existingNode, isAcItem, new EntryFilterImpl(principalNames, absPath, session));
    }

    /**
     * @see AbstractCompiledPermissions#clearCache()
     */
    @Override
    protected void clearCache() {
        synchronized (monitor) {
            readCache.clear();
        }
        super.clearCache();
    }

    //--------------------------------------------< CompiledPermissions >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.CompiledPermissions#close()
     */
    @Override
    public void close() {
        entryCollector.removeListener(this);
        // NOTE: do not logout shared session.
        super.close();
    }

    /**
     * @see org.apache.jackrabbit.core.security.authorization.CompiledPermissions#canRead(Path, ItemId)
     */
    public boolean canRead(Path path, ItemId itemId) throws RepositoryException {
        ItemId id = (itemId == null) ? session.getHierarchyManager().resolvePath(path) : itemId;
        // no extra check for existence as method may only be called for existing items.
        boolean isExistingNode = id.denotesNode();
        boolean canRead;
        synchronized (monitor) {
            if (readCache.containsKey(id)) {
                canRead = readCache.get(id);
            } else {
                ItemManager itemMgr = session.getItemManager();
                NodeId nodeId = (isExistingNode) ? (NodeId) id : ((PropertyId) id).getParentId();
                NodeImpl node = (NodeImpl) itemMgr.getItem(nodeId);
                // TODO: check again if retrieving the path can be avoided
                Path absPath = (path == null) ? session.getHierarchyManager().getPath(id) : path;
                Result result = buildResult(node, isExistingNode, util.isAcItem(node), new EntryFilterImpl(principalNames, absPath, session));

                canRead = result.grants(Permission.READ);
                readCache.put(id, canRead);
            }
        }
        return canRead;
    }

    //----------------------------------------< ACLModificationListener >---
    /**
     * @see org.apache.jackrabbit.core.security.authorization.AccessControlListener#acModified(org.apache.jackrabbit.core.security.authorization.AccessControlModifications)
     */
    public void acModified(AccessControlModifications modifications) {
        // ignore the details of the modifications and clear all caches.
        clearCache();
    }
}
