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

import javax.jcr.security.AccessControlPolicy;

import org.apache.jackrabbit.spi.Path;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * The AccessControlProvider is used to provide access control policy and entry
 * objects that apply to an item in a single workspace. The provider is bound
 * to a system session in contrast to the <code>AccessControlManager</code> that
 * is bound to a specific session/subject.
 * <p>
 * Please note following additional special conditions:
 * <ul>
 * <li>The detection of access control policy/entries is an implementation
 * detail. They may be resource based or retrieved by other means.</li>
 * <li>An access control policy/entry may be inherited across the item hierarchy.
 * The details are left to the implementation</li>
 * <li>If no policy can be determined for a particular Item the implementation
 * must return some implementation specific default policy.</li>
 * <li>Transient (NEW) items created within a regular Session object are unknown
 * to and cannot be handled by the <code>AccessControlProvider</code>.</li>
 * <li>If the item id passed to the corresponding calls doesn't point to an
 * existing item, <code>ItemNotFoundException</code> will be thrown. It is
 * therefore recommended to evaluate the id of the closest not-new ancestor
 * node before calling any methods on the provider.</li>
 * <li>Changes to access control policy and entries made through the
 * <code>AccessControlEditor</code> are not effective unless
 * they are persisted by calling <code>Session.save()</code> on the session
 * that has been used to obtain the editor.</li>
 * </ul>
 *
 * @see AccessControlProviderFactory
 */
public interface AccessControlProvider {

    /**
     * Allows the {@link AccessControlProviderFactory} to pass a session
     * and configuration parameters to the <code>AccessControlProvider</code>.
     *
     * @param systemSession System session.
     * @param configuration Configuration used to initialize this provider.
     * @throws RepositoryException If an error occurs.
     */
    void init(Session systemSession, Map configuration) throws RepositoryException;

    /**
     * Closes this provider when it is no longer used by the respective
     * workspace and release resources bound by this provider.
     */
    void close();

    /**
     * Returns <code>true</code>, if this provider is still alive and able to
     * evaluate permissions; <code>false</code> otherwise.
     *
     * @return <code>true</code>, if this provider is still alive and able to
     * evaluate permissions; <code>false</code> otherwise.
     */
    boolean isLive();

    /**
     * Returns the effective policies for the node at the given absPath.
     *
     * @param absPath an absolute path.
     * @param permissions The effective permissions of the editing
     * sessions that attempts to view the effective policies.
     * @return The effective policies that apply at <code>absPath</code> or
     * an empty array if the implementation cannot determine the effective
     * policy at the given path.
     * @throws ItemNotFoundException If no Node with the specified
     * <code>absPath</code> exists.
     * @throws RepositoryException If another error occurs.
     * @see javax.jcr.security.AccessControlManager#getEffectivePolicies(String)
     */
    AccessControlPolicy[] getEffectivePolicies(Path absPath, CompiledPermissions permissions) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the effective policies for the given principals.
     *
     * @param principals A set of principal.
     * @param permissions The effective permissions of the editing
     * sessions that attempts to view the effective policies.  @return The effective policies that are in effect for the given
     * <code>principal</code> or an empty array.
     * @throws RepositoryException If error occurs.
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getEffectivePolicies(Set)
     */
    AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals, CompiledPermissions permissions) throws RepositoryException;

    /**
     * Returns an <code>AccessControlEditor</code> for the given Session object
     * or <code>null</code> if the implementation does not support editing
     * of access control policies.
     *
     * @param session The editing session.
     * @return the ACL editor or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    AccessControlEditor getEditor(Session session) throws RepositoryException;

    /**
     * Compiles the effective policy for the specified set of
     * <code>Principal</code>s.
     *
     * @param principals Set of principals to compile the permissions for. If
     * the order of evaluating permissions for principals is meaningful, the
     * caller should pass a Set that respects the order of insertion.
     * @return The effective, compiled CompiledPolicy that applies for the
     * specified set of principals.
     * @throws RepositoryException If an error occurs.
     */
    CompiledPermissions compilePermissions(Set<Principal> principals) throws RepositoryException;

    /**
     * Returns <code>true</code> if the given set of principals can access the
     * root node of the workspace this provider has been built for;
     * <code>false</code> otherwise.
     *
     * @param principals Set of principals to be tested for being allowed to
     * access the root node.
     * @return <code>true</code> if the given set of principals can access the
     * root node of the workspace this provider has been built for;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an error occurs.
     */
    boolean canAccessRoot(Set<Principal> principals) throws RepositoryException;
}
