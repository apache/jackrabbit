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

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

/**
 * <code>AbstractRemove</code> is the base class for non-transient remove
 * operations executed on the workspace such as removing versions or activities.
 */
public abstract class AbstractRemove extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(Remove.class);

    protected ItemState removeState;
    protected NodeState parent;

    protected AbstractRemove(ItemState removeState, NodeState parent) throws RepositoryException {
        this.removeState = removeState;
        this.parent = parent;

        addAffectedItemState(removeState);
        addAffectedItemState(parent);
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        parent.getHierarchyEntry().complete(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public ItemId getRemoveId() throws RepositoryException {
        return removeState.getWorkspaceId();
    }

    public NodeState getParentState() {
        return parent;
    }
}
