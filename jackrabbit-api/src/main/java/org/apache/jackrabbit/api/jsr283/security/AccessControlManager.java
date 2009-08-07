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
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

/**
 * The <code>AccessControlManager</code> object is accessed via
 * {@link javax.jcr.Session#getAccessControlManager()}. It provides methods for:
 * <ul>
 * <li>Access control discovery</li>
 * <li>Assigning access control policies</li>
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
     * Returns the privilege with the specified <code>privilegeName</code>.
     * <p/>
     * A <code>AccessControlException</code> is thrown if no privilege with
     * the specified name exists.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param privilegeName the name of an existing privilege.
     * @return the <code>Privilege</code> with the specified <code>privilegeName</code>.
     * @throws AccessControlException if no privilege with the specified name exists.
     * @throws RepositoryException    if another error occurs.
     */
    public Privilege privilegeFromName(String privilegeName)
            throws AccessControlException, RepositoryException;

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
     * Returns the <code>AccessControlPolicy</code> objects that have been set to
     * the node at <code>absPath</code> or an empty array if no policy has been
     * set. This method reflects the binding state, including transient policy
     * modifications.
     * <p/>
     * Use {@link #getEffectivePolicies(String)} in order to determine the
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
     * @return an array of <code>AccessControlPolicy</code> objects or an empty
     * array if no policy has been set.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy[] getPolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> objects that currently are
     * in effect at the node at <code>absPath</code>. This may be policies
     * set through this API or some implementation specific (default) policies.
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
     * @return an array of <code>AccessControlPolicy</code> objects.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy[] getEffectivePolicies(String absPath)
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
     * <p>
     * The behavior of the call <code>acm.setPolicy(absPath, policy)</code>
     * differs depending on how the <code>policy</code> object was originally acquired.
     * <p>
     * If <code>policy</code> was acquired through
     * {@link #getApplicablePolicies acm.getApplicablePolicies(absPath)}
     * then that <code>policy</code> object is <i>added</i> to the node at <code>absPath</code>.
     * <p>
     * On the other hand, if <code>policy</code> was acquired through
     * {@link #getPolicies acm.getPolicies(absPath)}
     * then that <code>policy</code> object (usually after being altered) replaces its former version
     * on the node at <code>absPath</code>.
     * <p>
     * The access control policy does not take effect until a <code>save</code> is performed.
     * <p>
     * Any implementation-specific (non-JCR) access control settings may be
     * changed in response to a successful call to <code>setPolicy</code>.
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
     * Removes the specified <code>AccessControlPolicy</code> from the node at
     * <code>absPath</code>.
     * <p/>
     * An <code>AccessControlPolicy</code> can only be removed if it was
     * bound to the specified node through this API before. The effect of the
     * removal only takes place upon <code>Session.save()</code>.
     * Note, that an implementation default or any other effective
     * <code>AccessControlPolicy</code> that has not been applied to the node
     * before may never be removed using this method.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessControlException</code> is thrown if the policy to remove
     * does not exist at the node at <code>absPath</code>.
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
     * @param policy  the policy to be removed.
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
    public void removePolicy(String absPath, AccessControlPolicy policy)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, LockException, VersionException, RepositoryException;
}
