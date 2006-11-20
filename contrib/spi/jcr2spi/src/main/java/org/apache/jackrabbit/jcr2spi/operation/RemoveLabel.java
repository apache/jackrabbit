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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.state.NodeState;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>AddLabel</code>...
 */
public class RemoveLabel extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(RemoveLabel.class);

    private final NodeState versionHistoryState;
    private final NodeState versionState;
    private final QName label;

    private RemoveLabel(NodeState versionHistoryState, NodeState versionState, QName label) {
        this.versionHistoryState = versionHistoryState;
        this.versionState = versionState;
        this.label = label;

        // TODO: add affected states. required?
    }
    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     * @throws RepositoryException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() {
        // TODO
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeState getVersionHistoryState() {
        return versionHistoryState;
    }

    public NodeState getVersionState() {
        return versionState;
    }

    public QName getLabel() {
        return label;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param versionHistoryState
     * @param versionState
     * @param label
     * @return
     */
    public static Operation create(NodeState versionHistoryState, NodeState versionState, QName label) {
        return new RemoveLabel(versionHistoryState, versionState, label);
    }
}
