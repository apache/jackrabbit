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

import org.apache.jackrabbit.jcr2spi.state.TransientItemStateFactory;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>EntryFactory</code>...
 */
public class EntryFactory {

    /**
     * IdFactory to create an ItemId based on the parent NodeId.
     */
    private final IdFactory idFactory;

    private final PathFactory pathFactory;

    private final NodeEntry rootEntry;

    /**
     * Listener to creation and uid-changes of node entries.
     */
    private final NodeEntryListener listener;

    /**
     * The item state factory to create the the item state.
     */
    private final TransientItemStateFactory isf;

    /**
     * NamePathResolver used to generate human readable error messages.
     */
    private NamePathResolver resolver;

    /**
     * Strategy used for item state invalidation (refresh)
     */
    private final InvalidationStrategy invalidationStrategy;

    /**
     * Create a new instance of the <code>EntryFactory</code>.
     *
     * @param isf
     * @param idFactory
     * @param listener
     * @param pathFactory
     */
    public EntryFactory(TransientItemStateFactory isf, IdFactory idFactory,
                        NodeEntryListener listener, PathFactory pathFactory) {
        this.idFactory = idFactory;
        this.pathFactory = pathFactory;
        this.isf = isf;
        this.listener = listener;

        // todo: make this configurable if necessary
        // this.invalidationStrategy = new NodeEntryImpl.EagerInvalidation();
        this.invalidationStrategy = new NodeEntryImpl.LazyInvalidation();
        this.rootEntry = NodeEntryImpl.createRootEntry(this);
    }

    /**
     * @return the root entry.
     */
    public NodeEntry createRootEntry() {
        return rootEntry;
    }

    public NodeEntry createNodeEntry(NodeEntry parent, Name qName, String uniqueId) {
        if (!(parent instanceof NodeEntryImpl)) {
            throw new IllegalArgumentException();
        }
        return NodeEntryImpl.createNodeEntry((NodeEntryImpl) parent, qName, uniqueId, this);
    }

    public PropertyEntry createPropertyEntry(NodeEntry parent, Name qName) {
        if (!(parent instanceof NodeEntryImpl)) {
            throw new IllegalArgumentException();
        }
        return PropertyEntryImpl.create((NodeEntryImpl) parent, qName, this);
    }

    public IdFactory getIdFactory() {
        return idFactory;
    }

    public PathFactory getPathFactory() {
        return pathFactory;
    }

    public TransientItemStateFactory getItemStateFactory() {
        return isf;
    }

    public void notifyEntryCreated(NodeEntry entry) {
        listener.entryCreated(entry);
    }

    public void notifyIdChange(NodeEntry entry, String previousUniqueID) {
        listener.uniqueIdChanged(entry, previousUniqueID);
    }

    /**
     * @return  the strategy used for item state invalidation (refresh)
     */
    public InvalidationStrategy getInvalidationStrategy() {
        return invalidationStrategy;
    }

    //--------------------------------------------------------------------------
    /**
     * @param resolver
     */
    void setResolver(NamePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * @param path
     * @return jcr presentation of the specified path.
     */
    String saveGetJCRPath(Path path) {
        if (resolver == null) {
            return path.toString();
        } else {
            return LogUtil.safeGetJCRPath(path, resolver);
        }
    }

    //--------------------------------------------------< NodeEntryListener >---
    public interface NodeEntryListener {

        public void entryCreated(NodeEntry entry);

        public void uniqueIdChanged (NodeEntry entry, String previousUniqueID);
    }

    // ----------------------------------------------< InvalidationStrategy >---
    /**
     * Strategy for invalidating item states
     */
    public interface InvalidationStrategy {

        /**
         * Invalidate underlying {@link org.apache.jackrabbit.jcr2spi.state.ItemState} of this
         * <code>entry</code>. Implementors may choose to delay the actual call to
         * {@link org.apache.jackrabbit.jcr2spi.state.ItemState#invalidate()} for this
         * <code>entry</code> and for any of its child entries. They need to ensure however that
         * {@link #applyPending(HierarchyEntry)} properly invalidates the respective state when called.
         *
         * @param entry The <code>HierarchyEntry</code> to invalidate.
         * @param recursive Invalidate state of child entries if <code>true</code>.
         */
        public void invalidate(HierarchyEntry entry, boolean recursive);

        /**
         * Apply any pending {@link org.apache.jackrabbit.jcr2spi.state.ItemState#invalidate()
         * invalidation} of the underlying {@link org.apache.jackrabbit.jcr2spi.state.ItemState} of
         * this <code>entry</code>.
         *
         * @param entry The affected <code>NodeEntry</code>.
         */
        public void applyPending(HierarchyEntry entry);
    }
}
