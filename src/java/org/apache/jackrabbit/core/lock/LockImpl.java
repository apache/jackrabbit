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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.NodeId;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Implementation of a <code>Lock</code> that gets returned to clients asking
 * for a lock.
 */
class LockImpl implements Lock {

    /**
     * Lock info containing latest information
     */
    private final LockInfo info;

    /**
     * Node holding lock
     */
    private final Node node;

    /**
     * Create a new instance of this class.
     * @param info lock information
     * @param node node holding lock
     */
    public LockImpl(LockInfo info, Node node) {
        this.info = info;
        this.node = node;
    }

    //------------------------------------------------------------------< Lock >

    /**
     * {@inheritDoc}
     */
    public String getLockOwner() {
        return info.lockOwner;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeep() {
        return info.deep;
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode() {
        return node;
    }

    /**
     * {@inheritDoc}
     */
    public String getLockToken() {
        try {
            return info.getLockToken(node.getSession());
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        return info.isLive();
    }

    /**
     * {@inheritDoc}
     */
    public void refresh() throws LockException, RepositoryException {
        if (isLive()) {
            throw new LockException("Lock still alive.");
        }
        if (getLockToken() == null) {
            throw new LockException("Session does not hold lock.");
        }
        info.refresh((NodeImpl) node);
    }
}
