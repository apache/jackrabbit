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

/**
 * <code>PropertyReference</code> implements a reference to a property state.
 */
public class PropertyReference extends ChildItemReference {

    /**
     * Creates a new <code>PropertyReference</code>.
     *
     * @param parent the parent <code>NodeState</code> where the property
     *               belongs to.
     * @param name   the name of the property.
     */
    public PropertyReference(NodeState parent, QName name) {
        super(parent, name);
    }

    /**
     * @inheritDoc
     * @see ChildItemReference#doResolve(ItemStateFactory)
     * <p/>
     * Returns a <code>PropertyState</code>.
     */
    protected ItemState doResolve(ItemStateFactory isf)
            throws NoSuchItemStateException, ItemStateException {
        return isf.createPropertyState(parent, name);
    }
}
