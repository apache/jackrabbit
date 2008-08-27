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

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

/**
 * The <code>RetentionManager</code> object is accessed via
 * {@link javax.jcr.Session#getRetentionManager()}.
 *
 * @since JCR 2.0
 */
public interface RetentionManager {

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
     * @throws RepositoryException   if another error occurs.
     */
    public Hold[] getHolds(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException;

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
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
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
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
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
            throws PathNotFoundException, AccessDeniedException,
            LockException, VersionException, RepositoryException;

    /**
     * Removes the specified <code>hold</code> from the node at
     * <code>absPath</code>. The removal does not take effect until a
     * <code>save</code> is performed.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
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
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
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
            throws PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException;

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
     * @throws RepositoryException   if another error occurs.
     */
    public RetentionPolicy getRetentionPolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

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
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
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
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
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
            throws PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException;

    /**
     * Causes the current retention policy on the node at
     * <code>absPath</code> to no longer apply. The removal does not take effect
     * until a <code>save</code> is performed.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if no node at
     * <code>absPath</code> exists or the session does not have privilege to
     * retrieve the node.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session
     * does not have sufficient rights to perform the operation.
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
     * @throws AccessDeniedException  if the current session does not have
     *                                sufficient rights to perform the operation.
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
            throws PathNotFoundException, AccessDeniedException,
            LockException, VersionException, RepositoryException;
}
