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
package org.apache.jackrabbit.jcr2spi.lock;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>DefaultLockManager</code>...
 */
public class DefaultLockManager implements LockManager {

    private static Logger log = LoggerFactory.getLogger(DefaultLockManager.class);

    public Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped) throws LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking ist not supported by this repository.");
    }

    public Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerHint) throws LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking ist not supported by this repository.");
    }

    public void unlock(NodeState nodeState) throws LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking ist not supported by this repository.");
    }

    public Lock getLock(NodeState nodeState) throws LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking ist not supported by this repository.");
    }

    public boolean isLocked(NodeState nodeState) throws RepositoryException {
        return false;
    }

    public void checkLock(NodeState nodeState) throws LockException, RepositoryException {
        // nothing to do since locking is not supported
    }

    public String[] getLockTokens() {
        // return an empty string array
        return new String[0];
    }

    public void addLockToken(String lt) {
        // nothing to do since locking is not supported
    }

    public void removeLockToken(String lt) {
        // nothing to do since locking is not supported
    }
}