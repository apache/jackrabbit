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
package org.apache.jackrabbit.core.virtual;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;

import javax.jcr.PropertyType;

/**
 * This Class implements a virtual node state
 */
public class VirtualNodeState extends NodeState {

    protected VirtualItemStateProvider provider;
    
    /**
     * @param uuid
     * @param nodeTypeName
     * @param parentUUID
     */
    protected VirtualNodeState(VirtualItemStateProvider provider,
                               String uuid, QName nodeTypeName, String parentUUID) {
        super(uuid, nodeTypeName, parentUUID, ItemState.STATUS_EXISTING);

        this.provider = provider;

        // add some props
        addPropertyEntry(ItemImpl.PROPNAME_PRIMARYTYPE);
        addPropertyEntry(ItemImpl.PROPNAME_MIXINTYPES);
    }

    public VirtualPropertyState getPropertyState(QName name)
            throws NoSuchItemStateException {
        if (name.equals(ItemImpl.PROPNAME_PRIMARYTYPE)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(ItemImpl.PROPNAME_PRIMARYTYPE));
            state.setType(PropertyType.NAME);
            state.setValues(InternalValue.create(new QName[]{getNodeTypeName()}));
            return state;
        } else if (name.equals(ItemImpl.PROPNAME_MIXINTYPES)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(ItemImpl.PROPNAME_MIXINTYPES));
            state.setType(PropertyType.NAME);
            state.setValues(InternalValue.create((QName[]) getMixinTypeNames().toArray(new QName[getMixinTypeNames().size()])));
            return state;
        } else if (name.equals(ItemImpl.PROPNAME_UUID)) {
            VirtualPropertyState state = new VirtualPropertyState(name, getUUID());
            state.setDefinitionId(provider.getPropDefId(ItemImpl.PROPNAME_UUID));
            state.setType(PropertyType.STRING);
            state.setValues(InternalValue.create(new String[]{getUUID()}));
            return state;
        }
        throw new NoSuchItemStateException(name.toString());
    }
}
