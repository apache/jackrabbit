/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.QName;

/**
 * An update operation started inside an <code>ItemStateManager</code>
 */
public interface UpdateOperation {

    /**
     * Creates a {@link NodeState} instance representing new,
     * i.e. not yet existing state. Call {@link #store}
     * on the returned object to make it persistent.
     *
     * @param uuid         node UUID
     * @param nodeTypeName qualified node type name
     * @param parentUUID   parent node's UUID
     * @return a node state
     */
    public NodeState createNew(String uuid, QName nodeTypeName,
                               String parentUUID);

    /**
     * Creates a {@link PropertyState} instance representing new,
     * i.e. not yet existing state. Call {@link #store}
     * on the returned object to make it persistent.
     *
     * @param propName   qualified property name
     * @param parentUUID parent node UUID
     * @return a property state
     */
    public PropertyState createNew(QName propName, String parentUUID);

    /**
     * Store an item state.
     * @param state item state that should be stored
      */
    public void store(ItemState state);

    /**
     * Store a node references object
     * @param refs node references object that should be stored
     */
    public void store(NodeReferences refs);

    /**
     * Destroy an item state.
     * @param state item state that should be destroyed
      */
    public void destroy(ItemState state);

    /**
     * End this update operation. This will save all items
     * added to this update operation in a single step.
     * If this operation fails, no item will have been saved.
     * @throws ItemStateException if the update operation failed
     */
    public void end() throws ItemStateException;
}
