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

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;

import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This Class implements the virtual node state for a version history.
 */
public class VersionHistoryNodeState extends VirtualNodeState {

    /**
     * the rerpesenting version history
     */
    private final InternalVersionHistory vh;

    /**
     * Creates a new versiom history node state
     *
     * @param vm
     * @param vh
     * @param parentUUID
     * @throws RepositoryException
     */
    protected VersionHistoryNodeState(VersionItemStateProvider vm,
                                      InternalVersionHistory vh,
                                      String parentUUID)
            throws RepositoryException {
        super(vm, parentUUID, vh.getId(), NodeTypeRegistry.NT_VERSION_HISTORY, new QName[0]);

        // version history is referenceable
        setPropertyValue(ItemImpl.PROPNAME_UUID, InternalValue.create(vh.getId()));

        this.vh = vh;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#hasChildNodeEntry(org.apache.jackrabbit.core.QName)
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        return vh.hasVersion(name) ? true : super.hasChildNodeEntry(name);
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#hasChildNodeEntry(org.apache.jackrabbit.core.QName, int)
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        // no same name siblings
        return index <= 1 ?  (vh.hasVersion(name) ? true : super.hasChildNodeEntry(name, index)) : false;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntry(org.apache.jackrabbit.core.QName, int)
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        if (super.hasChildNodeEntry(nodeName, index)) {
            return super.getChildNodeEntry(nodeName, index);
        }
        try {
            if (index <= 1) {
                InternalVersion v = vh.getVersion(nodeName);
                if (v!=null) {
                    return createChildNodeEntry(nodeName, v.getId(), 1);
                }
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
        List list = new ArrayList(super.getChildNodeEntries());
        Iterator iter = vh.getVersions();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            list.add(createChildNodeEntry(v.getName(), v.getId(), 1));
        }
        return list;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntries(String)
     */
    public synchronized List getChildNodeEntries(String uuid) {
        List list = new ArrayList(super.getChildNodeEntries(uuid));
        InternalVersion v = vh.getVersion(uuid);
        if (v!=null) {
            list.add(createChildNodeEntry(v.getName(), uuid, 1));
        }
        return list;
    }

    /**
     * @see org.apache.jackrabbit.core.state.NodeState#getChildNodeEntries(org.apache.jackrabbit.core.QName)
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        List list = new ArrayList(super.getChildNodeEntries(nodeName));
        try {
            InternalVersion v = vh.getVersion(nodeName);
            if (v!=null) {
                list.add(createChildNodeEntry(nodeName, v.getId(), 1));
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return list;
    }
}
