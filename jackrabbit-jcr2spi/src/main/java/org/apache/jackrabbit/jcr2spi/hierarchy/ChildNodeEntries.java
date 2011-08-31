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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>ChildNodeEntries</code> represents a collection of <code>NodeEntry</code>s that
 * also maintains the index values of same-name siblings on insertion and removal.
 */
public interface ChildNodeEntries {

    /**
     * @return <code>true</code> if this <code>ChildNodeEntries</code> have
     * been updated or completely loaded without being invalidated in the
     * mean time.
     */
    boolean isComplete();

    /**
     * Reloads this <code>ChildNodeEntries</code> object.
     *
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    void reload() throws ItemNotFoundException, RepositoryException;

    /**
     * Returns an unmodifiable iterator over all NodeEntry objects present in
     * this ChildNodeEntries collection irrespective of their status.
     *
     * @return Iterator over all NodeEntry object
     */
    Iterator<NodeEntry> iterator();

    /**
     * Returns a <code>List</code> of <code>NodeEntry</code>s for the
     * given <code>nodeName</code>. This method does <b>not</b> filter out
     * removed <code>NodeEntry</code>s.
     *
     * @param nodeName the child node name.
     * @return same name sibling nodes with the given <code>nodeName</code>.
     */
    List<NodeEntry> get(Name nodeName);

    /**
     * Returns the <code>NodeEntry</code> with the given
     * <code>nodeName</code> and <code>index</code>. Note, that this method
     * does <b>not</b> filter out removed <code>NodeEntry</code>s.
     *
     * @param nodeName name of the child node entry.
     * @param index    the index of the child node entry.
     * @return the <code>NodeEntry</code> or <code>null</code> if there
     * is no such <code>NodeEntry</code>.
     */
    NodeEntry get(Name nodeName, int index);

    /**
     * Return the <code>NodeEntry</code> that matches the given nodeName and
     * uniqueID or <code>null</code> if no matching entry can be found.
     *
     * @param nodeName
     * @param uniqueID
     * @return
     * @throws IllegalArgumentException if the given uniqueID is null.
     */
    NodeEntry get(Name nodeName, String uniqueID);

    /**
     * Adds a <code>NodeEntry</code> to the end of the list. Same as
     * {@link #add(NodeEntry, int)}, where the index is {@link Path#INDEX_UNDEFINED}.
     *
     * @param cne the <code>NodeEntry</code> to add.
     */
    void add(NodeEntry cne);

    /**
     * Adds a <code>NodeEntry</code>.<br>
     * Note the following special cases:
     * <ol>
     * <li>If an entry with the given index already exists, the the new sibling
     * is inserted before.</li>
     * <li>If the given index is bigger that the last entry in the siblings list,
     * intermediate entries will be created.</li>
     * </ol>
     *
     * @param cne the <code>NodeEntry</code> to add.
     */
    void add(NodeEntry cne, int index);

    /**
     * Adds a the new  <code>NodeEntry</code> before <code>beforeEntry</code>.
     *
     * @param entry
     * @param index
     * @param beforeEntry
     */
    void add(NodeEntry entry, int index, NodeEntry beforeEntry);

    /**
     * Removes the child node entry referring to the node state.
     *
     * @param childEntry the entry to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     */
    NodeEntry remove(NodeEntry childEntry);

    /**
     * Reorders an existing <code>NodeEntry</code> before another
     * <code>NodeEntry</code>. If <code>beforeEntry</code> is
     * <code>null</code> <code>insertEntry</code> is moved to the end of the
     * child node entries.
     *
     * @param insertEntry the NodeEntry to move.
     * @param beforeEntry the NodeEntry where <code>insertEntry</code> is
     * reordered to.
     * @return the NodeEntry that followed the 'insertEntry' before the reordering.
     * @throws NoSuchElementException if <code>insertEntry</code> or
     * <code>beforeEntry</code> does not have a <code>NodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     */
    NodeEntry reorder(NodeEntry insertEntry, NodeEntry beforeEntry);

    /**
     * Reorders an existing <code>NodeEntry</code> after another
     * <code>NodeEntry</code>. If <code>afterEntry</code> is
     * <code>null</code> <code>insertEntry</code> is moved to the beginning of
     * the child node entries.
     *
     * @param insertEntry the NodeEntry to move.
     * @param afterEntry the NodeEntry where <code>insertEntry</code> is
     * reordered behind.
     * @throws NoSuchElementException if <code>insertEntry</code> or
     * <code>afterEntry</code> does not have a <code>NodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     */
    void reorderAfter(NodeEntry insertEntry, NodeEntry afterEntry);
}
