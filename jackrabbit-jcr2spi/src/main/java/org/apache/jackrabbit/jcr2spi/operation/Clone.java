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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Clone</code>...
 */
public class Clone extends AbstractCopy {

    private static Logger log = LoggerFactory.getLogger(Clone.class);

    private final boolean removeExisting;

    private Clone(Path srcPath, Path destPath, String srcWorkspaceName,
                  boolean removeExisting, ManagerProvider srcMgrProvider,
                  ManagerProvider destMgrProvider)
        throws RepositoryException {
        super(srcPath, destPath, srcWorkspaceName, srcMgrProvider, destMgrProvider);

        this.removeExisting = removeExisting;
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    @Override
    public void persisted() {
        assert status == STATUS_PENDING;
        if (removeExisting) {
            status = STATUS_PERSISTED;
            // invalidate the complete tree -> find root-hierarchy-entry
            HierarchyEntry he = destParentState.getHierarchyEntry();
            while (he.getParent() != null) {
                he = he.getParent();
            }
            he.invalidate(true);
        } else {
            super.persisted();
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public boolean isRemoveExisting() {
        return removeExisting;
    }

    //------------------------------------------------------------< Factory >---

    public static Operation create(Path srcPath, Path destPath,
                                   String srcWorkspaceName, boolean removeExisting,
                                   ManagerProvider srcMgrProvider,
                                   ManagerProvider destMgrProvider)
        throws RepositoryException, ConstraintViolationException, AccessDeniedException,
        ItemExistsException, VersionException {

        Clone cl = new Clone(srcPath, destPath, srcWorkspaceName, removeExisting, srcMgrProvider, destMgrProvider);
        return cl;
    }
}