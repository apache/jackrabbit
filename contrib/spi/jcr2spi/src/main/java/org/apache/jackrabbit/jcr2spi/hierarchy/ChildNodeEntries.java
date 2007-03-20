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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.commons.collections.list.AbstractLinkedList;
import org.apache.commons.collections.iterators.UnmodifiableIterator;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * <code>ChildNodeEntries</code> represents an insertion-ordered collection of
 * <code>NodeEntry</code>s that also maintains the index values of same-name
 * siblings on insertion and removal.
 * <p/>
 * <code>ChildNodeEntries</code> also provides an unmodifiable
 * <code>Collection</code> view.
 */
final class ChildNodeEntries implements Collection {

    private static Logger log = LoggerFactory.getLogger(ChildNodeEntries.class);

    static final int STATUS_OK = 0;
    static final int STATUS_INVALIDATED = 1;

    private final NodeEntryImpl parent;
    private int status = STATUS_OK;

    /**
     * Linked list of {@link NodeEntry} instances.
     */
    private final ChildNodeEntries.LinkedEntries entries = new LinkedEntries();

    /**
     * map used for lookup by name
     * (key=name, value=either a single {@link AbstractLinkedList.Node} or a
     * list of {@link AbstractLinkedList.Node}s which are sns entries)
     */
    private final Map nameMap = new HashMap();

    /**
     * Create <code>ChildNodeEntries</code> for the given node state.
     *
     * @param parent
     */
    ChildNodeEntries(NodeEntryImpl parent) {
        this.parent = parent;
    }

    /**
     * Mark <code>ChildNodeEntries</code> in order to force
     */
    void setStatus(int status) {
        if (status == STATUS_INVALIDATED || status == STATUS_OK) {
            this.status = status;
        } else {
            throw new IllegalArgumentException();
        }
    }

    int getStatus() {
        return status;
    }

    /**
     * Returns true, if this ChildNodeEntries contains a entry that matches
     * the given name and either index or uniqueID:<br>
     * If <code>uniqueID</code> is not <code>null</code> the given index is
     * ignored since it is not required to identify a child node entry.
     * Otherwise the given index is used.
     *
     * @param name
     * @param index
     * @param uniqueID
     * @return
     */
    boolean contains(QName name, int index, String uniqueID) {
        if (uniqueID == null) {
            return contains(name, index);
        } else {
            return contains(name, uniqueID);
        }
    }

    /**
     *
     * @param name
     * @param index
     * @return
     */
    private boolean contains(QName name, int index) {
        if (!nameMap.containsKey(name) || index < Path.INDEX_DEFAULT) {
            // no matching child node entry
            return false;
        }
        Object o = nameMap.get(name);
        if (o instanceof List) {
            // SNS
            int listIndex = index - 1;
            return listIndex < ((List) o).size();
        } else {
            // single child node with this name -> matches only if request
            // index equals the default-index
            return index == Path.INDEX_DEFAULT;
        }
    }

    /**
     *
     * @param name
     * @param uniqueID
     * @return
     */
    private boolean contains(QName name, String uniqueID) {
        if (uniqueID == null) {
            throw new IllegalArgumentException();
        }
        if (!nameMap.containsKey(name)) {
            // no matching child node entry
            return false;
        }
        Object o = nameMap.get(name);
        if (o instanceof List) {
            // SNS
            for (Iterator it = ((List) o).iterator(); it.hasNext(); ) {
                ChildNodeEntries.LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                NodeEntry cne = n.getNodeEntry();
                if (uniqueID.equals(cne.getUniqueID())) {
                    return true;
                }
            }
        } else {
            // single child node with this name
            NodeEntry cne = ((ChildNodeEntries.LinkedEntries.LinkNode) o).getNodeEntry();
            return uniqueID.equals(cne.getUniqueID());
        }
        // no matching entry found
        return false;
    }

