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

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;

import javax.jcr.RepositoryException;

/**
 * This Class implements a virtual node state that represents a version.
 * since some properties like 'jcr:versionLabels', 'jcr:predecessors' etc. can
 * change over time, we treat them specially.
 */
public class VersionNodeState extends VirtualNodeState {

    /**
     * the internal version
     */
    private final InternalVersion v;

    /**
     * Creates a new version node state
     *
     * @param vm
     * @param v
     * @param parentUUID
     * @throws RepositoryException
     */
    protected VersionNodeState(VersionItemStateProvider vm, InternalVersion v,
                               String parentUUID)
            throws RepositoryException {
        super(vm, parentUUID, v.getId(), NT_VERSION, new QName[0]);
        this.v = v;

        // version is referenceable
        setPropertyValue(JCR_UUID, InternalValue.create(v.getId()));

        // add the frozen node id if not root version
        if (!v.isRootVersion()) {
            addChildNodeEntry(JCR_FROZENNODE, v.getFrozenNode().getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    public VirtualPropertyState getProperty(QName name)
            throws NoSuchItemStateException {
        VirtualPropertyState state = super.getProperty(name);
        if (state != null) {
            if (name.equals(JCR_VERSIONLABELS)) {
                state.setValues(InternalValue.create(v.getLabels()));
            } else if (name.equals(JCR_PREDECESSORS)) {
                InternalVersion[] preds = v.getPredecessors();
                InternalValue[] predV = new InternalValue[preds.length];
                for (int i = 0; i < preds.length; i++) {
                    predV[i] = InternalValue.create(new UUID(preds[i].getId()));
                }
                state.setValues(predV);
            } else if (name.equals(JCR_SUCCESSORS)) {
                InternalVersion[] succs = v.getSuccessors();
                InternalValue[] succV = new InternalValue[succs.length];
                for (int i = 0; i < succs.length; i++) {
                    succV[i] = InternalValue.create(new UUID(succs[i].getId()));
                }
                state.setValues(succV);
            }
        }
        return state;
    }
}
