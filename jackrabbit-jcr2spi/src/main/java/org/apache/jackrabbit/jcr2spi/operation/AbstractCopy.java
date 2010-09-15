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

import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * <code>AbstractCopy</code>...
 */
public abstract class AbstractCopy extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(AbstractCopy.class);

    final NodeState destParentState;
    private final NodeState srcState;
    private final Name destName;
    private final String srcWorkspaceName;

    /**
     *
     * @param srcPath
     * @param destPath
     * @param srcMgrProvider
     */
    AbstractCopy(Path srcPath, Path destPath, String srcWorkspaceName,
                 ManagerProvider srcMgrProvider, ManagerProvider destMgrProvider)
        throws RepositoryException {

        NodeState srcItemState = getNodeState(srcPath, srcMgrProvider.getHierarchyManager());
        this.srcState = srcItemState;
        this.destParentState = getNodeState(destPath.getAncestor(1), destMgrProvider.getHierarchyManager());

        // check for illegal index present in destination path
        int index = destPath.getIndex();
        if (index != Path.INDEX_UNDEFINED) {
            // subscript in name element
            String msg = "invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        this.destName = destPath.getName();
        this.srcWorkspaceName = srcWorkspaceName;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * Invalidate the destination parent <code>NodeState</code>.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        destParentState.getHierarchyEntry().invalidate(false);
    }

    //----------------------------------------< Access Operation Parameters >---
    public String getWorkspaceName() {
        return srcWorkspaceName;
    }

    public NodeId getNodeId() throws RepositoryException {
        return srcState.getNodeId();
    }

    public NodeId getDestinationParentId() throws RepositoryException {
        return destParentState.getNodeId();
    }

    public Name getDestinationName() {
        return destName;
    }
}