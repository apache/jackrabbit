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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.name.NamespaceResolver;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

/**
 * <code>LockInfoImpl</code> implements a <code>LockInfo</code> on top of a
 * JCR repository.
 */
class LockInfoImpl extends org.apache.jackrabbit.spi.commons.LockInfoImpl {

    /**
     * Creates a new lock info for the given locked <code>node</code>.
     *
     * @param lock       the lock.
     * @param idFactory  the id factory.
     * @param nsResolver the namespace resolver in use.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code> or if <code>node</code> is
     *                             not locked.
     */
    public LockInfoImpl(Lock lock,
                        IdFactoryImpl idFactory,
                        NamespaceResolver nsResolver)
            throws RepositoryException {
        super(lock.getLockToken(), lock.getLockOwner(),
                lock.isDeep(), lock.isSessionScoped(),
                idFactory.createNodeId(lock.getNode(), nsResolver));
    }
}
