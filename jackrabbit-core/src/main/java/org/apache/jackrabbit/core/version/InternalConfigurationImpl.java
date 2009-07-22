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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implements a internal representation of a configuration node.
 */
class InternalConfigurationImpl extends InternalVersionItemImpl
        implements InternalConfiguration {

    /**
     * Creates a new InternalConfiguration object for the given node state.
     * @param vMgr version manager
     * @param node version history node state
     * @throws RepositoryException if an error occurs
     */
    public InternalConfigurationImpl(InternalVersionManagerBase vMgr, NodeStateEx node)
            throws RepositoryException {
        super(vMgr, node);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getRootId() throws RepositoryException {
        InternalValue value = node.getPropertyValue(NameConstants.JCR_ROOT);
        if (value == null) {
            throw new RepositoryException("Internal error: configuration has no recorded jcr:root");
        }
        return value.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public InternalBaseline getBaseline() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("InternalConfiguration.getBaseline()");
    }
}