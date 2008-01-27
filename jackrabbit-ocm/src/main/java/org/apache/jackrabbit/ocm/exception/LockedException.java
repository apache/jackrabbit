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
package org.apache.jackrabbit.ocm.exception;

/**
 * Throwed if a path is locked and a operation cannot be performed
 *
 * @author markoc
 */

public class LockedException extends LockingException {

    private final String lockOwner;

    private final String lockedNodePath;

    public LockedException(String lockOwner, String lockedNodePath) {
        super();
        this.lockOwner = lockOwner;
        this.lockedNodePath = lockedNodePath;
    }

    /**
     *
     * @return The JCR Lock Owner
     */
    public String getLockOwner() {
        return lockOwner;
    }

    /**
     *
     * @return The JCR locked node path
     */
    public String getLockedNodePath() {
        return lockedNodePath;
    }

}
