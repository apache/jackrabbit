/*
 * Copyright 2004 The Apache Software Foundation.
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
 * Extends the {@link ItemStateProvider} interface by providing methods that
 * deal with {@link PersistentNodeState}s and {@link PersistentPropertyState}s.
 */
public interface PersistentItemStateProvider extends ItemStateProvider {

    /**
     * Creates a {@link PersistentNodeState} instance representing new,
     * i.e. not yet existing state. Call {@link PersistentNodeState#store()}
     * on the returned object to make it persistent.
     *
     * @param uuid         node UUID
     * @param nodeTypeName qualified node type name
     * @param parentUUID   parent node's UUID
     * @return a persistent node state
     * @throws ItemStateException if an error occurs
     */
    public PersistentNodeState createNodeState(String uuid, QName nodeTypeName,
                                               String parentUUID)
            throws ItemStateException;

    /**
     * Creates a {@link PersistentPropertyState} instance representing new,
     * i.e. not yet existing state. Call {@link PersistentPropertyState#store()}
     * on the returned object to make it persistent.
     *
     * @param parentUUID parent node UUID
     * @param propName   qualified property name
     * @return a persistent property state
     * @throws ItemStateException if an error occurs
     */
    public PersistentPropertyState createPropertyState(String parentUUID,
                                                       QName propName)
            throws ItemStateException;
}
