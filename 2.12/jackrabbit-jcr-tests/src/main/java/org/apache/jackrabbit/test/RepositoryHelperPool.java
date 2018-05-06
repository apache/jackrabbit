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
package org.apache.jackrabbit.test;

/**
 * <code>RepositoryHelperPool</code> defines a pool of repository helper instances.
 */
public interface RepositoryHelperPool {

    /**
     * Borrows a repository helper instance.
     *
     * @return a repository helper.
     * @throws InterruptedException if this thread is interrupted while waiting
     *                              for a repository helper.
     */
    public RepositoryHelper borrowHelper() throws InterruptedException;

    /**
     * Borrows all available repository helper instances. Waits until one
     * becomes available.
     *
     * @return a repository helper.
     * @throws InterruptedException if this thread is interrupted while waiting
     *                              for a repository helper.
     */
    public RepositoryHelper[] borrowHelpers() throws InterruptedException;

    /**
     * Returns the given repository helper to the pool.
     *
     * @param helper the repository helper to return.
     */
    public void returnHelper(RepositoryHelper helper);
}
