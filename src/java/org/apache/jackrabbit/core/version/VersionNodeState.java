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

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;

import javax.jcr.PropertyType;
import java.util.Calendar;

/**
 * This Class implements...
 */
public class VersionNodeState extends VirtualNodeState {

    private final InternalVersion v;

    public VersionNodeState(VersionItemStateProvider vm, InternalVersion v) {
        super(vm, v.getId(), NodeTypeRegistry.NT_VERSION, v.getVersionHistory().getId());
        this.v = v;

        // we map the property entries, since they do not change
        addPropertyEntry(VersionManager.PROPNAME_CREATED);
        addPropertyEntry(VersionManager.PROPNAME_FROZEN_UUID);
        addPropertyEntry(VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE);
        addPropertyEntry(VersionManager.PROPNAME_FROZEN_MIXIN_TYPES);
        addPropertyEntry(VersionManager.PROPNAME_VERSION_LABELS);
        addPropertyEntry(VersionManager.PROPNAME_PREDECESSORS);
        addPropertyEntry(VersionManager.PROPNAME_SUCCESSORS);

        // and the frozen node if not root version
        if (!v.isRootVersion()) {
            addChildNodeEntry(v.getFrozenNode().getName(), v.getFrozenNode().getInternalUUID());
        }

        setDefinitionId(vm.getNodeDefId(VersionManager.NODENAME_ROOTVERSION));
    }

    public VirtualPropertyState getPropertyState(QName name) throws NoSuchItemStateException {
        if (name.equals(VersionManager.PROPNAME_CREATED)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_CREATED));
            state.setType(PropertyType.DATE);
            state.setValues(InternalValue.create(new Calendar[]{v.getCreated()}));
            return state;
        } else if (name.equals(VersionManager.PROPNAME_FROZEN_UUID)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_FROZEN_UUID));
            state.setType(PropertyType.STRING);
            state.setValues(InternalValue.create(new String[]{v.getFrozenNode().getUUID()}));
            return state;
        } else if (name.equals(VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_FROZEN_PRIMARY_TYPE));
            state.setType(PropertyType.NAME);
            state.setValues(InternalValue.create(new QName[]{v.getFrozenNode().getFrozenPrimaryType()}));
            return state;
        } else if (name.equals(VersionManager.PROPNAME_FROZEN_MIXIN_TYPES)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_FROZEN_MIXIN_TYPES));
            state.setType(PropertyType.NAME);
            state.setValues(InternalValue.create(v.getFrozenNode().getFrozenMixinTypes()));
            return state;
        } else if (name.equals(VersionManager.PROPNAME_VERSION_LABELS)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_VERSION_LABELS));
            state.setType(PropertyType.STRING);
            state.setValues(InternalValue.create(v.internalGetLabels()));
            return state;
        } else if (name.equals(VersionManager.PROPNAME_PREDECESSORS)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_PREDECESSORS));
            state.setType(PropertyType.STRING);
            InternalVersion[] preds = v.getPredecessors();
            InternalValue[] predV = new InternalValue[preds.length];
            for (int i = 0; i < preds.length; i++) {
                predV[i] = InternalValue.create(new UUID(preds[i].getId()));
            }
            state.setValues(predV);
            return state;
        } else if (name.equals(VersionManager.PROPNAME_SUCCESSORS)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(VersionManager.PROPNAME_SUCCESSORS));
            state.setType(PropertyType.STRING);
            InternalVersion[] succs = v.getSuccessors();
            InternalValue[] succV = new InternalValue[succs.length];
            for (int i = 0; i < succs.length; i++) {
                succV[i] = InternalValue.create(new UUID(succs[i].getId()));
            }
            state.setValues(succV);
            return state;
        } else {
            return super.getPropertyState(name);
        }
    }
}
