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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlException;
import org.apache.jackrabbit.core.security.jsr283.security.Privilege;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.AccessDeniedException;
import java.security.Principal;

/**
 * <code>AccessControlEditor</code> is used to edit the access control policy
 * and entry objects provided by the respective service.
 */
public interface AccessControlEditor {

    /**
     * Retrieves the policy template for the Node identified by the given
     * <code>NodeId</code>. In contrast to {@link #editPolicyTemplate} this method
     * returns <code>null</code> if no policy has been applied before by calling
     * {@link #setPolicyTemplate}). Still the returned PolicyTemplate is detached from
     * the AccessControlProvider and is only an external representation.
     * Modification will therefore not take effect, until it is written back to
     * the editor and persisted.
     * <p/>
     * Compared to the policy returned by {@link AccessControlProvider#getPolicy(NodeId)},
     * the scope of the PolicyTemplate it limited to the Node itself and does
     * not take inherited elements into account.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return the PolicyTemplate or <code>null</code> no policy has been
     * applied to the node before.
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow access control modifications (e.g.
     * the node itself stores the access control information for its parent).
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an error occurs
     */
    PolicyTemplate getPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Retrieves the policy template for the Node identified by the given
     * <code>NodeId</code>. If the node does not yet have an policy set an
     * new (empty) template is created (see also {@link #getPolicyTemplate(String)}.<br>
     * The PolicyTemplate returned is detached from the underlying
     * <code>AccessControlProvider</code> and is only an external
     * representation. Modification will therefore not take effect, until it is
     * written back to the editor and persisted.
     * <p/>
     * Compared to the policy returned by {@link AccessControlProvider#getPolicy(NodeId)},
     * the scope of the PolicyTemplate it limited to the Node itself and does
     * never not take inherited elements into account.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return policy template
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow access control modifications.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an error occurs
     */
    PolicyTemplate editPolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Returns a policy template for the given <code>principal</code>.
     *
     * @return policy template for the specified <code>principal.</code>.
     * @throws AccessControlException if the specified principal does not exist,
     * if this implementation does provide policy tempates for principals or
     * if same other access control related exception occurs.
     * @throws RepositoryException if another error occurs.
     */
    PolicyTemplate editPolicyTemplate(Principal principal) throws AccessDeniedException, AccessControlException, RepositoryException;

    /**
     * Stores the policy template to the respective node.
     *
     * @param nodePath Absolute path to an existing node object.
     * @param template the <code>PolicyTemplate</code> to store.
     * @throws AccessControlException If the PolicyTemplate is <code>null</code> or
     * if it is not applicable to the Node identified by the given
     * <code>nodePath</code>.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an other error occurs.
     */
    void setPolicyTemplate(String nodePath, PolicyTemplate template) throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Removes the template from the respective node.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return the PolicyTemplate that has been remove or <code>null</code>
     * if there was no policy to remove.
     * @throws AccessControlException If the Node identified by the given
     * <code>nodePath</code> does not allow policy modifications.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws RepositoryException if an other error occurs
     */
    PolicyTemplate removePolicyTemplate(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException;

    /**
     * Returns the access control entries present with the node
     * identified by  <code>id</code>, that have
     * been added using {@link #addAccessControlEntry(String,Principal,Privilege[])}.
     * The implementation may return other entries, if they can be removed
     * using {@link #removeAccessControlEntry(String,AccessControlEntry)}.
     *
     * @param nodePath Absolute path to an existing node object.
     * @return the (granting) access control entries present with the node
     * identified by  <code>id</code>.
     * @throws AccessControlException
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws UnsupportedRepositoryOperationException if only simple access
     * control is supported.
     * @throws RepositoryException
     */
    AccessControlEntry[] getAccessControlEntries(String nodePath) throws AccessControlException, PathNotFoundException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Adds an access control entry to the node identified by
     * <code>id</code>. An implementation that always keeps entries with an
     * existing <code>AccessControlPolicy</code> may choose to treat this
     * method as short-cut for {@link #editPolicyTemplate(String)} and
     * subsequent template modification.
     * Note, that in addition an implementation may only allow granting
     * ACEs as specified by JSR 283.
     *
     * @param nodePath Absolute path to an existing node object.
     * @param principal
     * @param privileges
     * @return The entry that results from adding the specified
     * privileges for the specified principal.
     * @throws AccessControlException If the Node identified by the given nodePath.
     * does not allow access control modifications, if the principal does not
     * exist or if any of the specified privileges is unknown.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws UnsupportedRepositoryOperationException if only simple access
     * control is supported.
     * @throws RepositoryException if an other error occurs
     */
    AccessControlEntry addAccessControlEntry(String nodePath, Principal principal, Privilege[] privileges) throws AccessControlException, PathNotFoundException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Removes the access control entry represented by the given
     * <code>template</code> from the node identified by
     * <code>id</code>. An implementation that always keeps entries with an
     * existing <code>AccessControlPolicy</code> may choose to treat this
     * method as short-cut for {@link #getPolicyTemplate(String)} and
     * subsequent template modification.
     * Note that only <code>PolicyEntry</code>s accessible through
     * {@link #getAccessControlEntries(String)} can be removed by this call.
     *
     * @param nodePath Absolute path to an existing node object.
     * @param entry The access control entry to be removed.
     * @return true if entry was contained could be successfully removed.
     * @throws AccessControlException If an access control specific exception
     * occurs (e.g. invalid entry implementation, entry cannot be removed
     * by this call, etc.).
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws UnsupportedRepositoryOperationException if only simple access
     * control is supported.
     * @throws RepositoryException if another error occurs.
     */
    boolean removeAccessControlEntry(String nodePath, AccessControlEntry entry) throws AccessControlException, PathNotFoundException, UnsupportedRepositoryOperationException, RepositoryException;
}
