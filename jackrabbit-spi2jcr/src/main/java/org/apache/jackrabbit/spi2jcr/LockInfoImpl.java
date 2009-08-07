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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.LockInfo;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

/**
 * <code>LockInfoImpl</code> implements a <code>LockInfo</code> on top of a
 * JCR repository.
 */
class LockInfoImpl extends org.apache.jackrabbit.spi.commons.LockInfoImpl {

    /**
     * Creates a new lock info for the given JCR lock object.
     *
     * @param lock the lock.
     * @param idFactory the id factory.
     * @param resolver the name and path resolver.
     * @throws RepositoryException if an error occurs while the node from the
     * given lock or while creating the node id.
     */
    private LockInfoImpl(Lock lock, IdFactoryImpl idFactory,
                         NamePathResolver resolver) throws RepositoryException {
        super(lock.getLockToken(), lock.getLockOwner(),
                lock.isDeep(), lock.isSessionScoped(),
                idFactory.createNodeId(lock.getNode(), resolver));
    }

    /**
     * Creates a new lock info for the given JCR lock object.
     * 
     * @param lock the JCR lock.
     * @param idFactory the id factory.
     * @param resolver the name and path resolver.
     * @throws RepositoryException If an error occurs while creating the info.
     * @since JCR 2.0
     */
    private LockInfoImpl(org.apache.jackrabbit.api.jsr283.lock.Lock lock,
                         IdFactoryImpl idFactory, NamePathResolver resolver) throws RepositoryException {
        super(lock.getLockToken(), lock.getLockOwner(), lock.isDeep(), lock.isSessionScoped(), lock.getSecondsRemaining(), lock.isLockOwningSession(), idFactory.createNodeId(lock.getNode(), resolver));
    }

    /**
     * Create a new <code>LockInfo</code> from the given parameters.
     * 
     * @param lock the JCR lock.
     * @param idFactory the id factory.
     * @param resolver the name and path resolver.
     * @return a new <code>LockInfo</code>
     * @throws RepositoryException If an error occurs while creating the info.
     */
    public static LockInfo createLockInfo(Lock lock, IdFactoryImpl idFactory, NamePathResolver resolver) throws RepositoryException {
        if (lock instanceof org.apache.jackrabbit.api.jsr283.lock.Lock) {
            return new LockInfoImpl((org.apache.jackrabbit.api.jsr283.lock.Lock) lock, idFactory, resolver);
        } else {
            return new LockInfoImpl(lock, idFactory, resolver);
        }
    }
}