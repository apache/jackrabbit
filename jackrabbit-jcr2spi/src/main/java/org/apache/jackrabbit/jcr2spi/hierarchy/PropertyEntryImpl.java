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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;

/**
 * <code>PropertyEntryImpl</code> implements a reference to a property state.
 */
public class PropertyEntryImpl extends HierarchyEntryImpl implements PropertyEntry {

    /**
     * Creates a new <code>PropertyEntryImpl</code>.
     *
     * @param parent    the parent <code>NodeEntry</code> where the property
     *                  belongs to.
     * @param name      the name of the property.
     * @param factory
     */
    private PropertyEntryImpl(NodeEntryImpl parent, Name name, EntryFactory factory) {
        super(parent, name, factory);
    }

    /**
     * Creates a new <code>PropertyEntry</code>.
     *
     * @param parent
     * @param name
     * @param factory
     * @return new <code>PropertyEntry</code>
     */
    static PropertyEntry create(NodeEntryImpl parent, Name name, EntryFactory factory) {
        return new PropertyEntryImpl(parent, name, factory);
    }

    //------------------------------------------------------< HierarchyEntryImpl >---
    /**
     * @see HierarchyEntryImpl#doResolve()
     * <p>
     * Returns a <code>PropertyState</code>.
     */
    @Override
    ItemState doResolve() throws ItemNotFoundException, RepositoryException {
        return getItemStateFactory().createPropertyState(getWorkspaceId(), this);
    }

    /**
     * @see HierarchyEntryImpl#buildPath(boolean)
     */
    @Override
    Path buildPath(boolean workspacePath) throws RepositoryException {
        Path parentPath = parent.buildPath(workspacePath);
        return getPathFactory().create(parentPath, getName(), true);
    }

    //------------------------------------------------------< PropertyEntry >---
    /**
     * @see PropertyEntry#getId()
     */
    public PropertyId getId() throws InvalidItemStateException, RepositoryException {
        return getIdFactory().createPropertyId(parent.getId(), getName());
    }

    /**
     * @see PropertyEntry#getWorkspaceId()
     */
    public PropertyId getWorkspaceId() throws InvalidItemStateException, RepositoryException {
        return getIdFactory().createPropertyId(parent.getWorkspaceId(), getName());
    }

    /**
     * @see PropertyEntry#getPropertyState()
     */
    public PropertyState getPropertyState() throws ItemNotFoundException, RepositoryException {
        return (PropertyState) getItemState();
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * Returns false.
     *
     * @see HierarchyEntry#denotesNode()
     */
    public boolean denotesNode() {
        return false;
    }

    /**
     * @see HierarchyEntry#complete(Operation)
     */
    public void complete(Operation operation) throws RepositoryException {
        if (!(operation instanceof SetPropertyValue)) {
            throw new IllegalArgumentException();
        }
        SetPropertyValue op = (SetPropertyValue) operation;
        if (op.getPropertyState().getHierarchyEntry() != this) {
            throw new IllegalArgumentException();
        }
        switch (operation.getStatus()) {
            case Operation.STATUS_PERSISTED:
                // Property can only be the change log target if it was existing
                // and has been modified. This includes the case where a property
                // was changed and then removed by removing its parent. See JCR-2462.
                // Removal, add and implicit modification of protected
                // properties must be persisted by save on parent.
                PropertyState state = op.getPropertyState();
                if (state.getStatus() != Status.REMOVED) {
                    state.setStatus(Status.EXISTING);
                }
                break;
            case Operation.STATUS_UNDO:
                revert();
                break;
            default:
                // ignore
        }
    }

}
