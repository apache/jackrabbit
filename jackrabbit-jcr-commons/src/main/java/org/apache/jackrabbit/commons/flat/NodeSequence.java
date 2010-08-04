/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.commons.flat;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Extension of {@link Sequence Sequence&lt;Node>} which provides methods for
 * adding and removing nodes by key.
 */
public interface NodeSequence extends Sequence<Node> {

    /**
     * Add a with the given <code>key</code> and primary node type name.
     *
     * @param key key of the node to add
     * @param primaryNodeTypeName primary node type of the node to add
     * @return the newly added node
     * @throws RepositoryException
     */
    Node addNode(String key, String primaryNodeTypeName) throws RepositoryException;

    /**
     * Remove the node with the given key.
     *
     * @param key The key of the node to remove
     * @throws RepositoryException If there is no node with such a key or
     *             another error occurs.
     */
    void removeNode(String key) throws RepositoryException;
}