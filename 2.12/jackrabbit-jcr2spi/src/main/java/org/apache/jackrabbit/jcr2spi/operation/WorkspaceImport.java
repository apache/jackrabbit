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

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.InputStream;

/**
 * <code>WorkspaceImport</code>...
 */
public class WorkspaceImport extends AbstractOperation {

    private final NodeState nodeState;
    private final InputStream xmlStream;
    private final int uuidBehaviour;

    private WorkspaceImport(NodeState nodeState, InputStream xmlStream, int uuidBehaviour) {
        if (nodeState == null || xmlStream == null) {
            throw new IllegalArgumentException();
        }
        this.nodeState = nodeState;
        this.xmlStream = xmlStream;
        this.uuidBehaviour = uuidBehaviour;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been updated and all
     * its descendants.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        NodeEntry entry;
        if (uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING ||
                uuidBehaviour == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            // invalidate the complete tree
            entry = nodeState.getNodeEntry();
            while (entry.getParent() != null) {
                entry = entry.getParent();
            }
            entry.invalidate(true);
        } else {
            // import only added new items below the import target. therefore
            // recursive invalidation is not required. // TODO correct?
            nodeState.getNodeEntry().invalidate(false);
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeId();
    }

    public InputStream getXmlStream() {
        return xmlStream;
    }

    public int getUuidBehaviour() {
        return uuidBehaviour;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param xmlStream
     * @return
     */
    public static Operation create(NodeState nodeState, InputStream xmlStream, int uuidBehaviour) {
        return new WorkspaceImport(nodeState, xmlStream, uuidBehaviour);
    }
}
