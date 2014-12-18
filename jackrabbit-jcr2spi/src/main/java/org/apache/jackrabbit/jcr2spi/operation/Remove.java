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

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.ItemId;

/**
 * <code>Remove</code>...
 */
public class Remove extends TransientOperation {

    private static final int REMOVE_OPTIONS =
            ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;

    private final ItemId removeId;
    protected ItemState removeState;
    protected NodeState parent;

    private Remove(ItemState removeState, NodeState parent, int options) throws RepositoryException {
        super(options);
        this.removeId = removeState.getId();
        this.removeState = removeState;
        this.parent = parent;

        addAffectedItemState(removeState);
        addAffectedItemState(parent);
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        parent.getHierarchyEntry().complete(this);
    }

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
        return removeId;
    }

    public ItemState getRemoveState() {
        return removeState;
    }

    public NodeState getParentState() {
        return parent;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(ItemState state) throws RepositoryException {
        return create(state, REMOVE_OPTIONS);
    }

    public static Operation create(ItemState state, int options) throws RepositoryException {
        if (state.isNode() && ((NodeState) state).getDefinition().allowsSameNameSiblings()) {
            // in case of SNS-siblings make sure the parent hierarchy entry has
            // its child entries loaded.
            assertChildNodeEntries(state.getParent());
        }
        return new Remove(state, state.getParent(), options);
    }
}