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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>HierarchyEntry</code>...
 */
public interface HierarchyEntry {

    /**
     * True if this <code>HierarchyEntry</code> would resolve to a <code>NodeState</code>.
     *
     * @return
     */
    public boolean denotesNode();

    /**
     * @return the name of this hierarchy entry.
     */
    public Name getName();

    /**
     * @return the path of this hierarchy entry.
     */
    public Path getPath() throws RepositoryException;

    /**
     * @return If this entry has not been modified this method returns the same
     * as {@link #getPath()}. In case of moved items this method return the
     * original path as it is present on the persistent layer.
     */
    public Path getWorkspacePath() throws RepositoryException;

    /**
     * Returns the <code>NodeEntry</code> being parent to this
     * <code>HierarchyEntry</code>.
     *
     * @return the parent <code>HierarchyEntry</code>
     */
    public NodeEntry getParent();

    /**
     * If this <code>HierarchyEntry</code> provides an underlying
     * <code>ItemState</code> this method returns the status of that state,
     * otherwise it returns {@link Status#_UNDEFINED_}.
     *
     * @return Status of the ItemState or {@link Status#_UNDEFINED_} if this
     * entry has not been resolved yet.
     * @see ItemState#getStatus()
     */
    public int getStatus();

    /**
     * Returns <code>true</code> if the referenced <code>ItemState</code> is
     * available. That is, the referenced <code>ItemState</code> has already
     * been resolved.<br>
     * Note, that the validity of the ItemState is not checked.
     *
     * @return <code>true</code> if the <code>ItemState</code> is available;
     * otherwise <code>false</code>.
     * @see #getItemState()
     */
    public boolean isAvailable();

    /**
     * If this <code>HierarchyEntry</code> has already been resolved before
     * (see {@link #isAvailable()}), that <code>ItemState</code> is returned.
     * Note however, that the validity of the State is not asserted.<br>
     * If the entry has not been resolved yet an attempt is made to resolve this
     * entry, which may fail if there exists no accessible <code>ItemState</code>
     * or if the corresponding state has been removed in the mean time.
     *
     * @return the referenced <code>ItemState</code>.
     * @throws ItemNotFoundException if the <code>ItemState</code> does not
     * exist anymore.
     * @throws RepositoryException If an error occurs while retrieving the
     * <code>ItemState</code>.
     */
    public ItemState getItemState() throws ItemNotFoundException, RepositoryException;

    /**
     * Set the ItemState this hierarchyEntry will be resolved to.
     *
     * @param state
     */
    public void setItemState(ItemState state);

    /**
     * Invalidates the underlying <code>ItemState</code> if available and if it
     * is not transiently modified. If the <code>recursive</code> flag is true,
     * also invalidates the child entries recursively.<br>
     * Note, that in contrast to {@link HierarchyEntry#reload(boolean)}
     * this method only sets the status of this item state to {@link
     * Status#INVALIDATED} and does not actually update it with the persistent
     * state in the repository.
     */
    public void invalidate(boolean recursive);

    /**
     * Calculates the status of the underlying <code>ItemState</code>: any pending
     * changes to the underlying <code>ItemState</code> are applied.
     */
    public void calculateStatus();

    /**
     * Traverses the hierarchy and reverts all transient modifications such as
     * adding, modifying or removing item states. 'Existing' item states
     * are reverted to their initial state and their status is reset to {@link Status#EXISTING}.
     *
     * @throws RepositoryException if an error occurs.
     */
    public void revert() throws RepositoryException;

    /**
     * Reloads this hierarchy entry and the corresponding ItemState, if this
     * entry has already been resolved. If '<code>recursive</code>' the complete
     * hierarchy below this entry is reloaded as well.
     *
     * @param recursive
     */
    public void reload(boolean recursive);

    /**
     * Traverses the hierarchy and marks all available item states as transiently
     * removed. They will change their status to either {@link Status#EXISTING_REMOVED} if
     * the item is existing in the persistent storage or {@link Status#REMOVED}
     * if the item has been transiently added before. In the latter case, the
     * corresponding HierarchyEntries can be removed as well from their parent.
     *
     * @throws InvalidItemStateException if this entry has been removed in the
     * mean time.
     * @throws RepositoryException if an error occurs while removing any of the item
     * states e.g. an item state is not valid anymore.
     */
    public void transientRemove() throws InvalidItemStateException, RepositoryException;

    /**
     * Removes this <code>HierarchyEntry</code> from its parent and sets the
     * status of the underlying ItemState to {@link Status#REMOVED} or to
     * {@link Status#STALE_DESTROYED}, respectively. If this entry is a
     * NodeEntry all descending ItemStates must get their status changed as well.
     */
    public void remove();

    /**
     * Clean up this entry upon {@link Operation#undo()} or {@link Operation#persisted()}.
     *
     * @param transientOperation
     */
    public void complete(Operation transientOperation) throws RepositoryException;

    /**
     * The required generation of this <code>HierarchyEntry</code> . This is used by the
     * {@link ItemInfoCache} to determine whether an item info in the cache is up to date or not.
     * That is whether the generation of the item info in the cache is the same or more recent
     * as the required generation of this entry.
     */
    public long getGeneration();
}
