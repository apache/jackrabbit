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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.Status;

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
    private PropertyEntryImpl(NodeEntryImpl parent, QName name, EntryFactory factory) {
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
    static PropertyEntry create(NodeEntryImpl parent, QName name, EntryFactory factory) {
        return new PropertyEntryImpl(parent, name, factory);
    }

    //------------------------------------------------------< HierarchyEntryImpl >---
    /**
     * @inheritDoc
     * @see HierarchyEntryImpl#doResolve()
     * <p/>
     * Returns a <code>PropertyState</code>.
     */
    ItemState doResolve() throws NoSuchItemStateException, ItemStateException {
        return factory.getItemStateFactory().createPropertyState(getWorkspaceId(), this);
    }

    //------------------------------------------------------< PropertyEntry >---
    /**
     * @see PropertyEntry#getId()
     */
    public PropertyId getId() {
        return factory.getIdFactory().createPropertyId(parent.getId(), getQName());
    }

    /**
     * @see PropertyEntry#getWorkspaceId()
     */
    public PropertyId getWorkspaceId() {
        return factory.getIdFactory().createPropertyId(parent.getWorkspaceId(), getQName());
    }

    /**
     * @see PropertyEntry#getPropertyState()
     */
    public PropertyState getPropertyState() throws NoSuchItemStateException, ItemStateException {
        return (PropertyState) getItemState();
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * Returns false.
     *
     * @inheritDoc
     * @see HierarchyEntry#denotesNode()
     */
    public boolean denotesNode() {
        return false;
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#remove()
     */
    public void remove() {
        ItemState state = internalGetItemState();
        if (state != null) {
            if (state.getStatus() == Status.NEW) {
                state.setStatus(Status.REMOVED);
            } else {
                state.getWorkspaceState().setStatus(Status.REMOVED);
            }
        }
        parent.internalRemovePropertyEntry(getQName());
    }
}
