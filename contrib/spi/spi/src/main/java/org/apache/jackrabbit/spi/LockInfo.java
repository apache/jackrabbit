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
 * <code>LockInfo</code>...
 */
public interface LockInfo {

    /**
     * Returns the lock token for this lock if it is hold by the requesting
     * session or <code>null</code> otherwise.
     *
     * @return lock token or <code>null</code>
     * @see javax.jcr.lock.Lock#getLockToken()
     */
    public String getLockToken();

    /**
     * Returns the user ID of the user who owns this lock.
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
     * Returns the <code>NodeId</code> of the lock-holding Node.
     *
     * @return the <code>NodeId</code> of the lock-holding Node.
     */
    public NodeId getNodeId();
}