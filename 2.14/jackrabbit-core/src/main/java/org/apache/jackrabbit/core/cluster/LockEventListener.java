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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.id.NodeId;

import javax.jcr.RepositoryException;

/**
 * Interface used to receive information about incoming, external lock events.
 */
public interface LockEventListener {

    /**
     * Handle an external lock operation.
     *
     * @param nodeId node id
     * @param isDeep <code>true</code> if the lock is deep;
     *               <code>false</code> otherwise
     * @param lockOwner lock owner
     * @throws RepositoryException if the lock cannot be processed
     */
    void externalLock(NodeId nodeId, boolean isDeep, String lockOwner) throws RepositoryException;

    /**
     * Handle an external unlock operation.
     *
     * @param nodeId node id
     * @throws RepositoryException if the unlock cannot be processed
     */
    void externalUnlock(NodeId nodeId) throws RepositoryException;

}
