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
package org.apache.jackrabbit.jcr2spi.state.entry;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;

/**
 * <code>PropertyReference</code> implements a reference to a property state.
 */
public class PropertyReference extends ChildItemReference implements ChildPropertyEntry {

    /**
     * IdFactory to create an ItemId based on the parent NodeId
     */
    private final IdFactory idFactory;

    /**
     * Creates a new <code>ChildPropertyEntry</code>.
     *
     * @param parent
     * @param name
     * @param isf
     * @param idFactory
     * @return new <code>ChildPropertyEntry</code>
     */
    public static ChildPropertyEntry create(NodeState parent, QName name, ItemStateFactory isf, IdFactory idFactory) {
        return new PropertyReference(parent, name, isf, idFactory);
    }

    /**
     * Creates a new <code>ChildPropertyEntry</code> for an property state that
     * already exists.
     *
     * @param propState
     * @param isf
     * @param idFactory
     * @return new <code>ChildPropertyEntry</code>
     */
    public static ChildPropertyEntry create(PropertyState propState, ItemStateFactory isf, IdFactory idFactory) {
        return new PropertyReference(propState, isf, idFactory);
    }

    /**
     * Creates a new <code>PropertyReference</code>.
     *
     * @param parent    the parent <code>NodeState</code> where the property
     *                  belongs to.
     * @param name      the name of the property.
     * @param isf       the item state factory to create the property state.
     * @param idFactory the id factory to create new ids.
     */
    private PropertyReference(NodeState parent, QName name, ItemStateFactory isf, IdFactory idFactory) {
        super(parent, name, isf);
        this.idFactory = idFactory;
    }

    /**
     * Creates a new <code>PropertyReference</code> for an property state that
     * already exists.
     *
     * @param propState the property state.
     * @param isf       the item state factory to re-create the property state.
     * @param idFactory the id factory to create new ids.
     */
    private PropertyReference(PropertyState propState, ItemStateFactory isf, IdFactory idFactory) {
        super(propState.getParent(), propState, propState.getQName(), isf);
        this.idFactory = idFactory;
    }

    /**
     * @inheritDoc
     * @see ChildItemReference#doResolve()
     * <p/>
     * Returns a <code>PropertyState</code>.
     */
    protected ItemState doResolve()
            throws NoSuchItemStateException, ItemStateException {
        return isf.createPropertyState(getId(), parent);
    }

    //-------------------------------------------------< ChildPropertyEntry >---
    /**
     * @inheritDoc
     */
    public PropertyId getId() {
        return idFactory.createPropertyId(parent.getNodeId(), getName());
    }

    /**
     * @inheritDoc
     */
    public PropertyState getPropertyState() throws NoSuchItemStateException, ItemStateException {
        return (PropertyState) resolve();
    }

    /**
     * Returns false.
     *
     * @inheritDoc
     * @see ChildItemEntry#denotesNode()
     */
    public boolean denotesNode() {
        return false;
    }

    /**
     * @inheritDoc
     * @see ChildItemEntry#getItemState()
     */
    public ItemState getItemState() throws NoSuchItemStateException, ItemStateException {
        return getPropertyState();
    }
}
