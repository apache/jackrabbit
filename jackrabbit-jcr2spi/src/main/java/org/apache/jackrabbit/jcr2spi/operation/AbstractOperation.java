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

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

/**
 * <code>AbstractOperation</code>...
 */
public abstract class AbstractOperation implements Operation {

    /**
     * The collection of affected ItemIds.
     */
    private final Collection affectedStates = new ArrayList();

    /**
     * Returns the name of the class
     *
     * @return the class name
     * @see #getClass()
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * @inheritDoc
     */
    public Collection getAffectedItemStates() {
        return (affectedStates.isEmpty()) ? Collections.EMPTY_LIST : Collections.unmodifiableCollection(affectedStates);
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
     * @param resolver
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static NodeState getNodeState(Path nodePath, HierarchyManager hierMgr, PathResolver resolver) throws PathNotFoundException, RepositoryException {
        ItemState itemState = hierMgr.getItemState(nodePath);
        if (!itemState.isNode()) {
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(nodePath, resolver));
        }
        return (NodeState) itemState;
    }
}