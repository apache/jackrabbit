/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This Class implements...
 */
public class HistoryRootNodeState extends VirtualNodeState {

    private VersionManager vm;

    public HistoryRootNodeState(VersionItemStateProvider vm, String uuid, String parentUUID) {
        super(vm, uuid, NodeTypeRegistry.NT_UNSTRUCTURED, parentUUID);
        this.vm = vm.getVersionManager();

        setDefinitionId(vm.getNodeDefId(VersionManager.NODENAME_HISTORY_ROOT));
    }

    public synchronized boolean hasChildNodeEntry(QName name) {
        return vm.hasVersionHistory(name);
    }

    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        return index <= 1 ? vm.hasVersionHistory(name) : false;
    }

    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        try {
            if (index <= 1) {
                InternalVersionHistory hist = vm.getVersionHistory(nodeName);
                return new ChildNodeEntry(nodeName, hist.getId(), 1);
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    public synchronized List getChildNodeEntries() {
        try {
            ArrayList list = new ArrayList(vm.getNumVersionHistories());
            Iterator iter = vm.getVersionHistories();
            while (iter.hasNext()) {
                InternalVersionHistory vh = (InternalVersionHistory) iter.next();
                QName name = new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, vh.getId());
                list.add(new ChildNodeEntry(name, vh.getId(), 1));
            }
            return list;
        } catch (RepositoryException e) {
            // ignore
        }
        return Collections.EMPTY_LIST;
    }

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
