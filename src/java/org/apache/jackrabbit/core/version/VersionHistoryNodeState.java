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

import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * This Class implements...
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class VersionHistoryNodeState extends VirtualNodeState {

    private final InternalVersionHistory vh;

    public VersionHistoryNodeState(VersionItemStateProvider vm, InternalVersionHistory vh, String parentUUID) {
        super(vm, vh.getId(), NodeTypeRegistry.NT_VERSION_HISTORY, parentUUID);
        this.vh =  vh;

        setDefinitionId(vm.getNodeDefId(NodeTypeRegistry.NT_VERSION_HISTORY));
        // we do not initialize the childnode entry array, but rather
        // generate it every time.
    }

    public synchronized boolean hasChildNodeEntry(QName name) {
        return vh.hasVersion(name);
    }

    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        // no same name siblings
        return index<=1 ? vh.hasVersion(name) : false;
    }

    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        try {
            if (index<=1) {
                InternalVersion v = vh.getVersion(nodeName);
                return new ChildNodeEntry(nodeName, v.getId(), 1);
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    public synchronized List getChildNodeEntries() {
        Iterator iter = vh.getVersions();
        ArrayList list = new ArrayList(vh.getNumVersions());
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            list.add(new ChildNodeEntry(v.getName(), v.getId(), 1));
        }
        return list;
    }

    public synchronized List getChildNodeEntries(String uuid) {
        ArrayList list = new ArrayList(1);
        InternalVersion v = vh.getVersion(uuid);
        list.add(new ChildNodeEntry(v.getName(), uuid, 1));
        return list;
    }

    public synchronized List getChildNodeEntries(QName nodeName) {
        ArrayList list = new ArrayList(1);
        try {
            InternalVersion v = vh.getVersion(nodeName);
            list.add(new ChildNodeEntry(nodeName, v.getId(), 1));
        } catch (RepositoryException e) {
            // ignore
        }
        return list;
    }

}
