/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * The history root node state represents the root node of all version histories.
 * the version histories are dynamically exposed. since there could be very many,
 * it does not return all the child nodes by the {@link #getChildNodeEntries()}}
 * method. this implies, that the version storage is not browsable, but the
 * nodes are nevertheless correctly exposed (this behaviour can be changed, by
 * modifying the compile-time constant {@link #LIST_ALL_HISTORIES}.
 */
public class HistoryRootNodeState extends VirtualNodeState {

    /**
     * flag for listing all histories
     */
    private static final boolean LIST_ALL_HISTORIES = true;

    /**
     * the version manager
     */
    private final VersionManager vm;

    /**
     * creates a new history root state
     * @param stateMgr
     * @param parentUUID
     * @param uuid
     * @throws RepositoryException
     */
    protected HistoryRootNodeState(VersionItemStateProvider stateMgr,
                                   VersionManager vm,
                                String parentUUID,
                                String uuid) throws RepositoryException {
        super(stateMgr, parentUUID, uuid, NodeTypeRegistry.NT_UNSTRUCTURED, new QName[0]);
        this.vm = vm;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#hasChildNodeEntry(org.apache.jackrabbit.core.QName)
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        return vm.hasVersionHistory(name.getLocalName());
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#hasChildNodeEntry(org.apache.jackrabbit.core.QName, int)
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        return index <= 1 ? vm.hasVersionHistory(name.getLocalName()) : false;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntry(org.apache.jackrabbit.core.QName, int)
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        try {
            if (index <= 1) {
                InternalVersionHistory hist = vm.getVersionHistory(nodeName.getLocalName());
                return new ChildNodeEntry(nodeName, hist.getId(), 1);
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntries()
     */
    public synchronized List getChildNodeEntries() {
        if (LIST_ALL_HISTORIES) {
            try {
                ArrayList list = new ArrayList(vm.getNumVersionHistories());
                Iterator iter = vm.getVersionHistoryIds();
                while (iter.hasNext()) {
                    String id = (String) iter.next();
                    QName name = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, id);
                    list.add(new ChildNodeEntry(name, id, 1));
                }
                return list;
            } catch (RepositoryException e) {
                // ignore
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntries(String)
     */
    public synchronized List getChildNodeEntries(String uuid) {
        // todo: do nicer
        try {
            ChildNodeEntry entry = getChildNodeEntry(new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, uuid), 1);
            if (entry != null) {
                ArrayList list = new ArrayList(1);
                list.add(entry);
                return list;
            }
        } catch (Exception e) {
            // ignore
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntries(org.apache.jackrabbit.core.QName)
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        // todo: do nicer
        try {
            ChildNodeEntry entry = getChildNodeEntry(nodeName, 1);
            if (entry != null) {
                ArrayList list = new ArrayList(1);
                list.add(entry);
                return list;
            }
        } catch (Exception e) {
            // ignore
        }
        return Collections.EMPTY_LIST;
    }
}
