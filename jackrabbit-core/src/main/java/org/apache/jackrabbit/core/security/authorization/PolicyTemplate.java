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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;

import javax.jcr.RepositoryException;

/**
 * PolicyTemplate is the editable view of an AccessControlPolicy and detached
 * from the later. Therefore modifications made to the template will not take
 * effect, until it is
 * {@link org.apache.jackrabbit.api.jsr283.security.AccessControlManager#setPolicy(String, AccessControlPolicy)
 * written back} and {@link javax.jcr.Session#save() saved}.
 */
public interface PolicyTemplate extends AccessControlPolicy {

    /**
     * Returns the path of the node this template has been created for.
     *  
     * @return the path of the node this template has been created for.
     */
    String getPath();

    /**
     * Returns <code>true</code> if this template does not yet define any
     * entries.
     *
     * @return If no entries are present.
     */
    boolean isEmpty();

    /**
     * Returns the number of entries.
     *
     * @return The number of entries present.
     */
    int size();

    /**
     * Returns all {@link PolicyEntry entries} of this
     * <code>PolicyTemplate</code>.
     *
     * @return the {@link PolicyEntry entries} present in this
     * <code>PolicyTemplate</code>.
     */
    PolicyEntry[] getEntries();

    /**
     * Set the parameters defined by the given <code>entry</code> to this
     * policy template. If this policy already contains that entry this method
     * will return <code>false</code>. If the entry differs from an existing
     * entry only by the privileges defined, that entry will be replaced.
     *
     * @param entry
     * @return true if this policy has changed by incorporating the given entry;
     * false otherwise.
     * @throws AccessControlException If the given entry cannot be handled,
     * contains invalid parameters or if some other access control specific
     * exception occurs.
     * @throws RepositoryException If another error occurs.
     */
    boolean setEntry(PolicyEntry entry) throws AccessControlException, RepositoryException;

    /**
     * Removes the specified entry. Returns true, if the entry was successfully
     * removed, false otherwise.
     *
     * @param entry the <code>PolicyEntry</code> to remove
     * @return true if this policy contained the specified entry and it could
     * be successfully removed; false otherwise.
     * @throws AccessControlException If an access control specific exception
     * occurs (e.g. invalid entry implementation).
     * @throws RepositoryException If another error occurs.
     */
    boolean removeEntry(PolicyEntry entry) throws AccessControlException, RepositoryException;
}
