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
package org.apache.jackrabbit.api.jsr283.lock;

import javax.jcr.RepositoryException;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0.
 *
 * @since JCR 2.0
 */
public interface Lock extends javax.jcr.lock.Lock {

    /**
     * Returns the seconds remaining until this locks times out
     * ({@link Long#MAX_VALUE} if the timeout is unknown or infinite).
     * @return a <code>long</code>
     * @throws RepositoryException If an error occurs.
     * @since JCR 2.0
     */
    public long getSecondsRemaining() throws RepositoryException;

    /**
     * Returns <code>true</code> if the current session is the owner of this
     * lock, either because it is session-scoped and bound to this session or
     * open-scoped and this session currently holds the token for this lock.
     * Returns <code>false</code> otherwise.
     *
     * @return a <code>boolean</code>.
     * @since JCR 2.0
     */
    public boolean isLockOwningSession();

}