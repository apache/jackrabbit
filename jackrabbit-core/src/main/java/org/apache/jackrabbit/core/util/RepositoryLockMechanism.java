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
package org.apache.jackrabbit.core.util;

import javax.jcr.RepositoryException;

/**
 * Exclusive lock on a repository home directory. This class encapsulates
 * collective experience on how to acquire an exclusive lock on a given
 * directory. The lock is expected to be exclusive both across process
 * boundaries and within a single JVM.
 */
public interface RepositoryLockMechanism {

    /**
     * Initialize the instance for the given directory path. The lock still needs to be 
     * explicitly acquired using the {@link #acquire()} method.
     *
     * @param homeDir directory path
     * @throws RepositoryException if the canonical path of the directory
     *                             can not be determined
     */
    void init(String homeDir) throws RepositoryException;
    
    /**
     * Lock the repository home.
     *
     * @throws RepositoryException if the repository lock can not be acquired
     */
    void acquire() throws RepositoryException;
    
    /**
     * Releases repository lock.
     * 
     * @throws RepositoryException if the repository lock can not be released
     */
    void release() throws RepositoryException;
}
