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
package org.apache.jackrabbit.core.version;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implements a internal representation of a baseline node.
 */
class InternalBaselineImpl extends InternalVersionImpl
        implements InternalBaseline {

    /**
     * Creates a new internal baseline with the given version history and
     * persistence node. please note, that versions must be created by the
     * version history.
     *
     * @param vh containing version history
     * @param node node state of this version
     * @param name name of this version
     */
    InternalBaselineImpl(InternalVersionHistoryImpl vh, NodeStateEx node, Name name) throws RepositoryException {
        super(vh, node, name);
    }

    /**
     * {@inheritDoc}
     */
    public VersionSet getBaseVersions() throws RepositoryException {
        InternalValue[] vs = ((InternalFrozenNodeImpl) getFrozenNode()).node.
                getPropertyValues(NameConstants.REP_VERSIONS);
        Map<NodeId, InternalVersion> versions = new HashMap<NodeId, InternalVersion>();
        if (vs != null) {
            for (InternalValue v: vs) {
                InternalVersion iv = vMgr.getVersion(v.getNodeId());
                if (iv != null) {
                    versions.put(iv.getVersionHistory().getId(), iv);
                }
            }
        }
        return new VersionSet(versions);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getConfigurationId() {
        return getVersionHistory().getVersionableId();
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getConfigurationRootId() {
        return ((InternalFrozenNodeImpl) getFrozenNode()).node.getPropertyValue
                (NameConstants.JCR_ROOT).getNodeId();
    }
}