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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.list.AbstractLinkedList;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * An implementation of a linked list which provides access to the internal
 * LinkNode which links the entries of the list.
 */
@SuppressWarnings("rawtypes")
class LinkedEntries extends AbstractLinkedList {

    private Node<?> header;
    private volatile int modCount;

    private final EntryFactory factory;
    private final NodeEntry parent;

    LinkedEntries(EntryFactory factory, NodeEntry parent) {
        super();
        this.factory = factory;
        this.parent = parent;
        init();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addNode(Node nodeToInsert, Node insertBeforeNode) {
        super.addNode(nodeToInsert, insertBeforeNode);
        modCount++;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void removeNode(Node node) {
        super.removeNode(node);
        modCount++;
    }

    @Override
    protected void removeAllNodes() {
        super.removeAllNodes();
        modCount++;
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
    protected LinkedEntries.LinkNode getLinkNode(NodeEntry nodeEntry) {
        for (Iterator<LinkedEntries.LinkNode> it = linkNodeIterator(); it.hasNext();) {
            LinkedEntries.LinkNode ln = it.next();
            if (ln.getNodeEntry() == nodeEntry) {
                return ln;
            }
        }
        // not found
        return null;
    }

    protected LinkedEntries.LinkNode getHeader() {
        return (LinkedEntries.LinkNode) header;
    }

    /**
     * Adds a child node entry at the end of this list.
     *
     * @param cne the child node entry to add.
     * @param index
     * @return the LinkNode which refers to the added <code>NodeEntry</code>.
     */
    LinkedEntries.LinkNode add(NodeEntry cne, int index) {
        LinkedEntries.LinkNode ln = new LinkedEntries.LinkNode(cne, index);
        addNode(ln, getHeader());
        return ln;
    }

    /**
     * Adds the given child node entry to this list after the specified
     * <code>entry</code> or at the beginning if <code>entry</code> is
     * <code>null</code>.
     *
     * @param cne the child node entry to add.
     * @param index
     * @param insertAfter after which to insert the new entry
     * @return the LinkNode which refers to the added <code>NodeEntry</code>.
     */
    LinkedEntries.LinkNode addAfter(NodeEntry cne, int index, LinkedEntries.LinkNode insertAfter) {
        LinkedEntries.LinkNode newNode;
        if (insertAfter == null) {
            // insert at the beginning
            newNode = new LinkedEntries.LinkNode(cne, index);
            addNode(newNode, getHeader());
        } else if (insertAfter.getNextLinkNode() == null) {
            newNode = add(cne, index);
        } else {
            newNode = new LinkedEntries.LinkNode(cne, index);
            addNode(newNode, insertAfter.getNextLinkNode());
        }
        return newNode;
    }

    /**
     * Remove the LinkEntry the contains the given NodeEntry as value.
     *
     * @param cne NodeEntry to be removed.
     * @return LinkedEntries.LinkNode that has been removed.
     */
    LinkedEntries.LinkNode removeNodeEntry(NodeEntry cne) {
        LinkedEntries.LinkNode ln = getLinkNode(cne);
        if (ln != null) {
            ln.remove();
        }
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
            addNode(insert, getHeader());
        } else {
            addNode(insert, before);
        }
    }

    /**
     * Create a new <code>LinkNode</code> for a given {@link NodeEntry}
     * <code>value</code>.
     *
     * @param value a child node entry.
     * @return a wrapping {@link LinkedEntries.LinkNode}.
     * @see AbstractLinkedList#createNode(Object)
     */
    @Override
    protected Node createNode(Object value) {
        return new LinkedEntries.LinkNode(value, Path.INDEX_DEFAULT);
    }

    /**
     * @return a new <code>LinkNode</code>.
     * @see AbstractLinkedList#createHeaderNode()
     */
    @Override
    protected Node createHeaderNode() {
        header = new LinkedEntries.LinkNode();
        return header;
    }

    /**
     * @return iterator over all LinkNode entries in this list.
     */
    protected Iterator<LinkedEntries.LinkNode> linkNodeIterator() {
        return new LinkNodeIterator();
    }

    //----------------------------------------------------------------------
    /**
     * Extends the <code>AbstractLinkedList.Node</code>.
     */
    protected final class LinkNode extends Node {

        protected final Name qName;

        protected LinkNode() {
            super();
            qName = null;
        }

        @SuppressWarnings("unchecked")
        protected LinkNode(Object value, int index) {
            // add soft reference from linkNode to the NodeEntry (value)
            // unless the entry is a SNSibling. TODO: review again.
            super(index > Path.INDEX_DEFAULT ? value : new SoftReference<Object>(value));
            qName = ((NodeEntry) value).getName();
        }

        @Override
        protected void setValue(Object value) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object getValue() {
            Object val = super.getValue();
            NodeEntry ne;
            if (val == null) {
                ne = null;
            } else if (val instanceof Reference) {
                ne = (NodeEntry) ((Reference) val).get();
            } else {
                ne = (NodeEntry) val;
            }
            // if the nodeEntry has been g-collected in the mean time
            // create a new NodeEntry in order to avoid returning null.
            if (ne == null && this != getHeader()) {
                ne = factory.createNodeEntry(parent, qName, null);
                super.setValue(new SoftReference<NodeEntry>(ne));
            }
            return ne;
        }

        /**
         * @return the wrapped <code>NodeEntry</code>.
         */
        public NodeEntry getNodeEntry() {
            return (NodeEntry) getValue();
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

        /**
         * @return the next LinkNode.
         */
        public LinkedEntries.LinkNode getPreviousLinkNode() {
            return (LinkedEntries.LinkNode) super.getPreviousNode();
        }
    }

    //----------------------------------------------------------------------
    private class LinkNodeIterator implements Iterator<LinkedEntries.LinkNode> {

        private LinkedEntries.LinkNode next = getHeader().getNextLinkNode();
        private final int expectedModCount = modCount;

        public boolean hasNext() {
            checkModCount();
            return next != header;
        }

        public LinkedEntries.LinkNode next() {
            checkModCount();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            LinkedEntries.LinkNode n = next;
            next = next.getNextLinkNode();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void checkModCount() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
