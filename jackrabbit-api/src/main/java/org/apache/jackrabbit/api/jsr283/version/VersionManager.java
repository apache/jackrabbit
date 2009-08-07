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
package org.apache.jackrabbit.api.jsr283.version;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.AccessDeniedException;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;

/**
 * The <code>VersionManager</code> object is accessed via
 * {@link org.apache.jackrabbit.api.jsr283.Workspace#getVersionManager()}.
 * It provides methods for:
 * <ul>
 * <li>Version graph functionality (version history, base version, successors
 * predecessors)</li>
 * <li>Basic version operations (checkin, checkout, checkpoint)</li>
 * <li>Restore feature</li>
 * <li>Label feature</li>
 * <li>Merge feature</li>
 * <li>Configuration feature</li>
 * <li>Activity feature</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface VersionManager {

    /**
     * Creates for the versionable node at <code>absPath</code> a new version
     * with a system generated version name and returns that version (which will
     * be the new base version of this node). Sets the <code>jcr:checkedOut</code>
     * property to false thus putting the node into the <i>checked-in</i> state.
     * This means that the node and its <i>connected non-versionable subtree</i>
     * become read-only. A node's connected non-versionable subtree is the set
     * of non-versionable descendant nodes reachable from that node through
     * child links without encountering any versionable nodes. In other words,
     * the read-only status flows down from the checked-in node along every
     * child link until either a versionable node is encountered or an item with
     * no children is encountered. In a system that supports only simple
     * versioning the connected non-versionable subtree will be equivalent to
     * the whole subtree, since simple-versionable nodes cannot have
     * simple-versionable descendants.
     * <p>
     * Read-only status means that an item cannot be altered by the client using
     * standard API methods (<code>addNode</code>, <code>setProperty</code>,
     * etc.). The only exceptions to this rule are the {@link #restore} (all
     * signatures), {@link #restoreByLabel}, {@link #restore}, {@link #merge}
     * and {@link Node#update} operations; these do not respect read-only status
     * due to check-in. Note that <code>remove</code> of a read-only node is
     * possible, as long as its parent is not read-only (since removal is an
     * alteration of the parent node).
     * <p>
     * If the node is already checked-in, this method has no effect but returns
     * the current base version of the node at <code>absPath</code>.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p>
     * A <code>VersionException</code> is thrown or if a child item of this node
     * has an <code>OnParentVersion</code> status of <code>ABORT</code>. This
     * includes the case where an unresolved merge failure exists on this node,
     * as indicated by the presence of the <code>jcr:mergeFailed</code>
     * property.
     * <p>
     * If there are unsaved changes pending on the node at <code>absPath</code>,
     * an <code>InvalidItemStateException</code> is thrown.
     * <p>
     * Throws a <code>LockException</code> if a lock prevents the operation.
     * <p>
     * If <code>checkin</code> succeeds, the change to the
     * <code>jcr:isCheckedOut</code> property is automatically persisted (there
     * is no need to do an additional <code>save</code>).
     * <p>
     *
     * @param absPath an absolute path.
     * @return the created version.
     * @throws VersionException          if <code>jcr:predecessors</code> does
     *                                   not contain at least one value or if a
     *                                   child item of the node at <code>absPath</code>
     *                                   has an <code>OnParentVersion</code>
     *                                   status of <code>ABORT</code>. This
     *                                   includes the case where an unresolved
     *                                   merge failure exists on the node, as
     *                                   indicated by the presence of a
     *                                   <code>jcr:mergeFailed</code> property.
     * @throws UnsupportedRepositoryOperationException
     *                                   If the node at <code>absPath</code>
     *                                   node is not versionable.
     * @throws InvalidItemStateException If unsaved changes exist on the node at
     *                                   <code>absPath</code>.
     * @throws LockException             if a lock prevents the operation.
     * @throws RepositoryException       If another error occurs.
     */
    public Version checkin(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * Sets the versionable node at <code>absPath</code> to checked-out status
     * by setting its <code>jcr:isCheckedOut</code> property to
     * <code>true</code>. Under full versioning it also sets the
     * <code>jcr:predecessors</code> property to be a reference to the current
     * base version (the same value as held in <code>jcr:baseVersion</code>).
     * <p>
     * This method puts the node into the <i>checked-out</i> state, making it
     * and its connected non-versionable subtree no longer read-only (see {@link
     * #checkin(String)} for an explanation of the term "connected
     * non-versionable subtree". Under simple versioning this will simply be the
     * whole subtree).
     * <p>
     * If successful, these changes are persisted immediately, there is no need
     * to call <code>save</code>.
     * <p>
     * If the node at <code>absPath</code> is already checked-out, this method
     * has no effect. </p> If the node at <code>absPath</code> is not
     * versionable, an <code>UnsupportedRepositoryOperationException</code> is
     * thrown.
     * <p>
     * Throws a <code>LockException</code> if a lock prevents the checkout.
     *
     * @param absPath an absolute path.
     * @throws UnsupportedRepositoryOperationException
     *                             If the node at <code>absPath</code> is not
     *                             versionable.
     * @throws LockException       if a lock prevents the checkout.
     * @throws RepositoryException If another error occurs.
     */
    public void checkout(String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Performs a <code>checkin()</code> followed by a <code>checkout()</code>
     * on the versionable node at <code>absPath</code>.
     * <p>
     * If the node is already checked-in, this method is equivalent to
     * <code>checkout()</code>.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p>
     * A <code>VersionException</code> is thrown if a child item of the node at
     * <code>absPath</code> has an <code>OnParentVersion</code> of
     * <code>ABORT</code>. This includes the case where an unresolved merge
     * failure exists on the node, as indicated by the presence of the
     * <code>jcr:mergeFailed</code> property.
     * <p>
     * If there are unsaved changes pending on the node at <code>absPath</code>,
     * an <code>InvalidItemStateException</code> is thrown.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the operation.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @return the created version.
     * @throws javax.jcr.version.VersionException          if a child item of the node at
     *                                   <code>absPath</code> has an
     *                                   <code>OnParentVersion</code> of
     *                                   <code>ABORT</code>. This includes the
     *                                   case where an unresolved merge failure
     *                                   exists on the node, as indicated by the
     *                                   presence of the <code>jcr:mergeFailed</code>.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws InvalidItemStateException if there are unsaved changes pending on
     *                                   the node at <code>absPath</code>.
     * @throws LockException             if a lock prevents the operation.
     * @throws RepositoryException       if another error occurs.
     * @since JCR 2.0
     */
    public Version checkpoint(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * Returns <code>true</code> if the node at <code>absPath</code> is either
     * <ul>
     * <li>versionable (full or simple) and currently checked-out,</li>
     * <li>non-versionable and its nearest versionable ancestor is checked-out or</li>
     * <li>non-versionable and it has no versionable ancestor.</li>
     * </ul>
     * <p>
     * Returns
     * <code>false</code> if the node at <code>absPath</code> is either
     * <ul>
     * <li>versionable (full or simple) and currently checked-in or</li>
     * <li>non-versionable and its nearest versionable ancestor is
     * checked-in.</li>
     * </ul>
     *
     * @param absPath an absolute path.
     * @return a boolean
     * @throws RepositoryException If another error occurs.
     */
    public boolean isCheckedOut(String absPath) throws RepositoryException;

    /**
     * Returns the <code>VersionHistory</code> object of the node at
     * <code>absPath</code>. This object provides access to the
     * <code>nt:versionHistory</code> node holding the node's versions.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     *
     * @param absPath an absolute path.
     * @return a <code>VersionHistory</code> object
     * @throws UnsupportedRepositoryOperationException
     *                             if the node at <code>absPath</code> is not
     *                             versionable.
     * @throws RepositoryException If another error occurs.
     */
    public VersionHistory getVersionHistory(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the current base version of the versionable node at
     * <code>absPath</code>.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     *
     * @param absPath an absolute path.
     * @return a <code>Version</code> object.
     * @throws UnsupportedRepositoryOperationException
     *                             if the node at <code>absPath</code> is not
     *                             versionable.
     * @throws RepositoryException If another error occurs.
     */
    public Version getBaseVersion(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Restores a set of versions at once. Used in cases where a "chicken and
     * egg" problem of mutually referring <code>REFERENCE</code> properties
     * would prevent the restore in any serial order.
     * <p>
     * If the restore succeeds the changes made to the corresponding node are
     * persisted immediately, there is no need to call <code>save</code>.
     * <p>
     * The following restrictions apply to the set of versions specified:
     * <p>
     * If <code>S</code> is the set of versions being restored simultaneously,
     * <ul>
     * <li> For every version <code>V</code> in <code>S</code> that
     * corresponds to a <i>missing</i> node, there must also be a parent of V in
     * S.</li>
     * <li><code>S</code> must contain at least one version that
     * corresponds to an existing node in the workspace.</li>
     * <li> No <code>V</code> in <code>S</code> can be a root version
     * (<code>jcr:rootVersion</code>).</li>
     * </ul>
     * <p>
     * If any of these restrictions does not hold, the restore will fail because
     * the system will be unable to determine the path locations to which one or
     * more versions are to be restored. In this case a <code>VersionException</code>
     * is thrown.
     * <p>
     * The versionable nodes in the current workspace that correspond to the
     * versions being restored define a set of (one or more) subtrees. An
     * identifier collision occurs when the current workspace contains a node
     * <i>outside these subtrees</i> that has the same identifier as one of the
     * nodes that would be introduced by the <code>restore</code> operation
     * <i>into one of these subtrees</i>. The result in such a case is governed
     * by the <code>removeExisting</code> flag. If <code>removeExisting</code>
     * is <code>true</code> then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed. If <code>removeExisting</code>
     * is <code>false</code> then a <code>ItemExistsException</code> is thrown
     * and no changes are made. Note that this applies not only to cases where
     * the restored node itself conflicts with an existing node but also to
     * cases where a conflict occurs with any node that would be introduced into
     * the workspace by the restore operation. In particular, conflicts
     * involving subnodes of the restored node that have
     * <code>OnParentVersion</code> settings of <code>COPY</code> or
     * <code>VERSION</code> are also governed by the <code>removeExisting</code>
     * flag.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if one
     * or more of the nodes to be restored is not versionable.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this
     * <code>Session</code> has pending unsaved changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the restore.
     *
     * @param versions       The set of versions to be restored.
     * @param removeExisting governs what happens on identifier collision.
     * @throws ItemExistsException       if <code>removeExisting</code> is
     *                                   <code>false</code> and an identifier
     *                                   collision occurs with a node being
     *                                   restored.
     * @throws UnsupportedRepositoryOperationException
     *                                   if one or more of the nodes to be
     *                                   restored is not versionable.
     * @throws VersionException          if the set of versions to be restored
     *                                   is such that the original path location
     *                                   of one or more of the versions cannot
     *                                   be determined or if the <code>restore</code>
     *                                   would change the state of a existing
     *                                   versionable node that is currently
     *                                   checked-in or if a root version
     *                                   (<code>jcr:rootVersion</code>) is among
     *                                   those being restored.
     * @throws LockException             if a lock prevents the restore.
     * @throws InvalidItemStateException if this <code>Session</code> has
     *                                   pending unsaved changes.
     * @throws RepositoryException       if another error occurs.
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Restores the node at <code>absPath</code> to the state defined by the
     * version with the specified <code>versionName</code>.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p>
     * If successful, the change is persisted immediately and there is no need
     * to call <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if no version with the
     * specified <code>versionName</code> exists in the node's version history
     * or if an attempt is made to restore the root version
     * (<code>jcr:rootVersion</code>).
     * <p>
     * An InvalidItemStateException is thrown if this <code>Session</code> (not
     * necessarily this <code>Node</code>) has pending unsaved changes.
     * <p>
     * A LockException is thrown if a lock prevents the addition of the mixin.
     * <p>
     * This method will work regardless of whether the node at
     * <code>absPath</code> is checked-in or not.
     * <p>
     * An identifier collision occurs when a node exists <i>outside the subtree
     * rooted at this node</i> with the same identifier as a node that would be
     * introduced by the <code>restore</code> operation <i>into the subtree at
     * this node</i>. The result in such a case is governed by the
     * <code>removeExisting</code> flag. If <code>removeExisting</code> is
     * <code>true</code>, then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed (if possible; otherwise a
     * <code>RepositoryException</code> is thrown). If <code>removeExisting</code>
     * is <code>false</code>, then a <code>ItemExistsException</code> is thrown
     * and no changes are made. Note that this applies not only to cases where
     * the restored node itself conflicts with an existing node but also to
     * cases where a conflict occurs with any node that would be introduced into
     * the workspace by the restore operation. In particular, conflicts
     * involving subnodes of the restored node that have
     * <code>OnParentVersion</code> settings of <code>COPY</code> or
     * <code>VERSION</code> are also governed by the <code>removeExisting</code>
     * flag.
     *
     * @param absPath        an absolute path.
     * @param versionName    a <code>Version</code> object
     * @param removeExisting a boolean flag that governs what happens in case of
     *                       an identifier collision.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws VersionException          if the specified <code>version</code>
     *                                   is not part of this node's version
     *                                   history or if an attempt is made to
     *                                   restore the root version (<code>jcr:rootVersion</code>).
     * @throws ItemExistsException       if <code>removeExisting</code> is
     *                                   <code>false</code> and an identifier
     *                                   collision occurs.
     * @throws LockException             if a lock prevents the restore.
     * @throws InvalidItemStateException if this <code>Session</code> (not
     *                                   necessarily the <code>Node</code> at
     *                                   <code>absPath</code>) has pending
     *                                   unsaved changes.
     * @throws RepositoryException       If another error occurs.
     */
    public void restore(String absPath, String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Restores the specified <code>version</code> and the changes the
     * corresponding node to reflect the state defined by the specified
     * <code>version</code>.
     * <p>
     * If the node at <code>absPath</code> is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p>
     * If successful, the change is persisted immediately and there is no need
     * to call <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the specified
     * <code>version</code> is not part of this node's version history or if an
     * attempt is made to restore the root version (<code>jcr:rootVersion</code>).
     * <p>
     * An InvalidItemStateException is thrown if this <code>Session</code> (not
     * necessarily the <code>Node</code> at <code>absPath</code>) has pending
     * unsaved changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the restore.
     * <p>
     * This method will work regardless of whether the node at
     * <code>absPath</code> is checked-in or not.
     * <p>
     * An identifier collision occurs when a node exists <i>outside the subtree
     * rooted at this node</i> with the same identifier as a node that would be
     * introduced by the <code>restore</code> operation <i>into the subtree at
     * this node</i>. The result in such a case is governed by the
     * <code>removeExisting</code> flag. If <code>removeExisting</code> is
     * <code>true</code>, then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed (if possible; otherwise a
     * <code>RepositoryException</code> is thrown). If <code>removeExisting</code>
     * is <code>false</code>, then a <code>ItemExistsException</code> is thrown
     * and no changes are made. Note that this applies not only to cases where
     * the restored node itself conflicts with an existing node but also to
     * cases where a conflict occurs with any node that would be introduced into
     * the workspace by the restore operation. In particular, conflicts
     * involving subnodes of the restored node that have
     * <code>OnParentVersion</code> settings of <code>COPY</code> or
     * <code>VERSION</code> are also governed by the <code>removeExisting</code>
     * flag.
     *
     * @param version        a <code>Version</code> object
     * @param removeExisting a boolean flag that governs what happens in case of
     *                       an identifier collision.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws VersionException          if the specified <code>version</code>
     *                                   does not have a corresponding node in
     *                                   the workspace <code>this</code>
     *                                   VersionManager has been created for or
     *                                   if an attempt is made to restore the
     *                                   root version (<code>jcr:rootVersion</code>).
     * @throws ItemExistsException       if <code>removeExisting</code> is
     *                                   <code>false</code> and an identifier
     *                                   collision occurs.
     * @throws InvalidItemStateException if this <code>Session</code> (not
     *                                   necessarily the <code>Node</code> at
     *                                   <code>absPath</code>) has pending
     *                                   unsaved changes.
     * @throws LockException             if a lock prevents the restore.
     * @throws RepositoryException       if another error occurs.
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Restores the specified version to <code>absPath</code>.
     * <p>
     * A node need not exist at <code>absPath</code>, though the parent of
     * <code>absPath</code> must exist, otherwise a <code>PathNotFoundException</code>
     * is thrown.
     * <p>
     * If a node <i>does</i> exist at absPath then it must correspond to the
     * version being restored (the version must be a version <i>of that
     * node</i>) and must not be a root version (<code>jcr:rootVersion</code>),
     * otherwise a <code>VersionException</code> is thrown.
     * <p>
     * If no node exists at <code>absPath</code> then a <code>VersionException</code>
     * is thrown if the parent node of <code>absPath</code> is versionable and
     * checked-in or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * <p>
     * If there <i>is</i> a node at <code>absPath</code> then the checked-in
     * status of that node itself and the checked-in status of its parent are
     * irrelevant. The restore will work even if one or both are checked-in.
     * <p>
     * An identifier collision occurs when a node exists <i>outside the subtree
     * rooted at <code>absPath</code></i> with the same identifier as a node
     * that would be introduced by the <code>restore</code> operation <i>into
     * the subtree at <code>absPath</code></i>. The result in such a case is
     * governed by the <code>removeExisting</code> flag. If
     * <code>removeExisting</code> is <code>true</code>, then the incoming node
     * takes precedence, and the existing node (and its subtree) is removed (if
     * possible; otherwise a <code>RepositoryException</code> is thrown). If
     * <code>removeExisting</code> is <code>false</code>, then a
     * <code>ItemExistsException</code> is thrown and no changes are made. Note
     * that this applies not only to cases where the restored node itself
     * conflicts with an existing node but also to cases where a conflict occurs
     * with any node that would be introduced into the workspace by the restore
     * operation. In particular, conflicts involving subnodes of the restored
     * node that have <code>OnParentVersion</code> settings of <code>COPY</code>
     * or <code>VERSION</code> are also governed by the <code>removeExisting</code>
     * flag.
     * <p>
     * If the would-be parent of the location <code>absPath</code> is actually a
     * property, or if a node type restriction would be violated, then a
     * <code>ConstraintViolationException</code> is thrown.
     * <p>
     * If the <code>restore</code> succeeds, the changes made to this node are
     * persisted immediately, there is no need to call <code>save</code>.
     * <p>
     * An InvalidItemStateException is thrown if this <code>Session</code> (not
     * necessarily this <code>Node</code>) has pending unsaved changes.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * versioning is not supported.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the restore.
     *
     * @param absPath        an absolute the path to which the version is to be
     *                       restored.
     * @param version        a version object
     * @param removeExisting covers what happens on identifier collision.
     * @throws PathNotFoundException        if the parent of <code>absPath</code>
     *                                      does not exist.
     * @throws ItemExistsException          if removeExisting is false and an
     *                                      identifier collision occurs
     * @throws ConstraintViolationException If the would-be parent of the
     *                                      location <code>absPath</code> is
     *                                      actually a property, or if a node
     *                                      type restriction would be violated
     * @throws VersionException             if the parent node of <code>absPath</code>
     *                                      is versionable and checked-in or is
     *                                      non-versionable but its nearest
     *                                      versionable ancestor is checked-in
     *                                      or if a node exists at absPath that
     *                                      is not the node corresponding to the
     *                                      specified <code>version</code> or if
     *                                      an attempt is made to restore the
     *                                      root version (<code>jcr:rootVersion</code>).
     * @throws UnsupportedRepositoryOperationException
     *                                      if versioning is not supported.
     * @throws LockException                if a lock prevents the restore.
     * @throws InvalidItemStateException    if this <code>Session</code> (not
     *                                      necessarily the <code>Node</code> at
     *                                      <code>absPath</code>) has pending
     *                                      unsaved changes.
     * @throws RepositoryException          if another error occurs
     */
    public void restore(String absPath, Version version, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Restores the version of the node at <code>absPath</code> with the
     * specified version label. If this node is not versionable, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown. If
     * successful, the change is persisted immediately and there is no need to
     * call <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the specified
     * <code>versionLabel</code> does not exist in this node's version history.
     * <p>
     * An InvalidItemStateException is thrown if this <code>Session</code> (not
     * necessarily <code>Node</code> at <code>absPath</code>) has pending
     * unsaved changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the restore.
     * <p>
     * This method will work regardless of whether the node at
     * <code>absPath</code> is checked-in or not.
     * <p>
     * An identifier collision occurs when a node exists <i>outside the subtree
     * rooted at this node</i> with the same identifier as a node that would be
     * introduced by the <code>restoreByLabel</code> operation <i>into the
     * subtree at this node</i>. The result in such a case is governed by the
     * <code>removeExisting</code> flag. If <code>removeExisting</code> is
     * <code>true</code>, then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed (if possible; otherwise a
     * <code>RepositoryException</code> is thrown). If <code>removeExisting</code>
     * is <code>false</code>, then a <code>ItemExistsException</code> is thrown
     * and no changes are made. Note that this applies not only to cases where
     * the restored node itself conflicts with an existing node but also to
     * cases where a conflict occurs with any node that would be introduced into
     * the workspace by the restore operation. In particular, conflicts
     * involving subnodes of the restored node that have
     * <code>OnParentVersion</code> settings of <code>COPY</code> or
     * <code>VERSION</code> are also governed by the <code>removeExisting</code>
     * flag.
     *
     * @param absPath        an absolute path.
     * @param versionLabel   a String
     * @param removeExisting a boolean flag that governs what happens in case of
     *                       an identifier collision.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws VersionException          if the specified <code>versionLabel</code>
     *                                   does not exist in this node's version
     *                                   history.
     * @throws ItemExistsException       if <code>removeExisting</code> is
     *                                   <code>false</code> and an identifier
     *                                   collision occurs.
     * @throws LockException             if a lock prevents the restore.
     * @throws InvalidItemStateException if this <code>Session</code> (not
     *                                   necessarily the <code>Node</code> at
     *                                   <code>absPath</code>) has pending
     *                                   unsaved changes.
     * @throws RepositoryException       If another error occurs.
     */
    public void restoreByLabel(String absPath, String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * This method can be thought of as a version-sensitive update.
     * <p>
     * It recursively tests each versionable node in the subtree of the node at
     * <code>absPath</code> against its corresponding node in
     * <code>srcWorkspace</code> with respect to the relation between their
     * respective base versions and either updates the node in question or not,
     * depending on the outcome of the test.
     * <p>
     * A <code>MergeException</code> is thrown if <code>bestEffort</code> is
     * <code>false</code> and a versionable node is encountered whose
     * corresponding node's base version is on a divergent branch from this
     * node's base version.
     * <p>
     * If successful, the changes are persisted immediately, there is no need to
     * call <code>save</code>.
     * <p>
     * This method returns a <code>NodeIterator</code> over all versionable
     * nodes in the subtree that received a merge result of <i>fail</i>. If
     * <code>bestEffort</code> is <code>false</code>, this iterator will be
     * empty (since if <code>merge</code> returns successfully, instead of
     * throwing an exception, it will be because no failures were encountered).
     * If <code>bestEffort</code> is <code>true</code>, this iterator will
     * contain all nodes that received a <i>fail</i> during the course of this
     * <code>merge</code> operation.
     * <p>
     * If the specified <code>srcWorkspace</code> does not exist, a
     * <code>NoSuchWorkspaceException</code> is thrown.
     * <p>
     * If the current session does not have sufficient permissions to perform
     * the operation, then an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this session (not
     * necessarily this <code>Node</code>) has pending unsaved changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the merge.
     *
     * @param absPath      an absolute path.
     * @param srcWorkspace the name of the source workspace.
     * @param bestEffort   a boolean
     * @return iterator over all nodes that received a merge result of "fail" in
     *         the course of this operation.
     * @throws MergeException            if <code>bestEffort</code> is
     *                                   <code>false</code> and a failed merge
     *                                   result is encountered.
     * @throws InvalidItemStateException if this session (not necessarily the
     *                                   node at <code>absPath</code>) has
     *                                   pending unsaved changes.
     * @throws NoSuchWorkspaceException  if the specified <code>srcWorkspace</code>
     *                                   does not exist.
     * @throws AccessDeniedException     if the current session does not have
     *                                   sufficient rights to perform the
     *                                   operation.
     * @throws LockException             if a lock prevents the merge.
     * @throws RepositoryException       if another error occurs.
     */
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * This method can be thought of as a version-sensitive update.
     * <p>
     * If <code>isShallow</code> is <code>true</code>, it tests this versionable
     * node against its corresponding node in <code>srcWorkspace</code> with
     * respect to the relation between their respective base versions and either
     * updates the node in question or not, depending on the outcome of the
     * test.
     * <p>
     * If <code>isShallow</code> is <code>false</code>, it recursively tests
     * each versionable node in the subtree as mentioned above.
     * <p>
     * If <code>isShallow</code> is <code>true</code> and this node is not
     * versionable, then this method returns and no changes are made.
     * <p>
     * A <code>MergeException</code> is thrown if <code>bestEffort</code> is
     * <code>false</code> and a versionable node is encountered whose
     * corresponding node's base version is on a divergent branch from the base
     * version of the node at <code>absPath</code>.
     * <p>
     * If successful, the changes are persisted immediately, there is no need to
     * call <code>save</code>.
     * <p>
     * This method returns a <code>NodeIterator</code> over all versionable
     * nodes in the subtree that received a merge result of <i>fail</i>. If
     * <code>bestEffort</code> is <code>false</code>, this iterator will be
     * empty (since if <code>merge</code> returns successfully, instead of
     * throwing an exception, it will be because no failures were encountered).
     * If <code>bestEffort</code> is <code>true</code>, this iterator will
     * contain all nodes that received a <i>fail</i> during the course of this
     * <code>merge</code> operation.
     * <p>
     * If the specified <code>srcWorkspace</code> does not exist, a
     * <code>NoSuchWorkspaceException</code> is thrown.
     * <p>
     * If the current session does not have sufficient permissions to perform
     * the operation, then an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this session (not
     * necessarily the node at <code>absPath</code>) has pending unsaved
     * changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the merge.
     *
     * @param absPath      an absolute path.
     * @param srcWorkspace the name of the source workspace.
     * @param bestEffort   a boolean
     * @param isShallow    a boolean
     * @return iterator over all nodes that received a merge result of "fail" in
     *         the course of this operation.
     * @throws MergeException            if <code>bestEffort</code> is
     *                                   <code>false</code> and a failed merge
     *                                   result is encountered.
     * @throws InvalidItemStateException if this session (not necessarily this
     *                                   node) has pending unsaved changes.
     * @throws NoSuchWorkspaceException  if <code>srcWorkspace</code> does not
     *                                   exist.
     * @throws AccessDeniedException     if the current session does not have
     *                                   sufficient rights to perform the
     *                                   operation.
     * @throws LockException             if a lock prevents the merge.
     * @throws RepositoryException       if another error occurs.
     * @since JCR 2.0
     */
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Completes the merge process with respect to the node at
     * <code>absPath</code> and the specified <code>version</code>.
     * <p>
     * When the {@link #merge} method is called on a node, every versionable
     * node in that subtree is compared with its corresponding node in the
     * indicated other workspace and a "merge test result" is determined
     * indicating one of the following:
     * <p>
     * <ol>
     * <li> This node will be updated to
     * the state of its correspondee (if the base version of the correspondee is
     * more recent in terms of version history) </li>
     * <li> This node will be
     * left alone (if this node's base version is more recent in terms of
     * version history). </li>
     * <li> This node will be marked as having failed
     * the merge test (if this node's base version is on a different branch of
     * the version history from the base version of its corresponding node in
     * the other workspace, thus preventing an automatic determination of which
     * is more recent). </li>
     * </ol>
     * <p>
     * (See {@link #merge} for more details)
     * <p>
     * In the last case the merge of the non-versionable subtree (the "content")
     * of this node must be done by the application (for example, by providing a
     * merge tool for the user).
     * <p>
     * Additionally, once the content of the nodes has been merged, their
     * version graph branches must also be merged. The JCR versioning system
     * provides for this by keeping a record, for each versionable node that
     * fails the merge test, of the base version of the corresponding node that
     * caused the merge failure. This record is kept in the
     * <code>jcr:mergeFailed</code> property of this node. After a
     * <code>merge</code>, this property will contain one or more (if multiple
     * merges have been performed) <code>REFERENCE</code>s that point to the
     * "offending versions".
     * <p>
     * To complete the merge process, the client calls <code>doneMerge(Version
     * v)</code> passing the version object referred to be the
     * <code>jcr:mergeFailed</code> property that the client wishes to connect
     * to <code>this</code> node in the version graph. This has the effect of
     * moving the reference to the indicated version from the
     * <code>jcr:mergeFailed</code> property of <code>this</code> node to the
     * <code>jcr:predecessors</code>.
     * <p>
     * If the client chooses not to connect this node to a particular version
     * referenced in the <code>jcr:mergeFailed</code> property, he calls {@link
     * #cancelMerge(String, Version)}. This has the effect of removing the
     * reference to the specified <code>version</code> from
     * <code>jcr:mergeFailed</code> <i>without</i> adding it to
     * <code>jcr:predecessors</code>.
     * <p>
     * Once the last reference in <code>jcr:mergeFailed</code> has been either
     * moved to <code>jcr:predecessors</code> (with <code>doneMerge</code>) or
     * just removed from <code>jcr:mergeFailed</code> (with
     * <code>cancelMerge</code>) the <code>jcr:mergeFailed</code> property is
     * automatically removed, thus enabling <code>this</code> node to be
     * checked-in, creating a new version (note that before the
     * <code>jcr:mergeFailed</code> is removed, its <code>OnParentVersion</code>
     * setting of <code>ABORT</code> prevents checkin). This new version will
     * have a predecessor connection to each version for which
     * <code>doneMerge</code> was called, thus joining those branches of the
     * version graph.
     * <p>
     * If successful, these changes are persisted immediately, there is no need
     * to call <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the <code>version</code>
     * specified is not among those referenced in this node's
     * <code>jcr:mergeFailed</code> property.
     * <p>
     * If there are unsaved changes pending on this node, an
     * <code>InvalidItemStateException</code> is thrown.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if this
     * node is not versionable.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param version a version referred to by the <code>jcr:mergeFailed</code>
     *                property of the node at <code>absPath</code>.
     * @throws VersionException          if the version specified is not among
     *                                   those referenced in this node's
     *                                   <code>jcr:mergeFailed</code> or if the
     *                                   node is currently checked-in.
     * @throws InvalidItemStateException if there are unsaved changes pending on
     *                                   the node at <code>absPath</code>.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws RepositoryException       if another error occurs.
     */
    public void doneMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Cancels the merge process with respect to the node at
     * <code>absPath</code> and the specified <code>version</code>.
     * <p>
     * See {@link #doneMerge} for a full explanation. Also see {@link #merge}
     * for more details.
     * <p>
     * If successful, these changes are persisted immediately, there is no need
     * to call <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the <code>version</code>
     * specified is not among those referenced in the <code>jcr:mergeFailed</code>
     * property of the node at <code>absPath</code> or if the node is currently
     * checked-in.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if the
     * node at <code>absPath</code> is not versionable.
     * <p>
     * If there are unsaved changes pending on the node at <code>absPath</code>,
     * an <code>InvalidItemStateException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath an absolute path.
     * @param version a version referred to by the <code>jcr:mergeFailed</code>
     *                property of the node at <code>absPath</code>.
     * @throws VersionException          if the version specified is not among
     *                                   those referenced in the <code>jcr:mergeFailed</code>
     *                                   property of the node at <code>absPath</code>
     *                                   or if the node is currently
     *                                   checked-in.
     * @throws InvalidItemStateException if there are unsaved changes pending on
     *                                   the node at <code>absPath</code>.
     * @throws UnsupportedRepositoryOperationException
     *                                   if the node at <code>absPath</code> is
     *                                   not versionable.
     * @throws RepositoryException       if another error occurs.
     */
    public void cancelMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Calling <code>createConfiguration</code> on the node <i>N</i> at
     * <code>absPath</code> creates, in the configuration storage, a new
     * <code>nt:configuration</code> node whose root is <i>N</i>. A reference to
     * <i>N</i> is recorded in the <code>jcr:root</code> property of the new
     * configuration, and a reference to the new configuration is recorded in
     * the <code>jcr:configuration</code> property of <i>N</i>.
     * <p>
     * If the specified <code>baseline</code> is <code>null</code>, a new
     * version history is created to store baselines of the new configuration,
     * and the <code>jcr:baseVersion</code> of the new configuration references
     * the root of the new version history. If the specified baseline is not
     * <code>null</code>, the <code>jcr:baseVersion</code> of the new
     * configuration references the specified baseline.
     * <p>
     * The changes are persisted immediately, a <code>save</code> is not
     * required.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * <i>N</i> is not versionable.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath  an absolute path.
     * @param baseline a <code>Version</code>
     * @return a new <code>nt:configuration</code> node
     * @throws UnsupportedRepositoryOperationException
     *                             if <i>N</i> is not versionable.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Node createConfiguration(String absPath, Version baseline) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * This method is called by the client to set the current activity on the
     * session from which this version manager has been obtained. Changing the
     * current activity is done by calling <code>setActivity</code> again.
     * Cancelling the current activity (so that the session has no current
     * activity) is done by calling <code>setActivity(null)</code>. The activity
     * <code>Node</code> is returned.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if the
     * repository does not support activities or if <code>activity</code> is not
     * a <code>nt:activity</code> node.
     *
     * @param activity an activity node
     * @return the activity node
     * @throws UnsupportedRepositoryOperationException
     *                             if the repository does not support activities
     *                             or if <code>activity</code> is not a
     *                             <code>nt:activity</code> node.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Node setActivity(Node activity)
            throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the node representing the current activity or <code>null</code> if there is no current activity.
     *
     * @return An <code>nt:activity</code> node or <code>null</code>.
     * @throws UnsupportedRepositoryOperationException if the repository does not support activities.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Node getActivity()throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * This method creates a new <code>nt:activity</code> at an implementation-determined
     * location in the <code>/jcr:system/jcr:activities</code> subtree.
     * <p>
     * The repository may, but is not required to, use the <code>title</code>
     * as a hint for what to name the new activity node. The new activity
     * <code>Node</code> is returned.
     * <p>
     * The new node is persisted immediately and does not require a <code>save</code>.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if the
     * repository does not support activities.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param title a String
     * @return the new activity <code>Node</code>.
     * @throws UnsupportedRepositoryOperationException if the repository does not support activities.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Node createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * This method removes an <code>nt:activity</code> at an implementation-determined
     * location in the <code>/jcr:system/jcr:activities</code> subtree.
     * <p>
     * <p>
     * The change is persisted immediately and does not require a <code>save</code>.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if the
     * repository does not support activities.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param title a String
     * @return the new activity <code>Node</code>.
     * @throws UnsupportedRepositoryOperationException if the repository does not support activities.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Node removeActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException;


    /**
     * This method merges the changes that were made under the specified
     * activity into the current workspace.
     * <p>
     * An activity <i>A</i> will be associated with a set of versions through the
     * <code>jcr:activity</code> reference of each version node in the set.
     * We call each such associated version a <i>member of A</i>.
     * <p>
     * For each version history <i>H</i> that contains one or more members of
     * <i>A</i>, one such member will be the latest member of <i>A</i> in <i>H</i>.
     * The latest member of <i>A</i> in <i>H</i> is the version in <i>H</i> that
     * is a member of <i>A</i> and that has no successor versions (to any degree)
     * that are also members of <i>A</i>.
     * <p>
     * The set of versions that are the latest members of <i>A</i> in their
     * respective version histories is called the change set of <i>A</i>. It
     * fully describes the changes made under the activity <i>A</i>.
     * <p>
     * This method performs a shallow merge into the current workspace of each version
     * in the change set of the activity specified by <code>activityNode</code>.
     * If there is no corresponding node in this workspace for a given member of
     * the change set, that member is ignored.
     * <p>
     * This method returns a <code>NodeIterator</code> over all versionable
     * nodes in the subtree that received a merge result of <i>fail</i>.
     * <p>
     * A <code>VersionException</code> is thrown if the specified node is not
     * an <code>nt:activity</code> node.
     * <p>
     * A <code>MergeException</code> is thrown in the same cases as in a regular
     * shallow merge (see {@link #merge(String, String, boolean, boolean)}).
     * <p>
     * If the current session does not have sufficient permissions to perform
     * the operation, then an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this <code>Session</code>
     * has pending unsaved changes.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the merge.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * this operation is not supported by this implementation.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param activityNode an <code>nt:activity</code> node
     * @return a <code>NodeIterator</code>
     * @throws AccessDeniedException if the current session does not have sufficient
     * rights to perform the operation.
     * @throws VersionException if the specified node is not an <code>nt:activity</code> node.
     * @throws MergeException in the same cases as in a regular shallow merge
     * (see {@link #merge(String, String, boolean, boolean)}.
     * @throws LockException if a lock prevents the merge.
     * @throws InvalidItemStateException if this <code>Session</code> has pending unsaved changes.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public NodeIterator merge(Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;
}
