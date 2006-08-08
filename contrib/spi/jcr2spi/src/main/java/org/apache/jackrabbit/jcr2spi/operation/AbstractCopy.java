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
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>AbstractCopy</code>...
 */
public abstract class AbstractCopy extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(AbstractCopy.class);

    private final NodeId srcId;
    private final NodeId destParentId;
    private final QName destName;
    
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

        ItemState srcItemState = srcMgrProvider.getHierarchyManager().getItemState(srcPath);
        if (!srcItemState.isNode()) {
            throw new PathNotFoundException("Source path " + LogUtil.safeGetJCRPath(srcPath, srcMgrProvider.getNamespaceResolver()) + " is not a valid path.");
        }
        this.srcId = (NodeId)srcItemState.getId();
        this.destParentId = getNodeId(destPath.getAncestor(1), destMgrProvider.getHierarchyManager(), destMgrProvider.getNamespaceResolver());
        addAffectedItemId(destParentId);

        // check for illegal index present in destination path
        Path.PathElement destElement = destPath.getNameElement();
        int index = destElement.getIndex();
        if (index > Path.INDEX_UNDEFINED) {
            // subscript in name element
            String msg = "invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        this.destName = destElement.getName();
        this.srcWorkspaceName = srcWorkspaceName;
    }

    //----------------------------------------< Access Operation Parameters >---
    public String getWorkspaceName() {
        return srcWorkspaceName;
    }

    public NodeId getNodeId() {
        return srcId;
    }

    public NodeId getDestinationParentId() {
        return destParentId;
    }

    public QName getDestinationName() {
        return destName;
    }
}