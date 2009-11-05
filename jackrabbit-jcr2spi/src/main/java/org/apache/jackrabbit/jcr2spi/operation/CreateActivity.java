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

import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * <code>Checkout</code>...
 */
public class CreateActivity extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(CreateActivity.class);

    private final String title;
    private final VersionManager mgr;

    private NodeId newActivityId;

    private CreateActivity(String title, VersionManager mgr) {
        this.title = title;
        this.mgr = mgr;
        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidate the target <code>NodeState</code>.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        
        // TODO: check if invalidation of the activity store is required.
    }

    //----------------------------------------< Access Operation Parameters >---
    /**
     *
     * @return
     */
    public String getTitle() throws RepositoryException {
        return title;
    }

    public void setNewActivityId(NodeId newActivityId) {
        this.newActivityId = newActivityId;
    }

    public NodeId getNewActivityId() {
        return this.newActivityId;
    }

    //------------------------------------------------------------< Factory >---
    public static CreateActivity create(String title, VersionManager mgr) {
        return new CreateActivity(title, mgr);
    }
}