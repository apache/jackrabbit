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

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import java.security.Principal;

/**
 * The <code>AccessControlManager</code> object is accessed via
 * {@link javax.jcr.Session#getAccessControlManager()}. It provides methods for:
 * <ul>
 * <li>Access control discovery</li>
 * <li>Assigning access control policies</li>
 * <li>Assigning access control entries</li>
 * <li>Retention and hold discovery</li>
 * <li>Adding hold(s) to existing nodes and removing them</li>
 * <li>Adding retention policies to existing nodes and removing them.</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface AccessControlManager {

    /**
     * Returns the privileges supported for absolute path <code>absPath</code>,
     * which must be an existing node.
     * <p/>
     * This method does not return the privileges held by the session. Instead,
     * it returns the privileges that the repository supports.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return an array of <code>Privilege</code>s.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public Privilege[] getSupportedPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns whether the session has the specified privileges for absolute
     * path <code>absPath</code>, which must be an existing node.
     * <p/>
     * Testing an aggregate privilege is equivalent to testing each non aggregate
     * privilege among the set returned by calling
     * <code>Privilege.getAggregatePrivileges()</code> for that privilege.
     * <p/>
     * The results reported by the this method reflect the net
     * <i>effect</i> of the currently applied control mechanisms. It does not reflect
     * unsaved access control policies or unsaved access control entries.
     * Changes to access control status caused by these mechanisms only take effect
     * on <code>Session.save()</code> and are only then reflected in the results of
     * the privilege test methods.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath    an absolute path.
     * @param privileges an array of <code>Privilege</code>s.
     * @return <code>true</code> if the session has the specified privileges;
     *         <code>false</code> otherwise.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns the privileges the session has for absolute path absPath, which
     * must be an existing node.
     * <p/>
     * The returned privileges are those for which {@link #hasPrivileges} would
     * return <code>true</code>.
     * <p/>
     * The results reported by the this method reflect the net
     * <i>effect</i> of the currently applied control mechanisms. It does not reflect
     * unsaved access control policies or unsaved access control entries.
     * Changes to access control status caused by these mechanisms only take effect
     * on <code>Session.save()</code> and are only then reflected in the results of
     * the privilege test methods.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return an array of <code>Privilege</code>s.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public Privilege[] getPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> that has been set to
     * the node at <code>absPath</code> or <code>null</code> if no
     * policy has been set. This method reflects the binding state, including
     * transient policy modifications.
     * <p/>
     * Use {@link #getEffectivePolicy(String)} in order to determine the
     * policy that effectively applies at <code>absPath</code>.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>READ_ACCESS_CONTROL</code> privilege for the <code>absPath</code> node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicy</code> object or <code>null</code>.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy getPolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> that currently is in effect
     * at the node at <code>absPath</code>. This may be an
     * <code>AccessControlPolicy</code> set through this API or some
     * implementation specific (default) policy.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>READ_ACCESS_CONTROL</code> privilege for the <code>absPath</code> node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicy</code> object.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy getEffectivePolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Returns the access control policies that are capable of being applied to
     * the node at <code>absPath</code>.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>READ_ACCESS_CONTROL</code> privilege for the <code>absPath</code> node.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicyIterator</code> over the applicable
     *         access control policies or an empty iterator if no policies are
     *         applicable.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Binds the <code>policy</code> to the node at <code>absPath</code>.
     * <p/>
     * Only one policy may be bound at a time. If more than one policy per node
     * is required, the implementation should provide an appropriate aggregate
     * policy among those returned by <code>getApplicablePolicies(absPath)</code>.
     * The access control policy does not take effect until a <code>save</code>
     * is performed.
     * <p/>
     * If the node has access control entries that were bound to it through the
     * JCR API prior to the <code>setPolicy</code> call, then these entries may
     * be deleted. Any implementation-specific (non-JCR) access control
     * settings may be changed in response to a successful call to
     * <code>setPolicy</code>.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the policy is not applicable.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege for the <code>absPath</code>
     * node.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param policy  the <code>AccessControlPolicy</code> to be applied.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the policy is not applicable.
     * @throws AccessDeniedException  if the session lacks
     *                                <code>MODIFY_ACCESS_CONTROL</code>
     *                                privilege for the <code>absPath</code> node.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public void setPolicy(String absPath, AccessControlPolicy policy)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, LockException, VersionException, RepositoryException;

    /**
     * Removes the <code>AccessControlPolicy</code> from the node at absPath and
     * returns it.
     * <p/>
     * An <code>AccessControlPolicy</code> can only be removed if it was
     * bound to the specified node through this API before. The effect of the
     * removal only takes place upon <code>Session.save()</code>. Whichever
     * defaults the implementation applies now take effect.
     * Note, that an implementation default or any other effective
     * <code>AccessControlPolicy</code> that has not been applied to the node
     * before may never be removed using this method.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if no policy exists.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege for the <code>absPath</code>
     * node.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return the removed <code>AccessControlPolicy</code>.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if no policy exists.
     * @throws AccessDeniedException  if the session lacks
     *                                <code>MODIFY_ACCESS_CONTROL</code>
     *                                privilege for the <code>absPath</code> node.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public AccessControlPolicy removePolicy(String absPath)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, LockException, VersionException, RepositoryException;

    /**
     * Returns all access control entries assigned to the node at <code>absPath</code>
     * including transient modifications made to the entries at <code>absPath</code>.
     * <p/>
     * This method is only guaranteed to return an <code>AccessControlEntry</code>
     * if that <code>AccessControlEntry</code> has been assigned <i>through this API</i>.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>READ_ACCESS_CONTROL</code> privilege for the <code>absPath</code> node.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * access control entries are not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path
     * @return all access control entries assigned at to specified node.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException
     *                               if access control entries
     *                               are not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlEntry[] getAccessControlEntries(String absPath)
            throws PathNotFoundException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the access control entries that are effective at the node at
     * <code>absPath</code>.
     * <p/>
     * This method performs a best-effort search for all access control entries in
     * effect on the node at <code>absPath</code>.
     * </p>
     * If an implementation is not able to determine the effective entries
     * present at the given node it returns <code>null</code>. If the implementation
     * positively determines that no entries present at the given node then an empty
     * array is returned.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>READ_ACCESS_CONTROL</code> privilege for the <code>absPath</code> node.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * access control entries are not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path
     * @return the access control entries that are currently effective at the
     *         node at <code>absPath</code> or <code>null</code> if the
     *         implementation is not able to determine the effective entries.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege for the
     *                               <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException
     *                               if access control entries
     *                               are not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlEntry[] getEffectiveAccessControlEntries(String absPath)
            throws PathNotFoundException, AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException;


    /**
     * Adds the access control entry consisting of the specified
     * <code>principal</code> and the specified <code>privileges</code> to the
     * node at <code>absPath</code>.
     * <p/>
     * This method returns the <code>AccessControlEntry</code> object constructed from the
     * specified <code>principal</code> and contains at least the given <code>privileges</code>.
     * An implementation may return a resulting ACE that combines the given <code>privileges</code>
     * with those added by a previous call to <code>addAccessControlEntry</code> for the same
     * <code>Principal</code>. However, a call to <code>addAccessControlEntry</code> for a given
     * <code>Principal</code> can never remove a <code>Privilege</code> added by a previous call
     * to <code>addAccessControlEntry</code>.
     * <p/>
     * The access control entry does not take effect until a <code>save</code>
     * is performed.
     * <p/>
     * This method is guaranteed to affect only the privileges of the specified
     * <code>principal</code>.
     * <p/>
     * This method <i>may</i> affect the privileges granted to that principal with
     * respect to nodes other than that specified. However, if it does, it is
     * guaranteed to only affect the privileges of those other nodes in the
     * same way as it affects the privileges of the specified node.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified principal
     * does not exist, if any of the specified privileges is not supported at
     * <code>absPath</code> or if some other access control related exception occurs.
     * <p/>
     * An <code>AccessDeniedException</code>  is thrown if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege for the <code>absPath</code>
     * node.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * access control entries are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath    an absolute path.
     * @param principal  a <code>Principal</code>.
     * @param privileges an array of <code>Privilege</code>s.
     * @return the <code>AccessControlEntry</code> object constructed from the
     *         specified <code>principal</code> and <code>privileges</code>.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the specified principal does not exist,
     *                                if any of the specified privileges is not supported at
     *                                <code>absPath</code> or if some other access control related
     *                                exception occurs.
     * @throws AccessDeniedException  if the session lacks
     *                                <code>MODIFY_ACCESS_CONTROL</code> privilege for the
     *                                <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException
     *                                if access control entries
     *                                are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public AccessControlEntry addAccessControlEntry(String absPath,
                                                    Principal principal,
                                                    Privilege[] privileges)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            LockException, VersionException, RepositoryException;

    /**
     * Removes the specified <code>AccessControlEntry</code> from the node at
     * <code>absPath</code>.
     * <p/>
     * This method is guaranteed to affect only the privileges of the principal
     * defined within the passed <code>AccessControlEntry</code>.
     * <p/>
     * This method <i>may</i> affect the privileges granted to that principal
     * with respect to nodes other than that specified. However, if it does,
     * it is guaranteed to only affect the privileges of those other nodes in
     * the same way as it affects the privileges of the specified node.
     * <p/>
     * Only exactly those entries obtained through
     * <code>getAccessControlEntries<code> can be removed. The effect of the
     * removal only takes effect upon <code>Session.save()</code>.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified entry
     * is not present on the specified node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege for the <code>absPath</code>
     * node.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * access control entries are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param ace     the access control entry to be removed.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the specified entry is not
     *                                present on the specified node.
     * @throws AccessDeniedException  if the session lacks
     *                                <code>MODIFY_ACCESS_CONTROL</code> privilege
     *                                for the <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException
     *                                if access control entries
     *                                are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public void removeAccessControlEntry(String absPath, AccessControlEntry ace)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            LockException, VersionException, RepositoryException;

    /**
     * Returns all hold objects that have been added through this API to the
     * existing node at <code>absPath</code>. If no hold has been set before,
     * this method returns an empty array.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to retrieve the holds.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return All hold objects that have been added to the existing node at
     *         <code>absPath</code> through this API or an empty array if no
     *         hold has been set.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessDeniedException if the current session does not have
     *                               sufficient rights to retrieve the holds.
     * @throws UnsupportedRepositoryOperationException
     *                               if retention and hold are not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public Hold[] getHolds(String absPath) throws PathNotFoundException,
            AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Places a hold on the existing node at <code>absPath</code>. If
     * <code>isDeep</code> is <code>true</code>) the hold applies to this node
     * and its subtree. The hold does not take effect until a <code>save</code>
     * is performed. A node may have more than one hold.
     * <p/>
     * The format and interpretation of the <code>name</code> are not specified.
     * They are application-dependent.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the
     * node at <code>absPath</code> cannot have a hold set.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param name  an application-dependent string.
     * @param isDeep  a boolean indicating if the hold applies to the subtree.
     * @return The <code>Hold</code> applied.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the node at
     *                                <code>absPath</code> cannot have a hold set.
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
     * @throws UnsupportedRepositoryOperationException
     *                                if retention and hold are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public Hold addHold(String absPath, String name, boolean isDeep)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            UnsupportedRepositoryOperationException, LockException, VersionException, RepositoryException;

    /**
     * Removes the specified <code>hold</code> from the node at
     * <code>absPath</code>. The removal does not take effect until a
     * <code>save</code> is performed.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified
     * <code>hold</code> does not apply to the node at <code>absPath</code>.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param hold    the hold to be removed.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the specified <code>hold</code> is not
     *                                present at the node at <code>absPath</code>.
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
     * @throws UnsupportedRepositoryOperationException
     *                                if retention and hold are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public void removeHold(String absPath, Hold hold)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            UnsupportedRepositoryOperationException, LockException, VersionException,
            RepositoryException;

    /**
     * Returns the retention policy that has been set using {@link #setRetentionPolicy}
     * on the node at <code>absPath</code> or <code>null</code> if no policy has been set.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to retrieve the retention policy.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path to an existing node.
     * @return The retention policy that applies to the existing node at
     *         <code>absPath</code> or <code>null</code> if no policy applies.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the current session does not have
     *                               sufficient rights to retrieve the policy.
     * @throws UnsupportedRepositoryOperationException
     *                               if retention and hold are not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public RetentionPolicy getRetentionPolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Sets the retention policy of the node at <code>absPath</code> to
     * that defined in the specified policy node. Interpretation and enforcement
     * of this policy is an implementation issue. In any case the policy does
     * does not take effect until a <code>save</code> is performed.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the specified
     * node is not a valid retention policy node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath         an absolute path to an existing node.
     * @param retentionPolicy a retention policy.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if the specified retention policy is not
     *                                valid on the specified node.
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
     * @throws UnsupportedRepositoryOperationException
     *                                if retention and hold are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public void setRetentionPolicy(String absPath, RetentionPolicy retentionPolicy)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            UnsupportedRepositoryOperationException, LockException, VersionException,
            RepositoryException;

    /**
     * Causes the current retention policy on the node at
     * <code>absPath</code> to no longer apply. The removal does not take effect
     * until a <code>save</code> is performed.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if this node does
     * not have a retention policy currently assigned.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * An <code>LockException</code> is thrown if the node at <code>absPath</code>
     * is locked and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * <p/>
     * An <code>VersionException</code> is thrown if the node at <code>absPath</code>
     * is versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in and this implementation performs this
     * validation immediately instead of waiting until <code>save</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path to an existing node.
     * @throws PathNotFoundException  if no node at <code>absPath</code> exists
     *                                or the session does not have privilege to
     *                                retrieve the node.
     * @throws AccessControlException if this node does not have a
     *                                retention policy currently assigned.
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
     * @throws UnsupportedRepositoryOperationException
     *                                if retention and hold are not supported.
     * @throws LockException          if a lock applies at the node at
     *                                <code>absPath</code> and this implementation
     *                                performs this validation immediately instead
     *                                of waiting until <code>save</code>.
     * @throws VersionException       if the node at <code>absPath</code> is
     *                                versionable and checked-in or is non-versionable
     *                                but its nearest versionable ancestor is
     *                                checked-in and this implementation performs
     *                                this validation immediately instead of
     *                                waiting until <code>save</code>.
     * @throws RepositoryException    if another error occurs.
     */
    public void removeRetentionPolicy(String absPath)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            UnsupportedRepositoryOperationException, LockException, VersionException, RepositoryException;
}
