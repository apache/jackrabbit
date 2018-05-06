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
package org.apache.jackrabbit.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;

/**
 * <code>LockedWrapper</code> is a wrapper class to {@link Locked} which adds
 * generics support and wraps the <code>Locked.TIMED_OUT</code> object into a
 * {@link LockException}.
 */
public abstract class LockedWrapper<T> extends Locked {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.util.Locked#with(javax.jcr.Node, boolean)
     */
    @Override
    @SuppressWarnings("unchecked")
    public T with(Node lockable, boolean isDeep) throws RepositoryException,
            InterruptedException {
        return (T) super.with(lockable, isDeep);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.util.Locked#with(javax.jcr.Node, boolean,
     * boolean)
     */
    @Override
    @SuppressWarnings("unchecked")
    public T with(Node lockable, boolean isDeep, boolean isSessionScoped)
            throws RepositoryException, InterruptedException {
        return (T) super.with(lockable, isDeep, isSessionScoped);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.util.Locked#with(javax.jcr.Node, boolean,
     * long)
     */
    @Override
    @SuppressWarnings("unchecked")
    public T with(Node lockable, boolean isDeep, long timeout)
            throws UnsupportedRepositoryOperationException,
            RepositoryException, InterruptedException {

        Object r = super.with(lockable, isDeep, timeout);
        if (r == Locked.TIMED_OUT) {
            throw new LockException("Node locked.");
        }
        return (T) r;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.util.Locked#with(javax.jcr.Node, boolean,
     * long, boolean)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object with(Node lockable, boolean isDeep, long timeout,
            boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException,
            RepositoryException, InterruptedException {

        Object r = super.with(lockable, isDeep, timeout, isSessionScoped);
        if (r == Locked.TIMED_OUT) {
            throw new LockException("Node locked.");
        }
        return (T) r;
    }

    /**
     * This method is executed while holding the lock.
     * 
     * @param node
     *            The <code>Node</code> on which the lock is placed.
     * @return an object which is then returned by {@link #with with()}.
     * @throws RepositoryException
     *             if an error occurs.
     */
    @Override
    protected abstract T run(Node node) throws RepositoryException;

}