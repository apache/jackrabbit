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
package org.apache.jackrabbit.jcr2spi.util;

import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple helper class that can be used to keep track of uuid mappings
 * (e.g. if the uuid of an imported or copied node is mapped to a new uuid)
 * and processed (e.g. imported or copied) reference properties that might
 * need correcting depending on the uuid mappings.
 */
public class ReferenceChangeTracker {

    private static Logger log = LoggerFactory.getLogger(ReferenceChangeTracker.class);

    /**
     * mapping <original uuid> to <new uuid> of mix:referenceable nodes
     */
    private final Map<String, String> uuidMap = new HashMap<String, String>();
    /**
     * list of processed reference properties that might need correction
     */
    private final List<PropertyState> references = new ArrayList<PropertyState>();

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
     * <code>#adjustReferences(UpdatableItemStateManager, ItemStateValidator)</code>.
     *
     * @param oldUUID
     * @param newUUID
     */
    public void mappedUUIDs(String oldUUID, String newUUID) {
        if (oldUUID == null || oldUUID.equals(newUUID)) {
            // only remember if uuid exists and has changed
            return;
        }
        uuidMap.put(oldUUID, newUUID);
    }

    /**
     * Returns the new UUID to which <code>oldUUID</code> has been mapped
     * or <code>null</code> if no such mapping exists.
     *
     * @param oldReference old uuid represented by the given <code>QValue</code>.
     * @param factory
     * @return mapped new QValue of the reference value or <code>null</code> if no such mapping exists
     * @see #mappedUUIDs(String,String)
     */
    public QValue getMappedReference(QValue oldReference, QValueFactory factory) {
        QValue remapped = null;
        if (oldReference.getType() == PropertyType.REFERENCE) {
            try {
                String oldValue = oldReference.getString();
                if (uuidMap.containsKey(oldValue)) {
                    String newValue = uuidMap.get(oldValue);
                    remapped = factory.create(newValue, PropertyType.REFERENCE);
                }
            } catch (RepositoryException e) {
                log.error("Unexpected error while creating internal value.", e);
            }
        }
        return remapped;
    }

    /**
     * Store the given reference property for later resolution.
     *
     * @param refPropertyState reference property state
     */
    public void processedReference(PropertyState refPropertyState) {
        // make sure only not-null states of type Reference are remembered
        if (refPropertyState != null && refPropertyState.getType() == PropertyType.REFERENCE) {
            references.add(refPropertyState);
        }
    }

    public Iterator<PropertyState> getReferences() {
        return references.iterator();
    }
}
