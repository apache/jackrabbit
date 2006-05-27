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

import org.apache.jackrabbit.uuid.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Simple helper class that can be used to keep track of uuid mappings
 * (e.g. if the uuid of an imported or copied node is mapped to a new uuid)
 * and processed (e.g. imported or copied) reference properties that might
 * need correcting depending on the uuid mappings.
 */
public class ReferenceChangeTracker {
    /**
     * mapping <original uuid> to <new uuid> of mix:referenceable nodes
     */
    private final HashMap uuidMap = new HashMap();
    /**
     * list of processed reference properties that might need correcting
     */
    private final ArrayList references = new ArrayList();

    /**
     * Creates a new instance.
     */
    public ReferenceChangeTracker() {
    }

    /**
     * Resets all internal state.
     */
    public void clear() {
        uuidMap.clear();
        references.clear();
    }

    /**
     * Store the given uuid mapping for later lookup using
     * <code>{@link #getMappedUUID(UUID)}</code>.
     *
     * @param oldUUID old uuid
     * @param newUUID new uuid
     */
    public void mappedUUID(UUID oldUUID, UUID newUUID) {
        uuidMap.put(oldUUID, newUUID);
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
     * Returns the new UUID to which <code>oldUUID</code> has been mapped
     * or <code>null</code> if no such mapping exists.
     *
     * @param oldUUID old uuid
     * @return mapped new uuid or <code>null</code> if no such mapping exists
     * @see #mappedUUID(UUID, UUID)
     */
    public UUID getMappedUUID(UUID oldUUID) {
        return (UUID) uuidMap.get(oldUUID);
    }

    /**
     * Returns an iterator over all processed reference properties.
     *
     * @return an iterator over all processed reference properties
     * @see #processedReference(Object)
     */
    public Iterator getProcessedReferences() {
        return references.iterator();
    }
}
