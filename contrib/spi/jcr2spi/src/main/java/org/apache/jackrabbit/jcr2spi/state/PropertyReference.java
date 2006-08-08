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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;

/**
 * <code>PropertyReference</code> implements a reference to a property state.
 */
class PropertyReference extends ChildItemReference implements ChildPropertyEntry {

    /**
     * IdFactory to create an ItemId based on the parent NodeId
     */
    private final IdFactory idFactory;

    /**
     * Creates a new <code>PropertyReference</code>.
     *
     * @param parent    the parent <code>NodeState</code> where the property
     *                  belongs to.
     * @param name      the name of the property.
     * @param isf       the item state factory to create the node state.
     * @param idFactory the id factory to create new ids.
     */
    public PropertyReference(NodeState parent, QName name, ItemStateFactory isf, IdFactory idFactory) {
        super(parent, name, isf);
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
        return isf.createPropertyState(getId(), getParent());
    }

    //-------------------------------------------------< ChildPropertyEntry >---
    /**
     * @inheritDoc
     */
    public PropertyId getId() {
        return idFactory.createPropertyId(parent.getNodeId(), name);
    }

    /**
     * @inheritDoc
     */
    public QName getName() {
        return name;
    }

    /**
     * @inheritDoc
     */
    public PropertyState getPropertyState() throws NoSuchItemStateException, ItemStateException {
        return (PropertyState) resolve();
    }
}
