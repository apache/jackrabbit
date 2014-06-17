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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import java.security.Principal;

/**
 * <code>AccessControlEditor</code> is used to edit the access control policy
 * and entry objects provided by the respective service.
 */
public interface AccessControlEditor {

    /**
     * Retrieves the policies for the Node identified by the given
     * <code>nodePath</code>. In contrast to {@link #editAccessControlPolicies} this method
     * returns an empty array if no policy has been applied before by calling
     * {@link #setPolicy}). Still the returned policies are detached from
     * the <code>AccessControlProvider</code> and are only an external representation.
     * Modification will therefore not take effect, until they are written back to
     * the editor and persisted.
     * <p>
     * Compared to the policy returned by {@link AccessControlProvider#getEffectivePolicies(org.apache.jackrabbit.spi.Path, CompiledPermissions)},
     * the scope of the policies it limited to the Node itself and does
     * not take inherited elements into account.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return the policies applied so far or an empty array if no
     * policy has been applied to the node before.
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow access control modifications (e.g.
     * the node itself stores the access control information for its parent).
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an error occurs
     */
    AccessControlPolicy[] getPolicies(String nodePath) throws AccessControlException,
            PathNotFoundException, RepositoryException;

    /**
     * Retrieves the policies that have been applied before for the given
     * <code>principal</code>. In contrast to {@link #editAccessControlPolicies}
     * this method returns an empty array if no policy has been applied before
     * by calling {@link #setPolicy}). Still the returned policies are detached from
     * the <code>AccessControlProvider</code> and are only an external representation.
     * Modification will therefore not take effect, until they are written back to
     * the editor and persisted.
     *
     * @param principal  Principal for which the editable policies should be
     * returned.
     * @return the policies applied so far or an empty array if no
     * policy has been applied before.
     * @throws AccessControlException if the specified principal does not exist,
     * if this implementation cannot provide policies for individual principals or
     * if same other access control related exception occurs.
     * @throws RepositoryException if an error occurs
     */
    JackrabbitAccessControlPolicy[] getPolicies(Principal principal)
            throws AccessControlException, RepositoryException;

    /**
     * Retrieves the editable policies for the Node identified by the given
     * <code>nodePath</code> that are applicable but have not yet have been set.<br>
     * The AccessControlPolicy objects returned are detached from the underlying
     * <code>AccessControlProvider</code> and is only an external
     * representation. Modification will therefore not take effect, until a
     * modified policy is written back to the editor and persisted.
     * <p>
     * See {@link #getPolicies(String)} for the corresponding method that returns
     * the editable policies that have been set to the node at
     * <code>nodePath</code> before.
     * <p>
     * Compared to the policies returned by {@link AccessControlProvider#getEffectivePolicies(org.apache.jackrabbit.spi.Path, CompiledPermissions)},
     * the scope of the policies returned by this methods it limited to the Node
     * itself and does never not take inherited elements into account.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return an array of editable access control policies.
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow access control modifications.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an error occurs
     */
    AccessControlPolicy[] editAccessControlPolicies(String nodePath)
            throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Returns an array of editable policies for the given <code>principal</code>.
     *
     * @param principal Principal for which the editable policies should be
     * returned.
     * @return an array of editable policies for the given <code>principal</code>.
     * @throws AccessDeniedException If the editing session is not allowed to
     * edit policies.
     * @throws AccessControlException if the specified principal does not exist,
     * if this implementation cannot provide policies for individual principals or
     * if same other access control related exception occurs.
     * @throws RepositoryException if another error occurs.
     */
    JackrabbitAccessControlPolicy[] editAccessControlPolicies(Principal principal)
            throws AccessDeniedException, AccessControlException, RepositoryException;

    /**
     * Stores the policy template to the respective node.
     *
     * @param nodePath Absolute path to an existing node object.
     * @param policy the <code>AccessControlPolicy</code> to store.
     * @throws AccessControlException If the policy is <code>null</code> or
     * if it is not applicable to the Node identified by the given
     * <code>nodePath</code>.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an other error occurs.
     */
    void setPolicy(String nodePath, AccessControlPolicy policy)
            throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Removes the specified policy from the node at <code>nodePath</code>.
     *
     * @param nodePath Absolute path to an existing node object.
     * @param policy The policy to be removed at <code>nodePath</code>.
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow policy modifications or does not have
     * the specified policy attached.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an other error occurs
     */
    void removePolicy(String nodePath, AccessControlPolicy policy)
            throws AccessControlException, PathNotFoundException, RepositoryException;
}
