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

import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.Map;

/**
 * <code>JackrabbitAccessControlList</code> is an extension of the <code>AccessControlList</code>.
 * Similar to the latter any modifications made will not take effect, until it is
 * {@link org.apache.jackrabbit.api.jsr283.security.AccessControlManager#setPolicy(String, AccessControlPolicy)
 * written back} and {@link javax.jcr.Session#save() saved}.
 */
public interface JackrabbitAccessControlList extends JackrabbitAccessControlPolicy, AccessControlList {

    /**
     * Returns the names of the supported restrictions or an empty array
     * if no restrictions are respected.
     *
     * @return the names of the supported restrictions or an empty array.
     * @see #addEntry(Principal, Privilege[], boolean, Map)
     */
    String[] getRestrictionNames();

    /**
     * Return the expected {@link javax.jcr.PropertyType property type} of the
     * restriction with the specified <code>restrictionName</code>.
     *
     * @param restrictionName Any of the restriction names retrieved from
     * {@link #getRestrictionNames()}.
     * @return expected {@link javax.jcr.PropertyType property type}.
     */
    int getRestrictionType(String restrictionName);

    /**
     * Returns <code>true</code> if this policy does not yet define any
     * entries.
     *
     * @return If no entries are present.
     */
    boolean isEmpty();

    /**
     * Returns the number of entries or 0 if the policy {@link #isEmpty() is empty}.
     *
     * @return The number of entries present or 0 if the policy {@link #isEmpty() is empty}.
     */
    int size();

    /**
     * Same as {@link #addEntry(Principal, Privilege[], boolean, Map)} using
     * some implementation specific restrictions.
     *
     * @param principal
     * @param privileges
     * @param isAllow
     * @return true if this policy has changed by incorporating the given entry;
     * false otherwise.
     * @throws AccessControlException If any of the given parameter is invalid
     * or cannot be handled by the implementation.
     * @throws RepositoryException If another error occurs.
     * @see AccessControlList#addAccessControlEntry(Principal, Privilege[])
     */
    boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow) throws AccessControlException, RepositoryException;

    /**
     * Adds an access control entry to this policy consisting of the specified
     * <code>principal</code>, the specified <code>privileges</code>, the
     * <code>isAllow</code> flag and an optional map containing additional
     * restrictions.
     * <p/>
     * This method returns <code>true</code> if this policy was modified,
     * <code>false</code> otherwise.
     * <p/>
     * An <code>AccessControlException</code> is thrown if any of the specified
     * parameters is invalid or if some other access control related exception occurs.
     * 
     * @param principal
     * @param privileges
     * @param isAllow
     * @param restrictions A map of additional restrictions used to narrow the
     * effect of the entry to be created. The map must map JCR names to a single
     * {@link javax.jcr.Value} object.
     * @return true if this policy has changed by incorporating the given entry;
     * false otherwise.
     * @throws AccessControlException If any of the given parameter is invalid
     * or cannot be handled by the implementation.
     * @throws RepositoryException If another error occurs.
     * @see AccessControlList#addAccessControlEntry(Principal, Privilege[])
     */
    boolean addEntry(Principal principal, Privilege[] privileges,
                     boolean isAllow, Map restrictions) throws AccessControlException, RepositoryException;
}
