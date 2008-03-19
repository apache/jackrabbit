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
import javax.jcr.ItemNotFoundException;
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
     * @param id the id of the Node to retrievethe PolicyTemplate for.
     * @return the PolicyTemplate or <code>null</code> no policy has been
     * applied to the node before.
     * @throws AccessControlException If the Node identified by the given id does
     * not allow ACL modifications.
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if an error occurs
     */
    PolicyTemplate getPolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Retrieves an editable policy template for the Node identified by the given
     * <code>NodeId</code>. If the node does not yet have an policy set an
     * new (empty) template is created (see also {@link #getPolicyTemplate(NodeId)}.<br>
     * The PolicyTemplate returned is detached from the underlying
     * <code>AccessControlProvider</code> and is only an external
     * representation. Modification will therefore not take effect, until it is
     * written back to the editor and persisted.
     * <p/>
     * Compared to the policy returned by {@link AccessControlProvider#getPolicy(NodeId)},
     * the scope of the PolicyTemplate it limited to the Node itself and does
     * never not take inherited elements into account.
     *
     * @param id the id of the Node to retrieve (or create) the PolicyTemplate for.
     * @return the PolicyTemplate
     * @throws AccessControlException If the Node identified by the given id does
     * not allow ACL modifications.
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if an error occurs
     */
    PolicyTemplate editPolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Stores the policy template to the respective node.
     *
     * @param id the id of the node to write the template for. Note, that a
     * {@link javax.jcr.Session#save()} is required to persist the changes. Upon
     * 'setPolicyTemplate' the modifications are applied in the transient space only.
     * @param template the <code>PolicyTemplate</code> to store.
     * @throws AccessControlException If the PolicyTemplate is <code>null</code> or
     * if it is not applicable to the Node identified by the given id.
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if an other error occurs.
     */
    void setPolicyTemplate(NodeId id, PolicyTemplate template) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Removes the template from the respective node.
     *
     * @param id the id of the node to remove the acl from.
     * @return the PolicyTemplate that has been remove or <code>null</code>
     * if there was no policy to remove.
     * @throws AccessControlException If the Node identified by the given id
     * does not allow policy modifications.
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if an other error occurs
     */
    PolicyTemplate removePolicyTemplate(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Returns the access control entries present with the node
     * identified by  <code>id</code>, that have
     * been added using {@link #addAccessControlEntry(NodeId,Principal,Privilege[])}.
     * The implementation may return other entries, if they can be removed
     * using {@link #removeAccessControlEntry(NodeId,AccessControlEntry)}.
     *
     * @param id
     * @return the (granting) access control entries present with the node
     * identified by  <code>id</code>.
     * @throws AccessControlException
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException
     */
    AccessControlEntry[] getAccessControlEntries(NodeId id) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Adds an access control entry to the node identified by
     * <code>id</code>. An implementation that always keeps entries with an
     * existing <code>AccessControlPolicy</code> may choose to treat this
     * method as short-cut for {@link #editPolicyTemplate(NodeId)} and
     * subsequent template modification.
     * Note, that in addition an implementation may only allow granting
     * ACEs as specified by JSR 283.
     *
     * @param id
     * @param principal
     * @param privileges
     * @return The entry that results from adding the specified
     * privileges for the specified principal.
     * @throws AccessControlException If the Node identified by the given id
     * does not allow access control modifications, if the principal does not
     * exist or if any of the specified privileges is unknown.
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if an other error occurs
     */
    AccessControlEntry addAccessControlEntry(NodeId id, Principal principal, Privilege[] privileges) throws AccessControlException, ItemNotFoundException, RepositoryException;

    /**
     * Removes the access control entry represented by the given
     * <code>template</code> from the node identified by
     * <code>id</code>. An implementation that always keeps entries with an
     * existing <code>AccessControlPolicy</code> may choose to treat this
     * method as short-cut for {@link #getPolicyTemplate(NodeId)} and
     * subsequent template modification.
     * Note that only <code>PolicyEntry</code>s accessible through
     * {@link #getAccessControlEntries(NodeId)} can be removed by this call.
     *
     * @param id
     * @param entry
     * @return true if entry was contained could be successfully removed.
     * @throws AccessControlException If an access control specific exception
     * occurs (e.g. invalid entry implementation, entry cannot be removed
     * by this call, etc.).
     * @throws ItemNotFoundException if no node exists for the given id.
     * @throws RepositoryException if another error occurs.
     */
    boolean removeAccessControlEntry(NodeId id, AccessControlEntry entry) throws AccessControlException, ItemNotFoundException, RepositoryException;
}
