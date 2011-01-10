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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.ReferentialIntegrityException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized SharedItemStateManager that filters out NodeReferences to
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
                                   ISMLocking locking,
                                   NodeIdFactory nodeIdFactory)
            throws ItemStateException {
        super(persistMgr, rootNodeId, ntReg, false, cacheFactory, locking, nodeIdFactory);
        this.pMgr = persistMgr;
    }

    @Override
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        // check persistence manager
        try {
            return pMgr.loadReferencesTo(id);
        } catch (NoSuchItemStateException e) {
            // ignore
        }
        // throw
        throw new NoSuchItemStateException(id.toString());
    }

    @Override
    public boolean hasNodeReferences(NodeId id) {
        // check persistence manager
        try {
            if (pMgr.existsReferencesTo(id)) {
                return true;
            }
        } catch (ItemStateException e) {
            // ignore
        }
        return false;
    }

    /**
     * Sets the
     * @param references
     * @return
     */
    public boolean setNodeReferences(ChangeLog references) {
        try {
            ChangeLog log = new ChangeLog();

            for (NodeReferences source : references.modifiedRefs()) {
                // filter out version storage intern ones
                NodeReferences target = new NodeReferences(source.getTargetId());
                for (PropertyId id : source.getReferences()) {
                    if (!hasNonVirtualItemState(id.getParentId())) {
                        target.addReference(id);
                    }
                }
                log.modified(target);
            }

            if (log.hasUpdates()) {
                pMgr.store(log);
            }
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
        Set<NodeId> remove = new HashSet<NodeId>();
        for (NodeReferences refs : changes.modifiedRefs()) {
            // no need to check existence of target if there are no references
            if (refs.hasReferences()) {
                NodeId id = refs.getTargetId();
                if (!changes.has(id) && !hasNonVirtualItemState(id)) {
                    remove.add(refs.getTargetId());
                }
            }
        }
        // remove references
        for (NodeId id : remove) {
            changes.removeReferencesEntry(id);
        }
    }
}
