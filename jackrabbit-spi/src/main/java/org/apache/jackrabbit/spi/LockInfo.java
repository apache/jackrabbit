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
package org.apache.jackrabbit.spi;

/**
 * <code>LockInfo</code> is used to transport lock information across the SPI
 * boundary.
 *
 * @see RepositoryService#getLockInfo(SessionInfo, NodeId)
 * @see RepositoryService#lock(SessionInfo, NodeId, boolean, boolean)
 */
public interface LockInfo {

    /**
     * Returns the lock token for this lock or <code>null</code> if the token
     * should not be exposed to the API user.
     *
     * @return lock token or <code>null</code>
     * @see javax.jcr.lock.Lock#getLockToken()
     */
    public String getLockToken();

    /**
     * Returns the user ID of the user who owns this lock or some user defined
     * information about the lock owner.
     *
     * @return user ID of the user who owns this lock.
     * @see javax.jcr.lock.Lock#getLockOwner()
     */
    public String getOwner();

    /**
     * Returns true if the Lock is deep. False otherwise.
     *
     * @return true if the Lock is deep. False otherwise.
     * @see javax.jcr.lock.Lock#isDeep()
     */
    public boolean isDeep();

    /**
     * Returns true if the Lock is session scoped. False otherwise.
     *
     * @return true if the Lock is session scoped. False otherwise.
     * @see javax.jcr.lock.Lock#isSessionScoped()
     */
    public boolean isSessionScoped();
 
    /**
     * Returns the seconds remaining until the lock times out or
     * ({@link Long#MAX_VALUE} if the timeout is unknown or infinite).
     *
     * @return number of seconds until the lock times out.
     * @see javax.jcr.lock.Lock#getSecondsRemaining()
     * @since JCR 2.0
     */
    public long getSecondsRemaining();

    /**
     * Returns <code>true</code> if the <code>SessionInfo</code> used to
     * retrieve this <code>LockInfo</code> is the lock holder and thus enabled
     * to refresh or release the lock.
     *
     * @return <code>true</code> if the <code>SessionInfo</code> used to
     * retrieve this <code>LockInfo</code> is the lock holder.
     * @see javax.jcr.lock.Lock#isLockOwningSession()
     * @since JCR 2.0
     */
    public boolean isLockOwner();

    /**
     * Returns the <code>NodeId</code> of the lock-holding Node.
     *
     * @return the <code>NodeId</code> of the lock-holding Node.
     */
    public NodeId getNodeId();
}