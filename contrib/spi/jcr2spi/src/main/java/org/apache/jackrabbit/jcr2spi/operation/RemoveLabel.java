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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;

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

    private final NodeId versionHistoryId;
    private final NodeId versionId;
    private final QName label;

    private RemoveLabel(NodeId versionHistoryId, NodeId versionId, QName label) {
        this.versionHistoryId = versionHistoryId;
        this.versionId = versionId;
        this.label = label;
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

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getVersionHistoryId() {
        return versionHistoryId;
    }

    public NodeId getVersionId() {
        return versionId;
    }

    public QName getLabel() {
        return label;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param versionHistoryId
     * @param versionId
     * @param label
     * @param moveLabel
     * @return
     */
    public static Operation create(NodeId versionHistoryId, NodeId versionId, QName label) {
        return new RemoveLabel(versionHistoryId, versionId, label);
    }
}
