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
package org.apache.jackrabbit.jcr2spi.operation;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveVersion</code>...
 */
public class RemoveVersion extends AbstractRemove {

    private static Logger log = LoggerFactory.getLogger(RemoveVersion.class);

    private NodeEntry versionableEntry = null;

    private RemoveVersion(ItemState removeState, NodeState parent, VersionManager mgr)
            throws RepositoryException {
        super(removeState, parent);
        try {
            versionableEntry = mgr.getVersionableNodeEntry((NodeState) removeState);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve the hierarchy entry of the versionable node.", e);
        }
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been updated and all
     * its descendants. Second, the parent state gets invalidated.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        // Invalidate the versionable node as well (version related properties)
        if (versionableEntry != null) {
            Iterator<PropertyEntry> propEntries = versionableEntry.getPropertyEntries();
            while (propEntries.hasNext()) {
                PropertyEntry pe = propEntries.next();
                pe.invalidate(false);
            }
            versionableEntry.invalidate(false);
        }

        // invalidate the versionhistory entry and all its children
        // in order to have the v-graph recalculated
        parent.getNodeEntry().invalidate(true);
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(NodeState versionState, NodeState vhState, VersionManager mgr)
            throws RepositoryException {
        RemoveVersion rm = new RemoveVersion(versionState, vhState, mgr);
        return rm;
    }
}
