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

import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.version.VersionException;

/**
 * <code>OperationVisitor</code>...
 */
public interface OperationVisitor {

    public void visit(AddNode operation) throws RepositoryException;

    public void visit(AddProperty operation) throws RepositoryException;

    public void visit(Remove operation) throws RepositoryException;

    public void visit(SetMixin operation) throws RepositoryException;

    /**
     * @since JCR 2.0
     */
    public void visit(SetPrimaryType operation) throws RepositoryException;

    public void visit(SetPropertyValue operation) throws RepositoryException;

    public void visit(ReorderNodes operation) throws RepositoryException;

    public void visit(SetTree operation) throws RepositoryException;

    public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException;

    public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException;

    public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException;

    public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException;

    public void visit(Checkout operation) throws RepositoryException, UnsupportedRepositoryOperationException;

    public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @since JCR 2.0
     */
    public void visit(Checkpoint operation) throws RepositoryException;

    public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException;

    public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException;

    public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException;

    public void visit(AddLabel operation) throws VersionException, RepositoryException;

    public void visit(RemoveLabel operation) throws VersionException, RepositoryException;

    public void visit(RemoveVersion operation) throws VersionException, AccessDeniedException, ReferentialIntegrityException, RepositoryException;

    public void visit(WorkspaceImport operation) throws RepositoryException;

    /**
     * @since JCR 2.0
     */
    public void visit(CreateActivity operation) throws RepositoryException;

    /**
     * @since JCR 2.0
     */
    public void visit(RemoveActivity operation) throws RepositoryException;

    /**
     * @since JCR 2.0
     */
    public void visit(CreateConfiguration operation) throws RepositoryException;
}
