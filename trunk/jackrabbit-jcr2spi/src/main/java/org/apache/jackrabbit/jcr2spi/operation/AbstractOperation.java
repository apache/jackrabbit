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
package org.apache.jackrabbit.jcr2spi.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>AbstractOperation</code>...
 */
public abstract class AbstractOperation implements Operation {

    /**
     * The collection of affected ItemStates.
     */
    private final Collection<ItemState> affectedStates = new ArrayList<ItemState>();
    protected int status;

    /**
     * Returns the name of the class
     *
     * @return the class name
     * @see #getClass()
     */
    public String getName() {
        return getClass().getName();
    }

    public Collection<ItemState> getAffectedItemStates() {
    	if (affectedStates.isEmpty()) {
    		return Collections.emptySet();
    	}
    	else {
    		return Collections.unmodifiableCollection(affectedStates);
    	}
    }

    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        throw new UnsupportedOperationException("Undo not supported.");
    }

    public int getStatus() {
        return status;
    }

    /**
     * Adds an affected <code>ItemState</code>.
     *
     * @param affectedState the <code>ItemState</code>s of the affected item.
     */
    protected void addAffectedItemState(ItemState affectedState) {
        affectedStates.add(affectedState);
    }

    /**
     *
     * @param nodePath
     * @param hierMgr
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static NodeState getNodeState(Path nodePath, HierarchyManager hierMgr) throws PathNotFoundException, RepositoryException {
        NodeState nodeState = hierMgr.getNodeState(nodePath);
        return nodeState;
    }

    /**
     * Asserts that the NodeEntry of the given parent state has it's child node
     * entries loaded.
     *
     * @param parentState
     * @throws RepositoryException
     */
    protected static void assertChildNodeEntries(NodeState parentState) throws RepositoryException {
        parentState.getNodeEntry().getNodeEntries();
    }
}