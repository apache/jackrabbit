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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ReferentialIntegrityException;
import java.util.Iterator;

/**
 * Spezialized SharedItemStateManager that filters out NodeReferences to
 * non-versioning states.
 */
public class VersionItemStateManager extends SharedItemStateManager {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(VersionItemStateManager.class);

    /**
     * The persistence manager.
     */
    private final PersistenceManager pMgr;

    public VersionItemStateManager(PersistenceManager persistMgr,
                                   NodeId rootNodeId,
                                   NodeTypeRegistry ntReg,
                                   ItemStateCacheFactory cacheFactory,
                                   ISMLocking locking)
            throws ItemStateException {
        super(persistMgr, rootNodeId, ntReg, false, cacheFactory, locking);
        this.pMgr = persistMgr;
    }

    /**
     * Sets the
     * @param references
     * @return
     */
    public boolean setNodeReferences(NodeReferences references) {
        try {
            // filter out version storage intern ones
            NodeReferences refs = new NodeReferences(references.getId());
            Iterator iter = references.getReferences().iterator();
            while (iter.hasNext()) {
                PropertyId id = (PropertyId) iter.next();
                if (!hasItemState(id.getParentId())) {
                    refs.addReference(id);
                }
            }

            ChangeLog log = new ChangeLog();
            log.modified(refs);
            pMgr.store(log);
            return true;
        } catch (ItemStateException e) {
            log.error("Error while setting references: " + e.toString());
            return false;
        }
    }

    protected void checkReferentialIntegrity(ChangeLog changes)
            throws ReferentialIntegrityException, ItemStateException {
        // only store VV-type references and NV-type references

        // check whether targets of modified node references exist
        for (Iterator iter = changes.modifiedRefs(); iter.hasNext();) {
            NodeReferences refs = (NodeReferences) iter.next();
            NodeId id = refs.getTargetId();
            // no need to check existence of target if there are no references
            if (refs.hasReferences()) {
                if (!changes.has(id) && !hasItemState(id)) {
                    // remove references
                    iter.remove();
                }
            }
        }
    }
}
