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
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.ItemId;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

/**
 * <code>Remove</code>...
 */
public class Remove extends AbstractOperation {

    private ItemId removeId;
    protected ItemState removeState;
    protected NodeState parent;

    protected Remove(ItemState removeState, NodeState parent) {
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
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        throw new UnsupportedOperationException("persisted() not implemented for transient modification.");
    }

    //----------------------------------------< Access Operation Parameters >---
    public ItemId getRemoveId() {
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
        return new Remove(state, state.getParent());
    }
}