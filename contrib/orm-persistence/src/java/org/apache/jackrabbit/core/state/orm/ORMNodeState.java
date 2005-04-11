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
package org.apache.jackrabbit.core.state.orm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeState.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState.PropertyEntry;
import org.apache.log4j.Logger;

/**
 * <p>This class represents an copy of Jackrabbit's node state, in an ORM
 * compatible format.</p>
 */
public abstract class ORMNodeState implements Serializable {

    private static Logger log = Logger.getLogger(ORMNodeState.class);

    protected String uuid;
    protected String parentUUID;
    protected String nodeTypeName;
    protected String definitionId;

    public ORMNodeState() {

    }

    public ORMNodeState(ItemId id) {
        uuid = id.toString();
    }

    public ORMNodeState(NodeState state) {
        fromPersistentNodeState(state);
    }

    public void fromPersistentNodeState(NodeState state) {
        getChildNodeEntries().clear();
        getPropertyEntries().clear();
        getMixinTypeNames().clear();
        getParentUUIDs().clear();
        uuid = state.getUUID();
        parentUUID = state.getParentUUID();
        if (state.getNodeTypeName() != null) {
            nodeTypeName = state.getNodeTypeName().toString();
        }
        if (state.getDefinitionId() != null) {
            definitionId = state.getDefinitionId().toString();
        }
        Iterator childNodeEntriesIter = state.getChildNodeEntries().iterator();
        int i=0;
        while (childNodeEntriesIter.hasNext()) {
            ChildNodeEntry curChildNodeEntry = (ChildNodeEntry) childNodeEntriesIter.next();
            log.debug("childNodeEntry " + curChildNodeEntry.getIndex() + " name=" + curChildNodeEntry.getName() + " uuid=" + curChildNodeEntry.getUUID());
            ORMChildNodeEntry childNode = new ORMChildNodeEntry(this, curChildNodeEntry, uuid, i);
            getChildNodeEntries().add(childNode);
            i++;
        }
        Iterator propertyEntryIter = state.getPropertyEntries().iterator();
        while (propertyEntryIter.hasNext()) {
            PropertyEntry curPropertyEntry = (PropertyEntry) propertyEntryIter.next();
            log.debug("propertyEntry " + curPropertyEntry.getName());
            ORMPropertyEntry propertyEntry = new ORMPropertyEntry(this, curPropertyEntry, uuid);
            getPropertyEntries().add(propertyEntry);
        }
        Iterator mixinTypeIter = state.getMixinTypeNames().iterator();
        while (mixinTypeIter.hasNext()) {
            QName curName = (QName) mixinTypeIter.next();
            getMixinTypeNames().add(new ORMNodeMixinType(this, uuid, curName.toString()));
        }
        Iterator parentIter = state.getParentUUIDs().iterator();
        while (parentIter.hasNext()) {
            String parentId = (String) parentIter.next();
            getParentUUIDs().add(new ORMNodeParent(this, uuid, parentId));
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public String getNodeTypeName() {
        return nodeTypeName;
    }

    public String getDefinitionId() {

        return definitionId;
    }

    public abstract Collection getChildNodeEntries();

    public abstract Collection getPropertyEntries();

    public abstract Collection getMixinTypeNames();

    public abstract Collection getParentUUIDs();

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setNodeTypeName(String nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    public void setDefinitionId(String definitionId) {

        this.definitionId = definitionId;
    }

    public abstract void setChildNodeEntries(Collection childNodeEntries);

    public abstract void setPropertyEntries(Collection propertyEntries);

    public abstract void setMixinTypeNames(Collection mixinTypeNames);

    public abstract void setParentUUIDs(Collection parentUUIDs);

    public void toPersistentNodeState(NodeState state) {
        state.setDefinitionId(NodeDefId.valueOf(getDefinitionId()));
        state.setNodeTypeName(QName.valueOf(getNodeTypeName()));
        state.setParentUUID(getParentUUID());

        Iterator childNodeEntryIter = getChildNodeEntries().iterator();
        while (childNodeEntryIter.hasNext()) {
            ORMChildNodeEntry curChildNodeEntry = (ORMChildNodeEntry) childNodeEntryIter.next();
            log.debug("  Loaded child node " + QName.valueOf(curChildNodeEntry.getName()) + " uuid=" + curChildNodeEntry.getUuid());
            state.addChildNodeEntry(QName.valueOf(curChildNodeEntry.getName()), curChildNodeEntry.getUuid());
        }
        Iterator propertyEntryIter = getPropertyEntries().iterator();
        while (propertyEntryIter.hasNext()) {
            ORMPropertyEntry curPropertyEntry = (ORMPropertyEntry) propertyEntryIter.next();
            log.debug("  Loaded property " + QName.valueOf(curPropertyEntry.getName()));
            state.addPropertyEntry(QName.valueOf(curPropertyEntry.getName()));
        }
        Iterator mixinTypeNameIter = getMixinTypeNames().iterator();
        Set mixinTypeQNames = new HashSet();
        while (mixinTypeNameIter.hasNext()) {
            ORMNodeMixinType curMixinType = (ORMNodeMixinType) mixinTypeNameIter.next();
            mixinTypeQNames.add(QName.valueOf(curMixinType.getMixinTypeName()));
        }
        state.setMixinTypeNames(mixinTypeQNames);
        Iterator parentUUIDIter = getParentUUIDs().iterator();
        List nParentUUIDs = new ArrayList();
        while (parentUUIDIter.hasNext()) {
            ORMNodeParent curNodeParent = (ORMNodeParent) parentUUIDIter.next();
            nParentUUIDs.add(curNodeParent.getParentUUID());
        }
        state.setParentUUIDs(nParentUUIDs);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMNodeState)) {
            return false;
        }
        ORMNodeState right = (ORMNodeState) obj;
        if (getUuid().equals(right.getUuid())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return getUuid().hashCode();
    }
}
