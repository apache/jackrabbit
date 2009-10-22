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
package org.apache.jackrabbit.core.util;

import org.apache.jackrabbit.core.id.NodeId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * Simple helper class that can be used to keep track of node id mappings
 * (e.g. if the id of an imported or copied node is mapped to a new id)
 * and processed (e.g. imported or copied) reference properties that might
 * need correcting depending on the id mappings.
 */
public class ReferenceChangeTracker {

    /**
     * mapping from original id to new id of mix:referenceable nodes
     */
    private final Map<NodeId, NodeId> idMap = new HashMap<NodeId, NodeId>();

    /**
     * list of processed reference properties that might need correcting
     */
    private final ArrayList<Object> references = new ArrayList<Object>();

    /**
     * Resets all internal state.
     */
    public void clear() {
        idMap.clear();
        references.clear();
    }

    /**
     * Store the given id mapping for later lookup using
     * <code>{@link #getMappedId(NodeId)}</code>.
     *
     * @param oldId old node id
     * @param newId new node id
     */
    public void mappedId(NodeId oldId, NodeId newId) {
        idMap.put(oldId, newId);
    }

    /**
     * Store the given reference property for later retrieval using
     * <code>{@link #getProcessedReferences()}</code>.
     *
     * @param refProp reference property
     */
    public void processedReference(Object refProp) {
        references.add(refProp);
    }

    /**
     * Returns the new node id to which <code>oldId</code> has been mapped
     * or <code>null</code> if no such mapping exists.
     *
     * @param oldId old node id
     * @return mapped new id or <code>null</code> if no such mapping exists
     * @see #mappedId(NodeId, NodeId)
     */
    public NodeId getMappedId(NodeId oldId) {
        return idMap.get(oldId);
    }

    /**
     * Returns an iterator over all processed reference properties.
     *
     * @return an iterator over all processed reference properties
     * @see #processedReference(Object)
     */
    public Iterator<Object> getProcessedReferences() {
        return references.iterator();
    }

    /**
     * Remove the given references that have already been processed from the
     * references list.
     * 
     * @param processedReferences
     * @return <code>true</code> if the internal list of references changed.
     */
    public boolean removeReferences(List<Object> processedReferences) {
        return references.removeAll(processedReferences);
    }
}
