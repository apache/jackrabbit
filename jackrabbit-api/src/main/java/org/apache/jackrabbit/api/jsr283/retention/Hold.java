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
package org.apache.jackrabbit.api.jsr283.retention;

import javax.jcr.RepositoryException;

/**
 * <code>Hold</code> represents a hold that can be applied to an existing node
 * in order to prevent the node from being modified or removed. The format and
 * interpretation of the name are not specified. They are application-dependent.
 * <p/>
 * If {@link #isDeep()} is <code>true</code>, the hold applies to the
 * node and its entire subtree. Otherwise the hold applies to the node and its
 * properties only.
 *
 * @see RetentionManager#getHolds(String)
 * @see RetentionManager#addHold(String, String, boolean)
 * @see RetentionManager#removeHold(String, Hold)
 * @since JCR 2.0
 */
public interface Hold {

    /**
     * Returns <code>true</code> if this <code>Hold</code> is deep.
     *
     * @return <code>true</code> if this <code>Hold</code> is deep.
     * @throws RepositoryException if an error occurs.
     */
    public boolean isDeep() throws RepositoryException;

    /**
     * Returns the name of this <code>Hold</code>. A JCR name.
     *
     * @return the name of this <code>Hold</code>. A JCR name.
     * @throws RepositoryException if an error occurs.
     */
    public String getName() throws RepositoryException;
}
