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
package org.apache.jackrabbit.api.jsr283.security;

import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * The <code>AccessControlList</code> is an <code>AccessControlPolicy</code>
 * representing a list of {@link AccessControlEntry access control entries}.
 * It is mutable before being {@link AccessControlManager#setPolicy(String, AccessControlPolicy) set}
 * to the AccessControlManager and consequently defines methods to read and
 * mutate the list i.e. to get, add or remove individual entries.
 *
 * @since JCR 2.0
 */
public interface AccessControlList extends AccessControlPolicy {

    /**
     * Returns all access control entries present with this policy.
     * <p/>
     * This method is only guaranteed to return an <code>AccessControlEntry</code>
     * if that <code>AccessControlEntry</code> has been assigned <i>through this
     * API</i>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if an error occurs.
     *
     * @return all access control entries present with this policy.
     * @throws RepositoryException   if an error occurs.
     */
    public AccessControlEntry[] getAccessControlEntries() throws RepositoryException;

    /**
     * Adds an access control entry to this policy consisting of the specified
     * <code>principal</code> and the specified <code>privileges</code>.
     * <p/>
     * This method returns <code>true</code> if this policy was modified,
     * <code>false</code> otherwise.
     * <p/>
     * How the entries are grouped within the list is an implementation detail.
     * An implementation may e.g. combine the specified privileges with those
     * added by a previous call to <code>addAccessControlEntry</code> for the
     * same <code>Principal</code>. However, a call to
     * <code>addAccessControlEntry</code> for a given <code>Principal</code> can
     * never remove a <code>Privilege</code> added by a previous call.
     * <p/>
     * The modification does not take effect until this policy has been
     * set to a node by calling {@link AccessControlManager#setPolicy(String, AccessControlPolicy)}
     * and <code>save</code> is performed.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified principal
     * or any of the privileges does not exist or if some other access control
     * related exception occurs.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param principal  a <code>Principal</code>.
     * @param privileges an array of <code>Privilege</code>s.
     * @return <code>true</code> if this policy was modify; <code>false</code>
     * otherwise.
     * @throws AccessControlException if the specified principal or any of the
     *                                privileges does not existor if some other
     *                                access control related exception occurs.
     * @throws RepositoryException    if another error occurs.
     */
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges)
            throws AccessControlException, RepositoryException;

    /**
     * Removes the specified <code>AccessControlEntry</code> from this policy.
     * <p/>
     * Only exactly those entries obtained through
     * <code>getAccessControlEntries<code> can be removed. This method does
     * not take effect until this policy has been re-set to a node by calling
     * {@link AccessControlManager#setPolicy(String, AccessControlPolicy)}
     * and <code>save</code> is performed.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified entry
     * is not present on this policy.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param ace     the access control entry to be removed.
     * @throws AccessControlException if the specified entry is not
     *                                present on the specified node.
     * @throws RepositoryException    if another error occurs.
     */
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException;
}
