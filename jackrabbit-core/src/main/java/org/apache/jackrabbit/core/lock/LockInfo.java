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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.id.NodeId;

import javax.jcr.Session;

/**
 * Lock information interface.
 */
public interface LockInfo {

    /**
     * Return the ID of the lock holding node
     * @return the id
     */
    public NodeId getId();
    
    /**
     * Return the lock owner.
     * 
     * @return lock owner
     */
    public String getLockOwner();
    
    /**
     * Return a flag indicating whether the lock is deep.
     * 
     * @return <code>true</code> if the lock is deep;
     *         <code>false</code> otherwise
     */
    public boolean isDeep();
    
    /**
     * Return a flag indicating whether the session given is lock holder. 
     *
     * @param session session to compare with
     */
    public boolean isLockHolder(Session session);

    /**
     * Return the lock token associated with this lock.
     *
     * @return lock token
     */
    public String getLockToken();

    /**
     * Return a flag indicating whether the lock is live
     *
     * @return <code>true</code> if the lock is live; otherwise <code>false</code>
     */
    public boolean isLive();

    /**
     * Return a flag indicating whether the lock is session-scoped
     *
     * @return <code>true</code> if the lock is session-scoped;
     *         otherwise <code>false</code>
     */
    public boolean isSessionScoped();

    /**
     * Return the number of seconds remaining until the lock expires.
     *
     * @return number of seconds remaining until the lock expires.
     */
    public long getSecondsRemaining();
}