    /**
     * Returns a <code>List</code> of <code>NodeEntry</code>s for the
     * given <code>nodeName</code>. This method does <b>not</b> filter out
     * removed <code>NodeEntry</code>s!
     *
     * @param nodeName the child node name.
     * @return same name sibling nodes with the given <code>nodeName</code>.
     */
    List get(QName nodeName) {
        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return Collections.EMPTY_LIST;
        }
        if (obj instanceof List) {
            final List sns = (List) obj;
            // map entry is a list of siblings
            return Collections.unmodifiableList(new AbstractList() {

                public Object get(int index) {
                    return ((LinkedEntries.LinkNode) sns.get(index)).getNodeEntry();
                }

                public int size() {
                    return sns.size();
                }

                public Iterator iterator() {
                    return new Iterator() {

                        private Iterator iter = sns.iterator();

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }

                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        public Object next() {
                            return ((LinkedEntries.LinkNode) iter.next()).getNodeEntry();
                        }
                    };
                }
            });
        } else {
            // map entry is a single child node entry
            return Collections.singletonList(((LinkedEntries.LinkNode) obj).getNodeEntry());
        }
    }

    /**
     * Returns the <code>NodeEntry</code> with the given
     * <code>nodeName</code> and <code>index</code>. This method ignores
     * <code>NodeEntry</code>s which are marked removed!
     *
     * @param nodeName name of the child node entry.
     * @param index    the index of the child node entry.
     * @return the <code>NodeEntry</code> or <code>null</code> if there
     *         is no such <code>NodeEntry</code>.
     */
    NodeEntry get(QName nodeName, int index) {
        if (index < Path.INDEX_DEFAULT) {
            throw new IllegalArgumentException("index is 1-based");
        }
        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            // map entry is a list of siblings
            List siblings = (List) obj;
            return findMatchingEntry(siblings, index, true);
        } else {
            // map entry is a single child node entry
            if (index == Path.INDEX_DEFAULT) {
                return ((LinkedEntries.LinkNode) obj).getNodeEntry();
            }
        }
        return null;
    }

    /**
     *
     * @param nodeName
     * @param uniqueID
     * @return
     * @throws IllegalArgumentException if the given uniqueID is null.
     */
    NodeEntry get(QName nodeName, String uniqueID) {
        if (uniqueID == null) {
            throw new IllegalArgumentException();
        }
        Iterator cneIter = (nodeName != null) ? get(nodeName).iterator() : entries.iterator();
        while (cneIter.hasNext()) {
            NodeEntry cne = (NodeEntry) cneIter.next();
            if (uniqueID.equals(cne.getUniqueID())) {
                return cne;
            }
        }
        return null;
    }

    /**
     * Find the matching NodeEntry for the given <code>ChildInfo</code>. Returns
     * <code>null</code> if no matching entry can be found. NOTE, that no check
     * for validity of the entries is made.
     *
     * @param childInfo
     * @return
     */
    NodeEntry get(ChildInfo childInfo) {
        String uniqueID = childInfo.getUniqueID();
        if (uniqueID != null) {
            return get(childInfo.getName(), uniqueID);
        } else {
            int index = childInfo.getIndex();
            Object obj = nameMap.get(childInfo.getName());
            if (obj == null) {
                return null;
            } else if (obj instanceof List) {
                // map entry is a list of siblings
                List siblings = (List) obj;
                return findMatchingEntry(siblings, index, false);
            } else if (index == Path.INDEX_DEFAULT) {
                // map entry is a single child node entry
                return ((LinkedEntries.LinkNode) obj).getNodeEntry();
            } // else return 'null'
        }
        return null;
    }

    private static NodeEntry findMatchingEntry(List siblings, int index, boolean checkValidity) {
        // shortcut if index can never match
        if (index > siblings.size()) {
            return null;
        }
        if (!checkValidity) {
            return ((LinkedEntries.LinkNode) siblings.get(index - 1)).getNodeEntry();
        } else {
            // filter out removed states
            for (Iterator it = siblings.iterator(); it.hasNext(); ) {
                NodeEntry cne = ((LinkedEntries.LinkNode) it.next()).getNodeEntry();
                if (cne.isAvailable()) {
                    try {
                        if (cne.getNodeState().isValid()) {
                            index--;
                        } else {
                            // child node removed
                        }
                    } catch (RepositoryException e) {
                        // ignore for index detection. entry does not exist or is
                        // not accessible
                    }
                } else {
                    // then this child node entry has never been accessed
                    // before and is assumed valid // TODO: check if correct.
                    index--;
                }
                if (index == 0) {
                    return cne;
                }
            }
        }
        return null;
    }

    /**
     * Adds a <code>NodeEntry</code> to the end of the list. Same as
     * {@link #add(NodeEntry, int)}, where the index is {@link Path#INDEX_UNDEFINED}.
     *
     * @param cne the <code>NodeEntry</code> to add.
     */
     void add(NodeEntry cne) {
        add(cne, Path.INDEX_UNDEFINED);
    }

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
    void add(NodeEntry cne, int index) {
        QName nodeName = cne.getQName();

        // retrieve ev. sibling node with same index if index is 'undefined'
        // the existing entry is always null and no reordering occur.
        LinkedEntries.LinkNode existing = (index < Path.INDEX_DEFAULT) ? null : getLinkNode(nodeName, index);

        // in case index greater than default -> make sure all intermediate
        // entries exist.
        if (index > Path.INDEX_DEFAULT) {
            int previousIndex = index - 1;
            LinkedEntries.LinkNode previous = getLinkNode(nodeName, previousIndex);
            if (previous == null) {
                // add missing entry (or entries)
                parent.addNodeEntry(nodeName, null, previousIndex);

            } // else: all intermediate entries exist
        } // else: undefined or default index are not affected

        // add new entry (same as #add(NodeEntry)
        List siblings = null;
        Object obj = nameMap.get(nodeName);
        if (obj != null) {
            if (obj instanceof List) {
                // map entry is a list of siblings
                siblings = (ArrayList) obj;
            } else {
                // map entry is a single child node entry,
                // convert to siblings list
                siblings = new ArrayList();
                siblings.add(obj);
                nameMap.put(nodeName, siblings);
            }
        }

        LinkedEntries.LinkNode ln = entries.add(cne);
        if (siblings != null) {
            siblings.add(ln);
        } else {
            nameMap.put(nodeName, ln);
        }

        // reorder the child entries if, the new entry must be inserted rather
        // than appended at the end of the list.
        if (existing != null) {
            reorder(obj, ln, existing);
        }
    }

    void add(NodeEntry entry, NodeEntry beforeEntry) {
        if (beforeEntry != null) {
            // the link node where the new entry is ordered before
            LinkedEntries.LinkNode beforeLN = getLinkNode(beforeEntry);
            if (beforeLN == null) {
                throw new NoSuchElementException();
            }
            add(entry);
            Object insertObj = nameMap.get(entry.getQName());
            LinkedEntries.LinkNode insertLN = getLinkNode(entry);
            reorder(insertObj, insertLN, beforeLN);
        } else {
            // 'before' is null -> simply append new entry at the end
            add(entry);
        }
    }

    /**
     * Removes the child node entry with the given <code>nodeName</code> and
     * <code>index</code>.
     *
     * @param nodeName the name of the child node entry to remove.
     * @param index    the index of the child node entry to remove.
     * @return the removed <code>NodeEntry</code> or <code>null</code>
     *         if there is no matching <code>NodeEntry</code>.
     */
    NodeEntry remove(QName nodeName, int index) {
        if (index < Path.INDEX_DEFAULT) {
            throw new IllegalArgumentException("index is 1-based");
        }

        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }

        if (obj instanceof LinkedEntries.LinkNode) {
            // map entry is a single child node entry
            if (index != Path.INDEX_DEFAULT) {
                return null;
            }
            LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) obj;
            nameMap.remove(nodeName);
            // remove LinkNode from entries
            ln.remove();
            return ln.getNodeEntry();
        }

        // map entry is a list of siblings
        List siblings = (List) obj;
        if (index > siblings.size()) {
            return null;
        }

        // remove from siblings list
        LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) siblings.remove(index - 1);
        NodeEntry removedEntry = ln.getNodeEntry();
        // remove from ordered entries
        ln.remove();

        // clean up name lookup map if necessary
        if (siblings.size() == 0) {
            // no more entries with that name left:
            // remove from name lookup map as well
            nameMap.remove(nodeName);
        } else if (siblings.size() == 1) {
            // just one entry with that name left:
            // discard siblings list and update name lookup map accordingly
            nameMap.put(nodeName, siblings.get(0));
        }

        // we're done
        return removedEntry;
    }

    /**
     * Removes the child node entry refering to the node state.
     *
     * @param childEntry the entry to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     */
    NodeEntry remove(NodeEntry childEntry) {
        List l = get(childEntry.getQName());
        for (int i = 0; i < l.size(); i++) {
            NodeEntry tmp = (NodeEntry) l.get(i);
            if (tmp == childEntry) {
                int index = i+1; // index is 1-based
                return remove(childEntry.getQName(), index);
            }
        }
        return null;
    }

    /**
     * Reorders an existing <code>NodeState</code> before another
     * <code>NodeState</code>. If <code>beforeNode</code> is
     * <code>null</code> <code>insertNode</code> is moved to the end of the
     * child node entries.
     *
     * @param insertNode the NodeEntry to move.
     * @param beforeNode the NodeEntry where <code>insertNode</code> is
     * reordered to.
     * @return the NodeEntry that followed the 'insertNode' before the reordering.
     * @throws NoSuchElementException if <code>insertNode</code> or
     * <code>beforeNode</code> does not have a <code>NodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     */
    NodeEntry reorder(NodeEntry insertNode, NodeEntry beforeNode) {
        Object insertObj = nameMap.get(insertNode.getQName());
        // the link node to move
        LinkedEntries.LinkNode insertLN = getLinkNode(insertNode);
        if (insertLN == null) {
            throw new NoSuchElementException();
        }
        // the link node where insertLN is ordered before
        LinkedEntries.LinkNode beforeLN = (beforeNode != null) ? getLinkNode(beforeNode) : null;
        if (beforeNode != null && beforeLN == null) {
            throw new NoSuchElementException();
        }

        NodeEntry previousBefore = insertLN.getNextLinkNode().getNodeEntry();
        if (previousBefore != beforeNode) {
            reorder(insertObj, insertLN, beforeLN);
        }
        return previousBefore;
    }

    /**
     *
     * @param insertObj
     * @param insertLN
     * @param beforeLN
     */
    private void reorder(Object insertObj, LinkedEntries.LinkNode insertLN, LinkedEntries.LinkNode beforeLN) {
        if (insertObj instanceof List) {
            // adapt name lookup lists
            List insertList = (List) insertObj;
            if (beforeLN == null) {
                // simply move to end of list
                insertList.remove(insertLN);
                insertList.add(insertLN);
            } else {
                // move based on position of beforeLN
                // count our same name siblings until we reach beforeLN
                int snsCount = 0;
                QName insertName = insertLN.getNodeEntry().getQName();
                for (Iterator it = entries.linkNodeIterator(); it.hasNext(); ) {
                    LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                    if (ln == beforeLN) {
                        insertList.remove(insertLN);
                        insertList.add(snsCount, insertLN);
                        break;
                    } else if (ln == insertLN) {
                        // do not increment snsCount for node to reorder
                    } else if (ln.getNodeEntry().getQName().equals(insertName)) {
                        snsCount++;
                    }
                }
            }
        } // else: no same name siblings -> no special handling required

        // reorder in linked list
        entries.reorderNode(insertLN, beforeLN);
    }

    /**
     * Returns the matching <code>LinkNode</code> from a list or a single
     * <code>LinkNode</code>. This method will return <code>null</code>
     * if none of the entries matches either due to missing entry for given
     * state name or due to missing availability of the <code>NodeEntry</code>.
     *
     * @param nodeEntry the <code>NodeEntry</code> that is compared to the
     * resolution of any <code>NodeEntry</code> that matches by name.
     * @return the matching <code>LinkNode</code> or <code>null</code>
     */
    private LinkedEntries.LinkNode getLinkNode(NodeEntry nodeEntry) {
        Object listOrLinkNode = nameMap.get(nodeEntry.getQName());
        if (listOrLinkNode == null) {
            // no matching child node entry
            return null;
        }

        if (listOrLinkNode instanceof List) {
            // has same name sibling
            for (Iterator it = ((List) listOrLinkNode).iterator(); it.hasNext();) {
                LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                NodeEntry cne = n.getNodeEntry();
                if (cne == nodeEntry) {
                    return n;
                }
            }
        } else {
            // single child node with this name
            NodeEntry cne = ((LinkedEntries.LinkNode) listOrLinkNode).getNodeEntry();
            if (cne == nodeEntry) {
                return (LinkedEntries.LinkNode) listOrLinkNode;
            }
        }
        // not found
        return null;
    }

    /**
     * Returns the matching <code>LinkNode</code> from a list or a single
     * <code>LinkNode</code>. This method will return <code>null</code>
     * if none of the entries matches.
     *
     * @param name
     * @param index
     * @return the matching <code>LinkNode</code> or <code>null</code>.
     */
    private LinkedEntries.LinkNode getLinkNode(QName name, int index) {
        Object listOrLinkNode = nameMap.get(name);
        if (listOrLinkNode == null) {
            // no matching child node entry
            return null;
        }

        if (listOrLinkNode instanceof List) {
            // has same name sibling -> check if list size matches
            int listIndex = index - 1;
            List lnList = (List) listOrLinkNode;
            if (listIndex < lnList.size()) {
                return (LinkedEntries.LinkNode) lnList.get(listIndex);
            }
        } else if (index == Path.INDEX_DEFAULT) {
            // single child node with this name -> matches is requested index
            // equals to the default index.
            return (LinkedEntries.LinkNode) listOrLinkNode;
        }

        // no matching entry
        return null;
    }
    //--------------------------------------------< unmodifiable Collection >---
    /**
     * @see Collection#contains(Object)
     */
    public boolean contains(Object o) {
        if (o instanceof NodeEntry) {
            // narrow down to same name sibling nodes and check list
            return get(((NodeEntry) o).getQName()).contains(o);
        } else {
            return false;
        }
    }

    /**
     * @see Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection c) {
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            if (!contains(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see Collection#isEmpty()
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see Collection#iterator()
     */
    public Iterator iterator() {
        return UnmodifiableIterator.decorate(entries.iterator());
    }

    /**
     * @see Collection#size()
     */
    public int size() {
        return entries.size();
    }

    /**
     * @see Collection#toArray()
     */
    public Object[] toArray() {
        NodeEntry[] array = new NodeEntry[size()];
        return toArray(array);
    }

    /**
     * @see Collection#toArray(Object[])
     */
    public Object[] toArray(Object[] a) {
        if (!a.getClass().getComponentType().isAssignableFrom(NodeEntry.class)) {
            throw new ArrayStoreException();
        }
        if (a.length < size()) {
            a = new NodeEntry[size()];
        }
        Iterator iter = entries.iterator();
        int i = 0;
        while (iter.hasNext()) {
            a[i++] = iter.next();
        }
        while (i < a.length) {
            a[i++] = null;
        }
        return a;
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#add(Object)
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#addAll(Collection)
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#remove(Object)
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------< AbstractLinkedList >---
    /**
     * An implementation of a linked list which provides access to the internal
     * LinkNode which links the entries of the list.
     */
    private static final class LinkedEntries extends AbstractLinkedList {

        LinkedEntries() {
            super();
            init();
        }

        /**
         * Adds a child node entry to this list.
         *
         * @param cne the child node entry to add.
         * @return the LinkNode which refers to the added <code>NodeEntry</code>.
         */
        LinkedEntries.LinkNode add(NodeEntry cne) {
            LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) createNode(cne);
            addNode(ln, header);
            return ln;
        }

        /**
         * Reorders an existing <code>LinkNode</code> before another existing
         * <code>LinkNode</code>. If <code>before</code> is <code>null</code>
         * the <code>insert</code> node is moved to the end of the list.
         *
         * @param insert the node to reorder.
         * @param before the node where to reorder node <code>insert</code>.
         */
        void reorderNode(LinkedEntries.LinkNode insert, LinkedEntries.LinkNode before) {
            removeNode(insert);
            if (before == null) {
                addNode(insert, header);
            } else {
                addNode(insert, before);
            }
        }

        /**
         * Replace the value of the given LinkNode with a new NodeEntry
         * value.
         *
         * @param node
         * @param value
         */
        void replaceNode(LinkedEntries.LinkNode node, NodeEntry value) {
            updateNode(node, value);
        }

        /**
         * Create a new <code>LinkNode</code> for a given {@link NodeEntry}
         * <code>value</code>.
         *
         * @param value a child node entry.
         * @return a wrapping {@link org.apache.jackrabbit.jcr2spi.hierarchy.ChildNodeEntries.LinkedEntries.LinkNode}.
         * @see AbstractLinkedList#createNode(Object)
         */
        protected Node createNode(Object value) {
            return new LinkedEntries.LinkNode(value);
        }

        /**
         * @return a new <code>LinkNode</code>.
         * @see AbstractLinkedList#createHeaderNode()
         */
        protected Node createHeaderNode() {
            return new LinkedEntries.LinkNode();
        }

        /**
         * Returns an iterator over all
         * @return
         */
        Iterator linkNodeIterator() {
            return new Iterator() {

                private LinkedEntries.LinkNode next = ((LinkedEntries.LinkNode) header).getNextLinkNode();

                private int expectedModCount = modCount;

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                public boolean hasNext() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    return next != header;
                }

                public Object next() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    LinkedEntries.LinkNode n = next;
                    next = next.getNextLinkNode();
                    return n;
                }
            };
        }

        //----------------------------------------------------------------------

        /**
         * Extends the <code>AbstractLinkedList.Node</code>.
         */
        private final class LinkNode extends Node {

            protected LinkNode() {
                super();
            }

            protected LinkNode(Object value) {
                super(value);
            }

            /**
             * @return the wrapped <code>NodeEntry</code>.
             */
            public NodeEntry getNodeEntry() {
                return (NodeEntry) super.getValue();
            }

            /**
             * Removes this <code>LinkNode</code> from the linked list.
             */
            public void remove() {
                removeNode(this);
            }

            /**
             * @return the next LinkNode.
             */
            public LinkedEntries.LinkNode getNextLinkNode() {
                return (LinkedEntries.LinkNode) super.getNextNode();
            }
        }
    }
}
