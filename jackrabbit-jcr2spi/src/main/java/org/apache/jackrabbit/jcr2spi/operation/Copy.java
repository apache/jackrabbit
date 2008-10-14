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

import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

/**
 * <code>Copy</code>...
 */
public class Copy extends AbstractCopy  {

    private Copy(Path srcPath, Path destPath, String srcWorkspaceName,
                 ManagerProvider srcMgrProvider, ManagerProvider destMgrProvider) throws RepositoryException {
        super(srcPath, destPath, srcWorkspaceName, srcMgrProvider, destMgrProvider);
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

    //------------------------------------------------------------< Factory >---
    public static Operation create(Path srcPath, Path destPath,
                                   String srcWorkspaceName,
                                   ManagerProvider srcMgrProvider,
                                   ManagerProvider destMgrProvider)
        throws RepositoryException, ConstraintViolationException, AccessDeniedException,
        ItemExistsException, VersionException {
        Copy cp = new Copy(srcPath, destPath, srcWorkspaceName, srcMgrProvider, destMgrProvider);
        return cp;
    }
}