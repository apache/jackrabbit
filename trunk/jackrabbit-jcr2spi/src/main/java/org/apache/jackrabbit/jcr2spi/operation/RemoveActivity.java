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

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveActivity</code>...
 */
public class RemoveActivity extends AbstractRemove {

    private static Logger log = LoggerFactory.getLogger(RemoveActivity.class);

    private final Iterator<PropertyId> refs;
    private final HierarchyManager hMgr;

    private RemoveActivity(NodeState removeActivity, HierarchyManager hierarchyMgr)
            throws RepositoryException {
        super(removeActivity, removeActivity.getParent());
        refs = removeActivity.getNodeReferences(null, false);
        hMgr = hierarchyMgr;
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see org.apache.jackrabbit.jcr2spi.operation.Operation#accept(org.apache.jackrabbit.jcr2spi.operation.OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been updated and all
     * its descendants. Second, the parent state gets invalidated.
     *
     * @see org.apache.jackrabbit.jcr2spi.operation.Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;

        // invalidate all references to the removed activity
        while (refs.hasNext()) {
            HierarchyEntry entry = hMgr.lookup(refs.next());
            if (entry != null) {
                entry.invalidate(false);
            }
        }

        // invalidate the activities parent
        parent.getNodeEntry().invalidate(false);
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(NodeState activityState, HierarchyManager hierarchyMgr)
            throws RepositoryException {
        RemoveActivity rm = new RemoveActivity(activityState, hierarchyMgr);
        return rm;
    }
}